import {
  ActiveSharerSummary,
  ClientMessage,
  ConnectedClient,
  LOCATION_PROTOCOL_VERSION,
  ServerErrorMessage,
  ServerInitialRosterMessage,
  ServerLocationUpdateMessage,
  ServerMemberStateMessage,
  ServerMessage,
  ServerPongMessage,
  ServerSharingStartedMessage,
  ServerSharingStoppedMessage,
} from '../types/location.js';

export const STALE_LOCATION_EXPIRY_MS = 120_000; // 120 seconds staleness threshold per privacy policy

/**
 * In-memory real-time session manager for a specific group/team.
 *
 * Implements:
 * - Server-side group isolation
 * - Authoritative user identity derivation (no trusting client-provided userId/teamId)
 * - Safe message parsing and coordinate validation
 * - Sequence tracking and outdated timestamp rejection
 * - Rate limiting (max 4 updates/sec per user)
 * - Automatic presence fan-out (sharing started, stopped, member connected/disconnected)
 * - Zero storage in D1 for high-frequency coordinate stream
 */
export class GroupLocationSession {
  private clients = new Map<string, ConnectedClient>();

  /**
   * Registers a new authenticated client connection for this group.
   */
  handleConnection(
    ws: WebSocket,
    user: { id: string; callsign: string; teamId: string }
  ) {
    const userId = user.id;

    // If user already had an active connection, close the stale one
    const existing = this.clients.get(userId);
    if (existing && existing.ws !== ws) {
      try {
        existing.ws.close(1000, 'Replaced by new connection');
      } catch {
        // Ignore close error on stale socket
      }
    }

    const client: ConnectedClient = {
      ws,
      userId: user.id,
      callsign: user.callsign,
      teamId: user.teamId,
      isSharing: false,
      lastSeenSeq: -1,
      lastSeenTimestamp: 0,
      windowStart: Date.now(),
      messageCountInWindow: 0,
    };

    this.clients.set(userId, client);

    // Send initial roster of currently sharing members in this group to the newly connected client
    const now = Date.now();
    const activeSharers: ActiveSharerSummary[] = [];
    for (const other of this.clients.values()) {
      if (
        other.userId !== userId &&
        other.isSharing &&
        other.lastLocation &&
        now - other.lastLocation.timestamp <= STALE_LOCATION_EXPIRY_MS
      ) {
        activeSharers.push(other.lastLocation);
      }
    }

    const initialRoster: ServerInitialRosterMessage = {
      type: 'INITIAL_ROSTER',
      version: LOCATION_PROTOCOL_VERSION,
      teamId: user.teamId,
      activeSharers,
      serverTimestamp: Date.now(),
    };
    this.sendToClient(ws, initialRoster);

    // Broadcast member connection state to other connected group members
    const memberState: ServerMemberStateMessage = {
      type: 'MEMBER_STATE',
      version: LOCATION_PROTOCOL_VERSION,
      userId: user.id,
      callsign: user.callsign,
      teamId: user.teamId,
      status: 'CONNECTED',
      isSharing: false,
      serverTimestamp: Date.now(),
    };
    this.broadcastToGroup(memberState, userId);

    // Attach message and close listeners
    ws.addEventListener('message', (event) => {
      try {
        const dataStr = typeof event.data === 'string' ? event.data : new TextDecoder().decode(event.data as ArrayBuffer);
        this.processIncomingMessage(client, dataStr);
      } catch (err: any) {
        this.sendError(ws, 'INVALID_MESSAGE', 'قالب پیام ارسالی نامعتبر است');
      }
    });

    const cleanup = () => {
      const current = this.clients.get(userId);
      if (current && current.ws === ws) {
        this.clients.delete(userId);

        // Notify group that member disconnected; if they were sharing, mark isSharing=false
        const disconnectMsg: ServerMemberStateMessage = {
          type: 'MEMBER_STATE',
          version: LOCATION_PROTOCOL_VERSION,
          userId: user.id,
          callsign: user.callsign,
          teamId: user.teamId,
          status: 'DISCONNECTED',
          isSharing: false,
          serverTimestamp: Date.now(),
        };
        this.broadcastToGroup(disconnectMsg, userId);
      }
    };

    ws.addEventListener('close', cleanup);
    ws.addEventListener('error', cleanup);
  }

