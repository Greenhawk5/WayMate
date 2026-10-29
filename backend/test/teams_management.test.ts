import { describe, it, expect, beforeEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';

describe('Team Management API (/api/v1/teams mutations)', () => {
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

  it('POST /teams creates a new team for COMMANDER', async () => {
    const res = await app.request(
      '/api/v1/teams',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${commanderToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: 'گشت براوو (پشتیبانی)',
          lead_user_id: 'usr_lead_001',
          vehicle_type: 'پژو لندکروزر',
          vehicle_plate: '۱۲-ج-۳۴',
          equipment_summary: 'تجهیزات رادیویی و کوهستان',
          members: [
            { full_name: 'علی احمدی', callsign: 'تفتان ۱', role_in_team: 'امدادگر' }
          ]
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.name).toBe('گشت براوو (پشتیبانی)');
    expect(json.data.vehicle_plate).toBe('۱۲-ج-۳۴');
    expect(json.data.members.length).toBe(1);
    expect(json.data.members[0].full_name).toBe('علی احمدی');
  });

  it('POST /teams is forbidden (403) for TEAM_LEAD', async () => {
    const res = await app.request(
      '/api/v1/teams',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${teamLeadToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          name: 'تیم غیرمجاز',
          lead_user_id: 'usr_lead_001',
          vehicle_type: 'تویوتا',
          vehicle_plate: '۹۹-الف-۱۱',
        }),
      },
      env()
    );

    expect(res.status).toBe(403);
    const json = await res.json<any>();
    expect(json.success).toBe(false);
    expect(json.error.code).toBe('FORBIDDEN');
  });

  it('PUT /teams/:id updates team details for COMMANDER', async () => {
    const res = await app.request(
      '/api/v1/teams/team_alpha_001',
      {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${commanderToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          vehicle_type: 'تویوتا هایلوکس جدید',
          is_active: true,
        }),
      },
      env()
    );

    expect(res.status).toBe(200);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.vehicle_type).toBe('تویوتا هایلوکس جدید');
  });

  it('POST /teams/:id/members adds a member to team roster', async () => {
    const res = await app.request(
      '/api/v1/teams/team_alpha_001/members',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${commanderToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          full_name: 'حسین مرادی',
          callsign: 'دماوند ۴',
          role_in_team: 'تک‌تیرانداز تاکتیکی',
        }),
      },
      env()
    );

    expect(res.status).toBe(201);
    const json = await res.json<any>();
    expect(json.success).toBe(true);
    expect(json.data.full_name).toBe('حسین مرادی');

    // Verify team details now has 3 members
    const teamRes = await app.request(
      '/api/v1/teams/team_alpha_001',
      {
        headers: { Authorization: `Bearer ${commanderToken}` },
      },
      env()
    );
    const teamJson = await teamRes.json<any>();
    expect(teamJson.data.members.length).toBe(3);
  });
});
