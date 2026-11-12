package ir.alifaniani.waymate.ui.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateSuccess
import kotlin.math.abs

data class TacticalChartPoint(
    val label: String,
    val value: Float,
    val formattedValue: String? = null
)

/**
 * Single source of truth for the horizontal placement of chart points.
 *
 * Compose `Canvas` drawing and pointer offsets are always expressed in physical
 * (left-to-right) pixels, while `Row`/`Arrangement` mirror automatically in RTL.
 * Previously the curve and tap detection used physical LTR coordinates while the
 * label row was mirrored by RTL, so index 0 was drawn on the left but labelled
 * on the right. Every consumer (curve, nodes, tap detection and labels) must
 * now go through [pointX] / [nearestIndex] so the mapping can never diverge.
 *
 * In RTL, index 0 (the logical start of the series) is placed on the right.
 */
internal object TacticalChartGeometry {
    fun pointX(index: Int, count: Int, width: Float, horizontalPadding: Float, isRtl: Boolean): Float {
        if (count <= 0) return 0f
        val usable = (width - horizontalPadding * 2f).coerceAtLeast(0f)
        val step = if (count > 1) usable / (count - 1) else 0f
        val ltrX = if (count > 1) horizontalPadding + index * step else width / 2f
        return if (isRtl) width - ltrX else ltrX
    }

    fun nearestIndex(tapX: Float, count: Int, width: Float, horizontalPadding: Float, isRtl: Boolean): Int {
        if (count <= 0) return -1
        var best = 0
        var bestDist = Float.MAX_VALUE
        for (i in 0 until count) {
            val d = abs(tapX - pointX(i, count, width, horizontalPadding, isRtl))
            if (d < bestDist) {
                bestDist = d
                best = i
            }
        }
        return best
    }
}

/**
 * Native Jetpack Compose Area Chart inspired by EvilCharts (ECharts Area Chart).
 *
 * Features:
 * - Smooth monotone/Catmull-Rom cubic Bezier curve
 * - Soft vertical gradient area fill underneath the curve
 * - Subtle dashed horizontal reference grid lines
 * - Interactive tap-to-inspect data point marker with live readout pill
 * - Full RTL/LTR compatibility and zero external charting library overhead
 */
