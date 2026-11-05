import { Hono } from 'hono';
import { Bindings, Variables } from '../types.js';
import { verifyJwt } from '../utils/crypto.js';
import { errorResponse } from '../utils/response.js';
import { resolveUserTeamId } from './points.js';
import { getOrCreateGroupSession } from '../websocket/locationSession.js';

const DEFAULT_DEV_JWT_SECRET = 'waymate-tactical-dev-insecure-secret-key-32bytes-min';

export const locationWsRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

/**
 * WebSocket handshake endpoint for consent-based real-time location sharing.
 *
 * GET /api/v1/ws/locations?token=<jwt>&team_id=<optional>
 */
locationWsRoutes.get('/', async (c) => {
  // Extract token from query parameter or Authorization header
  let token = c.req.query('token');
  if (!token) {
    const authHeader = c.req.header('Authorization');
    if (authHeader && authHeader.startsWith('Bearer ')) {
      token = authHeader.substring(7).trim();
    }
  }

  if (!token) {
    return errorResponse(c, 'UNAUTHORIZED', 'توکن احراز هویت برای برقراری اتصال زنده موقعیت ارسال نشده است', undefined, 401);
  }

  const secret = c.env.JWT_SECRET || DEFAULT_DEV_JWT_SECRET;
  const user = await verifyJwt(token, secret);
  if (!user) {
    return errorResponse(c, 'UNAUTHORIZED', 'توکن احراز هویت نامعتبر یا منقضی شده است', undefined, 401);
  }

  // Server-side group membership verification
  const requestedTeamId = c.req.query('team_id');
  const teamResolution = await resolveUserTeamId(c.env.DB, user.id, user.role, requestedTeamId);
  if (teamResolution.error || !teamResolution.teamId) {
    const err = teamResolution.error || {
      status: 403,
      code: 'FORBIDDEN',
      message: 'دسترسی به گروه موردنظر مجاز نمی‌باشد',
    };
    return errorResponse(c, err.code, err.message, undefined, err.status);
  }

  const teamId = teamResolution.teamId;

  // Verify WebSocket Upgrade header
  const upgradeHeader = c.req.header('Upgrade');
  if (!upgradeHeader || upgradeHeader.toLowerCase() !== 'websocket') {
    return c.text('Expected Upgrade: websocket', 426);
  }

  // If Durable Objects binding is present in current environment
  if (c.env.LOCATION_SESSIONS) {
    const doId = c.env.LOCATION_SESSIONS.idFromName(teamId);
    const doStub = c.env.LOCATION_SESSIONS.get(doId);

    const newHeaders = new Headers(c.req.raw.headers);
    newHeaders.set('X-User-Id', user.id);
    newHeaders.set('X-Callsign', user.callsign);
    newHeaders.set('X-Team-Id', teamId);

    const newReq = new Request(c.req.url, {
      method: c.req.method,
      headers: newHeaders,
    });

    return doStub.fetch(newReq);
  }

  // Fallback to Worker WebSocketPair session (in-worker session manager & test environments)
  // @ts-ignore
  if (typeof WebSocketPair === 'undefined') {
    return c.text('WebSocket not supported in this runtime without polyfill', 501);
  }

  // @ts-ignore
  const pair = new WebSocketPair();
  const [client, server] = Object.values(pair) as [WebSocket, WebSocket];

  // @ts-ignore
  if (typeof server.accept === 'function') {
    // @ts-ignore
    server.accept();
  }

  const session = getOrCreateGroupSession(teamId);
  session.handleConnection(server, { id: user.id, callsign: user.callsign, teamId });

  return new Response(null, {
    status: 101,
    // @ts-ignore
    webSocket: client,
  });
});
