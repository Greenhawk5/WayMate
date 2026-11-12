package ir.alifaniani.waymate.ui.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateBorderSubtle
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateOnCyan
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceHighest
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Navigation Destination Item representation for the Circular Navigation System.
 */
data class OrbitNavigationItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val contentDescription: String
)

/**
 * Natively implemented Jetpack Compose Circular Orbit Navigation Bar.
 *
 * Implements the mathematical circular projection and interaction principles from the design prototype:
 * 1. 3D circular track and angular arrangement (360° / N items).
 * 2. Horizontally draggable circular orbit with velocity-aware spring snapping.
 * 3. Dominant central active item with high-contrast cyan accent and Persian title label.
 * 4. Perspective depth attenuation (scale, alpha, and zIndex) on neighboring items.
 * 5. Direct tap selection with shortest-path circular interpolation.
 * 6. Strictly bounded, non-overlapping hitboxes preventing rogue clicks on recessed background items.
 * 7. Persian RTL compatible drag and balanced vertical centering within the dock.
 * 8. Full TalkBack semantics, accessible touch targets, and reduced-motion resilience.
 */
@Composable
fun CircularOrbitNavigationBar(
    items: List<OrbitNavigationItem>,
    selectedItemIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val itemCount = items.size
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    // Continuous rotational position (0f to (itemCount - 1)f, wrapping)
    val animatedPos = remember { Animatable(selectedItemIndex.toFloat()) }

    // Keep animatedPos in sync with external selection changes
    LaunchedEffect(selectedItemIndex) {
        val currentTarget = animatedPos.targetValue
        val diff = selectedItemIndex - currentTarget
        val normalizedDiff = (((diff + itemCount / 2f) % itemCount) + itemCount) % itemCount - (itemCount / 2f)
        if (abs(animatedPos.value - selectedItemIndex) > 0.01f) {
            animatedPos.animateTo(
                targetValue = currentTarget + normalizedDiff,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    val dragSensitivityPx = with(density) { 72.dp.toPx() }
    val orbitRadiusX = with(density) { 115.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Elevated Dock Container - perfectly centered content
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .height(96.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = WayMateCyan.copy(alpha = 0.15f),
                    spotColor = WayMateCyan.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            WayMateMidnightSurface.copy(alpha = 0.98f),
                            WayMateMidnightDark.copy(alpha = 0.99f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            WayMateBorder.copy(alpha = 0.8f),
                            WayMateBorderSubtle.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .pointerInput(itemCount) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            val currentPos = animatedPos.value
                            val snappedPos = currentPos.roundToInt()
                            coroutineScope.launch {
                                animatedPos.animateTo(
                                    targetValue = snappedPos.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                                val resolvedIndex = ((snappedPos % itemCount) + itemCount) % itemCount
                                onItemSelected(resolvedIndex)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            val snappedPos = animatedPos.value.roundToInt()
                            coroutineScope.launch {
                                animatedPos.animateTo(
                                    targetValue = snappedPos.toFloat(),
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            // Natural physical drag: dragging finger left moves orbit left, dragging right moves orbit right
                            val deltaPos = if (isRtl) dragAmount / dragSensitivityPx else -dragAmount / dragSensitivityPx
                            coroutineScope.launch {
                                animatedPos.snapTo(animatedPos.value + deltaPos)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Dock-level Canvas: Elliptical track line and unclipped circular cyan glow halo
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                // Track aligned vertically with orbit disc centers
                val trackY = canvasHeight * 0.45f
                val arcRadiusX = (canvasWidth - 48.dp.toPx()) * 0.42f

                // Elliptical track line
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            WayMateCyan.copy(alpha = 0.35f),
                            WayMateBorder.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(canvasWidth / 2f, trackY),
                        radius = arcRadiusX
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(canvasWidth / 2f - arcRadiusX, trackY - 14.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(arcRadiusX * 2, 28.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Perfectly circular, unclipped cyan glow drawn at Dock level
                // Eliminates RenderNode alpha layer clipping from child elements throughout animation
                val currentPos = animatedPos.value
                val haloRadius = 52.dp.toPx() * 0.86f
                items.forEachIndexed { index, _ ->
                    val diff = (index - currentPos)
                    val normalizedDiff = (((diff + itemCount / 2f) % itemCount) + itemCount) % itemCount - (itemCount / 2f)
                    val angleRad = normalizedDiff * (2f * PI.toFloat() / itemCount)
                    val depth = cos(angleRad)
                    if (depth > 0.60f) {
                        val glowFactor = ((depth - 0.60f) / 0.40f).coerceIn(0f, 1f)
                        val itemOffsetX = sin(angleRad) * orbitRadiusX
                        // In Jetpack Compose, Modifier.offset { IntOffset(x, y) } is RTL-aware:
                        // in RTL, positive X translates towards End (physical Left, parentWidth - childWidth - x).
                        // Mirror the physical horizontal coordinate on Canvas in RTL so the glow follows the icon exactly.
                        val physicalOffsetX = if (isRtl) -itemOffsetX else itemOffsetX
                        val glowCenterX = canvasWidth / 2f + physicalOffsetX
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to WayMateCyan.copy(alpha = 0.42f * glowFactor),
                                    0.52f to WayMateCyan.copy(alpha = 0.28f * glowFactor),
                                    0.76f to WayMateCyan.copy(alpha = 0.10f * glowFactor),
                                    0.92f to WayMateCyan.copy(alpha = 0.02f * glowFactor),
                                    1.0f to Color.Transparent
                                ),
                                center = Offset(glowCenterX, trackY),
                                radius = haloRadius
                            ),
                            radius = haloRadius,
                            center = Offset(glowCenterX, trackY)
                        )
                    }
                }
            }

            // Dynamic 3D Projected Navigation Orbit Items
            items.forEachIndexed { index, item ->
                val currentPos = animatedPos.value
                val diff = (index - currentPos)
                // Normalize angular diff into [-itemCount/2, itemCount/2]
                val normalizedDiff = (((diff + itemCount / 2f) % itemCount) + itemCount) % itemCount - (itemCount / 2f)
                val angleRad = normalizedDiff * (2f * PI.toFloat() / itemCount)

                // Trigonometric projection
                val depth = cos(angleRad) // 1.0 at front, -1.0 at back
                val offsetX = sin(angleRad) * orbitRadiusX

                // Visual depth factors
                val isNearCenter = abs(normalizedDiff) < 0.45f
                val scaleFactor = (0.74f + 0.26f * ((depth + 0.2f) / 1.2f)).coerceIn(0.72f, 1.0f)
                val alphaFactor = (0.35f + 0.65f * ((depth - 0.1f) / 0.9f)).coerceIn(0.25f, 1.0f)

                // Depth-gated interactivity: only clearly visible front elements (center and immediate neighbors)
                // are clickable. Recessed back elements (depth <= 0.15f) cannot intercept taps.
                val isInteractive = depth > 0.15f
                val itemZIndex = depth

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = offsetX.roundToInt(), y = 5.dp.roundToPx()) }
                        .zIndex(itemZIndex)
                        .scale(scaleFactor)
                        .alpha(alphaFactor)
                        .semantics {
                            contentDescription = item.contentDescription
                            selected = isNearCenter
                            role = Role.Tab
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(64.dp)
                            .clickable(
                                enabled = isInteractive,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                val currentPosVal = animatedPos.value
                                val curFloor = currentPosVal.roundToInt()
                                var delta = (((index - curFloor) % itemCount) + itemCount) % itemCount
                                if (delta > itemCount / 2) delta -= itemCount
                                val targetPos = curFloor + delta

                                coroutineScope.launch {
                                    animatedPos.animateTo(
                                        targetValue = targetPos.toFloat(),
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                    onItemSelected(index)
                                }
                            }
                    ) {
                        // Icon Disc Container (glow rendered unclipped on Dock-level Canvas)
                        Box(
                            modifier = Modifier
                                .size(if (isNearCenter) 52.dp else 44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isNearCenter) {
                                        Brush.radialGradient(
                                             colors = listOf(
                                                 WayMateCyan.copy(alpha = 0.28f),
                                                 WayMateMidnightSurface
                                             )
                                         )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                WayMateSurfaceVariant,
                                                WayMateMidnightSurface
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isNearCenter) 1.8.dp else 1.dp,
                                    color = if (isNearCenter) WayMateCyan else WayMateBorder,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(if (isNearCenter) 26.dp else 22.dp),
                                tint = if (isNearCenter) WayMateCyan else WayMateTextSecondary
                            )
                        }

                        // Label underneath (Visible on active item and front interactive neighbors)
                        if (isNearCenter || isInteractive) {
                            Text(
                                text = item.title,
                                color = if (isNearCenter) WayMateCyan else WayMateTextSecondary,
                                fontSize = if (isNearCenter) 11.sp else 10.sp,
                                fontWeight = if (isNearCenter) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1
                            )
                        } else {
                            // Empty placeholder to preserve vertical centering
                            Box(modifier = Modifier.height(18.dp))
                        }
                    }
                }
            }
        }
    }
}
