import { Hono } from 'hono';
import { Bindings, Variables, DbPointOfInterest, PointCategory } from '../types.js';
import { successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware } from '../middleware/auth.js';
import { validatePoiPayload } from '../utils/validation.js';

export const pointRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

pointRoutes.use('*', authMiddleware);

/**
 * Helper to resolve the active team ID for a user.
 * TEAM_LEAD users are bound to their assigned team.
 * OWNER and COMMANDER can query any active team or pass team_id explicitly.
 */
export async function resolveUserTeamId(
  db: D1Database,
  userId: string,
  userRole: string,
  requestedTeamId?: string
): Promise<{ teamId: string | null; error?: { status: 403 | 404; code: string; message: string } }> {
  if (userRole === 'TEAM_LEAD') {
    const team = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(userId)
      .first<{ id: string }>();

    if (!team) {
      return {
        teamId: null,
        error: {
          status: 403,
          code: 'FORBIDDEN',
          message: 'شما عضو یا سرتیم هیچ گروه فعالی نیستید',
        },
      };
    }

    if (requestedTeamId && requestedTeamId !== team.id) {
      return {
        teamId: null,
        error: {
          status: 403,
          code: 'FORBIDDEN',
          message: 'دسترسی به اطلاعات گروه دیگر مجاز نمی‌باشد',
        },
      };
    }

    return { teamId: team.id };
  }

  // For OWNER / COMMANDER
  if (requestedTeamId) {
    const team = await db
      .prepare('SELECT id FROM teams WHERE id = ? AND is_active = 1')
      .bind(requestedTeamId)
      .first<{ id: string }>();

    if (!team) {
      return {
        teamId: null,
        error: {
          status: 404,
          code: 'NOT_FOUND',
          message: `گروه با شناسه '${requestedTeamId}' یافت نشد یا غیرفعال است`,
        },
      };
    }
    return { teamId: team.id };
  }

  // Default to the first active team if none specified
  const firstTeam = await db
    .prepare('SELECT id FROM teams WHERE is_active = 1 ORDER BY created_at ASC LIMIT 1')
    .first<{ id: string }>();

  return { teamId: firstTeam ? firstTeam.id : null };
}

/**
 * GET /api/v1/points
 * Lists shared points of interest for a group.
 * Supports incremental sync via `since` timestamp query parameter.
 */
pointRoutes.get('/', async (c) => {
  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const requestedTeamId = c.req.query('team_id');
  const { teamId, error } = await resolveUserTeamId(db, user.id, user.role, requestedTeamId);
  if (error) {
    return errorResponse(c, error.code, error.message, undefined, error.status);
  }

  if (!teamId) {
    return successResponse(c, []);
  }

  const sinceParam = c.req.query('since');
  const since = sinceParam ? parseInt(sinceParam, 10) : 0;
  const includeDeleted = c.req.query('include_deleted') === 'true' || since > 0;

  let query = `
    SELECT p.*,
           cu.callsign as created_by_callsign,
           uu.callsign as updated_by_callsign
    FROM points_of_interest p
    LEFT JOIN users cu ON p.created_by_user_id = cu.id
    LEFT JOIN users uu ON p.updated_by_user_id = uu.id
    WHERE p.team_id = ?
  `;

  const bindings: any[] = [teamId];

  if (!includeDeleted) {
    query += ' AND p.is_deleted = 0';
  }

  if (since > 0) {
    query += ' AND p.updated_at > ?';
    bindings.push(since);
  }

  query += ' ORDER BY p.updated_at DESC';

  const { results } = await db.prepare(query).bind(...bindings).all<any>();

  const formatted = results.map((row) => ({
    id: row.id,
    team_id: row.team_id,
    title: row.title,
    description: row.description,
    category: row.category as PointCategory,
    latitude: row.latitude,
    longitude: row.longitude,
    is_deleted: row.is_deleted === 1,
    version: row.version,
    created_by_user_id: row.created_by_user_id,
    created_by_callsign: row.created_by_callsign || 'کاربر ناشناس',
    updated_by_user_id: row.updated_by_user_id,
    updated_by_callsign: row.updated_by_callsign || 'کاربر ناشناس',
    created_at: row.created_at,
    updated_at: row.updated_at,
  }));

  return successResponse(c, formatted);
});

/**
 * POST /api/v1/points
 * Creates a new point of interest in the user's active group.
 */
