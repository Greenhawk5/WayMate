import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('User Management API (/api/v1/users)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  let ownerToken: string;
  let commanderToken: string;
  let teamLeadToken: string;

  beforeEach(async () => {
    db = createTestD1Database();

    // Login Owner
    const ownerRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'owner_demo', password: 'demo123' }),
      },
      env()
    );
    ownerToken = (await ownerRes.json<any>()).data.token;

    // Login Commander
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

    // Login Team Lead
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

  it('GET /users succeeds for OWNER role', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        headers: { Authorization: `Bearer ${ownerToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(Array.isArray(json.data)).toBe(true);
    expect(json.data.length).toBeGreaterThanOrEqual(3);
    // Ensure password_hash is never leaked
    expect(json.data[0].password_hash).toBeUndefined();
  });

  it('GET /users is forbidden (403) for COMMANDER role', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        headers: { Authorization: `Bearer ${commanderToken}` },
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('GET /users is forbidden (403) for TEAM_LEAD role', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('POST /users creates a new user for OWNER', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${ownerToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: 'patrol_lead_bravo',
          password: 'securePass123',
          role: 'TEAM_LEAD',
          callsign: 'Bravo-Lead (سرتیم براوو)',
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.username).toBe('patrol_lead_bravo');
    expect(json.data.role).toBe('TEAM_LEAD');
    expect(json.data.is_active).toBe(true);
    expect(json.data.password_hash).toBeUndefined();

    // Verify user can now authenticate
    const loginRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'patrol_lead_bravo', password: 'securePass123' }),
      },
      env()
    );
    expect(loginRes.status).toBe(200);
  });

  it('POST /users rejects duplicate username with 409 CONFLICT', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${ownerToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: 'commander_demo', // already exists
          password: 'newPassword123',
          role: 'COMMANDER',
          callsign: 'Duplicate',
        }),
      },
      env()
    );

    expect(res.status).toBe(409);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('CONFLICT');
  });

  it('POST /users is forbidden (403) for COMMANDER', async () => {
    const res = await app.request(
      '/api/v1/users',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${commanderToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: 'unauthorized_create',
          password: 'pass123456',
          role: 'TEAM_LEAD',
          callsign: 'Test',
        }),
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('PUT /users/:id updates user callsign and role for OWNER', async () => {
    const res = await app.request(
      '/api/v1/users/usr_lead_001',
      {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${ownerToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          callsign: 'Falcon-Updated',
          is_active: false,
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.callsign).toBe('Falcon-Updated');
    expect(json.data.is_active).toBe(false);
  });
});
