import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { app } from '../src/index.js';
import { createTestD1Database } from './helpers/testDb.js';
import { signJwt } from '../src/utils/crypto.js';
import { GroupLocationSession, LocationSessionDO } from '../src/websocket/locationSession.js';
import { createMockWebSocketPair, MockWebSocket } from './helpers/mockWebSocket.js';
import {
  ClientLocationUpdateMessage,
  ClientPingMessage,
  ClientSharingStartedMessage,
  ClientSharingStoppedMessage,
  ServerInitialRosterMessage,
  ServerLocationUpdateMessage,
  ServerMemberStateMessage,
  ServerPongMessage,
} from '../src/types/location.js';

const JWT_SECRET = 'waymate-tactical-dev-insecure-secret-key-32bytes-min';

describe('Real-Time Location Sharing WebSocket API & Session', () => {
  let db: D1Database;
  const env: any = {
    DB: null,
    ENVIRONMENT: 'test',
    API_VERSION: 'v1',
    JWT_SECRET,
  };

  beforeEach(() => {
    db = createTestD1Database();
    env.DB = db;
  });

  describe('Handshake & Authentication (/api/v1/ws/locations)', () => {
    it('rejects connection without token (401 UNAUTHORIZED)', async () => {
      const res = await app.request('/api/v1/ws/locations', {
        headers: { Upgrade: 'websocket' },
      }, env);

      expect(res.status).toBe(401);
      const body = await res.json() as any;
      expect(body.success).toBe(false);
      expect(body.error.code).toBe('UNAUTHORIZED');
    });

    it('rejects connection with invalid token (401 UNAUTHORIZED)', async () => {
      const res = await app.request('/api/v1/ws/locations?token=invalid.token.here', {
        headers: { Upgrade: 'websocket' },
      }, env);

      expect(res.status).toBe(401);
      const body = await res.json() as any;
      expect(body.error.code).toBe('UNAUTHORIZED');
    });

    it('rejects connection if user belongs to no active team (403 FORBIDDEN)', async () => {
      const { token } = await signJwt(
        { id: 'usr_inactive', username: 'inactive', role: 'TEAM_LEAD', callsign: 'تیم نامعتبر' },
        JWT_SECRET
      );

      const res = await app.request(`/api/v1/ws/locations?token=${token}`, {
        headers: { Upgrade: 'websocket' },
      }, env);

      expect(res.status).toBe(403);
      const body = await res.json() as any;
      expect(body.error.code).toBe('FORBIDDEN');
    });

    it('rejects connection if team lead attempts to access another team (403 FORBIDDEN)', async () => {
      // usr_lead_001 belongs to team_alpha_001
      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'lead1', role: 'TEAM_LEAD', callsign: 'پیشرو ۱' },
        JWT_SECRET
      );

      const res = await app.request(`/api/v1/ws/locations?token=${token}&team_id=team_beta_002`, {
        headers: { Upgrade: 'websocket' },
      }, env);

      expect(res.status).toBe(403);
      const body = await res.json() as any;
      expect(body.error.code).toBe('FORBIDDEN');
    });

    it('rejects HTTP request without Upgrade: websocket (426 Upgrade Required)', async () => {
      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'lead1', role: 'TEAM_LEAD', callsign: 'پیشرو ۱' },
        JWT_SECRET
      );

      const res = await app.request(`/api/v1/ws/locations?token=${token}`, {}, env);
      expect(res.status).toBe(426);
    });

    it('authenticates handshake using Authorization: Bearer <token> header', async () => {
      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'lead1', role: 'TEAM_LEAD', callsign: 'پیشرو ۱' },
        JWT_SECRET
      );

      const res = await app.request('/api/v1/ws/locations', {
        headers: {
          Upgrade: 'websocket',
          Authorization: `Bearer ${token}`,
        },
      }, env);

      // In Cloudflare Worker test environment without DO stub, upgrade succeeds or attempts WebSocket upgrade
      // Status 101 or 501 depending on WebSocketPair mock availability
      expect([101, 501]).toContain(res.status);
    });

    it('rejects invalid token in Authorization header (401 UNAUTHORIZED)', async () => {
      const res = await app.request('/api/v1/ws/locations', {
        headers: {
          Upgrade: 'websocket',
          Authorization: 'Bearer invalid.jwt.signature',
        },
      }, env);

      expect(res.status).toBe(401);
      const body = await res.json() as any;
      expect(body.error.code).toBe('UNAUTHORIZED');
    });
  });

  describe('GroupLocationSession Fan-Out & Message Validation', () => {
    let session: GroupLocationSession;
    let client1Ws: MockWebSocket;
    let server1Ws: MockWebSocket;
    let client2Ws: MockWebSocket;
    let server2Ws: MockWebSocket;

    beforeEach(() => {
      session = new GroupLocationSession();
      [client1Ws, server1Ws] = createMockWebSocketPair();
      [client2Ws, server2Ws] = createMockWebSocketPair();
    });

    it('delivers INITIAL_ROSTER and MEMBER_STATE on connect', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      // Client 1 should receive empty INITIAL_ROSTER as first user
      const initialMsg = server1Ws.getAllSentJson().find((m) => m.type === 'INITIAL_ROSTER') as ServerInitialRosterMessage;
      expect(initialMsg).toBeDefined();
      expect(initialMsg.teamId).toBe('team_alpha');
      expect(initialMsg.activeSharers).toHaveLength(0);

      // Connect Client 2
      session.handleConnection(server2Ws as any, {
        id: 'usr_002',
        callsign: 'راهنما ۲',
        teamId: 'team_alpha',
      });

      // Client 1 should receive MEMBER_STATE announcement that Client 2 connected
      const connectMsg = server1Ws.getAllSentJson().find((m) => m.type === 'MEMBER_STATE' && m.userId === 'usr_002') as ServerMemberStateMessage;
      expect(connectMsg).toBeDefined();
      expect(connectMsg.status).toBe('CONNECTED');
    });

    it('responds to PING with PONG containing server timestamp', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      const ping: ClientPingMessage = {
        type: 'PING',
        version: 1,
        timestamp: 1730000000000,
      };
      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(ping));

      const pong = server1Ws.getAllSentJson().find((m) => m.type === 'PONG') as ServerPongMessage;
      expect(pong).toBeDefined();
      expect(pong.timestamp).toBe(1730000000000);
      expect(pong.serverTimestamp).toBeGreaterThan(0);
    });

    it('rejects malformed JSON with INVALID_MESSAGE error', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      session.processIncomingMessage(session['clients'].get('usr_001')!, '{ broken-json');
      const err = server1Ws.getLastSentJson();
      expect(err.type).toBe('ERROR');
      expect(err.code).toBe('INVALID_MESSAGE');
    });

    it('rejects unsupported protocol version with UNSUPPORTED_VERSION error', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      session.processIncomingMessage(
        session['clients'].get('usr_001')!,
        JSON.stringify({ type: 'PING', version: 99, timestamp: Date.now() })
      );
      const err = server1Ws.getLastSentJson();
      expect(err.type).toBe('ERROR');
      expect(err.code).toBe('UNSUPPORTED_VERSION');
    });

    it('rejects invalid coordinates with INVALID_COORDINATES error', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      const invalidUpdate: ClientLocationUpdateMessage = {
        type: 'LOCATION_UPDATE',
        version: 1,
        seq: 1,
        latitude: 95.0, // Invalid: latitude > 90
        longitude: 53.6000,
        accuracy: 5.0,
        timestamp: Date.now(),
      };

      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(invalidUpdate));
      const err = server1Ws.getLastSentJson();
      expect(err.type).toBe('ERROR');
      expect(err.code).toBe('INVALID_COORDINATES');
    });

    it('rejects outdated timestamps with OUTDATED_TIMESTAMP error', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'راهنما ۱',
        teamId: 'team_alpha',
      });

      // 15 minutes in the past (> 10 mins threshold)
      const staleTimestamp = Date.now() - 15 * 60 * 1000;
      const staleUpdate: ClientLocationUpdateMessage = {
        type: 'LOCATION_UPDATE',
        version: 1,
        seq: 1,
        latitude: 28.5303,
        longitude: 53.6008,
        accuracy: 4.0,
        timestamp: staleTimestamp,
      };

      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(staleUpdate));
      const err = server1Ws.getLastSentJson();
      expect(err.type).toBe('ERROR');
      expect(err.code).toBe('OUTDATED_TIMESTAMP');
    });

    it('broadcasts SHARING_STARTED and SHARING_STOPPED with authoritative identity', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'امید ۱',
        teamId: 'team_alpha',
      });
      session.handleConnection(server2Ws as any, {
        id: 'usr_002',
        callsign: 'سهراب ۲',
        teamId: 'team_alpha',
      });

      // User 1 starts sharing
      const startMsg: ClientSharingStartedMessage = {
        type: 'SHARING_STARTED',
        version: 1,
        seq: 1,
        timestamp: Date.now(),
      };
      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(startMsg));

      // User 2 should receive broadcast
      const broadcast1 = server2Ws.getAllSentJson().find((m) => m.type === 'SHARING_STARTED');
      expect(broadcast1).toBeDefined();
      expect(broadcast1.userId).toBe('usr_001');
      expect(broadcast1.callsign).toBe('امید ۱');
      expect(broadcast1.teamId).toBe('team_alpha');

      // User 1 stops sharing
      const stopMsg: ClientSharingStoppedMessage = {
        type: 'SHARING_STOPPED',
        version: 1,
        seq: 2,
        timestamp: Date.now(),
      };
      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(stopMsg));

      // User 2 should receive stopped broadcast
      const broadcast2 = server2Ws.getAllSentJson().find((m) => m.type === 'SHARING_STOPPED');
      expect(broadcast2).toBeDefined();
      expect(broadcast2.userId).toBe('usr_001');
    });

    it('broadcasts valid LOCATION_UPDATE with authoritative identity to group members', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'امید ۱',
        teamId: 'team_alpha',
      });
      session.handleConnection(server2Ws as any, {
        id: 'usr_002',
        callsign: 'سهراب ۲',
        teamId: 'team_alpha',
      });

      const update: ClientLocationUpdateMessage = {
        type: 'LOCATION_UPDATE',
        version: 1,
        seq: 1,
        latitude: 28.5303,
        longitude: 53.6008,
        accuracy: 4.2,
        altitude: 1050.0,
        speed: 1.25,
        bearing: 135.0,
        timestamp: Date.now(),
      };

      session.processIncomingMessage(session['clients'].get('usr_001')!, JSON.stringify(update));

      const broadcast = server2Ws.getAllSentJson().find((m) => m.type === 'LOCATION_UPDATE') as ServerLocationUpdateMessage;
      expect(broadcast).toBeDefined();
      expect(broadcast.userId).toBe('usr_001');
      expect(broadcast.callsign).toBe('امید ۱');
      expect(broadcast.teamId).toBe('team_alpha');
      expect(broadcast.latitude).toBe(28.5303);
      expect(broadcast.longitude).toBe(53.6008);
      expect(broadcast.accuracy).toBe(4.2);
      expect(broadcast.speed).toBe(1.25);
      expect(broadcast.bearing).toBe(135.0);
    });

    it('enforces rate limiting when messages exceed threshold', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'امید ۱',
        teamId: 'team_alpha',
      });

      const client = session['clients'].get('usr_001')!;

      // Send 5 rapid messages in same 1-second window
      for (let i = 1; i <= 5; i++) {
        const ping: ClientPingMessage = {
          type: 'PING',
          version: 1,
          timestamp: Date.now(),
        };
        session.processIncomingMessage(client, JSON.stringify(ping));
      }

      const rateLimitErr = server1Ws.getAllSentJson().find((m) => m.type === 'ERROR' && m.code === 'RATE_LIMITED');
      expect(rateLimitErr).toBeDefined();
    });

    it('enforces group isolation: updates in Group A never reach Group B', () => {
      const sessionA = new GroupLocationSession();
      const sessionB = new GroupLocationSession();

      const [c1, s1] = createMockWebSocketPair();
      const [c2, s2] = createMockWebSocketPair();

      sessionA.handleConnection(s1 as any, {
        id: 'usr_team_a',
        callsign: 'تیم آلفا',
        teamId: 'team_a',
      });

      sessionB.handleConnection(s2 as any, {
        id: 'usr_team_b',
        callsign: 'تیم بتا',
        teamId: 'team_b',
      });

      // User in Team A sends location
      const updateA: ClientLocationUpdateMessage = {
        type: 'LOCATION_UPDATE',
        version: 1,
        seq: 1,
        latitude: 28.5303,
        longitude: 53.6008,
        accuracy: 3.0,
        timestamp: Date.now(),
      };
      sessionA.processIncomingMessage(sessionA['clients'].get('usr_team_a')!, JSON.stringify(updateA));

      // User in Team B must NOT receive any LOCATION_UPDATE
      const updatesForB = s2.getAllSentJson().filter((m) => m.type === 'LOCATION_UPDATE');
      expect(updatesForB).toHaveLength(0);
    });

    it('handles disconnection and broadcasts MEMBER_STATE DISCONNECTED', () => {
      session.handleConnection(server1Ws as any, {
        id: 'usr_001',
        callsign: 'امید ۱',
        teamId: 'team_alpha',
      });
      session.handleConnection(server2Ws as any, {
        id: 'usr_002',
        callsign: 'سهراب ۲',
        teamId: 'team_alpha',
      });

      expect(session.getConnectedCount()).toBe(2);

      // User 1 disconnects
      server1Ws.close(1000, 'Normal closure');

      expect(session.getConnectedCount()).toBe(1);

      const disconnectBroadcast = server2Ws.getAllSentJson().find((m) => m.type === 'MEMBER_STATE' && m.status === 'DISCONNECTED');
      expect(disconnectBroadcast).toBeDefined();
      expect(disconnectBroadcast.userId).toBe('usr_001');
      expect(disconnectBroadcast.isSharing).toBe(false);
    });
  });

  describe('Durable Object Forwarding & LocationSessionDO', () => {
    it('delegates to LocationSessionDO stub when LOCATION_SESSIONS binding is present', async () => {
      let capturedRequest: Request | null = null;

      const mockDoNamespace: any = {
        idFromName: (name: string) => ({ toString: () => name }),
        get: (id: any) => ({
          fetch: async (req: Request) => {
            capturedRequest = req;
            return new Response('Mock WebSocket 101', { status: 200 });
          },
        }),
      };

      const envWithDO = {
        ...env,
        LOCATION_SESSIONS: mockDoNamespace,
      };

      const { token } = await signJwt(
        { id: 'usr_lead_001', username: 'patrol_lead_demo', role: 'TEAM_LEAD', callsign: 'Falcon-Lead' },
        JWT_SECRET
      );

      const res = await app.request('/api/v1/ws/locations?token=' + token, {
        headers: { Upgrade: 'websocket' },
      }, envWithDO);

      expect(capturedRequest).not.toBeNull();
      expect(capturedRequest!.headers.get('X-User-Id')).toBe('usr_lead_001');
      expect(capturedRequest!.headers.get('X-Callsign')).toBe('Falcon-Lead');
      expect(capturedRequest!.headers.get('X-Team-Id')).toBe('team_alpha_001');
      expect(res.status).toBe(200);
    });
  });
});
