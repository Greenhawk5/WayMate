import { MiddlewareHandler } from 'hono';
import { Bindings, Variables } from '../types.js';
import { errorResponse } from '../utils/response.js';

const MAX_BODY_BYTES = 65536; // 64 KB maximum payload for API requests

/**
 * Middleware preventing oversized request bodies and JSON memory exhaustion attacks.
 * Validates Content-Length header and checks raw body byteLength when header is omitted.
 */
export const bodySizeLimitMiddleware: MiddlewareHandler<{ Bindings: Bindings; Variables: Variables }> = async (c, next) => {
  const method = c.req.method;
  if (['POST', 'PUT', 'PATCH'].includes(method)) {
    const contentLength = c.req.header('content-length');
    if (contentLength) {
      const bytes = parseInt(contentLength, 10);
      if (!isNaN(bytes) && bytes > MAX_BODY_BYTES) {
        return errorResponse(
          c,
          'PAYLOAD_TOO_LARGE',
          'حجم بدنه درخواست بیش از سقف مجاز (۶۴ کیلوبایت) است',
          undefined,
          413
        );
      }
    } else {
      try {
        const cloned = c.req.raw.clone();
        const buf = await cloned.arrayBuffer();
        if (buf && buf.byteLength > MAX_BODY_BYTES) {
          return errorResponse(
            c,
            'PAYLOAD_TOO_LARGE',
            'حجم بدنه درخواست بیش از سقف مجاز (۶۴ کیلوبایت) است',
            undefined,
            413
          );
        }
      } catch {
        // Body reading failed or empty; proceed to route handler
      }
    }
  }

  await next();
};
