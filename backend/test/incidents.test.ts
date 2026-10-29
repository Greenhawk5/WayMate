import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Incidents API (/api/v1/incidents)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  let teamLeadToken: string;
  let commanderToken: string;

  beforeEach(async () => {
    db = createTestD1Database();

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
  });

  it('POST /quick triggers 1-tap emergency alert with URGENT severity', async () => {
    const res = await app.request(
      '/api/v1/incidents/quick',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          latitude: 35.6892,
          longitude: 51.389,
          preset_reason: 'حمله یا کمین محتمل در مسیر اصلی',
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.incident_id).toMatch(/^inc_/);
    expect(json.data.severity).toBe('URGENT');
    expect(json.data.status).toBe('OPEN');
    expect(json.data.acknowledged).toBe(true);
  });

  it('POST /quick validates coordinates', async () => {
    const res = await app.request(
      '/api/v1/incidents/quick',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          latitude: 190.0,
          longitude: 51.389,
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
  });

  it('POST / creates structured incident report', async () => {
    const res = await app.request(
      '/api/v1/incidents',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: 'انسداد مسیر توسط مانع فیزیکی',
          description: 'مسیر در مختصات مذکور توسط سنگ‌های بزرگ مسدود شده است.',
          incident_type: 'ROAD_HAZARD',
          severity: 'WARNING',
          latitude: 35.695,
          longitude: 51.392,
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.incident_id).toMatch(/^inc_/);
    expect(json.data.status).toBe('OPEN');
  });

  it('POST / validates incident payload fields', async () => {
    const res = await app.request(
      '/api/v1/incidents',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: '', // Missing title
          severity: 'INVALID_SEVERITY',
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
  });

  it('GET / lists incidents with pagination', async () => {
    // Create an incident first
    await app.request(
      '/api/v1/incidents/quick',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({ latitude: 35.689, longitude: 51.389 }),
      },
      env()
    );

    const res = await app.request(
      '/api/v1/incidents',
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
    expect(json.pagination.total).toBeGreaterThanOrEqual(1);
  });
});
