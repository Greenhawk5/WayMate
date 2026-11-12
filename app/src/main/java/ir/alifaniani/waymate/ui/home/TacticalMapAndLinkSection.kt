package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalEmptyStateCard
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.home.map.AssetGisLayerRepository
import ir.alifaniani.waymate.ui.home.map.BaseMapPresentation
import ir.alifaniani.waymate.ui.home.map.HikingSimulationBar
import ir.alifaniani.waymate.ui.home.map.HikingSimulationEngine
import ir.alifaniani.waymate.ui.home.map.PointOfInterestDetailCard
import ir.alifaniani.waymate.ui.home.map.SyntheticMarker
import ir.alifaniani.waymate.ui.home.map.TacticalLayerControlSheet
import ir.alifaniani.waymate.ui.home.map.TacticalMapCanvas
import ir.alifaniani.waymate.ui.home.map.TacticalMapUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tactical map preview and connection health presentation component.
 *
 * Implements a safe, decoupled educational map experience:
 * - Decoupled base map presentations (Offline Tactical, Standard Street, and unconfigured Satellite)
 * - Independent vector overlays (Jahrom road network, Waterways, Tactical Grid, Synthetic Units)
 * - Shared group Points of Interest (POI) with offline synchronization and category markers
 * - Hiking routes and waypoints visualization
 * - Synthetic deterministic hiking simulation with play/pause/reset controls
 * - Real-time consenting group members live location display
 * - Non-blocking background loading of audited GIS vector assets
 * - Transparent status for unconfigured services (Satellite)
 * - Explicitly labeled synthetic demonstration markers (no real-world GPS tracking)
 * - Preserves Dark Tactical RTL layout and link health metrics
 */
