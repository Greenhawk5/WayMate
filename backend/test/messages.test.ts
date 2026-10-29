import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Messages API (/api/v1/messages)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  let teamLeadToken: string;
  let commanderToken: string;
  let testMessageId: string;

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

    // Seed a test tactical message
    testMessageId = `msg_${Date.now()}`;
    await db
      .prepare(
        `INSERT INTO tactical_messages (id, sender_user_id, target_team_id, body, is_urgent, created_at)
         VALUES (?, ?, ?, ?, ?, ?)`
      )
      .bind(
        testMessageId,
        'usr_cmd_001',
        'team_alpha_001',
        'دستور توقف موقت در مختصات و تثبیت موقعیت صادر شد.',
        1,
        Date.now()
      )
      .run();
  });

  it('GET /messages returns targeted and broadcast tactical messages', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(Array.isArray(json.data)).toBe(true);
    expect(json.data.length).toBeGreaterThanOrEqual(1);

    const found = json.data.find((m: any) => m.id === testMessageId);
    expect(found).toBeDefined();
    expect(found.is_urgent).toBe(true);
    expect(found.acknowledged).toBe(false);
  });

  it('POST /:id/acknowledge updates acknowledged timestamp', async () => {
    const res = await app.request(
      `/api/v1/messages/${testMessageId}/acknowledge`,
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.acknowledged_at).toBeGreaterThan(0);

    // Verify subsequent GET reflects acknowledged = true
    const getRes = await app.request(
      '/api/v1/messages',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );
    const getJson = await getRes.json<any>();
    const found = getJson.data.find((m: any) => m.id === testMessageId);
    expect(found.acknowledged).toBe(true);
  });

  it('POST /:id/acknowledge returns 404 for unknown message id', async () => {
    const res = await app.request(
      '/api/v1/messages/msg_does_not_exist/acknowledge',
      {
        method: 'POST',
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(404);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('NOT_FOUND');
  });

  it('POST /messages succeeds for COMMANDER with broadcast announcement', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          body: 'پیام عمومی: وضعیت آماده‌باش زرد اعلام می‌شود.',
          is_urgent: true,
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.id).toMatch(/^msg_/);
    expect(json.data.body).toBe('پیام عمومی: وضعیت آماده‌باش زرد اعلام می‌شود.');
    expect(json.data.is_urgent).toBe(true);
    expect(json.data.target_team_id).toBeNull();
  });

  it('POST /messages succeeds for targeted team dispatch', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          body: 'واحد آلفا فوراً به موقعیت گشت ۱ بازگردد.',
          is_urgent: false,
          target_team_id: 'team_alpha_001',
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.target_team_id).toBe('team_alpha_001');
  });

  it('POST /messages is forbidden (403) for TEAM_LEAD', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          body: 'سرتیم نمی‌تواند دستور کلی صادر کند.',
        }),
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('POST /messages rejects empty body with 400 VALIDATION_FAILED', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          body: '   ',
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('VALIDATION_FAILED');
  });

  it('POST /messages returns 404 for non-existent target team', async () => {
    const res = await app.request(
      '/api/v1/messages',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${commanderToken}`,
        },
        body: JSON.stringify({
          body: 'دستور به تیم ناموجود',
          target_team_id: 'team_does_not_exist',
        }),
      },
      env()
    );

    expect(res.status).toBe(404);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('NOT_FOUND');
  });
});
