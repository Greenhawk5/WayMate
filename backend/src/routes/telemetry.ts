import { Hono } from 'hono';
import { Bindings, Variables } from '../types.js';
import { successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware, requireRoles } from '../middleware/auth.js';
import { validateTelemetryPayload } from '../utils/validation.js';

export const telemetryRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

telemetryRoutes.use('*', authMiddleware);

/**
 * POST /api/v1/telemetry/location
 * Ingests a single real-time GPS telemetry point from an active field unit.
 */
telemetryRoutes.post('/location', requireRoles('TEAM_LEAD'), async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const errors = validateTelemetryPayload(body);
  if (errors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'داده‌های موقعیت مکانی نامعتبر است', errors, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Find team for this team lead
  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (!team) {
    return errorResponse(c, 'FORBIDDEN', 'شما به عنوان سرتیم هیچ واحد عملیاتی فعالی ثبت نشده‌اید', undefined, 403);
  }

  const pingId = `ping_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();
  const recordedAt = body.recorded_at || now;

  await db
    .prepare(
      `INSERT INTO telemetry_pings
       (id, team_id, user_id, latitude, longitude, accuracy_meters, speed_mps, heading_degrees, operational_status, recorded_at, received_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      pingId,
      team.id,
      user.id,
      body.latitude,
      body.longitude,
      body.accuracy_meters,
      body.speed_mps || 0.0,
      body.heading_degrees || 0.0,
      body.operational_status || 'NORMAL',
      recordedAt,
      now
    )
    .run();

  return successResponse(c, { ping_id: pingId }, 201);
});

/**
 * POST /api/v1/telemetry/batch
 * Ingests store-and-forward buffered pings replayed after an offline period.
 */
telemetryRoutes.post('/batch', requireRoles('TEAM_LEAD'), async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  if (!body || !Array.isArray(body.pings) || body.pings.length === 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'آرایه pings الزامی بوده و نباید خالی باشد', undefined, 400);
  }

  if (body.pings.length > 100) {
    return errorResponse(c, 'VALIDATION_FAILED', 'حداکثر ۱۰۰ بسته موقعیت در هر ارسال مجاز است', undefined, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (!team) {
    return errorResponse(c, 'FORBIDDEN', 'شما به عنوان سرتیم هیچ واحد عملیاتی فعالی ثبت نشده‌اید', undefined, 403);
  }

  const now = Date.now();
  const statements: D1PreparedStatement[] = [];

  for (let i = 0; i < body.pings.length; i++) {
    const ping = body.pings[i];
    const errors = validateTelemetryPayload(ping);
    if (errors.length > 0) {
      return errorResponse(c, 'VALIDATION_FAILED', `بسته موقعیت در ردیف ${i + 1} نامعتبر است`, errors, 400);
    }

    const pingId = `ping_${crypto.randomUUID().replace(/-/g, '')}`;
    statements.push(
      db
        .prepare(
          `INSERT INTO telemetry_pings
           (id, team_id, user_id, latitude, longitude, accuracy_meters, speed_mps, heading_degrees, operational_status, recorded_at, received_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
        )
        .bind(
          pingId,
          team.id,
          user.id,
          ping.latitude,
          ping.longitude,
          ping.accuracy_meters,
          ping.speed_mps || 0.0,
          ping.heading_degrees || 0.0,
          ping.operational_status || 'NORMAL',
          ping.recorded_at || now,
          now
        )
    );
  }

  await db.batch(statements);

  return successResponse(c, { inserted_count: statements.length });
});

/**
 * POST /api/v1/telemetry/status
 * Updates the operational status of an active field team (NORMAL, PURSUING, ENGAGED)
 * without requiring real-time GPS coordinate acquisition.
 */
telemetryRoutes.post('/status', requireRoles('TEAM_LEAD'), async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const validStatuses = ['NORMAL', 'PURSUING', 'ENGAGED'];
  if (!body || !validStatuses.includes(body.operational_status)) {
    return errorResponse(
      c,
      'VALIDATION_FAILED',
      'وضعیت عملیاتی باید یکی از مقادیر NORMAL، PURSUING یا ENGAGED باشد',
      [{ field: 'operational_status', issue: 'مقدار وضعیت ارسالی نامعتبر است' }],
      400
    );
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Find team for this team lead
  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (!team) {
    return errorResponse(c, 'FORBIDDEN', 'شما به عنوان سرتیم هیچ واحد عملیاتی فعالی ثبت نشده‌اید', undefined, 403);
  }

  // Check if previous ping exists to preserve last known position
  const lastPing = await db
    .prepare(
      `SELECT latitude, longitude, accuracy_meters, speed_mps, heading_degrees
       FROM telemetry_pings
       WHERE team_id = ?
       ORDER BY recorded_at DESC
       LIMIT 1`
    )
    .bind(team.id)
    .first<any>();

  const lat = lastPing?.latitude ?? 28.5000;
  const lon = lastPing?.longitude ?? 53.5600;
  const accuracy = lastPing?.accuracy_meters ?? 10.0;
  const speed = 0.0;
  const heading = lastPing?.heading_degrees ?? 0.0;

  const pingId = `ping_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO telemetry_pings
       (id, team_id, user_id, latitude, longitude, accuracy_meters, speed_mps, heading_degrees, operational_status, recorded_at, received_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      pingId,
      team.id,
      user.id,
      lat,
      lon,
      accuracy,
      speed,
      heading,
      body.operational_status,
      now,
      now
    )
    .run();

  return successResponse(c, {
    ping_id: pingId,
    team_id: team.id,
    operational_status: body.operational_status,
    updated_at: now,
  });
});

/**
 * GET /api/v1/telemetry/live
 * Polls current positions and connection status of all active field teams for Commander Situational Map.
 */
telemetryRoutes.get('/live', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Compound query fetching the latest ping for each active team
  const query = `
    SELECT t.id as team_id, t.name as team_name, u.callsign,
           p.latitude, p.longitude, p.accuracy_meters, p.speed_mps, p.heading_degrees,
           p.operational_status, p.recorded_at as last_seen_at
    FROM teams t
    JOIN users u ON t.lead_user_id = u.id
    LEFT JOIN (
      SELECT tp.*
      FROM telemetry_pings tp
      INNER JOIN (
        SELECT team_id, MAX(recorded_at) as max_rec
        FROM telemetry_pings
        GROUP BY team_id
      ) latest ON tp.team_id = latest.team_id AND tp.recorded_at = latest.max_rec
    ) p ON t.id = p.team_id
    WHERE t.is_active = 1 AND p.id IS NOT NULL
  `;

  const { results } = await db.prepare(query).all<any>();
  const now = Date.now();

  const formatted = results.map((r) => {
    const elapsedMs = now - r.last_seen_at;
    const connectionStatus = elapsedMs <= 60000 ? 'ONLINE' : (elapsedMs <= 180000 ? 'STALE' : 'OFFLINE');
    return {
      team_id: r.team_id,
      team_name: r.team_name,
      callsign: r.callsign,
      latitude: r.latitude,
      longitude: r.longitude,
      accuracy_meters: r.accuracy_meters,
      speed_mps: r.speed_mps,
      heading_degrees: r.heading_degrees,
      operational_status: r.operational_status,
      last_seen_at: r.last_seen_at,
      connection_status: connectionStatus,
      is_stale: elapsedMs > 180000, // > 3 minutes staleness threshold per REQ-F-014
    };
  });

  return successResponse(c, formatted);
});