  /**
   * Processes an incoming raw text message from a verified client.
   */
  processIncomingMessage(client: ConnectedClient, rawText: string) {
    let msg: ClientMessage;
    try {
      msg = JSON.parse(rawText);
    } catch {
      this.sendError(client.ws, 'INVALID_MESSAGE', 'پیام ارسالی فرمت معتبر JSON ندارد');
      return;
    }

    if (!msg || typeof msg !== 'object' || !msg.type) {
      this.sendError(client.ws, 'INVALID_MESSAGE', 'نوع پیام مشخص نشده است');
      return;
    }

    if (msg.version !== LOCATION_PROTOCOL_VERSION) {
      this.sendError(client.ws, 'UNSUPPORTED_VERSION', `نسخه پیام باید ${LOCATION_PROTOCOL_VERSION} باشد`);
      return;
    }

    // Rate Limiting: Max 4 messages per second per client
    const now = Date.now();
    if (now - client.windowStart > 1000) {
      client.windowStart = now;
      client.messageCountInWindow = 1;
    } else {
      client.messageCountInWindow++;
      if (client.messageCountInWindow > 4) {
        this.sendError(client.ws, 'RATE_LIMITED', 'تعداد پیام‌های ارسالی بیش از سقف مجاز است');
        return;
      }
    }

    // Handle Ping/Pong
    if (msg.type === 'PING') {
      const pong: ServerPongMessage = {
        type: 'PONG',
        version: LOCATION_PROTOCOL_VERSION,
        timestamp: msg.timestamp || now,
        serverTimestamp: now,
      };
      this.sendToClient(client.ws, pong);
      return;
    }

    // Outdated timestamp checks (allow 60s future drift for clock skew, reject older than 10 mins)
    if (msg.timestamp > now + 60_000 || msg.timestamp < now - 600_000) {
      this.sendError(client.ws, 'OUTDATED_TIMESTAMP', 'برچسب زمانی پیام معتبر نمی‌باشد');
      return;
    }

    // Sequence & timestamp ordering check (discard older replays)
    if (typeof msg.seq === 'number') {
      if (msg.seq <= client.lastSeenSeq && msg.timestamp <= client.lastSeenTimestamp) {
        // Duplicate or outdated packet, silently drop
        return;
      }
      client.lastSeenSeq = msg.seq;
      client.lastSeenTimestamp = msg.timestamp;
    }

    switch (msg.type) {
      case 'SHARING_STARTED': {
        client.isSharing = true;
        const startedMsg: ServerSharingStartedMessage = {
          type: 'SHARING_STARTED',
          version: LOCATION_PROTOCOL_VERSION,
          userId: client.userId,
          callsign: client.callsign,
          teamId: client.teamId,
          timestamp: msg.timestamp,
          serverTimestamp: now,
        };
        this.broadcastToGroup(startedMsg, client.userId);
        break;
      }

      case 'SHARING_STOPPED': {
        client.isSharing = false;
        client.lastLocation = undefined;
        const stoppedMsg: ServerSharingStoppedMessage = {
          type: 'SHARING_STOPPED',
          version: LOCATION_PROTOCOL_VERSION,
          userId: client.userId,
          callsign: client.callsign,
          teamId: client.teamId,
          timestamp: msg.timestamp,
          serverTimestamp: now,
        };
        this.broadcastToGroup(stoppedMsg, client.userId);
        break;
      }

      case 'LOCATION_UPDATE': {
        // Validate coordinates
        if (
          typeof msg.latitude !== 'number' ||
          isNaN(msg.latitude) ||
          msg.latitude < -90.0 ||
          msg.latitude > 90.0 ||
          typeof msg.longitude !== 'number' ||
          isNaN(msg.longitude) ||
          msg.longitude < -180.0 ||
          msg.longitude > 180.0 ||
          typeof msg.accuracy !== 'number' ||
          isNaN(msg.accuracy) ||
          msg.accuracy < 0.0
        ) {
          this.sendError(client.ws, 'INVALID_COORDINATES', 'مختصات جغرافیایی یا دقت موقعیت نامعتبر است');
          return;
        }

        // Implicitly mark sharing active if a location update arrives
        client.isSharing = true;

        const sharerSummary: ActiveSharerSummary = {
          userId: client.userId,
          callsign: client.callsign,
          latitude: msg.latitude,
          longitude: msg.longitude,
          accuracy: msg.accuracy,
          altitude: typeof msg.altitude === 'number' ? msg.altitude : null,
          speed: typeof msg.speed === 'number' ? msg.speed : null,
          bearing: typeof msg.bearing === 'number' ? msg.bearing : null,
          timestamp: msg.timestamp,
        };
        client.lastLocation = sharerSummary;

        // Broadcast to all other authorized group members
        const updateMsg: ServerLocationUpdateMessage = {
          type: 'LOCATION_UPDATE',
          version: LOCATION_PROTOCOL_VERSION,
          seq: msg.seq,
          userId: client.userId,
          callsign: client.callsign,
          teamId: client.teamId,
          latitude: msg.latitude,
          longitude: msg.longitude,
          accuracy: msg.accuracy,
          altitude: typeof msg.altitude === 'number' ? msg.altitude : null,
          speed: typeof msg.speed === 'number' ? msg.speed : null,
          bearing: typeof msg.bearing === 'number' ? msg.bearing : null,
          timestamp: msg.timestamp,
          serverTimestamp: now,
        };
        this.broadcastToGroup(updateMsg, client.userId);
        break;
      }
    }
  }

