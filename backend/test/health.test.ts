import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Health & Global Routes', () => {
  let db: D1Database;

  beforeEach(() => {
    db = createTestD1Database();
  });

  it('GET /api/v1/health returns 200 with HEALTHY status and CONNECTED database', async () => {
    const res = await app.request('/api/v1/health', {}, { DB: db, ENVIRONMENT: 'test', API_VERSION: 'v1' });
    expect(res.status).toBe(200);

    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.status).toBe('HEALTHY');
    expect(json.data.database).toBe('CONNECTED');
    expect(json.data.version).toBe('v1');
    expect(json.meta.request_id).toBeDefined();
    expect(json.meta.timestamp).toBeDefined();
  });

  it('GET /api/v1/unknown-path returns 404 with structured error envelope', async () => {
    const res = await app.request('/api/v1/unknown-path', {}, { DB: db, ENVIRONMENT: 'test', API_VERSION: 'v1' });
    expect(res.status).toBe(404);

    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('NOT_FOUND');
    expect(json.error.message).toContain('مسیر درخواستی');
    expect(json.error.request_id).toBeDefined();
  });

  it('OPTIONS request returns correct CORS headers', async () => {
    const res = await app.request('/api/v1/health', { method: 'OPTIONS' });
    expect(res.status).toBe(204);
    expect(res.headers.get('access-control-allow-origin')).toBe('*');
    expect(res.headers.get('access-control-allow-methods')).toContain('GET');
  });
});
