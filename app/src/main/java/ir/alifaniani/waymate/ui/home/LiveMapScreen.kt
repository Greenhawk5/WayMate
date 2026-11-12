package ir.alifaniani.waymate.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.alifaniani.waymate.domain.model.GroupMemberLocation
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.home.map.AssetGisLayerRepository
import ir.alifaniani.waymate.ui.home.map.BaseMapPresentation
import ir.alifaniani.waymate.ui.home.map.HikingSimulationBar
import ir.alifaniani.waymate.ui.home.map.HikingSimulationEngine
import ir.alifaniani.waymate.ui.home.map.PointOfInterestDetailCard
import ir.alifaniani.waymate.ui.home.map.TacticalLayerControlSheet
import ir.alifaniani.waymate.ui.home.map.TacticalMapCanvas
import ir.alifaniani.waymate.ui.home.map.TacticalMapUiState
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateOnCyan
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Dedicated Destination A: Live Tactical Map.
 *
 * Immersive, full-screen map interface providing:
 * - Full-bleed TacticalMapCanvas with bottom dock inset awareness
 * - Floating Tactical HUD (Coordinates, Offline engine badge, active layers count)
 * - Floating action pill buttons (Layer controls, Simulation toggle, Add Point)
 * - Floating zoom and centering controls
 * - Bottom POI and team member inspection cards positioned safely above circular navigation
 */
@Composable
fun LiveMapScreen(
    points: List<PointOfInterest>,
    selectedPoint: PointOfInterest?,
    onSelectPoint: (PointOfInterest?) -> Unit,
    groupMembers: List<GroupMemberLocation>,
    selectedGroupMember: GroupMemberLocation?,
    onSelectGroupMember: (GroupMemberLocation?) -> Unit,
    onOpenCreatePointDialog: () -> Unit,
    onOpenEditPointDialog: (PointOfInterest) -> Unit,
    onDeletePoint: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gisRepository = remember { AssetGisLayerRepository(context) }
    val simulationEngine = remember { HikingSimulationEngine() }
    val simulationState by simulationEngine.state.collectAsState()

    var isSimulationBarVisible by remember { mutableStateOf(false) }
    var isLayerSheetVisible by remember { mutableStateOf(false) }

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

    LaunchedEffect(points, selectedPoint, groupMembers, selectedGroupMember) {
        mapState = mapState.copy(
            pointsOfInterest = points,
            selectedPointOfInterest = selectedPoint,
            groupMemberLocations = groupMembers,
            selectedGroupMember = selectedGroupMember
        )
    }

    LaunchedEffect(simulationState, isSimulationBarVisible) {
        mapState = mapState.copy(
            simulatedHikerPosition = if (isSimulationBarVisible) simulationState.currentPosition else null,
            simulatedHikerBearing = simulationState.currentBearing,
            selectedRoute = if (isSimulationBarVisible) simulationState.route else null
        )
    }

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
                errorMessage = "خطا در بارگذاری اولیه لایه‌های وکتور"
            )
        }
    }

    val hasBottomInspectionCard = selectedPoint != null ||
        mapState.selectedMarker != null ||
        mapState.selectedGroupMember != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WayMateMidnightDark)
    ) {
        // Full Bleed Tactical Map Canvas
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
                    centerLongitude = 53.6008,
                    zoomLevel = 14
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
            },
            showInternalCoordinatesHud = false,
            controlsBottomPadding = 166.dp,
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Tactical HUD & Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .zIndex(10f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Live Coordinates & Engine Status Pill
                Box(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(WayMateMidnightSurface.copy(alpha = 0.95f))
                        .border(1.dp, WayMateBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(WayMateCyan)
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = mapState.formattedCoordinates.replace("  ", " "),
                                color = WayMateTextPrimary,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Layer Settings Sheet Button
                Button(
                    onClick = { isLayerSheetVisible = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WayMateMidnightSurface.copy(alpha = 0.95f),
                        contentColor = WayMateCyan
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WayMateBorder),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = TacticalIcons.Layers,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "لایه‌ها (${mapState.activeLayerCount})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Add Point (+) Button
                Button(
                    onClick = onOpenCreatePointDialog,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WayMateCyan,
                        contentColor = WayMateOnCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(
                        imageVector = TacticalIcons.Plus,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "نشانه",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Simulation Bar (when toggled)
            AnimatedVisibility(
                visible = isSimulationBarVisible,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
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
            }
        }

        // Floating Simulation Toggle Pill (Positioned safely above navigation dock)
        AnimatedVisibility(
            visible = !hasBottomInspectionCard && !isLayerSheetVisible,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 148.dp)
                .zIndex(9f),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                onClick = { isSimulationBarVisible = !isSimulationBarVisible },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSimulationBarVisible) WayMateCyan else WayMateMidnightSurface.copy(alpha = 0.94f),
                    contentColor = if (isSimulationBarVisible) WayMateOnCyan else WayMateTextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSimulationBarVisible) WayMateCyan else WayMateBorder
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.height(42.dp)
            ) {
                Icon(
                    imageVector = if (isSimulationBarVisible) TacticalIcons.Close else TacticalIcons.Mountain,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSimulationBarVisible) "بستن شبیه‌ساز" else "شبیه‌ساز پیمایش",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        // Bottom Selected Inspection Card (POI)
        AnimatedVisibility(
            visible = selectedPoint != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 14.dp, end = 64.dp, bottom = 148.dp)
                .zIndex(15f),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            if (selectedPoint != null) {
                PointOfInterestDetailCard(
                    point = selectedPoint,
                    onEdit = onOpenEditPointDialog,
                    onDelete = onDeletePoint,
                    onDismiss = { onSelectPoint(null) }
                )
            }
        }

        // Tactical Layer Control Sheet Modal Overlay (High zIndex, Status Bar & Nav Dock Safe)
        AnimatedVisibility(
            visible = isLayerSheetVisible,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(25f),
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }
        ) {
            val backdropInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC050911))
                    .clickable(
                        interactionSource = backdropInteraction,
                        indication = null
                    ) { isLayerSheetVisible = false }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 144.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                val cardInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier.clickable(
                        interactionSource = cardInteraction,
                        indication = null
                    ) { /* Consume taps inside sheet */ }
                ) {
                    TacticalLayerControlSheet(
                        uiState = mapState,
                        onSelectBasePresentation = { presentation ->
                            mapState = mapState.selectBasePresentation(presentation)
                        },
                        onToggleLayer = { layerId ->
                            mapState = mapState.toggleLayer(layerId)
                        },
                        onDismiss = { isLayerSheetVisible = false }
                    )
                }
            }
        }
    }
}