@Composable
fun TacticalMapAndLinkSection(
    points: List<PointOfInterest> = emptyList(),
    selectedPoint: PointOfInterest? = null,
    onSelectPoint: (PointOfInterest?) -> Unit = {},
    groupMembers: List<GroupMemberLocation> = emptyList(),
    selectedGroupMember: GroupMemberLocation? = null,
    onSelectGroupMember: (GroupMemberLocation?) -> Unit = {},
    onOpenCreatePointDialog: () -> Unit = {},
    onOpenEditPointDialog: (PointOfInterest) -> Unit = {},
    onDeletePoint: (String) -> Unit = {},
    offlineQueueCount: Int = 0,
    lastSyncTimestamp: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gisRepository = remember { AssetGisLayerRepository(context) }
    val simulationEngine = remember { HikingSimulationEngine() }
    val simulationState by simulationEngine.state.collectAsState()
    var isSimulationBarVisible by remember { mutableStateOf(false) }

    var mapState by remember {
        mutableStateOf(
            TacticalMapUiState(
                pointsOfInterest = points,
                selectedPointOfInterest = selectedPoint,
                groupMemberLocations = groupMembers,
                selectedGroupMember = selectedGroupMember
            )
        )
    }

    // Keep map state synchronized with incoming POI and group member properties
    LaunchedEffect(points, selectedPoint, groupMembers, selectedGroupMember) {
        mapState = mapState.copy(
            pointsOfInterest = points,
            selectedPointOfInterest = selectedPoint,
            groupMemberLocations = groupMembers,
            selectedGroupMember = selectedGroupMember
        )
    }

    // Keep map state synchronized with simulation progress
    LaunchedEffect(simulationState, isSimulationBarVisible) {
        mapState = mapState.copy(
            simulatedHikerPosition = if (isSimulationBarVisible) simulationState.currentPosition else null,
            simulatedHikerBearing = simulationState.currentBearing,
            selectedRoute = if (isSimulationBarVisible) simulationState.route else null
        )
    }

    // Asynchronously load real GIS vector features from assets without blocking UI
    LaunchedEffect(Unit) {
        try {
            mapState = mapState.copy(isLoading = true)
            val segments = gisRepository.loadJahromVectors()
            mapState = mapState.copy(
                vectorSegments = segments,
                isLoading = false
            )
        } catch (e: Exception) {
            mapState = mapState.copy(
                isLoading = false,
                errorMessage = "خطا در بارگذاری اولیه فایل‌های GIS"
            )
        }
    }

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
            // Header Row
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
                        text = "نقشه عملیاتی و لایه‌های اطلاعاتی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نمایش چندلایه‌ای داده‌های مکانی جهرم، مسیرهای کوهنوردی و نقاط گروه",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TacticalStatusBadge(
                    text = "${mapState.activeLayerCount} لایه فعال",
                    tone = BadgeTone.NEUTRAL
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalOutlinedButton(
                    text = if (isSimulationBarVisible) "بستن شبیه‌ساز" else "شبیه‌ساز پیمایش",
                    leadingIcon = if (isSimulationBarVisible) TacticalIcons.Close else TacticalIcons.Mountain,
                    onClick = { isSimulationBarVisible = !isSimulationBarVisible },
                    modifier = Modifier.weight(1f)
                )
                TacticalOutlinedButton(
                    text = "نقطه جدید",
                    leadingIcon = TacticalIcons.Plus,
                    onClick = onOpenCreatePointDialog,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Educational & Synthetic Boundary Banner
            TacticalFeedbackBanner(
                title = "محیط آزمایشی نقشه (Educational Prototype)",
                message = "کلیه نشانگرها داده‌های شبیه‌سازی‌شده هستند. داده‌های خطی معابر و مسیل‌ها از فایل‌های وکتور جهرم (EPSG:4326) استخراج و ممیزی شده‌اند.",
                type = BannerType.INFO
            )

            // Hiking Simulation Bar (when expanded)
            if (isSimulationBarVisible) {
                Spacer(modifier = Modifier.height(10.dp))
                HikingSimulationBar(
                    simulationState = simulationState,
                    onTogglePlayPause = {
                        if (simulationState.isPlaying) {
                            simulationEngine.pause()
                        } else {
                            simulationEngine.start(coroutineScope)
                        }
                    },
                    onReset = { simulationEngine.reset() },
                    onSelectRoute = { route -> simulationEngine.selectRoute(route) },
                    onSetSpeedMultiplier = { speed -> simulationEngine.setSpeedMultiplier(speed) },
                    onDismiss = { isSimulationBarVisible = false }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Safe Multi-Layer Map Canvas
            TacticalMapCanvas(
                uiState = mapState,
                onMarkerSelected = { marker ->
                    mapState = mapState.copy(selectedMarker = marker)
                    if (marker != null) onSelectPoint(null)
                },
                onPointOfInterestSelected = { poi ->
                    if (poi != null) mapState = mapState.copy(selectedMarker = null)
                    onSelectPoint(poi)
                },
                onGroupMemberSelected = onSelectGroupMember,
                onZoomIn = {
                    if (mapState.zoomLevel < 18) {
                        mapState = mapState.copy(zoomLevel = mapState.zoomLevel + 1)
                    }
                },
                onZoomOut = {
                    if (mapState.zoomLevel > 10) {
                        mapState = mapState.copy(zoomLevel = mapState.zoomLevel - 1)
                    }
                },
                onCenterReset = {
                    mapState = mapState.copy(
                        centerLatitude = 28.5303,
                        centerLongitude = 53.6008
                    )
                },
                onRetry = {
                    coroutineScope.launch {
                        mapState = mapState.copy(isLoading = true, errorMessage = null)
                        val segments = gisRepository.loadJahromVectors()
                        delay(300)
                        mapState = mapState.copy(
                            isLoading = false,
                            basePresentation = BaseMapPresentation.OFFLINE_TACTICAL,
                            vectorSegments = segments,
                            errorMessage = null
                        )
                    }
                }
            )

            // Selected POI Inspection & Action Card
            if (selectedPoint != null) {
                Spacer(modifier = Modifier.height(12.dp))
                PointOfInterestDetailCard(
                    point = selectedPoint,
                    onEdit = onOpenEditPointDialog,
                    onDelete = onDeletePoint,
                    onDismiss = { onSelectPoint(null) }
                )
            }

            // Selected Synthetic Marker Tactical Inspection Card
            if (mapState.selectedMarker != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedMarkerCard(
                    marker = mapState.selectedMarker!!,
                    onDismiss = { mapState = mapState.copy(selectedMarker = null) }
                )
            }

            // Empty State when no Points of Interest are registered
            if (points.isEmpty() && selectedPoint == null && mapState.selectedMarker == null) {
                Spacer(modifier = Modifier.height(12.dp))
                TacticalEmptyStateCard(
                    illustrationResId = R.drawable.img_empty_no_saved_location,
                    title = "هیچ نقطه نشانه‌ای ثبت نشده است",
                    description = "برای علامت‌گذاری نقاط مهم مسیر، چشمه‌ها و محل‌های استقرار، دکمه «نقطه جدید (+)\" را لمس کنید.",
                    actionText = "افزودن اولین نشانه (+)",
                    onActionClicked = onOpenCreatePointDialog
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Layer Management Trigger & Base Presentation Quick Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TacticalOutlinedButton(
                    text = "تنظیم و مدیریت لایه‌ها (${mapState.activeLayerCount})",
                    onClick = {
                        mapState = mapState.copy(isLayerSheetVisible = !mapState.isLayerSheetVisible)
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                TacticalStatusBadge(
                    text = mapState.basePresentation.titlePersian,
                    tone = if (mapState.basePresentation.isConfigured) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            // Layer Control Sheet (Inline Expandable / Modal View)
            if (mapState.isLayerSheetVisible) {
                Spacer(modifier = Modifier.height(14.dp))
                TacticalLayerControlSheet(
                    uiState = mapState,
                    onSelectBasePresentation = { presentation ->
                        mapState = mapState.selectBasePresentation(presentation)
                    },
                    onToggleLayer = { layerId ->
                        mapState = mapState.toggleLayer(layerId)
                    },
                    onDismiss = {
                        mapState = mapState.copy(isLayerSheetVisible = false)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Connection Link & POI Diagnostics Metrics (Stage 11.6)
            val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "همگام‌سازی ابری D1",
                    value = if (lastSyncTimestamp > 0) "برخط (${timeFormatter.format(Date(lastSyncTimestamp))})" else "پایگاه محلی SQLite"
                )
                MetricItem(label = "پوشش جغرافیایی", value = "جهرم (Fars, IRN)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(
                    label = "صف ذخیره و ارسال",
                    value = if (offlineQueueCount > 0) "$offlineQueueCount عملیات معوق" else "۰ معوق (پایدار)"
                )
                MetricItem(
                    label = "نقاط نشانه مشترک",
                    value = "${points.size} نقطه فعال (${points.count { it.isPendingSync }} در صف)"
                )
            }
        }
    }
}

@Composable
private fun SelectedMarkerCard(
    marker: SyntheticMarker,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = marker.callsign,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TacticalStatusBadge(
                    text = "نشانگر شبیه‌سازی‌شده",
                    tone = marker.tone
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = marker.unitDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "مختصات نمایشی: ${String.format("%.4f°N , %.4f°E", marker.latitude, marker.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            TacticalOutlinedButton(
                text = "بستن جزئیات نشانگر",
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
