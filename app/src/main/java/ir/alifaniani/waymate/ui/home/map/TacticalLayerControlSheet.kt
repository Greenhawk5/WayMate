package ir.alifaniani.waymate.ui.home.map

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons

/**
 * Persian RTL Layer and Base Map Management Sheet.
 *
 * Provides granular user controls for:
 * 1. Base Map Presentation Selection (Offline Tactical, Standard Street, and unconfigured Satellite)
 * 2. Independent Vector Overlay Toggles (Jahrom road network, Waterways, Tactical Grid, Synthetic Markers)
 * 3. Clear feedback for unconfigured / unavailable services
 * 4. Attribution and licensing transparency
 */
@Composable
fun TacticalLayerControlSheet(
    uiState: TacticalMapUiState,
    onSelectBasePresentation: (BaseMapPresentation) -> Unit,
    onToggleLayer: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0E1520)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = TacticalIcons.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مدیریت لایه‌ها و کارتوگرافی نقشه",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF172234))
                        .border(1.dp, Color(0xFF283548), RoundedCornerShape(10.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TacticalIcons.Close,
                        contentDescription = "بستن مدیریت لایه‌ها",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "تنظیم نقشه پایه و مدیریت لایه‌های اطلاعاتی وکتور",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 1: Base Map Presentation Selector
            Text(
                text = "۱. انتخاب نقشه پایه",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(8.dp))

            BaseMapPresentation.entries.forEach { presentation ->
                val isSelected = uiState.basePresentation == presentation
                val isConfigured = presentation.isConfigured

                val borderColor = when {
                    isSelected && isConfigured -> MaterialTheme.colorScheme.primary
                    isSelected && !isConfigured -> Color(0xFFF57C00)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                }

                val backgroundColor = when {
                    isSelected && isConfigured -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    isSelected && !isConfigured -> Color(0xFFF57C00).copy(alpha = 0.12f)
                    else -> Color(0xFF151C24)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(backgroundColor)
                        .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(10.dp))
                        .clickable { onSelectBasePresentation(presentation) }
                        .padding(12.dp)
                ) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = presentation.titlePersian,
                                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                )

                                when {
                                    !isConfigured -> {
                                        TacticalStatusBadge(
                                            text = "پیکربندی‌نشده",
                                            tone = BadgeTone.WARNING
                                        )
                                    }
                                    presentation == BaseMapPresentation.OFFLINE_TACTICAL -> {
                                        TacticalStatusBadge(
                                            text = "۱۰۰٪ آفلاین",
                                            tone = BadgeTone.SUCCESS
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = presentation.subtitlePersian,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = TacticalIcons.Check,
                                contentDescription = null,
                                tint = if (isConfigured) MaterialTheme.colorScheme.primary else Color(0xFFF57C00),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Honest feedback banner if unconfigured presentation is selected
            if (!uiState.basePresentation.isConfigured) {
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.isSatelliteActive) {
                    TacticalFeedbackBanner(
                        title = "تصویر ماهواره‌ای پیکربندی نشده است",
                        message = "فایل‌های GIS ارائه‌شده صرفاً حاوی داده‌های وکتور معابر جهرم هستند و تصویر هوایی/ماهواره‌ای ندارند. فعال‌سازی این نما نیازمند منبع تصویربرداری (نظیر سنتینل یا ارائه‌دهنده تجاری) است. نقشه با داده‌های برداری محلی نمایش داده می‌شود.",
                        type = BannerType.WARNING
                    )
                } else if (uiState.basePresentation == BaseMapPresentation.STANDARD_STREET) {
                    TacticalFeedbackBanner(
                        title = "سرور کارتوگرافی خیابانی پیکربندی نشده است",
                        message = "سرورهای عمومی کاشی OpenStreetMap بر اساس مرام‌نامه OSMF استفاده آفلاین و دانلود دسته‌جمعی را ممنوع کرده‌اند. فعال‌سازی این نما نیازمند استقرار سرور تایل اختصاصی یا بسته آفلاین MBTiles است. نقشه با داده‌های برداری محلی نمایش داده می‌شود.",
                        type = BannerType.WARNING
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 2: Vector Overlay Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "۲. لایه‌های اطلاعاتی وکتور",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE2E8F0)
                )

                TacticalStatusBadge(
                    text = "${uiState.activeLayerCount} از ${uiState.layers.size} فعال",
                    tone = BadgeTone.SUCCESS
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            uiState.layers.forEach { layer ->
                val isAvailable = layer.isAvailable

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF151C24))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = layer.titlePersian,
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAvailable) Color.White else Color(0xFF64748B)
                            )

                            if (layer.featureCount != null) {
                                TacticalStatusBadge(
                                    text = "${layer.featureCount} عارضه",
                                    tone = BadgeTone.NEUTRAL
                                )
                            } else if (!isAvailable) {
                                TacticalStatusBadge(
                                    text = "فاز آینده",
                                    tone = BadgeTone.NEUTRAL
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = layer.descriptionPersian,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAvailable) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569),
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Switch(
                        checked = layer.isEnabled,
                        onCheckedChange = { onToggleLayer(layer.id) },
                        enabled = isAvailable,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = Color(0xFF94A3B8),
                            uncheckedTrackColor = Color(0xFF1E293B)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: Attribution & Sovereign Notice
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1015)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = Color(0xFF1E293B), shape = RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "حقوق داده‌ها و منبع کارتوگرافی:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• داده‌های وکتور معابر شهری جهرم استخراج‌شده و ممیزی‌شده (EPSG:4326)\n• نقشه‌های پایه آزاد © OpenStreetMap contributors تحت مجوز ODbL\n• موتور رندرینگ درون‌سازمانی با معماری مستقل و بدون وابستگی به APIهای تجاری",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TacticalOutlinedButton(
                text = "بستن و بازگشت به نقشه",
                onClick = onDismiss,
                leadingIcon = TacticalIcons.Close,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
