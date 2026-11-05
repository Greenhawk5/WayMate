import { Hono } from 'hono';
import { Bindings, Variables, DbTacticalMessage } from '../types.js';
import { successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware, requireRoles } from '../middleware/auth.js';

export const messageRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

messageRoutes.use('*', authMiddleware);

/**
 * POST /api/v1/messages
 * Dispatches an operational instruction or broadcast announcement.
 * Restricted to COMMANDER and OWNER roles.
 */
messageRoutes.post('/', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  if (!body || typeof body !== 'object') {
    return errorResponse(c, 'BAD_REQUEST', 'بدنه درخواست الزامی است', undefined, 400);
  }

  const messageBody = typeof body.body === 'string' ? body.body.trim() : '';
  if (messageBody.length === 0 || messageBody.length > 500) {
    return errorResponse(c, 'VALIDATION_FAILED', 'متن پیام الزامی بوده و حداکثر ۵۰۰ کاراکتر است', [
      { field: 'body', issue: 'متن پیام باید بین ۱ تا ۵۰۰ کاراکتر باشد' }
    ], 400);
  }

  const isUrgent = body.is_urgent === true;
  const targetTeamId = typeof body.target_team_id === 'string' && body.target_team_id.trim().length > 0
    ? body.target_team_id.trim()
    : null;

  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  if (targetTeamId) {
    const team = await db
      .prepare('SELECT id FROM teams WHERE id = ? AND is_active = 1')
      .bind(targetTeamId)
      .first<{ id: string }>();

    if (!team) {
      return errorResponse(c, 'NOT_FOUND', `تیم مقصد با شناسه '${targetTeamId}' یافت نشد`, undefined, 404);
    }
  }

  const messageId = `msg_${crypto.randomUUID().replace(/-/g, '')}`;
  const now = Date.now();

  await db
    .prepare(
      `INSERT INTO tactical_messages
       (id, sender_user_id, target_team_id, body, is_urgent, created_at, acknowledged_at)
       VALUES (?, ?, ?, ?, ?, ?, NULL)`
    )
    .bind(messageId, user.id, targetTeamId, messageBody, isUrgent ? 1 : 0, now)
    .run();

  return successResponse(
    c,
    {
      id: messageId,
      sender_callsign: user.callsign,
      target_team_id: targetTeamId,
      body: messageBody,
      is_urgent: isUrgent,
      created_at: now,
      acknowledged: false,
    },
    201
  );
});

/**
 * GET /api/v1/messages
 * Fetches pending operational orders and broadcast instructions.
 */
messageRoutes.get('/', async (c) => {
  const db = c.env.DB;
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Find team for user if TEAM_LEAD
  let userTeamId: string | null = null;
  const team = await db
    .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
    .bind(user.id)
    .first<{ id: string }>();

  if (team) {
    userTeamId = team.id;
  }

  const query = `
    SELECT m.id, u.callsign as sender_callsign, m.body, m.is_urgent, m.created_at, m.acknowledged_at
    FROM tactical_messages m
    JOIN users u ON m.sender_user_id = u.id
    WHERE m.target_team_id IS NULL OR m.target_team_id = ?
    ORDER BY m.created_at DESC
    LIMIT 50
  `;

  const { results } = await db.prepare(query).bind(userTeamId || '').all<any>();

  return successResponse(
    c,
    results.map((m) => ({
      id: m.id,
      sender_callsign: m.sender_callsign,
      body: m.body,
      is_urgent: m.is_urgent === 1,
      created_at: m.created_at,
      acknowledged: m.acknowledged_at !== null,
    }))
  );
});

/**
 * POST /api/v1/messages/:id/acknowledge
 * Transmits read confirmation for an operational instruction.
 */
messageRoutes.post('/:id/acknowledge', async (c) => {
  const db = c.env.DB;
  const messageId = c.req.param('id');
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  // Authorize recipient: verify message exists and caller is authorized to acknowledge
  const msg = await db
    .prepare('SELECT id, target_team_id, acknowledged_at FROM tactical_messages WHERE id = ?')
    .bind(messageId)
    .first<{ id: string; target_team_id: string | null; acknowledged_at: number | null }>();

  if (!msg) {
    return errorResponse(c, 'NOT_FOUND', `پیام عملیاتی با شناسه '${messageId}' یافت نشد`, undefined, 404);
  }

  if (msg.target_team_id && user.role === 'TEAM_LEAD') {
    const userTeam = await db
      .prepare('SELECT id FROM teams WHERE lead_user_id = ? AND is_active = 1')
      .bind(user.id)
      .first<{ id: string }>();

    if (!userTeam || userTeam.id !== msg.target_team_id) {
      return errorResponse(c, 'FORBIDDEN', 'شما مجوز تأیید دریافت پیام‌های سایر گروه‌ها را ندارید', undefined, 403);
    }
  }

  const now = Date.now();
  await db
    .prepare('UPDATE tactical_messages SET acknowledged_at = ? WHERE id = ? AND acknowledged_at IS NULL')
    .bind(now, messageId)
    .run();

  return successResponse(c, { acknowledged_at: msg.acknowledged_at || now });
});
