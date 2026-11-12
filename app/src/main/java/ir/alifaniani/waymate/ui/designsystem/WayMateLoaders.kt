package ir.alifaniani.waymate.ui.designsystem

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Checks whether system animations are enabled (respects accessibility reduced-motion settings).
 */
@Composable
private fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) > 0f
        }.getOrDefault(true)
    }
}

/**
 * Aperture Loader — Faithfully modeled after Circle Loaders (#aperture).
 *
 * Uses three concentric segmented rings rotating at coprime intervals (2.8s CW, 3.9s CCW, 2.1s CW)
 * around a breathing central core, surrounded by a subtle outer reference halo.
 * Ideal for general content loading, command panels, and action buttons.
 */
@Composable
fun ApertureLoader(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "ApertureTransition")

    val rot1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ApertureRing1"
    )

    val rot2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ApertureRing2"
    )

    val rot3 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ApertureRing3"
    )

    val corePulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ApertureCorePulse"
    )

    val r1Angle = if (animationsEnabled) rot1 else 0f
    val r2Angle = if (animationsEnabled) rot2 else 42f
    val r3Angle = if (animationsEnabled) rot3 else -28f
    val coreScale = if (animationsEnabled) 0.55f + 0.45f * corePulse else 1f
    val coreAlpha = if (animationsEnabled) 0.45f + 0.55f * corePulse else 1f

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
    ) {
        val scale = this.size.minDimension / 64f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer reference ring (r = 30.5, strokeWidth = 1.0, opacity = 0.10)
        drawCircle(
            color = color.copy(alpha = 0.12f),
            radius = 30.5f * scale,
            center = center,
            style = Stroke(width = 1.0f * scale)
        )

        // Ring 1 (r = 27, strokeWidth = 2.0, opacity = 0.95, dasharray = "34 14 9 43")
        val r1 = 27f * scale
        val r1TopLeft = Offset(center.x - r1, center.y - r1)
        val r1Size = Size(r1 * 2f, r1 * 2f)
        val r1Stroke = Stroke(width = 2.0f * scale, cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = 0.95f),
            startAngle = r1Angle - 90f,
            sweepAngle = 122.4f,
            useCenter = false,
            topLeft = r1TopLeft,
            size = r1Size,
            style = r1Stroke
        )
        drawArc(
            color = color.copy(alpha = 0.95f),
            startAngle = r1Angle - 90f + 172.8f,
            sweepAngle = 32.4f,
            useCenter = false,
            topLeft = r1TopLeft,
            size = r1Size,
            style = r1Stroke
        )

        // Ring 2 (r = 20, strokeWidth = 2.4, opacity = 0.72, dasharray = "24 10 24 42")
        val r2 = 20f * scale
        val r2TopLeft = Offset(center.x - r2, center.y - r2)
        val r2Size = Size(r2 * 2f, r2 * 2f)
        val r2Stroke = Stroke(width = 2.4f * scale, cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = 0.72f),
            startAngle = r2Angle - 90f,
            sweepAngle = 86.4f,
            useCenter = false,
            topLeft = r2TopLeft,
            size = r2Size,
            style = r2Stroke
        )
        drawArc(
            color = color.copy(alpha = 0.72f),
            startAngle = r2Angle - 90f + 122.4f,
            sweepAngle = 86.4f,
            useCenter = false,
            topLeft = r2TopLeft,
            size = r2Size,
            style = r2Stroke
        )

        // Ring 3 (r = 13, strokeWidth = 2.8, opacity = 0.52, dasharray = "16 12 16 56")
        val r3 = 13f * scale
        val r3TopLeft = Offset(center.x - r3, center.y - r3)
        val r3Size = Size(r3 * 2f, r3 * 2f)
        val r3Stroke = Stroke(width = 2.8f * scale, cap = StrokeCap.Round)
        drawArc(
            color = color.copy(alpha = 0.52f),
            startAngle = r3Angle - 90f,
            sweepAngle = 57.6f,
            useCenter = false,
            topLeft = r3TopLeft,
            size = r3Size,
            style = r3Stroke
        )
        drawArc(
            color = color.copy(alpha = 0.52f),
            startAngle = r3Angle - 90f + 100.8f,
            sweepAngle = 57.6f,
            useCenter = false,
            topLeft = r3TopLeft,
            size = r3Size,
            style = r3Stroke
        )

        // Central breathing core (r = 2.6)
        drawCircle(
            color = color.copy(alpha = coreAlpha),
            radius = 2.6f * scale * coreScale,
            center = center
        )
    }
}

private data class RadarBlipSpec(
    val cx: Float,
    val cy: Float,
    val bearingDeg: Float
)

