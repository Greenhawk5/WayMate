package ir.alifaniani.waymate.ui.home.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.HikingDifficulty
import ir.alifaniani.waymate.domain.model.HikingRoute
import ir.alifaniani.waymate.domain.model.defaultJahromHikingRoutes
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons

/**
 * Control panel for deterministic hiking route simulation.
 *
 * Provides safe playback controls (Play, Pause, Reset, Speed Multiplier, Route Switcher)
 * strictly for local UI testing and demonstration.
 */
@Composable
fun HikingSimulationBar(
    simulationState: HikingSimulationState,
    availableRoutes: List<HikingRoute> = defaultJahromHikingRoutes,
    onTogglePlayPause: () -> Unit,
    onReset: () -> Unit,
    onSelectRoute: (HikingRoute) -> Unit,
    onSetSpeedMultiplier: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFF00E5FF).copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xF5111927)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (simulationState.isPlaying) Color(0xFF00E5FF) else Color(0xFFFF9100))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "شبیه‌ساز پیمایش مسیر کوهنوردی",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TacticalStatusBadge(
                        text = if (simulationState.isFinished) "پایان مسیر" else if (simulationState.isPlaying) "در حال پیمایش" else "متوقف",
                        tone = if (simulationState.isPlaying) BadgeTone.SUCCESS else BadgeTone.NEUTRAL
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1A2536))
                            .border(1.dp, Color(0xFF283548), RoundedCornerShape(8.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TacticalIcons.Close,
                            contentDescription = "بستن شبیه‌ساز",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Synthetic Location Notice
            TacticalFeedbackBanner(
                title = "حالت موقعیت‌یابی مجازی",
                message = "این قابلیت صرفاً برای ارزیابی رابط کاربری و عملکرد نقشه طراحی شده است و هیچ داده مکانی واقعی ارسال نمی‌شود.",
                type = BannerType.INFO
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Route Selector Chips
            Text(
                text = "انتخاب مسیر کوه‌پیمایی:",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableRoutes.forEach { route ->
                    val isSelected = route.id == simulationState.route.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF1E3A5F) else Color(0xFF182232))
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF00E5FF) else Color(0xFF283548),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectRoute(route) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = route.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFCBD5E1),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Route Info & Difficulty Badge (weighted so difficulty badge never clips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${simulationState.route.name} • ${simulationState.route.lengthKm} ک‌م • شیب +${simulationState.route.elevationGainMeters}م",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE6EDF3),
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                TacticalStatusBadge(
                    text = simulationState.route.difficulty.displayNamePersian,
                    tone = when (simulationState.route.difficulty) {
                        HikingDifficulty.EASY -> BadgeTone.SUCCESS
                        HikingDifficulty.MODERATE -> BadgeTone.NEUTRAL
                        HikingDifficulty.DIFFICULT -> BadgeTone.WARNING
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { simulationState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF00E5FF),
                trackColor = Color(0xFF21262D)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetry & Waypoint Info (Separated cleanly so texts never overlap)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پیمایش: ${simulationState.formattedProgress}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1),
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
                Text(
                    text = "سمت: ${simulationState.currentBearing.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }

            simulationState.nextWaypoint?.let { wp ->
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = TacticalIcons.Location,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "ایستگاه بعد: ${wp.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB74D),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Controls: Play/Pause, Reset, Speed Multipliers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TacticalOutlinedButton(
                        text = if (simulationState.isPlaying) "توقف" else if (simulationState.isFinished) "تکرار" else "حرکت",
                        leadingIcon = if (simulationState.isPlaying) {
                            TacticalIcons.Pause
                        } else if (simulationState.isFinished) {
                            TacticalIcons.Replay
                        } else {
                            TacticalIcons.Play
                        },
                        onClick = onTogglePlayPause,
                        modifier = Modifier.wrapContentWidth()
                    )

                    TacticalOutlinedButton(
                        text = "مبدا",
                        leadingIcon = TacticalIcons.Replay,
                        onClick = onReset,
                        modifier = Modifier.wrapContentWidth()
                    )
                }

                // Speed Multiplier Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سرعت:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    listOf(1.0f, 2.0f, 5.0f).forEach { speed ->
                        val isSelected = simulationState.speedMultiplier == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E293B))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0xFF334155),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onSetSpeedMultiplier(speed) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${speed.toInt()}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color(0xFF0A101D) else Color(0xFFE2E8F0),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