  /**
   * Broadcasts a message to all connected clients in the group, optionally excluding sender.
   */
  broadcastToGroup(msg: ServerMessage, excludeUserId?: string) {
    const serialized = JSON.stringify(msg);
    for (const [userId, client] of this.clients.entries()) {
      if (userId !== excludeUserId) {
        try {
          client.ws.send(serialized);
        } catch {
          // Client might have dropped, will be removed on close event
        }
      }
    }
  }

  sendToClient(ws: WebSocket, msg: ServerMessage) {
    try {
      ws.send(JSON.stringify(msg));
    } catch {
      // Ignore send error
    }
  }

  sendError(ws: WebSocket, code: ServerErrorMessage['code'], message: string) {
    const errorMsg: ServerErrorMessage = {
      type: 'ERROR',
      version: LOCATION_PROTOCOL_VERSION,
      code,
      message,
      timestamp: Date.now(),
    };
    this.sendToClient(ws, errorMsg);
  }

  getConnectedCount(): number {
    return this.clients.size;
  }

  getActiveSharerCount(): number {
    let count = 0;
    for (const client of this.clients.values()) {
      if (client.isSharing) count++;
    }
    return count;
  }
}

/**
 * Global session registry mapping teamId to its GroupLocationSession.
 * Used for in-worker and test environments.
 */
export const groupLocationSessions = new Map<string, GroupLocationSession>();

export function getOrCreateGroupSession(teamId: string): GroupLocationSession {
  let session = groupLocationSessions.get(teamId);
  if (!session) {
    session = new GroupLocationSession();
    groupLocationSessions.set(teamId, session);
  }
  return session;
}

/**
 * Cloudflare Durable Object implementation for GroupLocationSession.
 * Activates when LOCATION_SESSIONS namespace is configured.
 */
export class LocationSessionDO {
  private session: GroupLocationSession;

  constructor(state?: any, env?: any) {
    this.session = new GroupLocationSession();
  }

  async fetch(request: Request): Promise<Response> {
    const upgradeHeader = request.headers.get('Upgrade');
    if (upgradeHeader !== 'websocket') {
      return new Response('Expected WebSocket upgrade', { status: 426 });
    }

    const userId = request.headers.get('X-User-Id');
    const callsign = request.headers.get('X-Callsign') || 'عضو تیم';
    const teamId = request.headers.get('X-Team-Id');

    if (!userId || !teamId) {
      return new Response('Missing user or team context', { status: 400 });
    }

    // @ts-ignore Cloudflare WebSocketPair
    const pair = new WebSocketPair();
    const [client, server] = Object.values(pair) as [WebSocket, WebSocket];

    // @ts-ignore
    if (typeof server.accept === 'function') {
      // @ts-ignore
      server.accept();
    }

    this.session.handleConnection(server, { id: userId, callsign, teamId });

    return new Response(null, {
      status: 101,
      // @ts-ignore
      webSocket: client,
    });
  }
}
