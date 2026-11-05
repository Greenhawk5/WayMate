import { Context } from 'hono';
import { ApiResponse, ApiErrorResponse, PaginatedResponse, ApiFieldError, Bindings, Variables } from '../types.js';

export function successResponse<T>(
  c: Context<{ Bindings: Bindings; Variables: Variables }>,
  data: T,
  status: 200 | 201 = 200
) {
  const requestId = c.get('requestId') || crypto.randomUUID();
  const payload: ApiResponse<T> = {
    success: true,
    data,
    meta: {
      timestamp: Date.now(),
      request_id: requestId,
    },
  };
  return c.json(payload, status);
}

export function paginatedResponse<T>(
  c: Context<{ Bindings: Bindings; Variables: Variables }>,
  data: T[],
  pagination: { offset: number; limit: number; total: number; has_more: boolean },
  status: 200 = 200
) {
  const requestId = c.get('requestId') || crypto.randomUUID();
  const payload: PaginatedResponse<T> = {
    success: true,
    data,
    pagination,
    meta: {
      timestamp: Date.now(),
      request_id: requestId,
    },
  };
  return c.json(payload, status);
}

export function errorResponse(
  c: Context<{ Bindings: Bindings; Variables: Variables }>,
  code: string,
  message: string,
  details?: ApiFieldError[],
  status: 400 | 401 | 403 | 404 | 409 | 413 | 422 | 429 | 500 | 503 = 400
) {
  const requestId = c.get('requestId') || crypto.randomUUID();
  const payload: ApiErrorResponse = {
    success: false,
    error: {
      code,
      message,
      ...(details && details.length > 0 ? { details } : {}),
      timestamp: Date.now(),
      request_id: requestId,
    },
  };
  return c.json(payload, status);
}
