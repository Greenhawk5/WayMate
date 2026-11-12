package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.service.TacticalNotificationType
import ir.alifaniani.waymate.service.WayMateNotificationManager
import ir.alifaniani.waymate.ui.designsystem.TacticalActionButton
import ir.alifaniani.waymate.ui.designsystem.TacticalCard
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.designsystem.TacticalScreenHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalSectionHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalStatusPill
import ir.alifaniani.waymate.ui.designsystem.TacticalTone
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary

/**
 * Dedicated Destination D: Operations & Communications.
 *
 * Consolidates in-band communications:
 * 1. Tactical messaging feed with read confirmations
 * 2. Tactical message compose & dispatch form
 * 3. Operational status selector
 * 4. Subordinate unit link roster & telemetry
 * 5. Tactical notification ecosystem diagnostics & emergency triggers
 */
@Composable
fun OperationsScreen(
    user: User,
    homeUiState: HomeUiState,
    onStatusSelected: (OperationalStatus) -> Unit,
    onAcknowledgeMessage: (String) -> Unit,
    onSendMessage: (body: String, isUrgent: Boolean, targetTeamId: String?) -> Unit,
    onRefresh: () -> Unit,
    onSyncQueue: () -> Unit,
    onDismissSyncFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifManager = remember { WayMateNotificationManager(context) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Screen Header
            TacticalScreenHeader(
                title = "ارتباطات و عملیات تاکتیکی",
                subtitle = "تبادل پیام، فرامین میدانی و اعلان‌های هشدار",
                trailingContent = {
                    TacticalStatusPill(
                        text = "پایش آنلاین",
                        tone = TacticalTone.SUCCESS
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Communications & Messages Feed
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

            // Tactical Notification Ecosystem Diagnostics
            TacticalSectionHeader(
                title = "سامانه مدیریت و آزمون اعلان‌های تاکتیکی",
                badgeText = "۱۵ آیکون اختصاصی"
            )

            TacticalCard(shape = RoundedCornerShape(16.dp)) {
                Text(
                    text = "ارسال پیام و هشدارهای آزمایشی به نوار اعلان‌های اندروید",
                    fontSize = 12.sp,
                    color = WayMateTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticalActionButton(
                        text = "دستور فوری",
                        icon = TacticalIcons.Alert,
                        onClick = {
                            notifManager.showTacticalMessageNotification(
                                senderCallsign = user.callsign,
                                body = "دستور عملیاتی: فوراً به مختصات چاتیز جهرم تغییر موضع دهید.",
                                isUrgent = true
                            )
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )

                    TacticalActionButton(
                        text = "امداد SOS",
                        icon = TacticalIcons.Sos,
                        onClick = {
                            notifManager.showEmergencySosNotification(
                                callsign = user.callsign,
                                details = "درخواست فوری پشتیبانی امدادی در شیب شمالی قلات جهرم."
                            )
                        },
                        isPrimary = false,
                        isDestructive = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TacticalActionButton(
                        text = "رادار همنورد",
                        icon = TacticalIcons.Radar,
                        onClick = {
                            notifManager.showProximityRadarNotification(
                                callsign = "SCOUT-REZA",
                                distanceMeters = 35
                            )
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )

                    TacticalActionButton(
                        text = "فتح نشانه",
                        icon = TacticalIcons.Mountain,
                        onClick = {
                            notifManager.showSummitWaypointNotification(
                                landmarkName = "قله چاتیز جهرم",
                                altitudeMeters = 1680
                            )
                        },
                        isPrimary = false,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                TacticalActionButton(
                    text = "ارسال آزمایشی کلیه ۱۵ اعلان تاکتیکی",
                    icon = TacticalIcons.Send,
                    onClick = {
                        TacticalNotificationType.entries.forEach { type ->
                            notifManager.showNotification(
                                type = type,
                                title = "اعلان تاکتیکی: ${type.name}",
                                body = "آزمون رندر آیکون مونوکروم و نشان رنگی در اندروید (کد شناسه ${type.notificationId})"
                            )
                        }
                    },
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Safe bottom margin ensuring complete scroll clearance above circular navigation bar
            Spacer(modifier = Modifier.height(148.dp).navigationBarsPadding())
        }
    }
}