@Composable
fun TacticalAreaChart(
    title: String,
    subtitle: String,
    points: List<TacticalChartPoint>,
    modifier: Modifier = Modifier,
    unitLabel: String = "",
    trendBadgeText: String? = null,
    lineColor: Color = WayMateCyan,
    accentColor: Color = WayMateSuccess,
    chartHeight: Dp = 148.dp,
    onPointSelected: ((index: Int, point: TacticalChartPoint) -> Unit)? = null
) {
    if (points.isEmpty()) return

    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val chartPaddingH = 14.dp

    var selectedIndex by remember(points) { mutableIntStateOf(points.lastIndex) }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val revealFraction by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "AreaChartReveal"
    )

    val safeIndex = selectedIndex.coerceIn(0, points.lastIndex)
    val selectedPoint = points[safeIndex]

    val maxVal = remember(points) { (points.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f) }
    val minVal = remember(points) { (points.minOfOrNull { it.value } ?: 0f).coerceAtMost(maxVal - 1f).coerceAtLeast(0f) }
    val range = (maxVal - minVal).coerceAtLeast(1f)

    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    val guideLineColor = lineColor.copy(alpha = 0.45f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Stable, adaptive header layout: prevents dynamic badge changes from truncating or reflowing the title
        val displayBadgeText = if (selectedPoint.formattedValue != null) {
            "${selectedPoint.formattedValue} • ${selectedPoint.label}"
        } else {
            val valueStr = if (unitLabel.isNotBlank()) "${selectedPoint.value.toInt()} $unitLabel" else "${selectedPoint.value.toInt()}"
            "$valueStr • ${selectedPoint.label}"
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 460.dp

            if (isCompact) {
                // Responsive layout for compact containers (e.g. mobile screens, nested management cards):
                // Title and Subtitle receive predictable, independent layout space so they never wrap awkwardly,
                // collide with the readout badge, or truncate with unintended ellipses.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartReadoutBadge(
                            text = displayBadgeText,
                            lineColor = lineColor,
                            accentColor = accentColor
                        )
                    }
                }
            } else {
                // Side-by-side header row with predictable space allocation for wide containers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    ChartReadoutBadge(
                        text = displayBadgeText,
                        lineColor = lineColor,
                        accentColor = accentColor
                    )
                }
            }
        }

        if (!trendBadgeText.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = trendBadgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Canvas Area Chart
        val linePath = remember { Path() }
        val fillPath = remember { Path() }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(points, isRtl) {
                    detectTapGestures { tapOffset ->
                        val idx = TacticalChartGeometry.nearestIndex(
                            tapX = tapOffset.x,
                            count = points.size,
                            width = size.width.toFloat(),
                            horizontalPadding = chartPaddingH.toPx(),
                            isRtl = isRtl
                        )
                        if (idx >= 0) {
                            selectedIndex = idx
                            onPointSelected?.invoke(idx, points[idx])
                        }
                    }
                }
        ) {
            val padH = chartPaddingH.toPx()
            val padTop = 12.dp.toPx()
            val padBottom = 10.dp.toPx()
            val usableH = (size.height - padTop - padBottom).coerceAtLeast(1f)
            val baselineY = padTop + usableH

            // 4 horizontal dashed grid rules
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (r in 0..3) {
                val gy = padTop + (usableH * r / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(padH, gy),
                    end = Offset(size.width - padH, gy),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (r == 3) null else dashEffect
                )
            }

            val coords = points.mapIndexed { idx, pt ->
                val norm = ((pt.value - minVal) / range).coerceIn(0f, 1f) * revealFraction
                val x = TacticalChartGeometry.pointX(idx, points.size, size.width, padH, isRtl)
                val y = baselineY - norm * (usableH * 0.88f)
                Offset(x, y)
            }

            if (coords.size >= 2) {
                linePath.reset()
                fillPath.reset()

                linePath.moveTo(coords.first().x, coords.first().y)
                fillPath.moveTo(coords.first().x, baselineY)
                fillPath.lineTo(coords.first().x, coords.first().y)

                for (i in 0 until coords.lastIndex) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX1 = p0.x + (p1.x - p0.x) * 0.45f
                    val controlX2 = p0.x + (p1.x - p0.x) * 0.55f
                    linePath.cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX1, p0.y, controlX2, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(coords.last().x, baselineY)
                fillPath.close()

                // Gradient area fill (EvilCharts style)
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.36f),
                            accentColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        startY = padTop,
                        endY = baselineY
                    )
                )

                // Smooth main stroke
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(
                        width = 2.6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // Vertical indicator line for selected point
            val selOffset = coords[safeIndex]
            drawLine(
                color = guideLineColor,
                start = Offset(selOffset.x, padTop),
                end = Offset(selOffset.x, baselineY),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )

            // Draw nodes
            coords.forEachIndexed { idx, pt ->
                val isSelected = idx == safeIndex
                if (isSelected) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.25f),
                        radius = 9.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = Color(0xFF0A101D),
                        radius = 2.2.dp.toPx(),
                        center = pt
                    )
                } else {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.75f),
                        radius = 3.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        // X-Axis Labels: each label is centred under the exact X of its point
        // (same TacticalChartGeometry as the curve and tap detection).
        Layout(
            modifier = Modifier.fillMaxWidth(),
            content = {
                points.forEachIndexed { index, point ->
                    val isSelected = index == safeIndex
                    Text(
                        text = point.label,
                        modifier = Modifier.clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            selectedIndex = index
                            onPointSelected?.invoke(index, point)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) lineColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        ) { measurables, constraints ->
            val width = constraints.maxWidth
            val count = measurables.size
            val padH = chartPaddingH.toPx()
            val slot = if (count > 1) ((width - padH * 2f) / (count - 1)).toInt() else width
            val placeables = measurables.map {
                it.measure(constraints.copy(minWidth = 0, maxWidth = slot.coerceAtLeast(1)))
            }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(width, height) {
                placeables.forEachIndexed { i, p ->
                    val cx = TacticalChartGeometry.pointX(i, count, width.toFloat(), padH, isRtl)
                    val x = (cx - p.width / 2f).toInt().coerceIn(0, (width - p.width).coerceAtLeast(0))
                    // placeRelative would mirror again in RTL; X is already physical.
                    p.place(x, 0)
                }
            }
        }
    }
}

@Composable
private fun ChartReadoutBadge(
    text: String,
    lineColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(lineColor.copy(alpha = 0.12f))
            .border(1.dp, lineColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = lineColor,
            maxLines = 1
        )
    }
}
