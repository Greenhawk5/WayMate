package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalScreenContainer
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge

import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.OperationalStatus

/**
 * Landing screen representing the authenticated destination,
 * formatted using the WayMate Dark Tactical design system.
 */
@Composable
fun HomeScreen(
    user: User,
    onLogoutClicked: () -> Unit,
    homeUiState: HomeUiState = HomeUiState(),
    onRetryTeams: () -> Unit = {},
    onStatusSelected: (OperationalStatus) -> Unit = {},
    onAcknowledgeMessage: (String) -> Unit = {},
    onSendMessage: (body: String, isUrgent: Boolean, targetTeamId: String?) -> Unit = { _, _, _ -> },
    onRefresh: () -> Unit = {},
    onCreateUser: (username: String, password: String, role: UserRole, callsign: String) -> Unit = { _, _, _, _ -> },
    onToggleUserStatus: (ManagedUser) -> Unit = {},
    onCreateTeam: (name: String, leadUserId: String, vehicleType: String, vehiclePlate: String, equipment: String) -> Unit = { _, _, _, _, _ -> },
    onToggleTeamStatus: (Team) -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    onSyncQueue: () -> Unit = {},
    onDismissSyncFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    TacticalScreenContainer(modifier = modifier) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "پایگاه عملیاتی وی‌میت",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "ارتباط برقرار است (محیط آزمایشی)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            TacticalStatusBadge(
                text = "فعال / برخط",
                tone = BadgeTone.SUCCESS
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Authenticated Tactical Credentials Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "مشخصات شناسه کاربری و عملیاتی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                DetailItem(label = "شناسه تماس تاکتیکی", value = user.callsign)
                DetailItem(
                    label = "سطح دسترسی و نقش",
                    value = when (user.role) {
                        UserRole.OWNER -> "متولی اصلی سامانه (OWNER)"
                        UserRole.COMMANDER -> "فرمانده عملیات (COMMANDER)"
                        UserRole.TEAM_LEAD -> "سرتیم گشت (TEAM_LEAD)"
                    }
                )
                DetailItem(label = "نام کاربری سیستم", value = user.username)
                DetailItem(label = "شناسه رکورد", value = user.id)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Operational Tactical Map & Link Health Section
        TacticalMapAndLinkSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Phase 8 & 10: Communications, In-Band Messaging, Operational Status & Offline Queue
        TacticalCommunicationsSection(
            currentUser = user,
            uiState = homeUiState,
            onStatusSelected = onStatusSelected,
            onAcknowledgeMessage = onAcknowledgeMessage,
            onSendMessage = onSendMessage,
            onRefresh = onRefresh,
            onSyncQueue = onSyncQueue,
            onDismissSyncFeedback = onDismissSyncFeedback
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phase 9: Tactical Management & Permissions Panel (OWNER / COMMANDER)
        if (user.role != UserRole.TEAM_LEAD) {
            TacticalManagementSection(
                currentUser = user,
                uiState = homeUiState,
                onCreateUser = onCreateUser,
                onToggleUserStatus = onToggleUserStatus,
                onCreateTeam = onCreateTeam,
                onToggleTeamStatus = onToggleTeamStatus,
                onDismissFeedback = onDismissFeedback
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Tactical Users and Operational Groups Section
        TacticalUserGroupSection(
            uiState = homeUiState,
            onRetry = onRetryTeams
        )

        Spacer(modifier = Modifier.height(24.dp))

        TacticalFeedbackBanner(
            title = "وضعیت پیاده‌سازی و یکپارچگی",
            message = "اطلاعات هویتی و فهرست گروه‌های عملیاتی مستقیماً از Backend فراخوانی شده است. نمونه آزمایشی نقشه با داده‌های شبیه‌سازی‌شده فعال است؛ موقعیت‌یاب زنده و مجوزهای مکانی در فازهای آینده پیاده‌سازی خواهند شد.",
            type = BannerType.INFO
        )

        Spacer(modifier = Modifier.height(32.dp))

        TacticalOutlinedButton(
            text = "خروج از حساب کاربری",
            onClick = onLogoutClicked,
            isDestructive = true
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}
