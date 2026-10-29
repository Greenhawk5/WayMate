import { MiddlewareHandler } from 'hono';
import { Bindings, Variables } from '../types.js';

export const safeLogger: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const start = Date.now();
  const requestId = crypto.randomUUID();
  c.set('requestId', requestId);

  const method = c.req.method;
  const path = c.req.path;
  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';

  await next();

  const duration = Date.now() - start;
  const status = c.res.status;

  // Safe structured log format excluding any authorization tokens, passwords, or PII
  console.log(
    JSON.stringify({
      level: status >= 500 ? 'error' : status >= 400 ? 'warn' : 'info',
      requestId,
      method,
      path,
      status,
      durationMs: duration,
      ip,
    })
  );
};
