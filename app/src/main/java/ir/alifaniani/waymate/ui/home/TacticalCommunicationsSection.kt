package ir.alifaniani.waymate.ui.home

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tactical Communications & Operational Status Section (Phase 8).
 *
 * Implements:
 * 1. Connection Link Health Indicator (Online/Stale/Offline)
 * 2. Field Unit Operational Status Selector (Normal / Pursuing / Engaged)
 * 3. In-band Tactical Messaging & Announcements Feed with Read Confirmations
 * 4. Commander Order Dispatch Form & Live Team Telemetry Status Roster
 */
@Composable
fun TacticalCommunicationsSection(
    currentUser: User,
    uiState: HomeUiState,
    onStatusSelected: (OperationalStatus) -> Unit,
    onAcknowledgeMessage: (String) -> Unit,
    onSendMessage: (body: String, isUrgent: Boolean, targetTeamId: String?) -> Unit,
    onRefresh: () -> Unit,
    onSyncQueue: () -> Unit = {},
    onDismissSyncFeedback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isCommanderOrOwner = currentUser.role == UserRole.COMMANDER || currentUser.role == UserRole.OWNER
    var isComposeVisible by remember { mutableStateOf(false) }
    var draftMessageText by remember { mutableStateOf("") }
    var isDraftUrgent by remember { mutableStateOf(false) }

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
            // Header Row: Communications & Link Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Text(
                        text = "ارتباطات و وضعیت عملیاتی (فاز ۸ و ۱۰)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.lastSyncTimestamp > 0) {
                            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            "آخرین همگام‌سازی: ${timeFormat.format(Date(uiState.lastSyncTimestamp))} (پایش ۵ ثانیه)"
                        } else {
                            "در انتظار دریافت نخستین همگام‌سازی..."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TacticalStatusBadge(
                    text = uiState.connectionStatus.titlePersian,
                    tone = when (uiState.connectionStatus) {
                        ConnectionStatus.ONLINE -> BadgeTone.SUCCESS
                        ConnectionStatus.STALE -> BadgeTone.WARNING
                        ConnectionStatus.OFFLINE -> BadgeTone.ERROR
                    }
                )
            }

            // Phase 10: Offline Queue Banner
            if (uiState.offlineQueueCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2E2010))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = "${uiState.offlineQueueCount} عملیات در صف آفلاین محلی",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300)
                        )
                        Text(
                            text = "تغییرات ذخیره شده و پس از برقراری ارتباط ارسال می‌شوند",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                    TacticalOutlinedButton(
                        text = if (uiState.isSyncing) "در حال ارسال..." else "همگام‌سازی",
                        onClick = onSyncQueue,
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.wrapContentWidth()
                    )
                }
            }

            if (uiState.syncFeedbackMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TacticalFeedbackBanner(
                    title = "وضعیت همگام‌سازی صف محلی",
                    message = uiState.syncFeedbackMessage,
                    type = BannerType.INFO
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 1: Operational Status Selector (Team Lead & All Units)
            Text(
                text = "تعیین وضعیت عملیاتی واحد میدانی:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OperationalStatusChip(
                    title = "عادی (Normal)",
                    status = OperationalStatus.NORMAL,
                    isSelected = uiState.myOperationalStatus == OperationalStatus.NORMAL,
                    onClick = { onStatusSelected(OperationalStatus.NORMAL) },
                    modifier = Modifier.weight(1f)
                )

                OperationalStatusChip(
                    title = "تعقیب (Pursuing)",
                    status = OperationalStatus.PURSUING,
                    isSelected = uiState.myOperationalStatus == OperationalStatus.PURSUING,
                    onClick = { onStatusSelected(OperationalStatus.PURSUING) },
                    modifier = Modifier.weight(1f)
                )

                OperationalStatusChip(
                    title = "درگیر (Engaged)",
                    status = OperationalStatus.ENGAGED,
                    isSelected = uiState.myOperationalStatus == OperationalStatus.ENGAGED,
                    onClick = { onStatusSelected(OperationalStatus.ENGAGED) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 2: Tactical Messages & Announcements Feed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "پیام‌ها و دستورات عملیاتی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (uiState.unacknowledgedMessagesCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        TacticalStatusBadge(
                            text = "${uiState.unacknowledgedMessagesCount} جدید",
                            tone = BadgeTone.WARNING
                        )
                    }
                }

                if (isCommanderOrOwner) {
                    Text(
                        text = if (isComposeVisible) "انصراف" else "+ ارسال دستور جدید",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { isComposeVisible = !isComposeVisible }
                            .padding(4.dp)
                    )
                }
            }

            // Commander Order Composition Form (Expandable)
            AnimatedVisibility(visible = isComposeVisible && isCommanderOrOwner) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .background(Color(0xFF151C24), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "صدور دستور تاکتیکی جدید (ارسال به کلیه واحدها):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = draftMessageText,
                        onValueChange = { draftMessageText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "متن دستور یا اعلامیه تاکتیکی را وارد نمایید...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color(0xFFCBD5E1)
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Switch(
                                checked = isDraftUrgent,
                                onCheckedChange = { isDraftUrgent = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFD32F2F)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "دستور فوری / اولویت بالا",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isDraftUrgent) Color(0xFFFF5252) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        TacticalPrimaryButton(
                            text = "ارسال دستور",
                            onClick = {
                                if (draftMessageText.isNotBlank()) {
                                    onSendMessage(draftMessageText.trim(), isDraftUrgent, null)
                                    draftMessageText = ""
                                    isDraftUrgent = false
                                    isComposeVisible = false
                                }
                            },
                            enabled = draftMessageText.isNotBlank(),
                            modifier = Modifier.wrapContentWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Messages Feed List
            if (uiState.messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هیچ دستور عملیاتی فعالی ثبت نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.messages.take(5).forEach { msg ->
                        TacticalMessageCard(
                            message = msg,
                            onAcknowledge = { onAcknowledgeMessage(msg.id) }
                        )
                    }
                }
            }

            // SECTION 3: Live Units Status Roster (Commander Only)
            if (isCommanderOrOwner && uiState.liveStatuses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "وضعیت برخط واحدهای تحت امر:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.liveStatuses.forEach { unit ->
                        LiveUnitStatusRow(unit = unit)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TacticalOutlinedButton(
                text = "همگام‌سازی فوری وضعیت و پیام‌ها",
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OperationalStatusChip(
    title: String,
    status: OperationalStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) {
        when (status) {
            OperationalStatus.NORMAL -> Color(0xFF4CAF50)
            OperationalStatus.PURSUING -> Color(0xFFFFB300)
            OperationalStatus.ENGAGED -> Color(0xFFEF5350)
        }
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val backgroundColor = if (isSelected) {
        borderColor.copy(alpha = 0.15f)
    } else {
        Color(0xFF151C24)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TacticalMessageCard(
    message: TacticalMessage,
    onAcknowledge: () -> Unit
) {
    val isUrgent = message.isUrgent
    val isAcked = message.isAcknowledged

    val borderColor = when {
        isUrgent && !isAcked -> Color(0xFFD32F2F)
        !isAcked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    val backgroundColor = when {
        isUrgent && !isAcked -> Color(0xFF261010)
        !isAcked -> Color(0xFF121B24)
        else -> Color(0xFF0F141A)
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.createdAt))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.senderCallsign,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isUrgent) Color(0xFFFF8A80) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }

                when {
                    message.isPendingSync -> {
                        TacticalStatusBadge(
                            text = "در صف ارسال ⏳",
                            tone = BadgeTone.WARNING
                        )
                    }
                    isUrgent && !isAcked -> {
                        TacticalStatusBadge(
                            text = "دستور فوری",
                            tone = BadgeTone.ERROR
                        )
                    }
                    isAcked -> {
                        TacticalStatusBadge(
                            text = "تأییدشده ✓",
                            tone = BadgeTone.NEUTRAL
                        )
                    }
                    else -> {
                        TacticalStatusBadge(
                            text = "جدید",
                            tone = BadgeTone.WARNING
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message.body,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAcked) Color(0xFF94A3B8) else Color.White,
                lineHeight = 18.sp
            )

            if (!isAcked) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TacticalOutlinedButton(
                        text = "مشاهده شد / تایید دریافت",
                        onClick = onAcknowledge,
                        modifier = Modifier.wrapContentWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveUnitStatusRow(unit: TeamLiveStatus) {
    val statusColor = when (unit.operationalStatus) {
        OperationalStatus.NORMAL -> Color(0xFF4CAF50)
        OperationalStatus.PURSUING -> Color(0xFFFFB300)
        OperationalStatus.ENGAGED -> Color(0xFFEF5350)
    }

    val statusPersian = when (unit.operationalStatus) {
        OperationalStatus.NORMAL -> "عادی"
        OperationalStatus.PURSUING -> "تعقیب"
        OperationalStatus.ENGAGED -> "درگیر"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF151C24))
            .border(1.dp, Color(0xFF242E3B), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = "${unit.teamName} (${unit.callsign})",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Text(
                text = "وضعیت عملیاتی: $statusPersian",
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontSize = 10.sp
            )
        }

        TacticalStatusBadge(
            text = unit.connectionStatus.titlePersian,
            tone = when (unit.connectionStatus) {
                ConnectionStatus.ONLINE -> BadgeTone.SUCCESS
                ConnectionStatus.STALE -> BadgeTone.WARNING
                ConnectionStatus.OFFLINE -> BadgeTone.ERROR
            }
        )
    }
}
