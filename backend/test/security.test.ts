import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';
import { signJwt, verifyJwt } from '../src/utils/crypto.js';
import { loginRateLimiter, apiGlobalLimiter } from '../src/middleware/rateLimiter.js';
import { GroupLocationSession } from '../src/websocket/locationSession.js';
import { createMockWebSocketPair } from './helpers/mockWebSocket.js';

const JWT_SECRET = 'waymate-tactical-dev-insecure-secret-key-32bytes-min';

describe('Phase 12: Security & Privacy Hardening Test Suite', () => {
  let db: D1Database;
  const env = () => ({
    DB: db,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET,
  });

  beforeEach(() => {
    db = createTestD1Database();
    loginRateLimiter.clear();
    apiGlobalLimiter.clear();
  });

  afterEach(() => {
    loginRateLimiter.clear();
    apiGlobalLimiter.clear();
  });

  // =========================================================================
  // 1. Unauthenticated Route Protection (Zero Trust Default)
  // =========================================================================
  describe('1. Unauthenticated Access Control', () => {
    const protectedRoutes: Array<{ method: string; path: string }> = [
      { method: 'GET', path: '/api/v1/auth/me' },
      { method: 'GET', path: '/api/v1/teams' },
      { method: 'POST', path: '/api/v1/teams' },
      { method: 'GET', path: '/api/v1/messages' },
      { method: 'POST', path: '/api/v1/messages' },
      { method: 'POST', path: '/api/v1/messages/msg_dummy/acknowledge' },
      { method: 'GET', path: '/api/v1/incidents' },
      { method: 'POST', path: '/api/v1/incidents' },
      { method: 'GET', path: '/api/v1/points' },
      { method: 'POST', path: '/api/v1/points' },
      { method: 'GET', path: '/api/v1/telemetry/stats' },
      { method: 'GET', path: '/api/v1/admin/stats' },
      { method: 'GET', path: '/api/v1/users' },
      { method: 'GET', path: '/api/v1/ws/locations' },
    ];

    for (const route of protectedRoutes) {
      it(`rejects unauthenticated ${route.method} ${route.path} with 401 UNAUTHORIZED`, async () => {
        const res = await app.request(
          route.path,
          {
            method: route.method,
            headers: route.path.includes('/ws/') ? { Upgrade: 'websocket' } : {},
          },
          env()
        );

        expect(res.status).toBe(401);
        const json = (await res.json()) as any;
        expect(json.success).toBe(false);
        expect(json.error.code).toBe('UNAUTHORIZED');
      });
    }
  });

  // =========================================================================
  // 2. JWT Cryptographic Hardening & Algorithm Confusion Defense
  // =========================================================================
  describe('2. JWT Cryptographic & Header Integrity Validation', () => {
    it('rejects expired JWT tokens with 401 UNAUTHORIZED', async () => {
      // Create expired token (negative expiresIn)
      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET,
        -10 // Expired 10 seconds ago
      );

      const res = await app.request(
        '/api/v1/auth/me',
        {
          headers: { Authorization: `Bearer ${token}` },
        },
        env()
      );

      expect(res.status).toBe(401);
      const json = (await res.json()) as any;
      expect(json.error.code).toBe('UNAUTHORIZED');
    });

    it('rejects JWT algorithm confusion: alg = "none" attack', async () => {
      // Craft an unverified token with alg: "none"
      const header = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');
      const payload = Buffer.from(
        JSON.stringify({
          id: 'usr_owner_001',
          username: 'owner_demo',
          role: 'OWNER',
          callsign: 'Alpha-Base',
          exp: Math.floor(Date.now() / 1000) + 3600,
        })
      ).toString('base64url');
      const forgedNoneToken = `${header}.${payload}.`;

      // 1. Direct crypto unit verification check
      const payloadResult = await verifyJwt(forgedNoneToken, JWT_SECRET);
      expect(payloadResult).toBeNull();

      // 2. HTTP Route check
      const res = await app.request(
        '/api/v1/admin/stats',
        {
          headers: { Authorization: `Bearer ${forgedNoneToken}` },
        },
        env()
      );

      expect(res.status).toBe(401);
    });

    it('rejects JWT algorithm confusion: alg = "RS256" with HMAC key', async () => {
      const header = Buffer.from(JSON.stringify({ alg: 'RS256', typ: 'JWT' })).toString('base64url');
      const payload = Buffer.from(
        JSON.stringify({
          id: 'usr_cmd_001',
          username: 'commander_demo',
          role: 'COMMANDER',
          callsign: 'Eagle-1',
          exp: Math.floor(Date.now() / 1000) + 3600,
        })
      ).toString('base64url');
      const forgedRs256Token = `${header}.${payload}.fake_signature`;

      const payloadResult = await verifyJwt(forgedRs256Token, JWT_SECRET);
      expect(payloadResult).toBeNull();

      const res = await app.request(
        '/api/v1/teams',
        {
          headers: { Authorization: `Bearer ${forgedRs256Token}` },
        },
        env()
      );

      expect(res.status).toBe(401);
    });

    it('rejects tampered signature', async () => {
      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET,
        3600
      );

      // Tamper last character of signature
      const tampered = token.slice(0, -2) + 'XX';
      const payloadResult = await verifyJwt(tampered, JWT_SECRET);
      expect(payloadResult).toBeNull();
    });
  });

  // =========================================================================
  // 3. Rate Limiting & Brute Force Defense
  // =========================================================================
  describe('3. Rate Limiting & Anti-Brute-Force', () => {
    it('blocks repeated failed login attempts from same IP after 5 tries (429 TOO_MANY_REQUESTS)', async () => {
      const testIp = '198.51.100.42';

      // 5 failed login attempts
      for (let i = 0; i < 5; i++) {
        const res = await app.request(
          '/api/v1/auth/login',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'CF-Connecting-IP': testIp,
            },
            body: JSON.stringify({
              username: 'commander_demo',
              password: 'wrong_password_attempt',
            }),
          },
          env()
        );
        expect(res.status).toBe(401);
      }

      // 6th attempt must be rejected with 429
      const blockedRes = await app.request(
        '/api/v1/auth/login',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'CF-Connecting-IP': testIp,
          },
          body: JSON.stringify({
            username: 'commander_demo',
            password: 'demo123',
          }),
        },
        env()
      );

      expect(blockedRes.status).toBe(429);
      expect(blockedRes.headers.get('Retry-After')).toBeDefined();
      const json = (await blockedRes.json()) as any;
      expect(json.error.code).toBe('TOO_MANY_REQUESTS');
    });

    it('resets rate limit on successful authentication', async () => {
      const testIp = '198.51.100.99';

      // 3 failed login attempts
      for (let i = 0; i < 3; i++) {
        await app.request(
          '/api/v1/auth/login',
          {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': testIp },
            body: JSON.stringify({ username: 'commander_demo', password: 'bad' }),
          },
          env()
        );
      }

      // 4th attempt succeeds with valid password
      const successRes = await app.request(
        '/api/v1/auth/login',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': testIp },
          body: JSON.stringify({ username: 'commander_demo', password: 'demo123' }),
        },
        env()
      );
      expect(successRes.status).toBe(200);

      // Now client can do more attempts without being blocked prematurely
      const checkRes = await app.request(
        '/api/v1/auth/login',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'CF-Connecting-IP': testIp },
          body: JSON.stringify({ username: 'commander_demo', password: 'demo123' }),
        },
        env()
      );
      expect(checkRes.status).toBe(200);
    });
  });

  // =========================================================================
  // 4. Request Body Size Limit (Payload Flood Protection)
  // =========================================================================
  describe('4. Payload Size Limiting (64KB Ceiling)', () => {
    it('rejects oversized JSON request payloads > 64KB with 413 PAYLOAD_TOO_LARGE', async () => {
      const { token } = await signJwt(
        { id: 'usr_cmd_001', username: 'commander_demo', role: 'COMMANDER', callsign: 'Eagle-1' },
        JWT_SECRET
      );

      // 70 KB payload
      const hugeString = 'X'.repeat(70 * 1024);
      const res = await app.request(
        '/api/v1/messages',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Content-Length': (70 * 1024).toString(),
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            body: hugeString,
          }),
        },
        env()
      );

      expect(res.status).toBe(413);
      const json = (await res.json()) as any;
      expect(json.error.code).toBe('PAYLOAD_TOO_LARGE');
    });
  });

  // =========================================================================
  // 5. IDOR Protection in Message Acknowledgment
  // =========================================================================
  describe('5. IDOR Defense: Cross-Team Message Acknowledgment', () => {
    it('prevents TEAM_LEAD of Team A from acknowledging messages targeted to Team B (403 FORBIDDEN)', async () => {
      // 1. Create Team Bravo and a lead user for Team Bravo
      await db
        .prepare('INSERT INTO users (id, username, password_hash, role, callsign, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 1, ?, ?)')
        .bind('usr_bravo_lead', 'bravo_lead', 'hash', 'TEAM_LEAD', 'Bravo-Lead', Date.now(), Date.now())
        .run();

      await db
        .prepare(
          'INSERT INTO teams (id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)'
        )
        .bind('team_bravo_002', 'گروه براوو', 'usr_bravo_lead', 'تویوتا هایلوکس', '۳۳-ج-۹۹', 'تجهیزات آفرود', Date.now(), Date.now())
        .run();

      // 2. Insert a tactical message targeted exclusively to Team Bravo
      const messageId = 'msg_confidential_bravo';
      await db
        .prepare('INSERT INTO tactical_messages (id, sender_user_id, target_team_id, body, is_urgent, created_at) VALUES (?, ?, ?, ?, 1, ?)')
        .bind(messageId, 'usr_cmd_001', 'team_bravo_002', 'دستور محرمانه به گروه براوو', Date.now())
        .run();

      // 3. Team Lead of Alpha tries to acknowledge Team Bravo's message
      const { token: alphaToken } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET
      );

      const ackRes = await app.request(
        `/api/v1/messages/${messageId}/acknowledge`,
        {
          method: 'POST',
          headers: { Authorization: `Bearer ${alphaToken}` },
        },
        env()
      );

      expect(ackRes.status).toBe(403);
      const json = (await ackRes.json()) as any;
      expect(json.error.code).toBe('FORBIDDEN');
      expect(json.error.message).toContain('سایر گروه‌ها');

      // 4. Commander CAN acknowledge any team message
      const { token: cmdToken } = await signJwt(
        { id: 'usr_cmd_001', username: 'commander_demo', role: 'COMMANDER', callsign: 'Eagle-1' },
        JWT_SECRET
      );

      const cmdAckRes = await app.request(
        `/api/v1/messages/${messageId}/acknowledge`,
        {
          method: 'POST',
          headers: { Authorization: `Bearer ${cmdToken}` },
        },
        env()
      );

      expect(cmdAckRes.status).toBe(200);
      const cmdJson = (await cmdAckRes.json()) as any;
      expect(cmdJson.success).toBe(true);
      expect(cmdJson.data.acknowledged_at).toBeGreaterThan(0);
    });
  });

  // =========================================================================
  // 6. Cross-Team Incident Isolation
  // =========================================================================
  describe('6. Incident GPS & Data Isolation Across Teams', () => {
    it('isolates incidents so TEAM_LEAD of Team A cannot view incidents of Team B', async () => {
      // 1. Create Team Bravo and a lead user for Team Bravo
      await db
        .prepare('INSERT INTO users (id, username, password_hash, role, callsign, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 1, ?, ?)')
        .bind('usr_bravo_lead2', 'bravo_lead2', 'hash', 'TEAM_LEAD', 'Bravo-Lead-2', Date.now(), Date.now())
        .run();

      await db
        .prepare(
          'INSERT INTO teams (id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)'
        )
        .bind('team_bravo_002', 'گروه براوو', 'usr_bravo_lead2', 'تویوتا هایلوکس', '۳۳-ج-۹۹', 'تجهیزات آفرود', Date.now(), Date.now())
        .run();

      // 2. Create an incident assigned to team_bravo
      const incidentId = 'inc_bravo_secret';
      await db
        .prepare('INSERT INTO incidents (id, team_id, reporter_user_id, title, description, incident_type, severity, latitude, longitude, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
        .bind(
          incidentId,
          'team_bravo_002',
          'usr_cmd_001',
          'سقوط کوهنورد در منطقه براوو',
          'مختصات دقیق حادثه',
          'OTHER',
          'URGENT',
          35.91234,
          51.45678,
          'OPEN',
          Date.now()
        )
        .run();

      // 2. Team Lead of Alpha queries incidents
      const { token: alphaToken } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET
      );

      const alphaRes = await app.request(
        '/api/v1/incidents',
        {
          headers: { Authorization: `Bearer ${alphaToken}` },
        },
        env()
      );

      expect(alphaRes.status).toBe(200);
      const alphaJson = (await alphaRes.json()) as any;
      const foundInAlpha = alphaJson.data.find((inc: any) => inc.id === incidentId);
      expect(foundInAlpha).toBeUndefined(); // Bravo's incident must be invisible to Alpha lead

      // 3. Commander queries incidents - should have global operational visibility
      const { token: cmdToken } = await signJwt(
        { id: 'usr_cmd_001', username: 'commander_demo', role: 'COMMANDER', callsign: 'Eagle-1' },
        JWT_SECRET
      );

      const cmdRes = await app.request(
        '/api/v1/incidents',
        {
          headers: { Authorization: `Bearer ${cmdToken}` },
        },
        env()
      );

      expect(cmdRes.status).toBe(200);
      const cmdJson = (await cmdRes.json()) as any;
      const foundInCmd = cmdJson.data.find((inc: any) => inc.id === incidentId);
      expect(foundInCmd).toBeDefined();
      expect(foundInCmd.title).toBe('سقوط کوهنورد در منطقه براوو');
    });
  });

  // =========================================================================
  // 7. Role-Based Access Control (RBAC) Hardening
  // =========================================================================
  describe('7. Role-Based Access Control (RBAC)', () => {
    it('restricts /admin/stats to OWNER and COMMANDER roles only (403 for TEAM_LEAD)', async () => {
      const { token: leadToken } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET
      );

      const res = await app.request(
        '/api/v1/admin/stats',
        {
          headers: { Authorization: `Bearer ${leadToken}` },
        },
        env()
      );

      expect(res.status).toBe(403);
      const json = (await res.json()) as any;
      expect(json.error.code).toBe('FORBIDDEN');
    });

    it('restricts /users management to OWNER only (403 for COMMANDER and TEAM_LEAD)', async () => {
      const { token: cmdToken } = await signJwt(
        { id: 'usr_cmd_001', username: 'commander_demo', role: 'COMMANDER', callsign: 'Eagle-1' },
        JWT_SECRET
      );

      const res = await app.request(
        '/api/v1/users',
        {
          headers: { Authorization: `Bearer ${cmdToken}` },
        },
        env()
      );

      expect(res.status).toBe(403);
    });

    it('prevents adding members to an inactive/archived team (400 BAD_REQUEST)', async () => {
      // Deactivate team_alpha_001
      await db.prepare('UPDATE teams SET is_active = 0 WHERE id = ?').bind('team_alpha_001').run();

      const { token: cmdToken } = await signJwt(
        { id: 'usr_cmd_001', username: 'commander_demo', role: 'COMMANDER', callsign: 'Eagle-1' },
        JWT_SECRET
      );

      const res = await app.request(
        '/api/v1/teams/team_alpha_001/members',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${cmdToken}`,
          },
          body: JSON.stringify({
            full_name: 'عضو جدید تست',
            callsign: 'تست-۳',
            role_in_team: 'امدادگر',
          }),
        },
        env()
      );

      expect(res.status).toBe(400);
      const json = (await res.json()) as any;
      expect(json.error.code).toBe('BAD_REQUEST');
      expect(json.error.message).toContain('غیرفعال');
    });
  });

  // =========================================================================
  // 8. WebSocket Location Privacy & Stale Coordinate Leakage Pruning
  // =========================================================================
  describe('8. WebSocket Stale Location Expiry (2-Minute Privacy Window)', () => {
    it('prunes locations older than 120s from INITIAL_ROSTER when a new peer connects', async () => {
      const session = new GroupLocationSession();

      // 1. Client A connects
      const [clientAWs, serverAWs] = createMockWebSocketPair();
      session.handleConnection(serverAWs as any, {
        id: 'usr_lead_001',
        callsign: 'Falcon-Lead',
        teamId: 'team_alpha_001',
      });

      // Inject a stale location update directly into session (timestamp: 150 seconds ago)
      const staleTimestamp = Date.now() - 150_000;
      session.processIncomingMessage(
        session['clients'].get('usr_lead_001')!,
        JSON.stringify({
          type: 'LOCATION_UPDATE',
          version: 1,
          seq: 1,
          latitude: 35.805,
          longitude: 51.412,
          accuracy: 5.0,
          timestamp: staleTimestamp,
        })
      );

      // Verify Client A is sharing
      expect(session['clients'].get('usr_lead_001')?.isSharing).toBe(true);

      // 2. Client B connects now
      const [clientBWs, serverBWs] = createMockWebSocketPair();
      session.handleConnection(serverBWs as any, {
        id: 'mem_001',
        callsign: 'البرز ۱',
        teamId: 'team_alpha_001',
      });

      // 3. Inspect INITIAL_ROSTER sent to Client B (via serverBWs.getAllSentJson())
      const rosterB = serverBWs.getAllSentJson().find((m) => m.type === 'INITIAL_ROSTER');
      expect(rosterB).toBeDefined();
      // Client A's position was older than 120s, so activeSharers must be empty!
      expect(rosterB.activeSharers).toHaveLength(0);

      // 4. Now Client A sends a fresh location update (< 120s)
      const freshTimestamp = Date.now() - 10_000;
      session.processIncomingMessage(
        session['clients'].get('usr_lead_001')!,
        JSON.stringify({
          type: 'LOCATION_UPDATE',
          version: 1,
          seq: 2,
          latitude: 35.806,
          longitude: 51.413,
          accuracy: 4.5,
          timestamp: freshTimestamp,
        })
      );

      // 5. Client C connects
      const [clientCWs, serverCWs] = createMockWebSocketPair();
      session.handleConnection(serverCWs as any, {
        id: 'mem_002',
        callsign: 'سهند ۲',
        teamId: 'team_alpha_001',
      });

      const rosterC = serverCWs.getAllSentJson().find((m) => m.type === 'INITIAL_ROSTER');
      expect(rosterC).toBeDefined();
      // Client A's fresh location MUST be included
      expect(rosterC.activeSharers).toHaveLength(1);
      expect(rosterC.activeSharers[0].userId).toBe('usr_lead_001');
      expect(rosterC.activeSharers[0].latitude).toBe(35.806);
    });
  });
});
