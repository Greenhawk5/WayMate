import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Telemetry API (/api/v1/telemetry)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  let commanderToken: string;
  let teamLeadToken: string;

  beforeEach(async () => {
    db = createTestD1Database();

    const cmdRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'commander_demo', password: 'demo123' }),
      },
      env()
    );
    commanderToken = (await cmdRes.json<any>()).data.token;

    const leadRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'patrol_lead_demo', password: 'demo123' }),
      },
      env()
    );
    teamLeadToken = (await leadRes.json<any>()).data.token;
  });

  it('POST /location succeeds for TEAM_LEAD with valid coordinates', async () => {
    const res = await app.request(
      '/api/v1/telemetry/location',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          latitude: 35.6892,
          longitude: 51.389,
          accuracy_meters: 6.5,
          speed_mps: 12.0,
          heading_degrees: 180.0,
          operational_status: 'NORMAL',
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.ping_id).toMatch(/^ping_/);
  });

  it('POST /location rejects invalid coordinates with 400 VALIDATION_FAILED', async () => {
    const res = await app.request(
      '/api/v1/telemetry/location',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          latitude: 95.0, // Invalid latitude
          longitude: 51.389,
          accuracy_meters: -5, // Invalid negative accuracy
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
    expect(json.error.details.length).toBeGreaterThanOrEqual(1);
  });

  it('POST /location is forbidden (403) for COMMANDER role', async () => {
    const res = await app.request(
      '/api/v1/telemetry/location',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          latitude: 35.6892,
          longitude: 51.389,
          accuracy_meters: 5.0,
        }),
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('POST /batch ingests multiple store-and-forward buffered pings', async () => {
    const res = await app.request(
      '/api/v1/telemetry/batch',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          pings: [
            { latitude: 35.689, longitude: 51.389, accuracy_meters: 5.0, recorded_at: Date.now() - 60000 },
            { latitude: 35.69, longitude: 51.39, accuracy_meters: 4.8, recorded_at: Date.now() - 30000 },
            { latitude: 35.691, longitude: 51.391, accuracy_meters: 5.2, recorded_at: Date.now() },
          ],
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.inserted_count).toBe(3);
  });

  it('GET /live returns latest ping per active team for COMMANDER', async () => {
    // Ingest a ping first
    await app.request(
      '/api/v1/telemetry/location',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          latitude: 35.7,
          longitude: 51.4,
          accuracy_meters: 8.0,
          speed_mps: 5.5,
          heading_degrees: 90.0,
          operational_status: 'PURSUING',
        }),
      },
      env()
    );

    const res = await app.request(
      '/api/v1/telemetry/live',
      {
        headers: { Authorization: `Bearer ${commanderToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(Array.isArray(json.data)).toBe(true);
    expect(json.data.length).toBeGreaterThanOrEqual(1);
    expect(json.data[0].team_id).toBe('team_alpha_001');
    expect(json.data[0].latitude).toBe(35.7);
    expect(json.data[0].is_stale).toBe(false);
    expect(json.data[0].connection_status).toBe('ONLINE');
  });

  it('GET /live is forbidden (403) for TEAM_LEAD', async () => {
    const res = await app.request(
      '/api/v1/telemetry/live',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(403);
  });

  it('POST /status updates operational status for TEAM_LEAD without GPS coordinates', async () => {
    const res = await app.request(
      '/api/v1/telemetry/status',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          operational_status: 'ENGAGED',
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.team_id).toBe('team_alpha_001');
    expect(json.data.operational_status).toBe('ENGAGED');
    expect(json.data.updated_at).toBeGreaterThan(0);

    // Verify GET /live reflects the updated status and ONLINE connection_status
    const liveRes = await app.request(
      '/api/v1/telemetry/live',
      {
        headers: { Authorization: `Bearer ${commanderToken}` },
      },
      env()
    );
    const liveJson = await liveRes.json<any>();
    const teamAlpha = liveJson.data.find((t: any) => t.team_id === 'team_alpha_001');
    expect(teamAlpha).toBeDefined();
    expect(teamAlpha.operational_status).toBe('ENGAGED');
    expect(teamAlpha.connection_status).toBe('ONLINE');
  });

  it('POST /status rejects invalid status with 400 VALIDATION_FAILED', async () => {
    const res = await app.request(
      '/api/v1/telemetry/status',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          operational_status: 'INVALID_STATUS',
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
  });

  it('POST /status is forbidden (403) for COMMANDER', async () => {
    const res = await app.request(
      '/api/v1/telemetry/status',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          operational_status: 'PURSUING',
        }),
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });
});
