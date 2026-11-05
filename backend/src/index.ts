import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { Bindings, Variables } from './types.js';
import { safeLogger } from './middleware/logger.js';
import { globalErrorHandler, notFoundHandler } from './middleware/errorHandler.js';
import { generalRateLimitMiddleware } from './middleware/rateLimiter.js';
import { bodySizeLimitMiddleware } from './middleware/bodyLimit.js';
import { healthRoutes } from './routes/health.js';
import { authRoutes } from './routes/auth.js';
import { teamRoutes } from './routes/teams.js';
import { telemetryRoutes } from './routes/telemetry.js';
import { incidentRoutes } from './routes/incidents.js';
import { messageRoutes } from './routes/messages.js';
import { userRoutes } from './routes/users.js';
import { adminRoutes } from './routes/admin.js';
import { pointRoutes } from './routes/points.js';
import { locationWsRoutes } from './routes/locationsWs.js';
export { LocationSessionDO } from './websocket/locationSession.js';

export const app = new Hono<{ Bindings: Bindings; Variables: Variables }>();

// Global Middlewares
app.use('*', safeLogger);
app.use('*', bodySizeLimitMiddleware);
app.use('*', generalRateLimitMiddleware);
app.use(
  '*',
  cors({
    origin: '*',
    allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization', 'Accept'],
    exposeHeaders: ['Content-Length', 'X-Request-Id'],
    maxAge: 86400,
  })
);

// Route Mounting under /api/v1
const v1 = new Hono<{ Bindings: Bindings; Variables: Variables }>();
v1.route('/health', healthRoutes);
v1.route('/auth', authRoutes);
v1.route('/teams', teamRoutes);
v1.route('/telemetry', telemetryRoutes);
v1.route('/incidents', incidentRoutes);
v1.route('/messages', messageRoutes);
v1.route('/users', userRoutes);
v1.route('/admin', adminRoutes);
v1.route('/points', pointRoutes);
v1.route('/ws/locations', locationWsRoutes);

app.route('/api/v1', v1);

// Error and Not Found Handlers
app.onError(globalErrorHandler);
app.notFound(notFoundHandler);

export default app;
