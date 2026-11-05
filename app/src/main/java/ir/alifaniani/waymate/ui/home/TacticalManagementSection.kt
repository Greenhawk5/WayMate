package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.domain.model.AuditLogEntry
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.SystemStats
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.common.TacticalTextField

/**
 * Phase 9 — Roles, Permissions & Tactical Management Panel.
 * Adapts strictly to the authenticated user's role (OWNER vs COMMANDER).
 * Completely hidden from TEAM_LEAD to respect hierarchical boundaries.
 */
@Composable
fun TacticalManagementSection(
    currentUser: User,
    uiState: HomeUiState,
    onCreateUser: (username: String, password: String, role: UserRole, callsign: String) -> Unit,
    onToggleUserStatus: (ManagedUser) -> Unit,
    onCreateTeam: (name: String, leadUserId: String, vehicleType: String, vehiclePlate: String, equipment: String) -> Unit,
    onToggleTeamStatus: (Team) -> Unit,
    onDismissFeedback: () -> Unit,
    onAddMember: (teamId: String, fullName: String, callsign: String, roleInTeam: String) -> Unit = { _, _, _, _ -> },
    onRemoveMember: (teamId: String, memberId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    if (currentUser.role == UserRole.TEAM_LEAD) {
        return // Field units are excluded from management controls
    }

    val isOwner = currentUser.role == UserRole.OWNER
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateUserDialog by remember { mutableStateOf(false) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var selectedTeamForAddMember by remember { mutableStateOf<Team?>(null) }
    var pendingUserDeactivation by remember { mutableStateOf<ManagedUser?>(null) }
    var pendingTeamDeactivation by remember { mutableStateOf<Team?>(null) }

    val tabs = if (isOwner) {
        listOf("مدیریت کاربران", "مدیریت گروه‌ها", "وضعیت و لاگ‌ها")
    } else {
        listOf("مدیریت گروه‌ها", "آمار عملیاتی")
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOwner) "پنل مدیریت و نقش‌های سامانه (فاز ۹)" else "پنل فرماندهی و مدیریت گروه‌ها (فاز ۹)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isOwner) "تعریف کاربران، تیم‌ها، رویدادها و گزارش‌ها" else "تعریف و مدیریت واحدهای گشتی تحت فرماندهی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TacticalStatusBadge(
                    text = if (isOwner) "اختیار کامل (OWNER)" else "فرماندهی (COMMANDER)",
                    tone = BadgeTone.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Management Feedback Banner
            if (uiState.managementFeedbackMessage != null) {
                TacticalFeedbackBanner(
                    title = if (uiState.isManagementFeedbackError) "خطا در عملیات مدیریتی" else "عملیات موفق",
                    message = uiState.managementFeedbackMessage,
                    type = if (uiState.isManagementFeedbackError) BannerType.ERROR else BannerType.SUCCESS
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Tab Navigation
            TabRow(
                selectedTabIndex = selectedTab.coerceIn(0, tabs.size - 1),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            onDismissFeedback()
                        },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading state
            if (uiState.isManagementLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "در حال همگام‌سازی اطلاعات مدیریتی...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tab Content
            if (isOwner) {
                when (selectedTab) {
                    0 -> UsersManagementTab(
                        users = uiState.managedUsers,
                        onAddUserClicked = { showCreateUserDialog = true },
                        onToggleStatus = { user ->
                            if (user.isActive) {
                                pendingUserDeactivation = user
                            } else {
                                onToggleUserStatus(user)
                            }
                        }
                    )
                    1 -> TeamsManagementTab(
                        teams = uiState.teams,
                        availableLeads = uiState.managedUsers.filter { it.role == UserRole.TEAM_LEAD },
                        onAddTeamClicked = { showCreateTeamDialog = true },
                        onToggleStatus = { team ->
                            if (team.isActive) {
                                pendingTeamDeactivation = team
                            } else {
                                onToggleTeamStatus(team)
                            }
                        },
                        onAddMemberClicked = { selectedTeamForAddMember = it },
                        onRemoveMember = onRemoveMember
                    )
                    2 -> SystemReportsTab(
                        stats = uiState.systemStats,
                        logs = uiState.auditLogs
                    )
                }
            } else {
                when (selectedTab) {
                    0 -> TeamsManagementTab(
                        teams = uiState.teams,
                        availableLeads = emptyList(),
                        onAddTeamClicked = { showCreateTeamDialog = true },
                        onToggleStatus = { team ->
                            if (team.isActive) {
                                pendingTeamDeactivation = team
                            } else {
                                onToggleTeamStatus(team)
                            }
                        },
                        onAddMemberClicked = { selectedTeamForAddMember = it },
                        onRemoveMember = onRemoveMember
                    )
                    1 -> CommanderStatsTab(stats = uiState.systemStats)
                }
            }
        }
    }

    // Dialogs
    if (showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { showCreateUserDialog = false },
            onConfirm = { username, password, role, callsign ->
                showCreateUserDialog = false
                onCreateUser(username, password, role, callsign)
            }
        )
    }

    if (showCreateTeamDialog) {
        CreateTeamDialog(
            availableLeads = uiState.managedUsers.filter { it.role == UserRole.TEAM_LEAD },
            onDismiss = { showCreateTeamDialog = false },
            onConfirm = { name, leadUserId, vehicleType, vehiclePlate, equipment ->
                showCreateTeamDialog = false
                onCreateTeam(name, leadUserId, vehicleType, vehiclePlate, equipment)
            }
        )
    }

    // Deactivation Confirmations
    pendingUserDeactivation?.let { user ->
        AlertDialog(
            onDismissRequest = { pendingUserDeactivation = null },
            title = { Text(text = "تأیید غیرفعال‌سازی کاربر", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از غیرفعال‌سازی کاربر '${user.callsign}' (${user.username}) اطمینان دارید؟ این کاربر دیگر امکان ورود نخواهد داشت.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingUserDeactivation
                        pendingUserDeactivation = null
                        target?.let { onToggleUserStatus(it) }
                    }
                ) {
                    Text("غیرفعال شود", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUserDeactivation = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    pendingTeamDeactivation?.let { team ->
        AlertDialog(
            onDismissRequest = { pendingTeamDeactivation = null },
            title = { Text(text = "تأیید غیرفعال‌سازی گروه عملیاتی", fontWeight = FontWeight.Bold) },
            text = { Text("آیا از غیرفعال‌سازی گروه '${team.name}' اطمینان دارید؟ وضعیت این تیم غیرفعال ثبت خواهد شد.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = pendingTeamDeactivation
                        pendingTeamDeactivation = null
                        target?.let { onToggleTeamStatus(it) }
                    }
                ) {
                    Text("غیرفعال شود", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingTeamDeactivation = null }) {
                    Text("انصراف")
                }
            }
        )
    }

    selectedTeamForAddMember?.let { team ->
        AddMemberDialog(
            teamName = team.name,
            onConfirm = { fullName, callsign, roleInTeam ->
                val targetId = team.id
                selectedTeamForAddMember = null
                onAddMember(targetId, fullName, callsign, roleInTeam)
            },
            onDismiss = { selectedTeamForAddMember = null }
        )
    }
}

@Composable
private fun UsersManagementTab(
    users: List<ManagedUser>,
    onAddUserClicked: () -> Unit,
    onToggleStatus: (ManagedUser) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کاربران ثبت‌شده (${users.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TacticalPrimaryButton(
                text = "+ کاربر جدید",
                onClick = onAddUserClicked,
                modifier = Modifier.width(130.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (users.isEmpty()) {
            Text(
                text = "هیچ کاربری بارگذاری نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            users.forEach { user ->
                UserItemCard(user = user, onToggleStatus = { onToggleStatus(user) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UserItemCard(
    user: ManagedUser,
    onToggleStatus: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.callsign,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val roleLabel = when (user.role) {
                        UserRole.OWNER -> "مالک"
                        UserRole.COMMANDER -> "فرمانده"
                        UserRole.TEAM_LEAD -> "سرتیم"
                    }
                    TacticalStatusBadge(text = roleLabel, tone = BadgeTone.NEUTRAL)
                }
                Text(
                    text = "نام کاربری: ${user.username} | شناسه: ${user.id.take(12)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TacticalStatusBadge(
                    text = if (user.isActive) "فعال" else "غیرفعال",
                    tone = if (user.isActive) BadgeTone.SUCCESS else BadgeTone.ERROR
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (user.isActive) "غیرفعال‌سازی" else "فعال‌سازی",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onToggleStatus() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
private fun TeamsManagementTab(
    teams: List<Team>,
    availableLeads: List<ManagedUser>,
    onAddTeamClicked: () -> Unit,
    onToggleStatus: (Team) -> Unit,
    onAddMemberClicked: (Team) -> Unit,
    onRemoveMember: (teamId: String, memberId: String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "گروه‌های عملیاتی (${teams.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            TacticalPrimaryButton(
                text = "+ گروه جدید",
                onClick = onAddTeamClicked,
                modifier = Modifier.width(130.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (teams.isEmpty()) {
            Text(
                text = "هیچ گروه عملیاتی تعریف نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            teams.forEach { team ->
                TeamManagementCard(
                    team = team,
                    onToggleStatus = { onToggleStatus(team) },
                    onAddMemberClicked = { onAddMemberClicked(team) },
                    onRemoveMember = { memberId -> onRemoveMember(team.id, memberId) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TeamManagementCard(
    team: Team,
    onToggleStatus: () -> Unit,
    onAddMemberClicked: () -> Unit,
    onRemoveMember: (memberId: String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TacticalStatusBadge(
                        text = if (team.isActive) "فعال" else "غیرفعال",
                        tone = if (team.isActive) BadgeTone.SUCCESS else BadgeTone.ERROR
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (team.isActive) "غیرفعال‌سازی" else "فعال‌سازی",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (team.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onToggleStatus() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "خودرو: ${team.vehicleType} (${team.vehiclePlate}) | تجهیزات: ${team.equipmentSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تعداد اعضا: ${team.members.size} نفر",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isExpanded) "بستن اعضا ▲" else "مشاهده اعضا ▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { isExpanded = !isExpanded }
                            .padding(2.dp)
                    )
                    Text(
                        text = "+ افزودن عضو",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onAddMemberClicked() }
                            .padding(2.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(6.dp))

                if (team.members.isEmpty()) {
                    Text(
                        text = "هیچ عضوی در این گروه ثبت نشده است.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    team.members.forEach { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${member.callsign} (${member.fullName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = member.roleInTeam,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "حذف",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable { onRemoveMember(member.id) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMemberDialog(
    teamName: String,
    onConfirm: (fullName: String, callsign: String, roleInTeam: String) -> Unit,
    onDismiss: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var callsign by remember { mutableStateOf("") }
    var roleInTeam by remember { mutableStateOf("عضو عملیاتی") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "افزودن عضو به $teamName", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (errorText != null) {
                    Text(text = errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                TacticalTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "نام و نام خانوادگی",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TacticalTextField(
                    value = callsign,
                    onValueChange = { callsign = it },
                    label = "شناسه تماس تاکتیکی (Callsign)",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                TacticalTextField(
                    value = roleInTeam,
                    onValueChange = { roleInTeam = it },
                    label = "نقش در گروه (مثلاً امدادگر، دیده‌بان، راننده)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (fullName.isBlank() || callsign.isBlank()) {
                        errorText = "نام کامل و شناسه تماس الزامی هستند"
                    } else {
                        onConfirm(fullName.trim(), callsign.trim(), roleInTeam.trim())
                    }
                }
            ) {
                Text("ثبت عضو")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun SystemReportsTab(
    stats: SystemStats?,
    logs: List<AuditLogEntry>
) {
    Column {
        Text(
            text = "گزارش وضعیت سامانه و آمار کلی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (stats != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("کل کاربران", "${stats.totalUsers} (${stats.activeUsers} فعال)", Modifier.weight(1f))
                MetricCard("گروه‌ها", "${stats.totalTeams} (${stats.activeTeams} فعال)", Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("رخدادهای باز", "${stats.openIncidents} مورد", Modifier.weight(1f))
                MetricCard("رویدادهای ۲۴ ساعت", "${stats.recentAuditEvents} مورد", Modifier.weight(1f))
            }
        } else {
            Text(
                text = "در حال دریافت آمار...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "آخرین رویدادهای امنیتی و مدیریتی (${logs.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Text(
                text = "هیچ لاگی ثبت نشده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            logs.take(5).forEach { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.eventType,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "IP: ${log.ipAddress}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "منبع: ${log.resourceType} (${log.resourceId})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CommanderStatsTab(stats: SystemStats?) {
    Column {
        Text(
            text = "خلاصه آمار عملیاتی فرماندهی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (stats != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("واحدهای فعال", "${stats.activeTeams} گروه", Modifier.weight(1f))
                MetricCard("وضعیت ارتباط", stats.systemStatus, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("رخدادهای عملیاتی", "${stats.totalIncidents} ثبت‌شده", Modifier.weight(1f))
                MetricCard("پیام‌های تاکتیکی", "${stats.totalMessages} ارسال‌شده", Modifier.weight(1f))
            }
        } else {
            Text(
                text = "در حال بارگذاری آمار عملیاتی...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onConfirm: (username: String, password: String, role: UserRole, callsign: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var callsign by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.TEAM_LEAD) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "صدور دسترسی و کاربر جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (validationError != null) {
                    Text(text = validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TacticalTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = "نام کاربری (لاتین: مثال lead_bravo)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "گذرواژه (حداقل ۶ کاراکتر)",
                    isPassword = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = callsign,
                    onValueChange = { callsign = it },
                    label = "شناسه تماس تاکتیکی (مثال: البرز ۱)"
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "نقش کاربری:", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RoleChip("سرتیم", UserRole.TEAM_LEAD, selectedRole) { selectedRole = it }
                    RoleChip("فرمانده", UserRole.COMMANDER, selectedRole) { selectedRole = it }
                    RoleChip("مالک", UserRole.OWNER, selectedRole) { selectedRole = it }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (username.isBlank() || password.length < 6 || callsign.isBlank()) {
                        validationError = "لطفاً تمام فیلدها را با مقادیر معتبر تکمیل نمایید"
                    } else {
                        onConfirm(username.trim(), password, selectedRole, callsign.trim())
                    }
                }
            ) {
                Text("ایجاد کاربر")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun RoleChip(
    label: String,
    role: UserRole,
    currentRole: UserRole,
    onSelect: (UserRole) -> Unit
) {
    val isSelected = role == currentRole
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSelect(role) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CreateTeamDialog(
    availableLeads: List<ManagedUser>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, leadUserId: String, vehicleType: String, vehiclePlate: String, equipment: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var leadUserId by remember { mutableStateOf(availableLeads.firstOrNull()?.id ?: "usr_lead_001") }
    var vehicleType by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var equipment by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "ایجاد گروه عملیاتی جدید", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (validationError != null) {
                    Text(text = validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                TacticalTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "نام گروه عملیاتی (مثال: گشت براوو)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = leadUserId,
                    onValueChange = { leadUserId = it },
                    label = "شناسه کاربر سرتیم (مثال: usr_lead_001)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = vehicleType,
                    onValueChange = { vehicleType = it },
                    label = "نوع خودرو (مثال: تویوتا هایلوکس)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = vehiclePlate,
                    onValueChange = { vehiclePlate = it },
                    label = "شماره پلاک تاکتیکی (مثال: ۱۲-ب-۳۴)"
                )
                Spacer(modifier = Modifier.height(8.dp))

                TacticalTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = "تجهیزات و ابزارها (مثال: بی‌سیم، جعبه امداد)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank() || vehicleType.isBlank() || vehiclePlate.isBlank() || leadUserId.isBlank()) {
                        validationError = "نام گروه، سرتیم، نوع و پلاک خودرو الزامی هستند"
                    } else {
                        onConfirm(name.trim(), leadUserId.trim(), vehicleType.trim(), vehiclePlate.trim(), equipment.trim())
                    }
                }
            ) {
                Text("ایجاد گروه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
