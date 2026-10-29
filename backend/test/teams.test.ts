import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Teams API (/api/v1/teams)', () => {
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

  it('GET /teams succeeds for COMMANDER role', async () => {
    const res = await app.request(
      '/api/v1/teams',
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
    expect(json.data[0].id).toBe('team_alpha_001');
    expect(json.data[0].member_count).toBe(2);
    expect(json.pagination.total).toBeGreaterThanOrEqual(1);
  });

  it('GET /teams is forbidden (403) for TEAM_LEAD role', async () => {
    const res = await app.request(
      '/api/v1/teams',
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

  it('GET /teams requires authentication (401)', async () => {
    const res = await app.request('/api/v1/teams', {}, env());
    expect(res.status).toBe(401);
  });

  it('GET /teams/:id returns team details and roster members for assigned team lead', async () => {
    const res = await app.request(
      '/api/v1/teams/team_alpha_001',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.id).toBe('team_alpha_001');
    expect(json.data.name).toBe('گشت آلفا (واکنش سریع)');
    expect(json.data.members).toHaveLength(2);
    expect(json.data.members[0].callsign).toBe('البرز ۱');
  });

  it('GET /teams/:id returns 404 for unknown team id', async () => {
    const res = await app.request(
      '/api/v1/teams/team_non_existent',
      {
        headers: { Authorization: `Bearer ${commanderToken}` },
      },
      env()
    );

    expect(res.status).toBe(404);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('NOT_FOUND');
  });
});
