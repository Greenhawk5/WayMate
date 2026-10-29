import { Hono } from 'hono';
import { Bindings, Variables, DbUser } from '../types.js';
import { paginatedResponse, successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware, requireRoles } from '../middleware/auth.js';
import { hashPassword } from '../utils/crypto.js';

export const userRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// All user management routes require valid authentication and OWNER role
userRoutes.use('*', authMiddleware);
userRoutes.use('*', requireRoles('OWNER'));

/**
 * GET /api/v1/users
 * Lists system user accounts with optional role filtering and pagination.
 * Restricted strictly to OWNER role.
 */
userRoutes.get('/', async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const offset = parseInt(c.req.query('offset') || '0', 10);
  const limit = Math.min(parseInt(c.req.query('limit') || '50', 10), 100);
  const roleFilter = c.req.query('role');

  let countQuery = 'SELECT COUNT(*) as total FROM users';
  let listQuery = 'SELECT id, username, role, callsign, is_active, created_at, updated_at FROM users';
  const params: (string | number)[] = [];

  if (roleFilter && ['OWNER', 'COMMANDER', 'TEAM_LEAD'].includes(roleFilter.toUpperCase())) {
    countQuery += ' WHERE role = ?';
    listQuery += ' WHERE role = ?';
    params.push(roleFilter.toUpperCase());
  }

  listQuery += ' ORDER BY created_at ASC LIMIT ? OFFSET ?';

  const countRow = params.length > 0
    ? await db.prepare(countQuery).bind(...params).first<{ total: number }>()
    : await db.prepare(countQuery).first<{ total: number }>();
  const total = countRow ? countRow.total : 0;

  const bindParams = [...params, limit, offset];
  const { results } = await db.prepare(listQuery).bind(...bindParams).all<Omit<DbUser, 'password_hash'>>();

  return paginatedResponse(
    c,
    results.map((u) => ({
      id: u.id,
      username: u.username,
      role: u.role,
      callsign: u.callsign,
      is_active: u.is_active === 1,
      created_at: u.created_at,
      updated_at: u.updated_at,
    })),
    {
      offset,
      limit,
      total,
      has_more: offset + results.length < total,
    }
  );
});

/**
 * POST /api/v1/users
 * Provisions a new operational user account.
 * Restricted strictly to OWNER role.
 */
