package ir.alifaniani.waymate.ui.home.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.ui.common.TacticalEmptyStateCard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.HikingRoute
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.model.WaypointType
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.designsystem.RadarLoader
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Interactive tactical map canvas supporting multi-layer rendering:
 * - Real GIS vector road lines and waterways
 * - Concentric range rings and spatial coordinate grid
 * - Interactive synthetic demonstration markers
 * - Generic shared Points of Interest (POI)
 * - Hiking routes and waypoints
 * - Simulated hiker movement avatar with directional bearing
 * - Real-time consenting group member live locations
 * - Smooth touch pan and drag behavior
 * - Transparent state indicators for unconfigured base map sources
 */
@Composable
fun TacticalMapCanvas(
    uiState: TacticalMapUiState,
    onMarkerSelected: (SyntheticMarker?) -> Unit,
    onPointOfInterestSelected: (PointOfInterest?) -> Unit = {},
    onGroupMemberSelected: (GroupMemberLocation?) -> Unit = {},
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onCenterReset: () -> Unit,
    onRetry: () -> Unit,
    showInternalCoordinatesHud: Boolean = true,
    controlsBottomPadding: Dp = 10.dp,
    modifier: Modifier = Modifier
) {
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var userZoomScale by remember { mutableFloatStateOf(1f) }
    var userRotationDegrees by remember { mutableFloatStateOf(0f) }
    val reusableSegmentPath = remember { Path() }

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

    val zoomLevelMultiplier = remember(uiState.zoomLevel) {
        1.22f.pow((uiState.zoomLevel - 14).toFloat())
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color = Color(0xFF0D1218), shape = RoundedCornerShape(10.dp))
            .border(width = 1.dp, color = outlineColor, shape = RoundedCornerShape(10.dp))
    ) {
        // Base Tactical Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures(panZoomLock = false) { _, pan, zoom, rotation ->
                        val nextZoomScale = (userZoomScale * zoom).coerceIn(0.4f, 4.0f)
                        val appliedZoomRatio = nextZoomScale / userZoomScale
                        panOffset = (panOffset * appliedZoomRatio) + pan
                        userZoomScale = nextZoomScale
                        userRotationDegrees = (userRotationDegrees + rotation) % 360f
                    }
                }
                .pointerInput(
                    uiState.markers,
                    uiState.pointsOfInterest,
                    uiState.groupMemberLocations,
                    uiState.zoomLevel,
                    uiState.isSyntheticUnitsEnabled,
                    uiState.isPointsOfInterestEnabled,
                    uiState.isGroupLocationsEnabled,
                    panOffset,
                    userZoomScale,
                    userRotationDegrees
                ) {
                    detectTapGestures { tapOffset ->
                        val effectiveZoom = zoomLevelMultiplier * userZoomScale
                        val scale = 35000f * effectiveZoom
                        val cosLat = cos(Math.toRadians(uiState.centerLatitude)).toFloat()

                        val centerX = (size.width / 2f) + panOffset.x
                        val centerY = (size.height / 2f) + panOffset.y
                        val center = Offset(centerX, centerY)

                        // Convert tapOffset into unrotated coordinate space relative to center
                        val tapRad = Math.toRadians(userRotationDegrees.toDouble())
                        val dTap = tapOffset - center
                        val unrotX = (dTap.x * cos(tapRad) + dTap.y * sin(tapRad)).toFloat()
                        val unrotY = (-dTap.x * sin(tapRad) + dTap.y * cos(tapRad)).toFloat()
                        val unrotatedTap = Offset(center.x + unrotX, center.y + unrotY)

                        val hitRadius = 56f

                        // 1. Check if tap hit any live group member location
                        if (uiState.isGroupLocationsEnabled) {
                            val hitMember = uiState.groupMemberLocations.find { member ->
                                val dx = (member.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                                val dy = -(member.latitude - uiState.centerLatitude).toFloat() * scale
                                val memX = centerX + dx
                                val memY = centerY + dy
                                val distance = kotlin.math.hypot(unrotatedTap.x - memX, unrotatedTap.y - memY)
                                distance <= hitRadius
                            }
                            if (hitMember != null) {
                                onMarkerSelected(null)
                                onPointOfInterestSelected(null)
                                onGroupMemberSelected(hitMember)
                                return@detectTapGestures
                            }
                        }

                        // 2. Check if tap hit any POI
                        if (uiState.isPointsOfInterestEnabled) {
                            val hitPoi = uiState.pointsOfInterest.find { poi ->
                                val dx = (poi.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                                val dy = -(poi.latitude - uiState.centerLatitude).toFloat() * scale
                                val poiX = centerX + dx
                                val poiY = centerY + dy
                                val distance = kotlin.math.hypot(unrotatedTap.x - poiX, unrotatedTap.y - poiY)
                                distance <= hitRadius
                            }
                            if (hitPoi != null) {
                                onMarkerSelected(null)
                                onGroupMemberSelected(null)
                                onPointOfInterestSelected(hitPoi)
                                return@detectTapGestures
                            }
                        }

                        // 3. Check if tap hit any synthetic marker
                        if (uiState.isSyntheticUnitsEnabled) {
                            val hitMarker = uiState.markers.find { marker ->
                                val dx = (marker.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                                val dy = -(marker.latitude - uiState.centerLatitude).toFloat() * scale
                                val markerX = centerX + dx
                                val markerY = centerY + dy
                                val distance = kotlin.math.hypot(unrotatedTap.x - markerX, unrotatedTap.y - markerY)
                                distance <= hitRadius
                            }
                            if (hitMarker != null) {
                                onPointOfInterestSelected(null)
                                onGroupMemberSelected(null)
                                onMarkerSelected(hitMarker)
                                return@detectTapGestures
                            }
                        }

                        // Tap in empty area: deselect all
                        onMarkerSelected(null)
                        onPointOfInterestSelected(null)
                        onGroupMemberSelected(null)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f + panOffset.x, height / 2f + panOffset.y)
            val effectiveZoom = zoomLevelMultiplier * userZoomScale
            val scale = 35000f * effectiveZoom
            val cosLat = cos(Math.toRadians(uiState.centerLatitude)).toFloat()

            clipRect(0f, 0f, width, height) {
                rotate(degrees = userRotationDegrees, pivot = center) {

            // 1. LAYER: Tactical Grid & Range Rings
            if (uiState.isTacticalGridEnabled) {
                val gridSpacing = 40.dp.toPx()
                val gridColor = Color(0xFF1E2836)

                var x = ((center.x % gridSpacing) + gridSpacing) % gridSpacing
                while (x < width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1f
                    )
                    x += gridSpacing
                }

                var y = ((center.y % gridSpacing) + gridSpacing) % gridSpacing
                while (y < height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                    y += gridSpacing
                }

                // Range rings centered on visible screen center
                val screenCenter = Offset(width / 2f, height / 2f)
                val maxRadius = kotlin.math.min(width, height) * 0.45f
                val ringSteps = 3
                for (i in 1..ringSteps) {
                    val r = maxRadius * (i.toFloat() / ringSteps.toFloat())
                    drawCircle(
                        color = Color(0xFF1E2836),
                        radius = r,
                        center = screenCenter,
                        style = Stroke(
                            width = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    )
                }

                // Crosshair at screen center
                val crossLength = 14.dp.toPx()
                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = Offset(screenCenter.x - crossLength, screenCenter.y),
                    end = Offset(screenCenter.x + crossLength, screenCenter.y),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = primaryColor.copy(alpha = 0.7f),
                    start = Offset(screenCenter.x, screenCenter.y - crossLength),
                    end = Offset(screenCenter.x, screenCenter.y + crossLength),
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

                // Compute geographical center of the current panned screen viewport
                val viewportCenterLon = uiState.centerLongitude - (panOffset.x / (scale * cosLat)).toDouble()
                val viewportCenterLat = uiState.centerLatitude + (panOffset.y / scale).toDouble()
                val maxDim = kotlin.math.max(width, height)
                val halfLonSpan = (maxDim / (2f * scale * cosLat)) * 1.65f
                val halfLatSpan = (maxDim / (2f * scale)) * 1.65f
                val minLon = viewportCenterLon - halfLonSpan
                val maxLon = viewportCenterLon + halfLonSpan
                val minLat = viewportCenterLat - halfLatSpan
                val maxLat = viewportCenterLat + halfLatSpan

                for (segment in uiState.vectorSegments) {
                    if (segment.isWaterway && !isWaterwaysEnabled) continue
                    if (!segment.isWaterway && !isRoadsEnabled) continue

                    if (segment.points.size >= 2) {
                        // Bounding-box intersection culling so zoomed-in long segments never vanish
                        var segMinLon = Double.POSITIVE_INFINITY
                        var segMaxLon = Double.NEGATIVE_INFINITY
                        var segMinLat = Double.POSITIVE_INFINITY
                        var segMaxLat = Double.NEGATIVE_INFINITY
                        for (pt in segment.points) {
                            if (pt.longitude < segMinLon) segMinLon = pt.longitude
                            if (pt.longitude > segMaxLon) segMaxLon = pt.longitude
                            if (pt.latitude < segMinLat) segMinLat = pt.latitude
                            if (pt.latitude > segMaxLat) segMaxLat = pt.latitude
                        }
                        val intersectsViewport = segMaxLon >= minLon && segMinLon <= maxLon &&
                            segMaxLat >= minLat && segMinLat <= maxLat
                        if (!intersectsViewport) continue

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

                        reusableSegmentPath.reset()
                        val p0 = segment.points[0]
                        val x0 = center.x + ((p0.longitude - uiState.centerLongitude).toFloat() * scale * cosLat)
                        val y0 = center.y - ((p0.latitude - uiState.centerLatitude).toFloat() * scale)
                        reusableSegmentPath.moveTo(x0, y0)

                        for (i in 1 until segment.points.size) {
                            val pt = segment.points[i]
                            val px = center.x + ((pt.longitude - uiState.centerLongitude).toFloat() * scale * cosLat)
                            val py = center.y - ((pt.latitude - uiState.centerLatitude).toFloat() * scale)
                            reusableSegmentPath.lineTo(px, py)
                        }

                        drawPath(
                            path = reusableSegmentPath,
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

            // 4. LAYER: Shared Group Points of Interest (POI)
            if (uiState.isPointsOfInterestEnabled) {
                uiState.pointsOfInterest.forEach { poi ->
                    val dx = (poi.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                    val dy = -(poi.latitude - uiState.centerLatitude).toFloat() * scale
                    val poiPos = Offset(center.x + dx, center.y + dy)

                    val poiColor = when (poi.category) {
                        PointCategory.START_POINT -> Color(0xFF00E676)
                        PointCategory.DESTINATION -> Color(0xFFFFD600)
                        PointCategory.CAMPSITE -> Color(0xFFFF9100)
                        PointCategory.REST_AREA -> Color(0xFF29B6F6)
                        PointCategory.WATER_SOURCE -> Color(0xFF00B0FF)
                        PointCategory.GENERAL -> Color(0xFFE040FB)
                    }

                    // Halo outer glow
                    drawCircle(
                        color = poiColor.copy(alpha = 0.22f),
                        radius = 16.dp.toPx(),
                        center = poiPos
                    )

                    // Core point marker
                    drawCircle(
                        color = poiColor,
                        radius = 6.5.dp.toPx(),
                        center = poiPos
                    )

                    // Inner pupil
                    drawCircle(
                        color = Color.Black,
                        radius = 2.dp.toPx(),
                        center = poiPos
                    )

                    // Pending synchronization dashed indicator
                    if (poi.isPendingSync) {
                        drawCircle(
                            color = Color(0xFFFFAB00),
                            radius = 11.dp.toPx(),
                            center = poiPos,
                            style = Stroke(
                                width = 1.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)
                            )
                        )
                    }

                    // Selection ring
                    if (uiState.selectedPointOfInterest?.id == poi.id) {
                        drawCircle(
                            color = Color.White,
                            radius = 20.dp.toPx(),
                            center = poiPos,
                            style = Stroke(
                                width = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        )
                    }
                }
            }

            // 5. LAYER: Hiking Routes & Waypoints
            if (uiState.isHikingRoutesEnabled && uiState.hikingRoutes.isNotEmpty()) {
                for (route in uiState.hikingRoutes) {
                    val isSelected = uiState.selectedRoute?.id == route.id
                    val routeColor = if (isSelected) Color(0xFFFF9100) else Color(0xFFFFB74D).copy(alpha = 0.7f)
                    val strokeWidth = if (isSelected) 3.5f else 2.2f

                    if (route.coordinates.size >= 2) {
                        val path = Path()
                        var first = true
                        for (coord in route.coordinates) {
                            val rDx = (coord.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                            val rDy = -(coord.latitude - uiState.centerLatitude).toFloat() * scale
                            val rPos = Offset(center.x + rDx, center.y + rDy)
                            if (first) {
                                path.moveTo(rPos.x, rPos.y)
                                first = false
                            } else {
                                path.lineTo(rPos.x, rPos.y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = routeColor,
                            style = Stroke(
                                width = strokeWidth,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                            )
                        )
                    }

                    // Waypoints along route
                    for (wp in route.waypoints) {
                        val wpDx = (wp.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                        val wpDy = -(wp.latitude - uiState.centerLatitude).toFloat() * scale
                        val wpPos = Offset(center.x + wpDx, center.y + wpDy)

                        val wpColor = when (wp.type) {
                            WaypointType.TRAILHEAD -> Color(0xFF00E676)
                            WaypointType.WATER_SPRING -> Color(0xFF00B0FF)
                            WaypointType.REST_SITE -> Color(0xFFFFB300)
                            WaypointType.SUMMIT -> Color(0xFFFF5252)
                            WaypointType.VIEWPOINT -> Color(0xFFE040FB)
                        }

                        // Outer halo
                        drawCircle(
                            color = wpColor.copy(alpha = 0.25f),
                            radius = 8.dp.toPx(),
                            center = wpPos
                        )
                        // Core point
                        drawCircle(
                            color = wpColor,
                            radius = 4.dp.toPx(),
                            center = wpPos
                        )
                        // Inner dot
                        drawCircle(
                            color = Color.White,
                            radius = 1.5.dp.toPx(),
                            center = wpPos
                        )
                    }
                }
            }

            // 6. LAYER: Simulated Hiker Location Avatar with Directional Heading
            uiState.simulatedHikerPosition?.let { pos ->
                val hx = (pos.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                val hy = -(pos.latitude - uiState.centerLatitude).toFloat() * scale
                val hikerPos = Offset(center.x + hx, center.y + hy)

                val hikerColor = Color(0xFF00E5FF)

                // Outer pulsing halo
                drawCircle(
                    color = hikerColor.copy(alpha = 0.25f),
                    radius = 18.dp.toPx(),
                    center = hikerPos
                )
                // Mid ring
                drawCircle(
                    color = hikerColor.copy(alpha = 0.6f),
                    radius = 10.dp.toPx(),
                    center = hikerPos,
                    style = Stroke(width = 1.5f)
                )
                // Core
                drawCircle(
                    color = hikerColor,
                    radius = 5.dp.toPx(),
                    center = hikerPos
                )
                // Center pupil
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = hikerPos
                )

                // Directional pointer arrow (bearing in degrees: 0° = North, 90° = East)
                val bearingRad = Math.toRadians(uiState.simulatedHikerBearing.toDouble())
                val arrowLength = 16.dp.toPx()
                val arrowTip = Offset(
                    hikerPos.x + (kotlin.math.sin(bearingRad) * arrowLength).toFloat(),
                    hikerPos.y - (kotlin.math.cos(bearingRad) * arrowLength).toFloat()
                )

                // Arrow head triangle
                val wingLength = 7.dp.toPx()
                val leftWingAngle = bearingRad + Math.toRadians(150.0)
                val rightWingAngle = bearingRad - Math.toRadians(150.0)

                val arrowLeft = Offset(
                    arrowTip.x + (kotlin.math.sin(leftWingAngle) * wingLength).toFloat(),
                    arrowTip.y - (kotlin.math.cos(leftWingAngle) * wingLength).toFloat()
                )
                val arrowRight = Offset(
                    arrowTip.x + (kotlin.math.sin(rightWingAngle) * wingLength).toFloat(),
                    arrowTip.y - (kotlin.math.cos(rightWingAngle) * wingLength).toFloat()
                )

                val arrowPath = Path().apply {
                    moveTo(arrowTip.x, arrowTip.y)
                    lineTo(arrowLeft.x, arrowLeft.y)
                    lineTo(hikerPos.x, hikerPos.y)
                    lineTo(arrowRight.x, arrowRight.y)
                    close()
                }

                drawPath(
                    path = arrowPath,
                    color = hikerColor.copy(alpha = 0.85f)
                )
            }

            // 7. LAYER: Real-time Live Group Member Locations
            if (uiState.isGroupLocationsEnabled && uiState.groupMemberLocations.isNotEmpty()) {
                val now = System.currentTimeMillis()
                for (member in uiState.groupMemberLocations) {
                    val isStale = member.isStale(now)
                    val alpha = if (isStale) 0.55f else 1.0f
                    val memberColor = if (isStale) Color(0xFFFFB300) else Color(0xFF00E676)

                    val mDx = (member.longitude - uiState.centerLongitude).toFloat() * scale * cosLat
                    val mDy = -(member.latitude - uiState.centerLatitude).toFloat() * scale
                    val memberPos = Offset(center.x + mDx, center.y + mDy)

                    // Outer pulsing aura
                    drawCircle(
                        color = memberColor.copy(alpha = 0.22f * alpha),
                        radius = 18.dp.toPx(),
                        center = memberPos
                    )
                    // Middle ring
                    drawCircle(
                        color = memberColor.copy(alpha = 0.8f * alpha),
                        radius = 9.dp.toPx(),
                        center = memberPos,
                        style = Stroke(width = 2f)
                    )
                    // Core
                    drawCircle(
                        color = memberColor.copy(alpha = alpha),
                        radius = 5.dp.toPx(),
                        center = memberPos
                    )
                    // White center dot
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = 2.dp.toPx(),
                        center = memberPos
                    )

                    // Directional bearing pointer (if bearing available)
                    member.bearingDegrees?.let { bearing ->
                        val bearingRad = Math.toRadians(bearing.toDouble())
                        val arrowLen = 14.dp.toPx()
                        val arrowTip = Offset(
                            memberPos.x + (kotlin.math.sin(bearingRad) * arrowLen).toFloat(),
                            memberPos.y - (kotlin.math.cos(bearingRad) * arrowLen).toFloat()
                        )
                        drawLine(
                            color = memberColor.copy(alpha = alpha),
                            start = memberPos,
                            end = arrowTip,
                            strokeWidth = 2.5f
                        )
                    }

                    // Selection ring
                    if (uiState.selectedGroupMember?.userId == member.userId) {
                        drawCircle(
                            color = Color.White,
                            radius = 24.dp.toPx(),
                            center = memberPos,
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        )
                    }
                }
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

        // Loading overlay (Radar Loader)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xD90A0E17)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RadarLoader(
                        color = primaryColor,
                        size = 54.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "در حال بارگذاری عوارض وکتور جهرم...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
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
                TacticalEmptyStateCard(
                    illustrationResId = R.drawable.img_empty_map_not_downloaded,
                    title = "خطا در پردازش نقشه جهرم",
                    description = uiState.errorMessage ?: "عوارض وکتور جهرم در دسترس نیستند.",
                    actionText = "بازنشانی به حالت پایه",
                    onActionClicked = onRetry
                )
            }
        }

        // Map HUD: Top Coordinate & Mode Bar (Enforce LTR for coordinate display)
        if (showInternalCoordinatesHud) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xCC151B22), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF30363D), shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
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
            }
        }

        // Map HUD: Controls Bar (Zoom, Compass Reset, Reset Center)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = controlsBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xEE111927), shape = RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF283548), shape = RoundedCornerShape(10.dp))
                    .clickable {
                        if (uiState.zoomLevel < 18) {
                            panOffset *= 1.22f
                            onZoomIn()
                        } else {
                            val nextScale = (userZoomScale * 1.22f).coerceAtMost(4.0f)
                            panOffset *= (nextScale / userZoomScale)
                            userZoomScale = nextScale
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TacticalIcons.Plus,
                    contentDescription = "بزرگ‌نمایی نقشه",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xEE111927), shape = RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF283548), shape = RoundedCornerShape(10.dp))
                    .clickable {
                        if (uiState.zoomLevel > 10) {
                            panOffset /= 1.22f
                            onZoomOut()
                        } else {
                            val nextScale = (userZoomScale / 1.22f).coerceAtLeast(0.4f)
                            panOffset *= (nextScale / userZoomScale)
                            userZoomScale = nextScale
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TacticalIcons.Minus,
                    contentDescription = "کوچک‌نمایی نقشه",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (kotlin.math.abs(userRotationDegrees) > 1f) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xEE111927), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, primaryColor, shape = RoundedCornerShape(10.dp))
                        .clickable { userRotationDegrees = 0f },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = TacticalIcons.Compass,
                        contentDescription = "تنظیم شمال نقشه",
                        tint = primaryColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xEE111927), shape = RoundedCornerShape(10.dp))
                    .border(1.dp, primaryColor.copy(alpha = 0.55f), shape = RoundedCornerShape(10.dp))
                    .clickable {
                        panOffset = Offset.Zero
                        userZoomScale = 1f
                        userRotationDegrees = 0f
                        onCenterReset()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = TacticalIcons.Crosshair,
                    contentDescription = "بازگشت به مرکز جهرم",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Map HUD: Selected Marker Detail Bottom Sheet/Pill
        uiState.selectedMarker?.let { marker ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 64.dp, top = 10.dp, bottom = controlsBottomPadding)
                    .fillMaxWidth()
                    .background(Color(0xF2111927), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, primaryColor, shape = RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = marker.callsign,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TacticalStatusBadge(
                            text = "شبیه‌سازی",
                            tone = marker.tone
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = marker.unitDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Map HUD: Selected Live Group Member Detail Pill
        uiState.selectedGroupMember?.let { member ->
            val isStale = member.isStale()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 64.dp, top = 10.dp, bottom = controlsBottomPadding)
                    .fillMaxWidth()
                    .background(Color(0xF2111927), shape = RoundedCornerShape(12.dp))
                    .border(1.dp, if (isStale) warningColor else Color(0xFF00E676), shape = RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = member.callsign,
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        TacticalStatusBadge(
                            text = if (isStale) "قدیمی" else "موقعیت زنده",
                            tone = if (isStale) BadgeTone.WARNING else BadgeTone.SUCCESS
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${member.formattedCoordinates} • دقت: ±${member.accuracyMeters.toInt()}م",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (member.speedMps != null || member.altitudeMeters != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        val speedKmH = (member.speedMps ?: 0f) * 3.6f
                        val altText = member.altitudeMeters?.let { "ارتفاع: ${it.toInt()}م" } ?: ""
                        val speedText = if (member.speedMps != null) String.format("سرعت: %.1f ک‌م/س", speedKmH) else ""
                        Text(
                            text = listOf(speedText, altText).filter { it.isNotBlank() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}