pointRoutes.post('/', async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const validationErrors = validatePoiPayload(body, false);
  if (validationErrors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'اطلاعات نقطه نامعتبر است', validationErrors, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const { teamId, error } = await resolveUserTeamId(db, user.id, user.role, body.team_id);
  if (error) {
    return errorResponse(c, error.code, error.message, undefined, error.status);
  }

  if (!teamId) {
    return errorResponse(c, 'FORBIDDEN', 'امکان ثبت نقطه بدون تعیین گروه مشخص وجود ندارد', undefined, 403);
  }

  const pointId = `poi_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();
  const description = typeof body.description === 'string' ? body.description.trim() : '';

  await db
    .prepare(
      `INSERT INTO points_of_interest
       (id, team_id, created_by_user_id, updated_by_user_id, title, description, category, latitude, longitude, is_deleted, version, created_at, updated_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 1, ?, ?)`
    )
    .bind(
      pointId,
      teamId,
      user.id,
      user.id,
      body.title.trim(),
      description,
      body.category,
      body.latitude,
      body.longitude,
      now,
      now
    )
    .run();

  // Audit log record
  await db
    .prepare(
      `INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      crypto.randomUUID(),
      user.id,
      'POI_CREATED',
      'POINT_OF_INTEREST',
      pointId,
      c.req.header('cf-connecting-ip') || 'local',
      JSON.stringify({ title: body.title.trim(), category: body.category, team_id: teamId }),
      now
    )
    .run()
    .catch(() => {});

  const newPoi = {
    id: pointId,
    team_id: teamId,
    title: body.title.trim(),
    description,
    category: body.category as PointCategory,
    latitude: body.latitude,
    longitude: body.longitude,
    is_deleted: false,
    version: 1,
    created_by_user_id: user.id,
    created_by_callsign: user.callsign,
    updated_by_user_id: user.id,
    updated_by_callsign: user.callsign,
    created_at: now,
    updated_at: now,
  };

  return successResponse(c, newPoi, 201);
});

/**
 * GET /api/v1/points/:id
 * Retrieves a single point of interest by ID.
 */
pointRoutes.get('/:id', async (c) => {
  const pointId = c.req.param('id');
  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const row = await db
    .prepare(
      `SELECT p.*,
              cu.callsign as created_by_callsign,
              uu.callsign as updated_by_callsign
       FROM points_of_interest p
       LEFT JOIN users cu ON p.created_by_user_id = cu.id
       LEFT JOIN users uu ON p.updated_by_user_id = uu.id
       WHERE p.id = ?`
    )
    .bind(pointId)
    .first<any>();

  if (!row || row.is_deleted === 1) {
    return errorResponse(c, 'NOT_FOUND', `نقطه مورد نظر با شناسه '${pointId}' یافت نشد`, undefined, 404);
  }

  // Check group access
  if (user.role === 'TEAM_LEAD') {
    const userTeam = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(user.id)
      .first<{ id: string }>();

    if (!userTeam || userTeam.id !== row.team_id) {
      return errorResponse(c, 'FORBIDDEN', 'شما به نقطه این گروه دسترسی ندارید', undefined, 403);
    }
  }

  return successResponse(c, {
    id: row.id,
    team_id: row.team_id,
    title: row.title,
    description: row.description,
    category: row.category as PointCategory,
    latitude: row.latitude,
    longitude: row.longitude,
    is_deleted: row.is_deleted === 1,
    version: row.version,
    created_by_user_id: row.created_by_user_id,
    created_by_callsign: row.created_by_callsign || 'کاربر ناشناس',
    updated_by_user_id: row.updated_by_user_id,
    updated_by_callsign: row.updated_by_callsign || 'کاربر ناشناس',
    created_at: row.created_at,
    updated_at: row.updated_at,
  });
});

/**
 * PUT /api/v1/points/:id
 * Updates an existing point of interest. Supports optimistic concurrency via version.
 */
