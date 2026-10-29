import { Hono } from 'hono';
import { Bindings, Variables, DbTeam, DbTeamMember, DbUser } from '../types.js';
import { paginatedResponse, successResponse, errorResponse } from '../utils/response.js';
import { authMiddleware, requireRoles } from '../middleware/auth.js';

export const teamRoutes = new Hono<{ Bindings: Bindings; Variables: Variables }>();

teamRoutes.use('*', authMiddleware);

/**
 * GET /api/v1/teams
 * Lists all active tactical teams in the operational perimeter.
 * Restricted to COMMANDER and OWNER.
 */
teamRoutes.get('/', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const offset = parseInt(c.req.query('offset') || '0', 10);
  const limit = Math.min(parseInt(c.req.query('limit') || '50', 10), 100);

  const countRow = await db.prepare('SELECT COUNT(*) as total FROM teams WHERE is_active = 1').first<{ total: number }>();
  const total = countRow ? countRow.total : 0;

  const { results } = await db
    .prepare(
      `SELECT t.id, t.name, t.lead_user_id, t.vehicle_type, t.vehicle_plate, t.equipment_summary, t.is_active,
              COUNT(m.id) as member_count
       FROM teams t
       LEFT JOIN team_members m ON t.id = m.team_id
       WHERE t.is_active = 1
       GROUP BY t.id
       ORDER BY t.created_at ASC
       LIMIT ? OFFSET ?`
    )
    .bind(limit, offset)
    .all<DbTeam & { member_count: number }>();

  return paginatedResponse(
    c,
    results.map((t) => ({
      id: t.id,
      name: t.name,
      lead_user_id: t.lead_user_id,
      vehicle_type: t.vehicle_type,
      vehicle_plate: t.vehicle_plate,
      equipment_summary: t.equipment_summary,
      member_count: t.member_count,
      is_active: t.is_active === 1,
    })),
    {
      offset,
      limit,
      total,
      has_more: offset + results.length < total,
    }
  );
});

/**
 * POST /api/v1/teams
 * Creates a new tactical team and assigns its Team Lead and initial roster.
 * Restricted to OWNER and COMMANDER.
 */
teamRoutes.post('/', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, members } = body;
  const validationErrors: { field: string; issue: string }[] = [];

  if (!name || typeof name !== 'string' || name.trim().length < 2) {
    validationErrors.push({ field: 'name', issue: 'نام گروه عملیاتی الزامی است' });
  }

  if (!lead_user_id || typeof lead_user_id !== 'string') {
    validationErrors.push({ field: 'lead_user_id', issue: 'شناسه سرتیم الزامی است' });
  }

  if (!vehicle_type || typeof vehicle_type !== 'string') {
    validationErrors.push({ field: 'vehicle_type', issue: 'نوع خودرو الزامی است' });
  }

  if (!vehicle_plate || typeof vehicle_plate !== 'string') {
    validationErrors.push({ field: 'vehicle_plate', issue: 'شماره پلاک تاکتیکی خودرو الزامی است' });
  }

  if (validationErrors.length > 0) {
    return errorResponse(c, 'VALIDATION_FAILED', 'یک یا چند پارامتر نامعتبر است', validationErrors, 400);
  }

  // Verify lead user exists and is a TEAM_LEAD
  const leadUser = await db
    .prepare('SELECT id, role, is_active FROM users WHERE id = ?')
    .bind(lead_user_id.trim())
    .first<DbUser>();

  if (!leadUser) {
    return errorResponse(c, 'VALIDATION_FAILED', 'کاربر سرتیم با این شناسه یافت نشد', [{ field: 'lead_user_id', issue: 'کاربر سرتیم وجود ندارد' }], 400);
  }

  if (leadUser.role !== 'TEAM_LEAD') {
    return errorResponse(c, 'VALIDATION_FAILED', 'کاربر انتخابی باید نقش TEAM_LEAD داشته باشد', [{ field: 'lead_user_id', issue: 'نقش کاربر سرتیم نیست' }], 400);
  }

  const teamId = `team_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  const now = Date.now();
  const cleanName = name.trim();
  const cleanVehicleType = vehicle_type.trim();
  const cleanVehiclePlate = vehicle_plate.trim();
  const cleanEquipment = (equipment_summary || '').toString().trim();

  // Insert team
  await db
    .prepare(
      'INSERT INTO teams (id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)'
    )
    .bind(teamId, cleanName, lead_user_id.trim(), cleanVehicleType, cleanVehiclePlate, cleanEquipment, now, now)
    .run();

  // Insert initial members if provided
  const createdMembers: Array<{ id: string; full_name: string; callsign: string; role_in_team: string }> = [];
  if (Array.isArray(members)) {
    for (const m of members) {
      if (m && typeof m.full_name === 'string' && typeof m.callsign === 'string') {
        const memberId = `mem_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
        const memberRole = (m.role_in_team || 'عضو تیم').toString().trim();
        await db
          .prepare(
            'INSERT INTO team_members (id, team_id, full_name, callsign, role_in_team, created_at) VALUES (?, ?, ?, ?, ?, ?)'
          )
          .bind(memberId, teamId, m.full_name.trim(), m.callsign.trim(), memberRole, now)
          .run();
        createdMembers.push({
          id: memberId,
          full_name: m.full_name.trim(),
          callsign: m.callsign.trim(),
          role_in_team: memberRole,
        });
      }
    }
  }

  // Audit log
  const actor = c.get('user')!;
  const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';
  await db
    .prepare(
      'INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
    )
    .bind(
      crypto.randomUUID(),
      actor.id,
      'TEAM_CREATED',
      'TEAM',
      teamId,
      ip,
      JSON.stringify({ name: cleanName, lead_user_id }),
      now
    )
    .run()
    .catch(() => {});

  return successResponse(
    c,
    {
      id: teamId,
      name: cleanName,
      lead_user_id: lead_user_id.trim(),
      vehicle_type: cleanVehicleType,
      vehicle_plate: cleanVehiclePlate,
      equipment_summary: cleanEquipment,
      is_active: true,
      members: createdMembers,
      created_at: now,
      updated_at: now,
    },
    201
  );
});

