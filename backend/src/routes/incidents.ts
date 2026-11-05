import { Hono } from 'hono';
import { Bindings, Variables, DbIncident } from '../types.js';
import { successResponse, errorResponse, paginatedResponse } from '../utils/response.js';
import { authMiddleware } from '../middleware/auth.js';
import { validateCoordinates, validateIncidentPayload } from '../utils/validation.js';

export const incidentRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

incidentRoutes.use('*', authMiddleware);

/**
 * POST /api/v1/incidents/quick
 * 1-tap instant emergency alert trigger.
 */
incidentRoutes.post('/quick', async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const coordErrors = validateCoordinates(body.latitude, body.longitude);
  if (coordErrors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'مختصات موقعیت هشدار سریع نامعتبر است', coordErrors, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Find team for this user
  let teamId = 'team_unknown';
  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (team) {
    teamId = team.id;
  }

  const incidentId = `inc_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();
  const presetReason = typeof body.preset_reason === 'string' && body.preset_reason.trim()
    ? body.preset_reason.trim()
    : 'هشدار سریع اضطراری (مورد مشکوک تاکتیکی رویت شد)';

  await db
    .prepare(
      `INSERT INTO incidents
       (id, team_id, reporter_user_id, title, description, incident_type, severity, latitude, longitude, status, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      incidentId,
      teamId,
      user.id,
      'هشدار سریع اضطراری',
      presetReason,
      'QUICK_ALERT',
      'URGENT',
      body.latitude,
      body.longitude,
      'OPEN',
      now
    )
    .run();

  // Audit emergency alert
  await db
    .prepare(
      `INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      crypto.randomUUID(),
      user.id,
      'EMERGENCY_ALERT_DISPATCHED',
      'INCIDENT',
      incidentId,
      c.req.header('cf-connecting-ip') || 'local',
      JSON.stringify({ latitude: body.latitude, longitude: body.longitude }),
      now
    )
    .run()
    .catch(() => {});

  return successResponse(
    c,
    {
      incident_id: incidentId,
      severity: 'URGENT',
      status: 'OPEN',
      acknowledged: true,
    },
    201
  );
});

/**
 * POST /api/v1/incidents
 * Submits structured tactical observation report.
 */
incidentRoutes.post('/', async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const errors = validateIncidentPayload(body);
  if (errors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'اطلاعات گزارش رخداد نامعتبر است', errors, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  let teamId = 'team_unknown';
  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (team) {
    teamId = team.id;
  }

  const incidentId = `inc_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO incidents
       (id, team_id, reporter_user_id, title, description, incident_type, severity, latitude, longitude, status, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      incidentId,
      teamId,
      user.id,
      body.title.trim(),
      body.description.trim(),
      body.incident_type,
      body.severity,
      body.latitude,
      body.longitude,
      'OPEN',
      now
    )
    .run();

  return successResponse(
    c,
    {
      incident_id: incidentId,
      status: 'OPEN',
    },
    201
  );
});

/**
 * GET /api/v1/incidents
 * Lists active and historical incidents.
 */
incidentRoutes.get('/', async (c) => {
  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const offset = parseInt(c.req.query('offset') || '0', 10);
  const limit = Math.min(parseInt(c.req.query('limit') || '50', 10), 100);

  let teamFilter: string | null = null;
  if (user.role === 'TEAM_LEAD') {
    const userTeam = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(user.id)
      .first<{ id: string }>();

    if (!userTeam) {
      return paginatedResponse(c, [], { offset, limit, total: 0, has_more: false });
    }
    teamFilter = userTeam.id;
  } else {
    const queryTeamId = c.req.query('team_id');
    if (queryTeamId) {
      teamFilter = queryTeamId.trim();
    }
  }

  let countQuery = 'SELECT COUNT(*) as total FROM incidents';
  let listQuery = `SELECT id, team_id, reporter_user_id, title, description, incident_type, severity, latitude, longitude, status, created_at, resolved_at
     FROM incidents`;
  const countParams: (string | number)[] = [];
  const listParams: (string | number)[] = [];

  if (teamFilter) {
    countQuery += ' WHERE team_id = ?';
    listQuery += ' WHERE team_id = ?';
    countParams.push(teamFilter);
    listParams.push(teamFilter);
  }

  listQuery += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
  listParams.push(limit, offset);

  const countRow = countParams.length > 0
    ? await db.prepare(countQuery).bind(...countParams).first<{ total: number }>()
    : await db.prepare(countQuery).first<{ total: number }>();
  const total = countRow ? countRow.total : 0;

  const { results } = await db.prepare(listQuery).bind(...listParams).all<DbIncident>();

  return paginatedResponse(
    c,
    results.map((inc) => ({
      id: inc.id,
      team_id: inc.team_id,
      reporter_user_id: inc.reporter_user_id,
      title: inc.title,
      description: inc.description,
      incident_type: inc.incident_type,
      severity: inc.severity,
      latitude: inc.latitude,
      longitude: inc.longitude,
      status: inc.status,
      created_at: inc.created_at,
      resolved_at: inc.resolved_at,
    })),
    {
      offset,
      limit,
      total,
      has_more: offset + results.length < total,
    }
  );
});
