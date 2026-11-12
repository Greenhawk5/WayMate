package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.service.WayMateNotificationManager
import ir.alifaniani.waymate.ui.designsystem.TacticalActionButton
import ir.alifaniani.waymate.ui.designsystem.TacticalAreaChart
import ir.alifaniani.waymate.ui.designsystem.TacticalCard
import ir.alifaniani.waymate.ui.designsystem.TacticalChartPoint
import ir.alifaniani.waymate.ui.designsystem.TacticalConfirmDialog
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.designsystem.TacticalSectionHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalStatChip
import ir.alifaniani.waymate.ui.designsystem.TacticalStatusPill
import ir.alifaniani.waymate.ui.designsystem.TacticalTone
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateBorderSubtle
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateError
import ir.alifaniani.waymate.ui.theme.WayMateErrorContainer
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateOnCyan
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextMuted
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary
import ir.alifaniani.waymate.ui.theme.WayMateWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated Destination B: Operational Overview.
 *
 * Provides clear situational awareness:
 * - Current tactical identity, callsign, and role badge
 * - Operational Status selector (Normal, Pursuing, Engaged)
 * - Consent-based location sharing status & controls
 * - Operational metrics summary (Active teams, Sharers, Points, Offline queue)
 * - Elevation & telemetry area chart (EvilCharts style)
 * - Recent in-band tactical messages snippet
 * - Safety-confirmed Emergency SOS trigger
 */