/**
 * GET /api/v1/teams/:id
 * Retrieves detailed team metadata and roster members.
 */
teamRoutes.get('/:id', async (c) => {
  const db = c.env.DB;
  const teamId = c.req.param('id');
  const user = c.get('user')!;

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const team = await db
    .prepare('SELECT id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active FROM teams WHERE id = ?')
    .bind(teamId)
    .first<DbTeam>();

  if (!team) {
    return errorResponse(c, 'NOT_FOUND', `تیم عملیاتی با شناسه '${teamId}' یافت نشد`, undefined, 404);
  }

  // Authorization check: Field team lead can only view their own assigned team
  if (user.role === 'TEAM_LEAD' && team.lead_user_id !== user.id) {
    return errorResponse(c, 'FORBIDDEN', 'شما مجوز دسترسی به مشخصات سایر تیم‌ها را ندارید', undefined, 403);
  }

  const { results: members } = await db
    .prepare('SELECT id, full_name, callsign, role_in_team FROM team_members WHERE team_id = ? ORDER BY created_at ASC')
    .bind(teamId)
    .all<DbTeamMember>();

  return successResponse(c, {
    id: team.id,
    name: team.name,
    lead_user_id: team.lead_user_id,
    vehicle_type: team.vehicle_type,
    vehicle_plate: team.vehicle_plate,
    equipment_summary: team.equipment_summary,
    is_active: team.is_active === 1,
    members: members.map((m) => ({
      id: m.id,
      full_name: m.full_name,
      callsign: m.callsign,
      role_in_team: m.role_in_team,
    })),
  });
});

/**
 * PUT /api/v1/teams/:id
 * Updates team specifications, vehicle/equipment, or active status.
 * Restricted to OWNER and COMMANDER.
 */
