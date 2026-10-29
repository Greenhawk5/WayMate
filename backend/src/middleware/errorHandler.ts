import { ErrorHandler, NotFoundHandler } from 'hono';
import { Bindings, Variables } from '../types.js';
import { errorResponse } from '../utils/response.js';

export const globalErrorHandler: ErrorHandler<{ Bindings: Bindings; Variables: Variables }> = (err, c) => {
  const requestId = c.get('requestId') || crypto.randomUUID();
  console.error(`[ERROR] [${requestId}] ${err.message}`, err.stack);

  if (err instanceof SyntaxError && 'body' in err) {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است (خطای ساختار JSON)', undefined, 400);
  }

  return errorResponse(
    c,
    'INTERNAL_ERROR',
    'خطای داخلی در پردازش درخواست سمت سرور رخ داد',
    undefined,
    500
  );
};

export const notFoundHandler: NotFoundHandler<{ Bindings: Bindings; Variables: Variables }> = (c) => {
  return errorResponse(
    c,
    'NOT_FOUND',
    `مسیر درخواستی '${c.req.path}' در سامانه یافت نشد`,
    undefined,
    404
  );
};
