package ir.alifaniani.waymate.ui.home.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import kotlin.math.cos
import kotlin.math.sin

/**
 * Interactive tactical map canvas supporting multi-layer rendering:
 * - Real GIS vector road lines and waterways
 * - Concentric range rings and spatial coordinate grid
 * - Interactive synthetic demonstration markers
 * - Transparent state indicators for unconfigured base map sources
 */
@Composable
fun TacticalMapCanvas(
    uiState: TacticalMapUiState,
    onMarkerSelected: (SyntheticMarker?) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterReset: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val successColor = Color(0xFF4CAF50)
    val warningColor = Color(0xFFFFB300)
    val neutralColor = Color(0xFF90A4AE)

    val roadPrimaryColor = Color(0xFFFF9800)
    val roadSecondaryColor = Color(0xFFFFC107)
    val roadLocalColor = Color(0xFF64748B)
    val waterwayColor = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(color = Color(0xFF0D1218), shape = RoundedCornerShape(10.dp))
            .border(width = 1.dp, color = outlineColor, shape = RoundedCornerShape(10.dp))
    ) {
        // Base Tactical Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uiState.markers, uiState.zoomLevel, uiState.isSyntheticUnitsEnabled) {
                    detectTapGestures { tapOffset ->
                        if (!uiState.isSyntheticUnitsEnabled) {
                            onMarkerSelected(null)
                            return@detectTapGestures
                        }

                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val scale = 35000f * (uiState.zoomLevel.toFloat() / 14f)
                        val cosLat = cos(Math.toRadians(uiState.centerLatitude)).toFloat()

                        // Check if tap hit any synthetic marker
                        val hitMarker = uiState.markers.find { marker ->
                            val dx = (marker.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                            val dy = -(marker.latitude - uiState.centerLatitude).toFloat() * scale
                            val markerX = centerX + dx
                            val markerY = centerY + dy
                            val distance = kotlin.math.hypot(tapOffset.x - markerX, tapOffset.y - markerY)
                            distance <= 40f
                        }
                        onMarkerSelected(hitMarker)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)
            val scale = 35000f * (uiState.zoomLevel.toFloat() / 14f)
            val cosLat = cos(Math.toRadians(uiState.centerLatitude)).toFloat()

            // 1. LAYER: Tactical Grid & Range Rings
            if (uiState.isTacticalGridEnabled) {
                val gridSpacing = 40.dp.toPx()
                val gridColor = Color(0xFF1E2836)

                var x = (center.x % gridSpacing)
                while (x < width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                var y = (center.y % gridSpacing)
                while (y < height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }

                // Range rings
                val maxRadius = kotlin.math.min(width, height) * 0.45f
                val ringSteps = 3
                for (i in 1..ringSteps) {
                    val r = maxRadius * (i.toFloat() / ringSteps.toFloat())
                    drawCircle(
                        color = Color(0xFF1E2836),
                        radius = r,
                        center = center,
                        style = Stroke(
                            width = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                }

                // Crosshair at center
                val crossLength = 14.dp.toPx()
                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = Offset(center.x - crossLength, center.y),
                    end = Offset(center.x + crossLength, center.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = Offset(center.x, center.y - crossLength),
                    end = Offset(center.x, center.y + crossLength),
                    strokeWidth = 1.5f
                )
            }

            // 2. LAYER: Real GIS Vector Road Lines & Waterways
            if (uiState.vectorSegments.isNotEmpty()) {
                val isRoadsEnabled = uiState.isRoadOverlayEnabled
                val isWaterwaysEnabled = uiState.isWaterwayOverlayEnabled

                val strokeWaterway = Stroke(width = 2.5f)
                val strokeRoadPrimary = Stroke(width = 2.8f)
                val strokeRoadSecondary = Stroke(width = 2.0f)
                val strokeRoadLocal = Stroke(width = 1.2f)

                // Viewport bounding box in geographical degrees (with safety padding)
                val halfLonSpan = (width / (2f * scale * cosLat)) * 1.15f
                val halfLatSpan = (height / (2f * scale)) * 1.15f
                val minLon = uiState.centerLongitude - halfLonSpan
                val maxLon = uiState.centerLongitude + halfLonSpan
                val minLat = uiState.centerLatitude - halfLatSpan
                val maxLat = uiState.centerLatitude + halfLatSpan

                for (segment in uiState.vectorSegments) {
                    if (segment.isWaterway && !isWaterwaysEnabled) continue
                    if (!segment.isWaterway && !isRoadsEnabled) continue

                    if (segment.points.size >= 2) {
                        // Quick viewport culling: skip segments completely outside visible bounds
                        var inViewport = false
                        for (pt in segment.points) {
                            if (pt.longitude in minLon..maxLon && pt.latitude in minLat..maxLat) {
                                inViewport = true
                                break
                            }
                        }
                        if (!inViewport) continue

                        val strokeColor = when {
                            segment.isWaterway -> waterwayColor
                            segment.roadType == "جاده اصلی" -> roadPrimaryColor
                            segment.roadType == "خیابان اصلی" -> roadSecondaryColor
                            else -> roadLocalColor
                        }

                        val strokeStyle = when {
                            segment.isWaterway -> strokeWaterway
                            segment.roadType == "جاده اصلی" -> strokeRoadPrimary
                            segment.roadType == "خیابان اصلی" -> strokeRoadSecondary
                            else -> strokeRoadLocal
                        }

                        val path = Path()
                        val p0 = segment.points[0]
                        val x0 = center.x + ((p0.longitude - uiState.centerLongitude).toFloat() * scale * cosLat)
                        val y0 = center.y - ((p0.latitude - uiState.centerLatitude).toFloat() * scale)
                        path.moveTo(x0, y0)

                        for (i in 1 until segment.points.size) {
                            val pt = segment.points[i]
                            val px = center.x + ((pt.longitude - uiState.centerLongitude).toFloat() * scale * cosLat)
                            val py = center.y - ((pt.latitude - uiState.centerLatitude).toFloat() * scale)
                            path.lineTo(px, py)
                        }

                        drawPath(
                            path = path,
                            color = strokeColor.copy(alpha = if (segment.isWaterway) 0.85f else 0.75f),
                            style = strokeStyle
                        )
                    }
                }
            }

            // 3. LAYER: Synthetic Demonstration Markers
            if (uiState.isSyntheticUnitsEnabled) {
                uiState.markers.forEach { marker ->
                    val dx = (marker.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                    val dy = -(marker.latitude - uiState.centerLatitude).toFloat() * scale
                    val markerPos = Offset(center.x + dx, center.y + dy)

                    val markerColor = when (marker.tone) {
                        BadgeTone.SUCCESS -> successColor
                        BadgeTone.WARNING -> warningColor
                        BadgeTone.NEUTRAL -> neutralColor
                        BadgeTone.ERROR -> Color(0xFFEF5350)
                    }

                    // Outer pulse/halo
                    drawCircle(
                        color = markerColor.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = markerPos
                    )

                    // Core marker
                    drawCircle(
                        color = markerColor,
                        radius = 6.dp.toPx(),
                        center = markerPos
                    )

                    // Selection indicator
                    if (uiState.selectedMarker?.id == marker.id) {
                        drawCircle(
                            color = Color.White,
                            radius = 20.dp.toPx(),
                            center = markerPos,
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                            )
                        )
                    }
                }
            }
        }

        // Unconfigured Base Map Notice Watermark
        if (!uiState.basePresentation.isConfigured) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color(0xFF261808), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFF57C00), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (uiState.isSatelliteActive) {
                        "سرویس ماهواره‌ای فعال نیست — نمایش لایه‌های وکتور آفلاین"
                    } else {
                        "سرور تایل خیابانی فعال نیست — نمایش لایه‌های وکتور آفلاین"
                    },
                    color = Color(0xFFFFB74D),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp
                )
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC0D1218)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "در حال بارگذاری عوارض وکتور جهرم...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        // Recoverable Error Overlay
        if (uiState.errorMessage != null && !uiState.isSatelliteActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE0D1218))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "خطا در پردازش نقشه",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF5350)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TacticalOutlinedButton(
                        text = "بازنشانی به حالت پایه",
                        onClick = onRetry
                    )
                }
            }
        }

        // Map HUD: Top Coordinate & Mode Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .background(Color(0xCC151B22), shape = RoundedCornerShape(6.dp))
                .border(1.dp, Color(0xFF30363D), shape = RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = uiState.formattedCoordinates,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFE6EDF3),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.size(6.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(successColor, shape = CircleShape)
            )
        }

        // Map HUD: Controls Bar (Zoom, Reset Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xEE151B22), shape = RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF30363D), shape = RoundedCornerShape(6.dp))
                    .clickable { onZoomIn() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xEE151B22), shape = RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF30363D), shape = RoundedCornerShape(6.dp))
                    .clickable { onZoomOut() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xEE151B22), shape = RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFF30363D), shape = RoundedCornerShape(6.dp))
                    .clickable { onCenterReset() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⌖",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        // Map HUD: Selected Marker Detail Bottom Sheet/Pill
        uiState.selectedMarker?.let { marker ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .fillMaxWidth(0.75f)
                    .background(Color(0xEE151B22), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, primaryColor, shape = RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = marker.callsign,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TacticalStatusBadge(
                            text = "شبیه‌سازی",
                            tone = marker.tone
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = marker.unitDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