userRoutes.post('/', async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { username, password, role, callsign } = body;
  const validationErrors: { field: string; issue: string }[] = [];

  if (!username || typeof username !== 'string' || !/^[a-zA-Z0-9_]{3,32}$/.test(username.trim())) {
    validationErrors.push({
      field: 'username',
      issue: 'نام کاربری باید بین ۳ تا ۳۲ کاراکتر لاتین یا رقم باشد',
    });
  }

  if (!password || typeof password !== 'string' || password.length < 6) {
    validationErrors.push({
      field: 'password',
      issue: 'گذرواژه باید حداقل شامل ۶ کاراکتر باشد',
    });
  }

  const validRoles = ['OWNER', 'COMMANDER', 'TEAM_LEAD'];
  if (!role || typeof role !== 'string' || !validRoles.includes(role.toUpperCase())) {
    validationErrors.push({
      field: 'role',
      issue: "نقش کاربری نامعتبر است (باید یکی از مقادیر 'OWNER', 'COMMANDER', 'TEAM_LEAD' باشد)",
    });
  }

  if (!callsign || typeof callsign !== 'string' || callsign.trim().length < 2) {
    validationErrors.push({
      field: 'callsign',
      issue: 'شناسه تماس تاکتیکی باید حداقل ۲ کاراکتر باشد',
    });
  }

  if (validationErrors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'یک یا چند پارامتر نامعتبر است', validationErrors, 400);
  }

  const cleanUsername = username.trim().toLowerCase();

  // Check if username already exists
  const existing = await db
    .prepare('SELECT id FROM users WHERE username = ?')
    .bind(cleanUsername)
    .first();

  if (existing) {
    return errorResponse(c, 'CONFLICT', 'نام کاربری از قبل وجود دارد', undefined, 409);
  }

  const userId = `usr_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  const passwordHash = await hashPassword(password);
  const now = Date.now();
  const normalizedRole = role.toUpperCase() as 'OWNER' | 'COMMANDER' | 'TEAM_LEAD';
  const cleanCallsign = callsign.trim();

  await db
    .prepare(
      'INSERT INTO users (id, username, password_hash, role, callsign, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, 1, ?, ?)'
    )
    .bind(userId, cleanUsername, passwordHash, normalizedRole, cleanCallsign, now, now)
    .run();

  // Write audit log
  const actor = c.get('user')!;
  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';
  await db
    .prepare(
      'INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
    )
    .bind(
      crypto.randomUUID(),
      actor.id,
      'USER_CREATED',
      'USER',
      userId,
      ip,
      JSON.stringify({ username: cleanUsername, role: normalizedRole, callsign: cleanCallsign }),
      now
    )
    .run()
    .catch(() => {});

  return successResponse(
    c,
    {
      id: userId,
      username: cleanUsername,
      role: normalizedRole,
      callsign: cleanCallsign,
      is_active: true,
      created_at: now,
      updated_at: now,
    },
    201
  );
});

/**
 * PUT /api/v1/users/:id
 * Updates an existing user's role, callsign, or active status.
 * Restricted strictly to OWNER role.
 */
userRoutes.put('/:id', async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const targetUserId = c.req.param('id');
  const existingUser = await db
    .prepare('SELECT id, username, role, callsign, is_active, created_at, updated_at FROM users WHERE id = ?')
    .bind(targetUserId)
    .first<Omit<DbUser, 'password_hash'>>();

  if (!existingUser) {
    return errorResponse(c, 'NOT_FOUND', `کاربری با شناسه '${targetUserId}' یافت نشد`, undefined, 404);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { role, callsign, is_active } = body;
  const updates: string[] = [];
  const bindValues: (string | number)[] = [];

  if (role !== undefined) {
    const validRoles = ['OWNER', 'COMMANDER', 'TEAM_LEAD'];
    if (typeof role !== 'string' || !validRoles.includes(role.toUpperCase())) {
      return errorResponse(c, 'VALIDATION_FAILED', 'نقش کاربری نامعتبر است', [{ field: 'role', issue: 'نقش نامعتبر است' }], 400);
    }
    updates.push('role = ?');
    bindValues.push(role.toUpperCase());
  }

  if (callsign !== undefined) {
    if (typeof callsign !== 'string' || callsign.trim().length < 2) {
      return errorResponse(c, 'VALIDATION_FAILED', 'شناسه تماس نامعتبر است', [{ field: 'callsign', issue: 'شناسه تماس نامعتبر است' }], 400);
    }
    updates.push('callsign = ?');
    bindValues.push(callsign.trim());
  }

  if (is_active !== undefined) {
    const activeVal = is_active === true || is_active === 1 ? 1 : 0;
    updates.push('is_active = ?');
    bindValues.push(activeVal);
  }

  if (updates.length === 0) {
    return successResponse(c, {
      id: existingUser.id,
      username: existingUser.username,
      role: existingUser.role,
      callsign: existingUser.callsign,
      is_active: existingUser.is_active === 1,
      created_at: existingUser.created_at,
      updated_at: existingUser.updated_at,
    });
  }

  const now = Date.now();
  updates.push('updated_at = ?');
  bindValues.push(now);

  bindValues.push(targetUserId);
  await db
    .prepare(`UPDATE users SET ${updates.join(', ')} WHERE id = ?`)
    .bind(...bindValues)
    .run();

  const updatedUser = await db
    .prepare('SELECT id, username, role, callsign, is_active, created_at, updated_at FROM users WHERE id = ?')
    .bind(targetUserId)
    .first<Omit<DbUser, 'password_hash'>>();

  // Audit log
  const actor = c.get('user')!;
  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';
  const eventType = is_active === false || is_active === 0 ? 'USER_DEACTIVATED' : 'USER_UPDATED';

  await db
    .prepare(
      'INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
    )
    .bind(
      crypto.randomUUID(),
      actor.id,
      eventType,
      'USER',
      targetUserId,
      ip,
      JSON.stringify({ updates: body }),
      now
    )
    .run()
    .catch(() => {});

  return successResponse(c, {
    id: updatedUser!.id,
    username: updatedUser!.username,
    role: updatedUser!.role,
    callsign: updatedUser!.callsign,
    is_active: updatedUser!.is_active === 1,
    created_at: updatedUser!.created_at,
    updated_at: updatedUser!.updated_at,
  });
});