pointRoutes.put('/:id', async (c) => {
  const pointId = c.req.param('id');
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const validationErrors = validatePoiPayload(body, true);
  if (validationErrors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'اطلاعات ارسالی برای ویرایش نامعتبر است', validationErrors, 400);
  }

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const existing = await db
    .prepare('SELECT * FROM points_of_interest WHERE id = ?')
    .bind(pointId)
    .first<DbPointOfInterest>();

  if (!existing || existing.is_deleted === 1) {
    return errorResponse(c, 'NOT_FOUND', `نقطه مورد نظر با شناسه '${pointId}' یافت نشد`, undefined, 404);
  }

  // Authorization check
  if (user.role === 'TEAM_LEAD') {
    const userTeam = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(user.id)
      .first<{ id: string }>();

    if (!userTeam || userTeam.id !== existing.team_id) {
      return errorResponse(c, 'FORBIDDEN', 'شما مجوز ویرایش نقطه متعلق به گروه دیگر را ندارید', undefined, 403);
    }
  }

  // Optimistic concurrency check
  if (typeof body.version === 'number' && body.version !== existing.version) {
    return errorResponse(
      c,
      'CONFLICT',
      'تعارض در ویرایش: اطلاعات این نقطه توسط کاربر دیگری به‌روزرسانی شده است',
      [{ field: 'version', issue: `نسخه مورد انتظار ${existing.version} است اما نسخه ${body.version} ارسال شد` }],
      409
    );
  }

  const newTitle = body.title !== undefined ? body.title.trim() : existing.title;
  const newDescription = body.description !== undefined ? body.description.trim() : existing.description;
  const newCategory = body.category !== undefined ? body.category : existing.category;
  const newLat = body.latitude !== undefined ? body.latitude : existing.latitude;
  const newLon = body.longitude !== undefined ? body.longitude : existing.longitude;
  const now = Date.now();
  const nextVersion = existing.version + 1;

  await db
    .prepare(
      `UPDATE points_of_interest
       SET title = ?, description = ?, category = ?, latitude = ?, longitude = ?,
           updated_by_user_id = ?, updated_at = ?, version = ?
       WHERE id = ?`
    )
    .bind(newTitle, newDescription, newCategory, newLat, newLon, user.id, now, nextVersion, pointId)
    .run();

  // Audit log record
  await db
    .prepare(
      `INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      crypto.randomUUID(),
      user.id,
      'POI_UPDATED',
      'POINT_OF_INTEREST',
      pointId,
      c.req.header('cf-connecting-ip') || 'local',
      JSON.stringify({ previous_version: existing.version, new_version: nextVersion }),
      now
    )
    .run()
    .catch(() => {});

  // Retrieve creator callsign
  const creator = await db
    .prepare('SELECT callsign FROM users WHERE id = ?')
    .bind(existing.created_by_user_id)
    .first<{ callsign: string }>();

  return successResponse(c, {
    id: pointId,
    team_id: existing.team_id,
    title: newTitle,
    description: newDescription,
    category: newCategory as PointCategory,
    latitude: newLat,
    longitude: newLon,
    is_deleted: false,
    version: nextVersion,
    created_by_user_id: existing.created_by_user_id,
    created_by_callsign: creator?.callsign || 'کاربر ناشناس',
    updated_by_user_id: user.id,
    updated_by_callsign: user.callsign,
    created_at: existing.created_at,
    updated_at: now,
  });
});

/**
 * DELETE /api/v1/points/:id
 * Soft deletes a point of interest.
 */
pointRoutes.delete('/:id', async (c) => {
  const pointId = c.req.param('id');
  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const existing = await db
    .prepare('SELECT * FROM points_of_interest WHERE id = ?')
    .bind(pointId)
    .first<DbPointOfInterest>();

  if (!existing || existing.is_deleted === 1) {
    return errorResponse(c, 'NOT_FOUND', `نقطه مورد نظر با شناسه '${pointId}' یافت نشد`, undefined, 404);
  }

  // Authorization check
  if (user.role === 'TEAM_LEAD') {
    const userTeam = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(user.id)
      .first<{ id: string }>();

    if (!userTeam || userTeam.id !== existing.team_id) {
      return errorResponse(c, 'FORBIDDEN', 'شما مجوز حذف نقطه متعلق به گروه دیگر را ندارید', undefined, 403);
    }
  }

  const now = Date.now();
  const nextVersion = existing.version + 1;

  await db
    .prepare(
      `UPDATE points_of_interest
       SET is_deleted = 1, updated_by_user_id = ?, updated_at = ?, version = ?
       WHERE id = ?`
    )
    .bind(user.id, now, nextVersion, pointId)
    .run();

  // Audit log record
  await db
    .prepare(
      `INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
    )
    .bind(
      crypto.randomUUID(),
      user.id,
      'POI_DELETED',
      'POINT_OF_INTEREST',
      pointId,
      c.req.header('cf-connecting-ip') || 'local',
      JSON.stringify({ title: existing.title }),
      now
    )
    .run()
    .catch(() => {});

  return successResponse(c, {
    deleted: true,
    id: pointId,
    version: nextVersion,
  });
});
