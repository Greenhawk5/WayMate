import { MiddlewareHandler } from 'hono';
import { Bindings, Variables } from '../types.js';
import { verifyJwt } from '../utils/crypto.js';
import { errorResponse } from '../utils/response.js';

const DEFAULT_DEV_JWT_SECRET = 'waymate-tactical-dev-insecure-secret-key-32bytes-min';

export const authMiddleware: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const authHeader = c.req.header('Authorization');
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return errorResponse(c, 'UNAUTHORIZED', 'نشست کاربری نامعتبر است یا توکن ارسال نشده است', undefined, 401);
  }

  const token = authHeader.substring(7).trim();
  const secret = c.env.JWT_SECRET || DEFAULT_DEV_JWT_SECRET;

  const user = await verifyJwt(token, secret);
  if (!user) {
    return errorResponse(c, 'UNAUTHORIZED', 'توکن احراز هویت نامعتبر است یا منقضی شده است', undefined, 401);
  }

  c.set('user', user);
  await next();
};

export function requireRoles(...allowedRoles: Array<'OWNER' | 'COMMANDER' | 'TEAM_LEAD'>) {
  const middleware: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
    const user = c.get('user');
    if (!user) {
      return errorResponse(c, 'UNAUTHORIZED', 'احراز هویت انجام نشده است', undefined, 401);
    }

    if (!allowedRoles.includes(user.role)) {
      return errorResponse(c, 'FORBIDDEN', 'دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد', undefined, 403);
    }

    await next();
  };
  return middleware;
}