@Composable
fun OverviewScreen(
    user: User,
    homeUiState: HomeUiState,
    onStatusSelected: (OperationalStatus) -> Unit,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    onSyncQueue: () -> Unit,
    onAcknowledgeMessage: (String) -> Unit,
    onNavigateToOperations: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifManager = remember { WayMateNotificationManager(context) }
    var showSosConfirmDialog by remember { mutableStateOf(false) }

    val elevationChartPoints = remember {
        listOf(
            TacticalChartPoint(label = "پایگاه", value = 1080f, formattedValue = "۱۰۸۰ متر"),
            TacticalChartPoint(label = "دره نخل", value = 1240f, formattedValue = "۱۲۴۰ متر"),
            TacticalChartPoint(label = "چشمه", value = 1410f, formattedValue = "۱۴۱۰ متر"),
            TacticalChartPoint(label = "جان‌پناه", value = 1650f, formattedValue = "۱۶۵۰ متر"),
            TacticalChartPoint(label = "خط‌الراس", value = 1810f, formattedValue = "۱۸۱۰ متر"),
            TacticalChartPoint(label = "قله البرز", value = 1920f, formattedValue = "۱۹۲۰ متر")
        )
    }

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
            // Top Bar: Callsign & System Header (No truncation on callsign)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_waymate_logo),
                        contentDescription = "نشان سامانه",
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "پایگاه عملیاتی وی‌میت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = WayMateTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = user.callsign,
                            style = MaterialTheme.typography.bodySmall,
                            color = WayMateCyan,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                TacticalStatusPill(
                    text = "ارتباط فعال",
                    tone = TacticalTone.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Operational Status Selector Card (Two-line buttons so nothing clips)
            TacticalCard(shape = RoundedCornerShape(18.dp)) {
                Text(
                    text = "تعیین وضعیت عملیاتی واحد میدانی",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = WayMateTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentStatus = homeUiState.myOperationalStatus
                    OperationalStatus.entries.forEach { status ->
                        val isSelected = currentStatus == status
                        val (persianTitle, subtitleCode, accentColor) = when (status) {
                            OperationalStatus.NORMAL -> Triple("عادی", "NORMAL", WayMateCyan)
                            OperationalStatus.PURSUING -> Triple("تعقیب و پایش", "PURSUIT", WayMateWarning)
                            OperationalStatus.ENGAGED -> Triple("درگیر عملیات", "ENGAGED", WayMateError)
                        }

                        Button(
                            onClick = { onStatusSelected(status) },
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) accentColor else WayMateSurfaceVariant,
                                contentColor = if (isSelected) WayMateMidnightDark else WayMateTextPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else WayMateBorderSubtle
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = persianTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = subtitleCode,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) {
                                        WayMateMidnightDark.copy(alpha = 0.78f)
                                    } else {
                                        WayMateTextSecondary
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Location Sharing Card
            LocationSharingControlCard(
                sharingState = homeUiState.locationSharingState,
                onStartSharing = onStartSharing,
                onStopSharing = onStopSharing
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Operational Metrics 2x2 Grid
            TacticalSectionHeader(
                title = "شاخص‌های وضعیت عملیات",
                actionText = "همگام‌سازی",
                onActionClick = onSyncQueue
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TacticalStatChip(
                    label = "گروه‌های فعال",
                    value = "${homeUiState.teams.size} گروه",
                    icon = TacticalIcons.Teams,
                    modifier = Modifier.weight(1f)
                )
                TacticalStatChip(
                    label = "اعضای فعال در موقعیت",
                    value = "${homeUiState.locationSharingState.activeSharers.size} نفر",
                    icon = TacticalIcons.Location,
                    highlight = homeUiState.locationSharingState.activeSharers.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TacticalStatChip(
                    label = "نقاط نشانه (POI)",
                    value = "${homeUiState.points.size} نقطه",
                    icon = TacticalIcons.TacticalMap,
                    modifier = Modifier.weight(1f)
                )
                TacticalStatChip(
                    label = "صف ذخیره آفلاین",
                    value = "${homeUiState.offlineQueueCount} معوق",
                    icon = TacticalIcons.Clock,
                    highlight = homeUiState.offlineQueueCount > 0,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // EvilCharts-Inspired Area Chart: Operational Elevation & Telemetry Profile
            TacticalAreaChart(
                title = "پروفایل ارتفاعی محور عملیاتی البرز جهرم",
                subtitle = "نقاط ارتفاعی مسیر صعود و ایستگاه‌های پایش رادیویی (متر از سطح دریا)",
                points = elevationChartPoints,
                unitLabel = "متر",
                trendBadgeText = "اختلاف ارتفاع صعود: +۸۴۰ متر • پوشش کامل وکتور"
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Recent Tactical Message Snippet
            TacticalSectionHeader(
                title = "آخرین پیام‌های عملیاتی",
                badgeText = "${homeUiState.messages.size} پیام",
                actionText = "مشاهده همه",
                onActionClick = onNavigateToOperations
            )

            if (homeUiState.messages.isEmpty()) {
                TacticalCard(shape = RoundedCornerShape(14.dp)) {
                    Text(
                        text = "هیچ پیام عملیاتی جدیدی در صف ارتباطات موجود نیست.",
                        fontSize = 12.sp,
                        color = WayMateTextMuted
                    )
                }
            } else {
                val latestMessage = homeUiState.messages.first()
                TacticalCard(
                    shape = RoundedCornerShape(14.dp),
                    borderColor = if (latestMessage.isUrgent) WayMateError.copy(alpha = 0.5f) else WayMateBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val timeStr = remember(latestMessage.createdAt) {
                            if (latestMessage.createdAt > 0) {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latestMessage.createdAt))
                            } else "تاکتیکی"
                        }
                        Text(
                            text = "${latestMessage.senderCallsign} • $timeStr",
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (latestMessage.isUrgent) WayMateError else WayMateCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (latestMessage.isAcknowledged) {
                            TacticalStatusPill(text = "تأییدشده", tone = TacticalTone.SUCCESS)
                        } else {
                            TacticalActionButton(
                                text = "تأیید پیام",
                                icon = TacticalIcons.Check,
                                onClick = { onAcknowledgeMessage(latestMessage.id) },
                                isPrimary = false,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = latestMessage.body,
                        fontSize = 13.sp,
                        color = WayMateTextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Emergency Tactical SOS Alert Broadcast Button (Emoji replaced with Kubai Sos vector icon)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WayMateErrorContainer.copy(alpha = 0.35f))
                    .border(1.dp, WayMateError.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "هشدار فوری امداد میدانی (SOS)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WayMateError
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "ارسال بلادرنگ سیگنال اضطراری به کلیه اعضای گروه و فرماندهی",
                            fontSize = 11.sp,
                            color = WayMateTextSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    TacticalActionButton(
                        text = "اعلام SOS",
                        icon = TacticalIcons.Sos,
                        onClick = { showSosConfirmDialog = true },
                        isPrimary = false,
                        isDestructive = true,
                        modifier = Modifier.height(44.dp)
                    )
                }
            }

            // Bottom Spacing to ensure complete scroll clearance above the circular navigation dock
            Spacer(modifier = Modifier.height(148.dp).navigationBarsPadding())
        }

        // Safety Confirmation Dialog for Emergency SOS
        if (showSosConfirmDialog) {
            TacticalConfirmDialog(
                title = "تأیید اعلام وضعیت اضطراری SOS",
                message = "آیا از ارسال هشدار فوری امدادی اطمینان دارید؟ این پیام موقعیت مکانی و وضعیت بحرانی شما را به کلیه گشت‌ها اعلام می‌کند.",
                confirmText = "ارسال فوری SOS",
                dismissText = "انصراف",
                isDestructive = true,
                onConfirm = {
                    showSosConfirmDialog = false
                    notifManager.showEmergencySosNotification(
                        callsign = user.callsign,
                        details = "درخواست فوری پشتیبانی امدادی در مختصات شهرستان جهرم."
                    )
                },
                onDismiss = { showSosConfirmDialog = false }
            )
        }
    }
}
