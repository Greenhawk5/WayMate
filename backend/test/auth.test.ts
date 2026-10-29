import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Auth API (/api/v1/auth)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  beforeEach(() => {
    db = createTestD1Database();
  });

  it('POST /login succeeds with valid credentials and returns JWT and user profile', async () => {
    const res = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: 'commander_demo',
          password: 'demo123',
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.token).toBeDefined();
    expect(json.data.user.username).toBe('commander_demo');
    expect(json.data.user.role).toBe('COMMANDER');
    expect(json.data.user.callsign).toBe('Eagle-1 (فرمانده)');
  });

  it('POST /login fails with 401 when password is incorrect', async () => {
    const res = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: 'commander_demo',
          password: 'wrong_password',
        }),
      },
      env()
    );

    expect(res.status).toBe(401);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('UNAUTHORIZED');
  });

  it('POST /login fails with 401 when username does not exist', async () => {
    const res = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: 'non_existent_user',
          password: 'demo123',
        }),
      },
      env()
    );

    expect(res.status).toBe(401);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('UNAUTHORIZED');
  });

  it('POST /login validates required fields', async () => {
    const res = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({}),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
    expect(json.error.details.length).toBeGreaterThan(0);
  });

  it('GET /me returns user profile when valid token is supplied', async () => {
    // 1. Login first
    const loginRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: 'patrol_lead_demo',
          password: 'demo123',
        }),
      },
      env()
    );
    const loginJson = await loginRes.json<any>();
    const token = loginJson.data.token;

    // 2. Fetch /me
    const meRes = await app.request(
      '/api/v1/auth/me',
      {
        headers: { Authorization: `Bearer ${token}` },
      },
      env()
    );

    expect(meRes.status).toBe(200);
    const meJson = await meRes.json<any>();
    expect(meJson.success).toBe(true);
    expect(meJson.data.username).toBe('patrol_lead_demo');
    expect(meJson.data.role).toBe('TEAM_LEAD');
  });

  it('GET /me returns 401 when token is missing or corrupted', async () => {
    const res = await app.request('/api/v1/auth/me', {}, env());
    expect(res.status).toBe(401);

    const badRes = await app.request(
      '/api/v1/auth/me',
      {
        headers: { Authorization: 'Bearer invalid.jwt.token' },
      },
      env()
    );
    expect(badRes.status).toBe(401);
  });

  it('POST /logout returns success with valid token', async () => {
    const loginRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: 'owner_demo',
          password: 'demo123',
        }),
      },
      env()
    );
    const { token } = (await loginRes.json<any>()).data;

    const logoutRes = await app.request(
      '/api/v1/auth/logout',
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
      },
      env()
    );

    expect(logoutRes.status).toBe(200);
    const logoutJson = await logoutRes.json<any>();
    expect(logoutJson.success).toBe(true);
  });
});
