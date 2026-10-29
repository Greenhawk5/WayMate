import { Hono } from 'hono';
import { Bindings, Variables } from '../types.js';
import { successResponse, errorResponse } from '../utils/response.js';

export const healthRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

healthRoutes.get('/', async (c) => {
  let dbStatus = 'DISCONNECTED';

  try {
    if (c.env.DB) {
      const result = await c.env.DB.prepare('SELECT 1 as is_alive').first<{ is_alive: number }>();
      if (result && result.is_alive === 1) {
        dbStatus = 'CONNECTED';
      }
    }
  } catch (err: any) {
    dbStatus = `ERROR: ${err.message}`;
  }

  return successResponse(c, {
    status: dbStatus === 'CONNECTED' ? 'HEALTHY' : 'DEGRADED',
    database: dbStatus,
    environment: c.env.ENVIRONMENT || 'development',
    version: c.env.API_VERSION || 'v1',
    timestamp: Date.now(),
  });
});