teamRoutes.put('/:id', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  const teamId = c.req.param('id');

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const existingTeam = await db
    .prepare('SELECT id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active FROM teams WHERE id = ?')
    .bind(teamId)
    .first<DbTeam>();

  if (!existingTeam) {
    return errorResponse(c, 'NOT_FOUND', `تیم عملیاتی با شناسه '${teamId}' یافت نشد`, undefined, 404);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active } = body;
  const updates: string[] = [];
  const bindValues: (string | number)[] = [];

  if (name !== undefined) {
    if (typeof name !== 'string' || name.trim().length < 2) {
      return errorResponse(c, 'VALIDATION_FAILED', 'نام گروه عملیاتی نامعتبر است', [{ field: 'name', issue: 'نام نامعتبر است' }], 400);
    }
    updates.push('name = ?');
    bindValues.push(name.trim());
  }

  if (lead_user_id !== undefined) {
    const leadUser = await db.prepare('SELECT id, role FROM users WHERE id = ?').bind(lead_user_id).first<DbUser>();
    if (!leadUser || leadUser.role !== 'TEAM_LEAD') {
      return errorResponse(c, 'VALIDATION_FAILED', 'کاربر سرتیم باید نقش TEAM_LEAD داشته باشد', [{ field: 'lead_user_id', issue: 'سرتیم نامعتبر است' }], 400);
    }
    updates.push('lead_user_id = ?');
    bindValues.push(lead_user_id);
  }

  if (vehicle_type !== undefined) {
    updates.push('vehicle_type = ?');
    bindValues.push(vehicle_type.trim());
  }

  if (vehicle_plate !== undefined) {
    updates.push('vehicle_plate = ?');
    bindValues.push(vehicle_plate.trim());
  }

  if (equipment_summary !== undefined) {
    updates.push('equipment_summary = ?');
    bindValues.push(equipment_summary.trim());
  }

  if (is_active !== undefined) {
    const activeVal = is_active === true || is_active === 1 ? 1 : 0;
    updates.push('is_active = ?');
    bindValues.push(activeVal);
  }

  if (updates.length > 0) {
    const now = Date.now();
    updates.push('updated_at = ?');
    bindValues.push(now);
    bindValues.push(teamId);

    await db
      .prepare(`UPDATE teams SET ${updates.join(', ')} WHERE id = ?`)
      .bind(...bindValues)
      .run();

    // Audit log
    const actor = c.get('user')!;
    const ip = c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for') || 'local';
    const eventType = is_active === false || is_active === 0 ? 'TEAM_DEACTIVATED' : 'TEAM_UPDATED';

    await db
      .prepare(
        'INSERT INTO audit_logs (id, actor_user_id, event_type, resource_type, resource_id, ip_address, details_json, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)'
      )
      .bind(
        crypto.randomUUID(),
        actor.id,
        eventType,
        'TEAM',
        teamId,
        ip,
        JSON.stringify({ updates: body }),
        now
      )
      .run()
      .catch(() => {});
  }

  const updatedTeam = await db
    .prepare('SELECT id, name, lead_user_id, vehicle_type, vehicle_plate, equipment_summary, is_active FROM teams WHERE id = ?')
    .bind(teamId)
    .first<DbTeam>();

  return successResponse(c, {
    id: updatedTeam!.id,
    name: updatedTeam!.name,
    lead_user_id: updatedTeam!.lead_user_id,
    vehicle_type: updatedTeam!.vehicle_type,
    vehicle_plate: updatedTeam!.vehicle_plate,
    equipment_summary: updatedTeam!.equipment_summary,
    is_active: updatedTeam!.is_active === 1,
  });
});

/**
 * POST /api/v1/teams/:id/members
 * Adds a new personnel member to the team roster.
 * Restricted to OWNER and COMMANDER.
 */
teamRoutes.post('/:id/members', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  const teamId = c.req.param('id');

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const team = await db.prepare('SELECT id FROM teams WHERE id = ?').bind(teamId).first();
  if (!team) {
    return errorResponse(c, 'NOT_FOUND', `تیم عملیاتی با شناسه '${teamId}' یافت نشد`, undefined, 404);
  }

  let body: any;
  try {
    body = await c.req.json();
  } catch {
    return errorResponse(c, 'BAD_REQUEST', 'قالب بدنه درخواست نامعتبر است', undefined, 400);
  }

  const { full_name, callsign, role_in_team } = body;
  if (!full_name || !callsign) {
    return errorResponse(c, 'VALIDATION_FAILED', 'نام کامل و شناسه تماس الزامی هستند', undefined, 400);
  }

  const memberId = `mem_${crypto.randomUUID().replace(/-/g, '').slice(0, 12)}`;
  const now = Date.now();
  const cleanRole = (role_in_team || 'عضو تیم').toString().trim();

  await db
    .prepare('INSERT INTO team_members (id, team_id, full_name, callsign, role_in_team, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(memberId, teamId, full_name.trim(), callsign.trim(), cleanRole, now)
    .run();

  return successResponse(
    c,
    {
      id: memberId,
      team_id: teamId,
      full_name: full_name.trim(),
      callsign: callsign.trim(),
      role_in_team: cleanRole,
      created_at: now,
    },
    201
  );
});

/**
 * DELETE /api/v1/teams/:id/members/:memberId
 * Removes a member from the team roster.
 * Restricted to OWNER and COMMANDER.
 */
teamRoutes.delete('/:id/members/:memberId', requireRoles('OWNER', 'COMMANDER'), async (c) => {
  const db = c.env.DB;
  const { id: teamId, memberId } = c.req.param();

  if (!db) {
    return errorResponse(c, 'INTERNAL_ERROR', 'اتصال به پایگاه داده در دسترس نیست', undefined, 500);
  }

  const member = await db
    .prepare('SELECT id FROM team_members WHERE id = ? AND team_id = ?')
    .bind(memberId, teamId)
    .first();

  if (!member) {
    return errorResponse(c, 'NOT_FOUND', 'عضو تیم یافت نشد', undefined, 404);
  }

  await db.prepare('DELETE FROM team_members WHERE id = ? AND team_id = ?').bind(memberId, teamId).run();

  return successResponse(c, { message: 'عضو تیم با موفقیت حذف شد' });
});
