import { Hono } from 'hono';
import { Bindings, Variables, DbUser } from '../types.js';
import { successResponse, errorResponse } from '../utils/response.js';
import { verifyPassword, signJwt } from '../utils/crypto.js';
import { authMiddleware } from '../middleware/auth.js';

export const authRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

const DEFAULT_DEV_JWT_SECRET = 'waymate-tactical-dev-insecure-secret-key-32bytes-min';

authRoutes.post('/login', async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { username, password } = body;
  if (!username || !password || typeof username !== 'string' || typeof password !== 'string') {
    return errorResponse(
      c,
      'VALIDATION_FAILED',
      'نام کاربری و گذرواژه الزامی هستند',
      [
        ...(!username ? [{ field: 'username', issue: 'نام کاربری الزامی است' }] : []),
        ...(!password ? [{ field: 'password', issue: 'گذرواژه الزامی است' }] : []),
      ],
      400
    );
  }

  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Lookup user in D1
  const user = await db
    .prepare('SELECT id, username, password_hash, role, callsign, is_active, created_at, updated_at FROM users WHERE username = ?')
    .bind(username.trim())
    .first<DbUser>();

  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';

  if (!user || user.is_active !== 1) {
    // Record audit failure
    await db
      .prepare('INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .bind(crypto.randomUUID(), null, 'AUTH_LOGIN_FAILED', 'USER', username, ip, JSON.stringify({ reason: 'USER_NOT_FOUND' }), Date.now())
      .run()
      .catch(() => {});

    return errorResponse(c, 'UNAUTHORIZED', 'نام کاربری یا گذرواژه اشتباه است', undefined, 401);
  }

  // Verify password hash
  const isPasswordValid = await verifyPassword(password, user.password_hash);
  if (!isPasswordValid) {
    await db
      .prepare('INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
      .bind(crypto.randomUUID(), user.id, 'AUTH_LOGIN_FAILED', 'USER', user.id, ip, JSON.stringify({ reason: 'INVALID_PASSWORD' }), Date.now())
      .run()
      .catch(() => {});

    return errorResponse(c, 'UNAUTHORIZED', 'نام کاربری یا گذرواژه اشتباه است', undefined, 401);
  }

  // Issue JWT
  const secret = c.env.JWT_SECRET || DEFAULT_DEV_JWT_SECRET;
  const { token, expiresAt } = await signJwt(
    {
      id: user.id,
      username: user.username,
      role: user.role,
      callsign: user.callsign,
    },
    secret,
    86400 // 24 hours
  );

  // Record audit success
  await db
    .prepare('INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)')
    .bind(crypto.randomUUID(), user.id, 'AUTH_LOGIN_SUCCESS', 'USER', user.id, ip, JSON.stringify({ role: user.role }), Date.now())
    .run()
    .catch(() => {});

  return successResponse(c, {
    token,
    token_type: 'Bearer',
    expires_at: expiresAt,
    user: {
      id: user.id,
      username: user.username,
      role: user.role,
      callsign: user.callsign,
    },
  });
});

authRoutes.get('/me', authMiddleware, async (c) => {
  const user = c.get('user')!;
  return successResponse(c, {
    id: user.id,
    username: user.username,
    role: user.role,
    callsign: user.callsign,
    is_active: true,
  });
});

authRoutes.post('/logout', authMiddleware, async (c) => {
  return successResponse(c, { message: 'خروج با موفقیت انجام شد' });
});
