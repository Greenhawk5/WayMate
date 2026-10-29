import { Hono } from 'hono';
import { Bindings, Variables, DbAuditLog } from '../types.js';
import { paginatedResponse, successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware, requireRoles } from '../middleware/auth.js';

export const adminRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

adminRoutes.use('*', authMiddleware);

/**
 * GET /api/v1/admin/stats
 * Aggregates foundational operational metrics and system status.
 * Restricted to OWNER and COMMANDER.
 */
adminRoutes.get('/stats', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Users counts
  const userCounts = await db
    .prepare(
      `SELECT
         COUNT(*) as total,
         SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active,
         SUM(CASE WHEN role = 'OWNER' THEN 1 ELSE 0 END) as owners,
         SUM(CASE WHEN role = 'COMMANDER' THEN 1 ELSE 0 END) as commanders,
         SUM(CASE WHEN role = 'TEAM_LEAD' THEN 1 ELSE 0 END) as team_leads
       FROM users`
    )
    .first<{ total: number; active: number; owners: number; commanders: number; team_leads: number }>();

  // Teams counts
  const teamCounts = await db
    .prepare(
      `SELECT
         COUNT(*) as total,
         SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) as active
       FROM teams`
    )
    .first<{ total: number; active: number }>();

  // Incidents counts
  const incidentCounts = await db
    .prepare(
      `SELECT
         COUNT(*) as total,
         SUM(CASE WHEN status = 'OPEN' THEN 1 ELSE 0 END) as open_count
       FROM incidents`
    )
    .first<{ total: number; open_count: number }>();

  // Tactical messages count
  const messageCounts = await db
    .prepare('SELECT COUNT(*) as total FROM tactical_messages')
    .first<{ total: number }>();

  // Recent audit log count (last 24 hours)
  const oneDayAgo = Date.now() - 86400000;
  const auditCounts = await db
    .prepare('SELECT COUNT(*) as recent FROM audit_logs WHERE created_at >= ?')
    .bind(oneDayAgo)
    .first<{ recent: number }>();

  return successResponse(c, {
    system_status: 'OPERATIONAL',
    environment: c.env.ENVIRONMENT || 'development',
    users: {
      total: userCounts?.total || 0,
      active: userCounts?.active || 0,
      by_role: {
        owners: userCounts?.owners || 0,
        commanders: userCounts?.commanders || 0,
        team_leads: userCounts?.team_leads || 0,
      },
    },
    teams: {
      total: teamCounts?.total || 0,
      active: teamCounts?.active || 0,
    },
    incidents: {
      total: incidentCounts?.total || 0,
      open: incidentCounts?.open_count || 0,
    },
    messages: {
      total: messageCounts?.total || 0,
    },
    audit: {
      recent_24h_events: auditCounts?.recent || 0,
    },
    timestamp: Date.now(),
  });
});

/**
 * GET /api/v1/admin/audit-logs
 * Retrieves immutable audit event logs.
 * Restricted strictly to OWNER role.
 */
adminRoutes.get('/audit-logs', requireRoles('OWNER'), async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const offset = parseInt(c.req.query('offset') || '0', 10);
  const limit = Math.min(parseInt(c.req.query('limit') || '50', 10), 100);
  const eventType = c.req.query('event_type');

  let countQuery = 'SELECT COUNT(*) as total FROM audit_logs';
  let listQuery = 'SELECT id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at FROM audit_logs';
  const params: (string | number)[] = [];

  if (eventType) {
    countQuery += ' WHERE event_type = ?';
    listQuery += ' WHERE event_type = ?';
    params.push(eventType);
  }

  listQuery += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';

  const countRow = params.length > 0
    ? await db.prepare(countQuery).bind(...params).first<{ total: number }>()
    : await db.prepare(countQuery).first<{ total: number }>();
  const total = countRow ? countRow.total : 0;

  const bindParams = [...params, limit, offset];
  const { results } = await db.prepare(listQuery).bind(...bindParams).all<DbAuditLog>();

  return paginatedResponse(
    c,
    results.map((log) => ({
      id: log.id,
      actor_user_id: log.actor_user_id,
      event_type: log.event_type,
      resource_type: log.resource_type,
      resource_id: log.resource_id,
      ip_address: log.ip_address,
      details: (() => {
        try {
          return JSON.parse(log.details_json);
        } catch {
          return {};
        }
      })(),
      created_at: log.created_at,
    })),
    {
      offset,
      limit,
      total,
      has_more: offset + results.length < total,
    }
  );
});