private val DefaultRadarBlips = listOf(
    RadarBlipSpec(cx = 43.8f, cy = 18.2f, bearingDeg = 40f),
    RadarBlipSpec(cx = 21.2f, cy = 47.9f, bearingDeg = 214f),
    RadarBlipSpec(cx = 18.27f, cy = 20.48f, bearingDeg = 310f)
)

/**
 * Radar Loader — Faithfully modeled after Circle Loaders (#radar).
 *
 * Uses concentric range rings, orthogonal crosshairs, a 36-wedge phosphor sweep sector,
 * and three target contacts that flare and decay right as the sweep line crosses their bearing.
 * Ideal for map loading, GIS vector parsing, and location discovery.
 */
@Composable
fun RadarLoader(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val transition = rememberInfiniteTransition(label = "RadarTransition")

    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweepAngle"
    )

    val currentAngle = if (animationsEnabled) sweepAngle else 214f
    val wedgePath = remember { Path() }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
    ) {
        val scale = this.size.minDimension / 64f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = 31.5f * scale
        val innerRadius = 15.0f * scale

        // Guide rings (opacity = 0.16)
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = 1.0f * scale)
        )
        drawCircle(
            color = color.copy(alpha = 0.18f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = 1.0f * scale)
        )

        // Crosshairs (opacity = 0.13)
        drawLine(
            color = color.copy(alpha = 0.14f),
            start = Offset(center.x - outerRadius, center.y),
            end = Offset(center.x + outerRadius, center.y),
            strokeWidth = 1.0f * scale
        )
        drawLine(
            color = color.copy(alpha = 0.14f),
            start = Offset(center.x, center.y - outerRadius),
            end = Offset(center.x, center.y + outerRadius),
            strokeWidth = 1.0f * scale
        )

        // 36-wedge phosphor sweep trail (covering 64.8 degrees behind the leading edge)
        val wedgeCount = 36
        val wedgeStepDeg = 1.8f
        val topLeft = Offset(center.x - outerRadius, center.y - outerRadius)
        val arcSize = Size(outerRadius * 2f, outerRadius * 2f)

        for (i in 0 until wedgeCount) {
            val fraction = 1f - (i.toFloat() / wedgeCount.toFloat())
            val alpha = 0.36f * fraction * fraction
            val startDeg = currentAngle - 90f - (i + 1) * wedgeStepDeg
            wedgePath.reset()
            wedgePath.moveTo(center.x, center.y)
            wedgePath.arcTo(
                rect = androidx.compose.ui.geometry.Rect(topLeft, arcSize),
                startAngleDegrees = startDeg,
                sweepAngleDegrees = wedgeStepDeg + 0.35f,
                forceMoveTo = false
            )
            wedgePath.close()
            drawPath(
                path = wedgePath,
                color = color.copy(alpha = alpha)
            )
        }

        // Leading sweep line
        val rad = (currentAngle - 90f) * (PI / 180.0)
        val lineEnd = Offset(
            x = center.x + (outerRadius * cos(rad)).toFloat(),
            y = center.y + (outerRadius * sin(rad)).toFloat()
        )
        drawLine(
            color = color.copy(alpha = 0.92f),
            start = center,
            end = lineEnd,
            strokeWidth = 1.6f * scale,
            cap = StrokeCap.Round
        )

        // Bearing-synchronized target blips
        for (blip in DefaultRadarBlips) {
            val delta = ((currentAngle - blip.bearingDeg) % 360f + 360f) % 360f
            val progress = delta / 360f // 0..1 since sweep crossed blip

            val blipAlpha: Float
            val blipScale: Float
            if (!animationsEnabled) {
                blipAlpha = 0.7f
                blipScale = 1.0f
            } else if (progress < 0.05f) {
                val t = progress / 0.05f
                blipAlpha = t
                blipScale = 0.4f + 0.85f * t
            } else if (progress < 0.45f) {
                val t = (progress - 0.05f) / 0.40f
                blipAlpha = 1.0f - 0.65f * t
                blipScale = 1.25f - 0.25f * t
            } else if (progress < 0.72f) {
                val t = (progress - 0.45f) / 0.27f
                blipAlpha = 0.35f * (1f - t)
                blipScale = 1.0f - 0.6f * t
            } else {
                blipAlpha = 0f
                blipScale = 0.4f
            }

            if (blipAlpha > 0.01f) {
                val blipCenter = Offset(
                    x = center.x + (blip.cx - 32f) * scale,
                    y = center.y + (blip.cy - 32f) * scale
                )
                // Soft glow halo on flare
                if (blipAlpha > 0.5f) {
                    drawCircle(
                        color = color.copy(alpha = (blipAlpha - 0.5f) * 0.45f),
                        radius = 4.2f * scale * blipScale,
                        center = blipCenter
                    )
                }
                drawCircle(
                    color = color.copy(alpha = blipAlpha),
                    radius = 2.1f * scale * blipScale,
                    center = blipCenter
                )
            }
        }
    }
}

