import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Points of Interest API (/api/v1/points)', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET: 'test-secret-key-32bytes-for-jwt-signing',
  });

  let teamLeadToken: string;
  let commanderToken: string;
  let otherTeamLeadToken: string;

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

    // Create a second team and a separate team lead for cross-group isolation tests
    await db
      .prepare(
        `INSERT INTO users (id, username, password_hash, role, callsign, is_active, created_at, updated_at)
         VALUES ('usr_lead_002', 'other_lead', 'pbkdf2_sha256$100000$tactical_salt_2026$d28d64f188a0c41972d60c5c430594548d080bdbb0bf4da2d13fa48c167d115e', 'TEAM_LEAD', 'Beta-Lead', 1, 1729000000000, 1729000000000)`
      )
      .run();

    await db
      .prepare(
        `INSERT INTO teams (id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active, created_at, updated_at)
         VALUES ('team_beta_002', 'گشت بتا', 'usr_lead_002', 'پیکاپ', '۲۲-ج-۳۳', 'تجهیزات عمومی', 1, 1729000000000, 1729000000000)`
      )
      .run();

    const otherLeadRes = await app.request(
      '/api/v1/auth/login',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: 'other_lead', password: 'demo123' }),
      },
      env()
    );
    otherTeamLeadToken = (await otherLeadRes.json<any>()).data.token;
  });

  it('GET /points lists initial seeded points for active team', async () => {
    const res = await app.request(
      '/api/v1/points',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const body = await res.json<any>();
    expect(body.success).toBe(true);
    expect(Array.isArray(body.data)).toBe(true);
    expect(body.data.length).toBe(3);

    const first = body.data.find((p: any) => p.id === 'poi_demo_001');
    expect(first).toBeDefined();
    expect(first.title).toBe('نقطه شروع پیمایش غار سنگ‌شکنان');
    expect(first.category).toBe('START_POINT');
    expect(first.team_id).toBe('team_alpha_001');
    expect(first.is_deleted).toBe(false);
  });

  it('POST /points creates a new point of interest successfully', async () => {
    const newPoint = {
      title: 'قله چاتیز جهرم',
      description: 'نقطه مرتفع کوهستانی با دید ۳۶۰ درجه به دشت',
      category: 'DESTINATION',
      latitude: 28.5420,
      longitude: 53.6210,
    };

    const res = await app.request(
      '/api/v1/points',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify(newPoint),
      },
      env()
    );

    expect(res.status).toBe(201);
    const body = await res.json<any>();
    expect(body.success).toBe(true);
    expect(body.data.id).toMatch(/^poi_/);
    expect(body.data.title).toBe(newPoint.title);
    expect(body.data.category).toBe('DESTINATION');
    expect(body.data.version).toBe(1);
    expect(body.data.is_deleted).toBe(false);

    // Verify audit log
    const audit = await db
      .prepare('SELECT * FROM audit_logs WHERE event_type = ? AND resource_id = ?')
      .bind('POI_CREATED', body.data.id)
      .first<any>();
    expect(audit).not.toBeNull();
  });

  it('POST /points rejects invalid latitude or longitude', async () => {
    const res = await app.request(
      '/api/v1/points',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: 'نقطه غیرمجاز',
          category: 'GENERAL',
          latitude: 95.0, // Invalid: > 90
          longitude: 53.0,
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const body = await res.json<any>();
    expect(body.error.code).toBe('VALIDATION_FAILED');
  });

  it('POST /points rejects invalid category', async () => {
    const res = await app.request(
      '/api/v1/points',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: 'نقطه با دسته‌بندی غلط',
          category: 'MILITARY_BASE', // Invalid category
          latitude: 28.5,
          longitude: 53.5,
        }),
      },
      env()
    );

    expect(res.status).toBe(400);
    const body = await res.json<any>();
    expect(body.error.code).toBe('VALIDATION_FAILED');
  });

  it('GET /points/:id retrieves an existing point with creator callsign', async () => {
    const res = await app.request(
      '/api/v1/points/poi_demo_001',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(res.status).toBe(200);
    const body = await res.json<any>();
    expect(body.data.id).toBe('poi_demo_001');
    expect(body.data.created_by_callsign).toContain('Falcon-Lead');
  });

  it('PUT /points/:id updates point and increments version', async () => {
    const res = await app.request(
      '/api/v1/points/poi_demo_001',
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: 'نقطه شروع پیمایش غار سنگ‌شکنان (به‌روزشده)',
          category: 'START_POINT',
          version: 1, // Correct optimistic version
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const body = await res.json<any>();
    expect(body.data.title).toBe('نقطه شروع پیمایش غار سنگ‌شکنان (به‌روزشده)');
    expect(body.data.version).toBe(2);
  });

  it('PUT /points/:id rejects concurrent update on mismatched version (409 CONFLICT)', async () => {
    const res = await app.request(
      '/api/v1/points/poi_demo_001',
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${teamLeadToken}`,
        },
        body: JSON.stringify({
          title: 'ویرایش با نسخه قدیمی',
          version: 99, // Mismatched version
        }),
      },
      env()
    );

    expect(res.status).toBe(409);
    const body = await res.json<any>();
    expect(body.error.code).toBe('CONFLICT');
  });

  it('DELETE /points/:id soft-deletes a point and removes it from active list', async () => {
    const delRes = await app.request(
      '/api/v1/points/poi_demo_002',
      {
        method: 'DELETE',
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );

    expect(delRes.status).toBe(200);
    const delBody = await delRes.json<any>();
    expect(delBody.data.deleted).toBe(true);

    // Active GET should no longer include it
    const listRes = await app.request(
      '/api/v1/points',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );
    const listBody = await listRes.json<any>();
    const found = listBody.data.find((p: any) => p.id === 'poi_demo_002');
    expect(found).toBeUndefined();

    // Direct GET returns 404
    const getRes = await app.request(
      '/api/v1/points/poi_demo_002',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );
    expect(getRes.status).toBe(404);

    // Incremental sync with `since` still receives it with is_deleted=true
    const syncRes = await app.request(
      '/api/v1/points?since=1',
      {
        headers: { Authorization: `Bearer ${teamLeadToken}` },
      },
      env()
    );
    const syncBody = await syncRes.json<any>();
    const deletedInSync = syncBody.data.find((p: any) => p.id === 'poi_demo_002');
    expect(deletedInSync).toBeDefined();
    expect(deletedInSync.is_deleted).toBe(true);
  });

  it('Enforces cross-group isolation: user from team B cannot view or edit team A points', async () => {
    // Other team lead tries to view team A's point
    const getRes = await app.request(
      '/api/v1/points/poi_demo_001',
      {
        headers: { Authorization: `Bearer ${otherTeamLeadToken}` },
      },
      env()
    );
    expect(getRes.status).toBe(403);

    // Other team lead tries to edit team A's point
    const putRes = await app.request(
      '/api/v1/points/poi_demo_001',
      {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${otherTeamLeadToken}`,
        },
        body: JSON.stringify({ title: 'تغییر غیرمجاز' }),
      },
      env()
    );
    expect(putRes.status).toBe(403);
  });
});
