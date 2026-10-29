package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge

/**
 * Tactical user and group roster presentation component.
 *
 * Displays live, read-only team data fetched from the backend API,
 * with explicit states for loading, success, empty list, and recoverable errors.
 * All demonstration records are explicitly identified as synthetic data.
 */
@Composable
fun TacticalUserGroupSection(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "فهرست کاربران و گروه‌های عملیاتی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "واحدهای مستقر در حوزه ماموریت تاکتیکی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val headerBadge = when {
                    uiState.isLoading -> "در حال بارگذاری..." to BadgeTone.WARNING
                    uiState.isForbidden -> "دسترسی محدود" to BadgeTone.WARNING
                    uiState.errorMessage != null -> "خطای اتصال" to BadgeTone.ERROR
                    uiState.isEmpty -> "بدون واحد" to BadgeTone.NEUTRAL
                    else -> "${uiState.teams.size} گروه فعال" to BadgeTone.SUCCESS
                }

                TacticalStatusBadge(
                    text = headerBadge.first,
                    tone = headerBadge.second
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Synthetic Demonstration Tag
            TacticalStatusBadge(
                text = "داده‌های آزمایشی توسعه (Synthetic Dev Data)",
                tone = BadgeTone.NEUTRAL
            )

            Spacer(modifier = Modifier.height(16.dp))

            when {
                uiState.isLoading -> {
                    // Loading State
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "در حال دریافت اطلاعات تیم‌های عملیاتی از سرور...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.isForbidden -> {
                    // Recoverable Forbidden Role State
                    TacticalFeedbackBanner(
                        title = "محدودیت سطح دسترسی",
                        message = uiState.errorMessage ?: "مشاهده فهرست جامع تیم‌ها مخصوص نقش‌های فرماندهی و مدیریت سامانه است.",
                        type = BannerType.WARNING
                    )
                }

                uiState.errorMessage != null -> {
                    // Recoverable Error State
                    Column {
                        TacticalFeedbackBanner(
                            title = "خطا در برقراری ارتباط با سرور",
                            message = uiState.errorMessage,
                            type = BannerType.ERROR
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TacticalOutlinedButton(
                            text = "تلاش مجدد برای دریافت اطلاعات",
                            onClick = onRetry
                        )
                    }
                }

                uiState.isEmpty -> {
                    // Empty State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "هیچ تیم عملیاتی فعالی در پایگاه ثبت نشده است.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "در حال حاضر هیچ واحد گشتی در دسترس نیست.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TacticalOutlinedButton(
                            text = "به‌روزرسانی",
                            onClick = onRetry
                        )
                    }
                }

                else -> {
                    // Success State: Live Teams from Backend
                    uiState.teams.forEachIndexed { index, team ->
                        if (index > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        TeamCard(team = team)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamCard(team: Team) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (team.vehicleType.isNotBlank() || team.vehiclePlate.isNotBlank()) {
                    Text(
                        text = "${team.vehicleType} — پلاک تاکتیکی ${team.vehiclePlate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            TacticalStatusBadge(
                text = if (team.isActive) "فعال" else "غیرفعال",
                tone = if (team.isActive) BadgeTone.SUCCESS else BadgeTone.NEUTRAL
            )
        }

        if (team.equipmentSummary.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "تجهیزات: ${team.equipmentSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (team.members.isNotEmpty()) {
            team.members.forEach { member ->
                MemberRow(member = member)
            }
        } else {
            Text(
                text = "${team.memberCount} عضو سازمانی در این واحد",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemberRow(member: TeamMember) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${member.callsign} (${member.fullName})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = member.roleInTeam,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TacticalStatusBadge(
            text = "برخط",
            tone = BadgeTone.SUCCESS
        )
    }
}
