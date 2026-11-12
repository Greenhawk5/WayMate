package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.ui.designsystem.TacticalActionButton
import ir.alifaniani.waymate.ui.designsystem.TacticalCard
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.designsystem.TacticalScreenHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalSectionHeader
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceHighest
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextMuted
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary

/**
 * Dedicated Tactical Settings Screen.
 * Exposes real operational, navigation, and notification preferences.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onSyncQueue: () -> Unit,
    offlineQueueCount: Int,
    modifier: Modifier = Modifier
) {
    var highAccuracyGps by remember { mutableStateOf(true) }
    var showCoordinatesGrid by remember { mutableStateOf(true) }
    var soundAlertsEnabled by remember { mutableStateOf(true) }
    var vibrationAlertsEnabled by remember { mutableStateOf(true) }
    var autoSyncOfflineQueue by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            TacticalScreenHeader(
                title = "تنظیمات سامانه",
                subtitle = "پیکربندی موقعیت‌یابی، نقشه و اعلان‌ها",
                trailingContent = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WayMateMidnightSurface)
                            .border(1.dp, WayMateBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = TacticalIcons.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = WayMateCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1: Location & Telemetry
            TacticalSectionHeader(title = "موقعیت‌یابی و ناوبری")

            TacticalCard(shape = RoundedCornerShape(16.dp)) {
                SettingsToggleRow(
                    title = "دقت بالای GPS (Fused Location)",
                    description = "استفاده همزمان از ماهواره و سنسورهای حرکتی در عملیات کوهستانی",
                    checked = highAccuracyGps,
                    onCheckedChange = { highAccuracyGps = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "نمایش مختصات MGRS / جغرافیایی",
                    description = "نمایش مداوم برچسب مختصات روی بوم نقشه",
                    checked = showCoordinatesGrid,
                    onCheckedChange = { showCoordinatesGrid = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Notifications & Emergency Alerts
            TacticalSectionHeader(title = "اعلان‌ها و هشدارهای اضطراری")

            TacticalCard(shape = RoundedCornerShape(16.dp)) {
                SettingsToggleRow(
                    title = "صدای اعلان دستورات و هشدارهای فوری",
                    description = "پخش هشدار صوتی برای پیام‌های اضطراری SOS و دستورات فوری",
                    checked = soundAlertsEnabled,
                    onCheckedChange = { soundAlertsEnabled = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingsToggleRow(
                    title = "لرزش تاکتیکی (Haptic Feedback)",
                    description = "لرزش هنگام نزدیک شدن به محدوده رادار همنورد",
                    checked = vibrationAlertsEnabled,
                    onCheckedChange = { vibrationAlertsEnabled = it }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Data & Offline Storage
            TacticalSectionHeader(title = "ذخیره‌سازی و صف آفلاین")

            TacticalCard(shape = RoundedCornerShape(16.dp)) {
                SettingsToggleRow(
                    title = "ارسال خودکار صف آفلاین در برقراری شبکه",
                    description = "همگام‌سازی بلافاصله موقعیت‌ها و پیام‌های معوق با سرور ابری D1",
                    checked = autoSyncOfflineQueue,
                    onCheckedChange = { autoSyncOfflineQueue = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اقلام معوق صف ذخیره محلی",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WayMateTextPrimary
                        )
                        Text(
                            text = "$offlineQueueCount مورد ذخیره شده در پایگاه محلی SQLite",
                            fontSize = 11.sp,
                            color = WayMateTextMuted
                        )
                    }

                    TacticalActionButton(
                        text = "همگام‌سازی فوری",
                        onClick = onSyncQueue,
                        isPrimary = false,
                        modifier = Modifier.height(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = WayMateTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = WayMateTextSecondary,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = WayMateCyan,
                checkedTrackColor = WayMateSurfaceHighest,
                uncheckedThumbColor = WayMateTextSecondary,
                uncheckedTrackColor = WayMateSurfaceVariant
            )
        )
    }
}

