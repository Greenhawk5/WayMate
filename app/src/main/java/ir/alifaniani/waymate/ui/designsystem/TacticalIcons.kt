package ir.alifaniani.waymate.ui.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Dedicated, zero-dependency tactical vector icon suite for WayMate.
 * Inspired by Kubai Icons' layered geometric aesthetic: crisp 24x24 keylines,
 * balanced dual-layer opacity accents, and rounded stroke geometry.
 */
object TacticalIcons {

    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 4f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(16.17f, 11f)
                lineTo(4f, 11f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(12.17f)
                lineToRelative(-5.58f, 5.59f)
                lineTo(12f, 20f)
                lineToRelative(8f, -8f)
                close()
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(20f, 11f)
                horizontalLineTo(7.83f)
                lineToRelative(5.59f, -5.59f)
                lineTo(12f, 4f)
                lineToRelative(-8f, 8f)
                lineToRelative(8f, 8f)
                lineToRelative(1.41f, -1.41f)
                lineTo(7.83f, 13f)
                horizontalLineTo(20f)
                verticalLineToRelative(-2f)
                close()
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12.5f)
                lineTo(9.5f, 17f)
                lineTo(19f, 7.5f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6.5f, 6.5f)
                lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f)
                lineTo(6.5f, 17.5f)
            }
        }.build()
    }

    val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 5.5f)
                verticalLineTo(18.5f)
                moveTo(5.5f, 12f)
                horizontalLineTo(18.5f)
            }
        }.build()
    }

    val Minus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5.5f, 12f)
                horizontalLineTo(18.5f)
            }
        }.build()
    }

    val ChevronUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 14.5f)
                lineTo(12f, 8.5f)
                lineTo(18f, 14.5f)
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(6f, 9.5f)
                lineTo(12f, 15.5f)
                lineTo(18f, 9.5f)
            }
        }.build()
    }

    // Kubai-inspired Media & Simulation Controls
    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.2f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f
            ) {
                moveTo(9.5f, 7.6f)
                curveTo(9.5f, 6.95f, 10.22f, 6.56f, 10.77f, 6.9f)
                lineTo(17.2f, 11.1f)
                curveTo(17.7f, 11.42f, 17.7f, 12.18f, 17.2f, 12.5f)
                lineTo(10.77f, 16.7f)
                curveTo(10.22f, 17.04f, 9.5f, 16.65f, 9.5f, 16f)
                verticalLineTo(7.6f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.2f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f
            ) {
                moveTo(8.5f, 7.5f)
                horizontalLineTo(10.8f)
                verticalLineTo(16.5f)
                horizontalLineTo(8.5f)
                close()
                moveTo(13.2f, 7.5f)
                horizontalLineTo(15.5f)
                verticalLineTo(16.5f)
                horizontalLineTo(13.2f)
                close()
            }
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.2f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f
            ) {
                moveTo(8.5f, 8.5f)
                horizontalLineTo(15.5f)
                verticalLineTo(15.5f)
                horizontalLineTo(8.5f)
                close()
            }
        }.build()
    }

    val Replay: ImageVector by lazy {
        ImageVector.Builder(
            name = "Replay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4.5f, 10.5f)
                curveTo(5.3f, 6.8f, 8.6f, 4f, 12.5f, 4f)
                curveTo(17f, 4f, 20.5f, 7.6f, 20.5f, 12f)
                curveTo(20.5f, 16.4f, 17f, 20f, 12.5f, 20f)
                curveTo(9.2f, 20f, 6.4f, 18f, 5.1f, 15.1f)
                moveTo(4f, 6f)
                verticalLineTo(10.8f)
                horizontalLineTo(8.8f)
            }
        }.build()
    }

    // Kubai-inspired Map, GIS & Tactical Icons
    val Layers: ImageVector by lazy {
        ImageVector.Builder(
            name = "Layers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.3f
            ) {
                moveTo(12f, 13.2f)
                lineTo(3f, 17.5f)
                lineTo(12f, 21.8f)
                lineTo(21f, 17.5f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.8f)
                lineTo(3f, 7.3f)
                lineTo(12f, 11.8f)
                lineTo(21f, 7.3f)
                close()
                moveTo(3f, 12.4f)
                lineTo(12f, 16.9f)
                lineTo(21f, 12.4f)
                moveTo(3f, 17.3f)
                lineTo(12f, 21.8f)
                lineTo(21f, 17.3f)
            }
        }.build()
    }

    val Compass: ImageVector by lazy {
        ImageVector.Builder(
            name = "Compass",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
            }
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f
            ) {
                moveTo(16.2f, 7.8f)
                lineTo(13.6f, 13.6f)
                lineTo(7.8f, 16.2f)
                lineTo(10.4f, 10.4f)
                close()
            }
        }.build()
    }

    val Crosshair: ImageVector by lazy {
        ImageVector.Builder(
            name = "Crosshair",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(19f, 12f)
                curveTo(19f, 15.87f, 15.87f, 19f, 12f, 19f)
                curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
                curveTo(5f, 8.13f, 8.13f, 5f, 12f, 5f)
                curveTo(15.87f, 5f, 19f, 8.13f, 19f, 12f)
                close()
                moveTo(12f, 2f)
                verticalLineTo(5f)
                moveTo(12f, 19f)
                verticalLineTo(22f)
                moveTo(2f, 12f)
                horizontalLineTo(5f)
                moveTo(19f, 12f)
                horizontalLineTo(22f)
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(14f, 12f)
                curveTo(14f, 13.1f, 13.1f, 14f, 12f, 14f)
                curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
                curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f)
                curveTo(13.1f, 10f, 14f, 10.9f, 14f, 12f)
                close()
            }
        }.build()
    }

    val Mountain: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mountain",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.25f
            ) {
                moveTo(14.5f, 4.5f)
                lineTo(22f, 19.5f)
                horizontalLineTo(7f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(14.5f, 4.5f)
                lineTo(22f, 19.5f)
                horizontalLineTo(2f)
                lineTo(8f, 9.5f)
                lineTo(11.5f, 15f)
                moveTo(11.8f, 9.8f)
                lineTo(14.5f, 11.5f)
                lineTo(17.2f, 9.8f)
            }
        }.build()
    }

    val Radar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Radar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(16.5f, 12f)
                curveTo(16.5f, 14.48f, 14.48f, 16.5f, 12f, 16.5f)
                curveTo(9.52f, 16.5f, 7.5f, 14.48f, 7.5f, 12f)
                curveTo(7.5f, 9.52f, 9.52f, 7.5f, 12f, 7.5f)
                moveTo(12f, 12f)
                lineTo(18.2f, 5.8f)
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(13.8f, 12f)
                curveTo(13.8f, 12.99f, 12.99f, 13.8f, 12f, 13.8f)
                curveTo(11.01f, 13.8f, 10.2f, 12.99f, 10.2f, 12f)
                curveTo(10.2f, 11.01f, 11.01f, 10.2f, 12f, 10.2f)
                curveTo(12.99f, 10.2f, 13.8f, 11.01f, 13.8f, 12f)
                close()
            }
        }.build()
    }

    val Alert: ImageVector by lazy {
        ImageVector.Builder(
            name = "Alert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.2f
            ) {
                moveTo(12f, 3f)
                lineTo(22f, 20f)
                horizontalLineTo(2f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 3.2f)
                lineTo(21.5f, 19.8f)
                horizontalLineTo(2.5f)
                close()
                moveTo(12f, 9.5f)
                verticalLineTo(14f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 17.2f)
                curveTo(12.66f, 17.2f, 13.2f, 16.66f, 13.2f, 16f)
                curveTo(13.2f, 15.34f, 12.66f, 14.8f, 12f, 14.8f)
                curveTo(11.34f, 14.8f, 10.8f, 15.34f, 10.8f, 16f)
                curveTo(10.8f, 16.66f, 11.34f, 17.2f, 12f, 17.2f)
                close()
            }
        }.build()
    }

    val Sos: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sos",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Kubai siren / beacon icon
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.25f
            ) {
                moveTo(7f, 17f)
                verticalLineTo(12.5f)
                curveTo(7f, 9.74f, 9.24f, 7.5f, 12f, 7.5f)
                curveTo(14.76f, 7.5f, 17f, 9.74f, 17f, 12.5f)
                verticalLineTo(17f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(7f, 17f)
                verticalLineTo(12.5f)
                curveTo(7f, 9.74f, 9.24f, 7.5f, 12f, 7.5f)
                curveTo(14.76f, 7.5f, 17f, 9.74f, 17f, 12.5f)
                verticalLineTo(17f)
                moveTo(4.5f, 17f)
                horizontalLineTo(19.5f)
                verticalLineTo(20f)
                horizontalLineTo(4.5f)
                close()
                moveTo(12f, 3f)
                verticalLineTo(4.8f)
                moveTo(5.6f, 5.6f)
                lineTo(6.9f, 6.9f)
                moveTo(18.4f, 5.6f)
                lineTo(17.1f, 6.9f)
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 7.2f)
                verticalLineTo(12f)
                lineTo(15.4f, 14f)
            }
        }.build()
    }

    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.22f
            ) {
                moveTo(3f, 11.2f)
                lineTo(21f, 3.5f)
                lineTo(14.2f, 21f)
                lineTo(10.8f, 13.2f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 11.2f)
                lineTo(21f, 3.5f)
                lineTo(14.2f, 21f)
                lineTo(10.8f, 13.2f)
                close()
                moveTo(21f, 3.5f)
                lineTo(10.8f, 13.2f)
            }
        }.build()
    }

    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.2f
            ) {
                moveTo(12f, 2.5f)
                lineTo(20f, 5.8f)
                verticalLineTo(11.5f)
                curveTo(20f, 16.8f, 16.6f, 20.6f, 12f, 22f)
                curveTo(7.4f, 20.6f, 4f, 16.8f, 4f, 11.5f)
                verticalLineTo(5.8f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 2.5f)
                lineTo(20f, 5.8f)
                verticalLineTo(11.5f)
                curveTo(20f, 16.8f, 16.6f, 20.6f, 12f, 22f)
                curveTo(7.4f, 20.6f, 4f, 16.8f, 4f, 11.5f)
                verticalLineTo(5.8f)
                close()
                moveTo(8.8f, 12f)
                lineTo(11.1f, 14.3f)
                lineTo(15.5f, 9.8f)
            }
        }.build()
    }

    val Signal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Signal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.4f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4.5f, 19f)
                verticalLineTo(15f)
                moveTo(9.5f, 19f)
                verticalLineTo(11.5f)
                moveTo(14.5f, 19f)
                verticalLineTo(8f)
                moveTo(19.5f, 19f)
                verticalLineTo(4.5f)
            }
        }.build()
    }

    val Location: ImageVector by lazy {
        ImageVector.Builder(
            name = "Location",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 0.22f
            ) {
                moveTo(12f, 21.2f)
                curveTo(9f, 17.8f, 5f, 14f, 5f, 9.5f)
                curveTo(5f, 5.63f, 8.13f, 2.5f, 12f, 2.5f)
                curveTo(15.87f, 2.5f, 19f, 5.63f, 19f, 9.5f)
                curveTo(19f, 14f, 15f, 17.8f, 12f, 21.2f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 21.2f)
                curveTo(9f, 17.8f, 5f, 14f, 5f, 9.5f)
                curveTo(5f, 5.63f, 8.13f, 2.5f, 12f, 2.5f)
                curveTo(15.87f, 2.5f, 19f, 5.63f, 19f, 9.5f)
                curveTo(19f, 14f, 15f, 17.8f, 12f, 21.2f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(14.5f, 9.5f)
                curveTo(14.5f, 10.88f, 13.38f, 12f, 12f, 12f)
                curveTo(10.62f, 12f, 9.5f, 10.88f, 9.5f, 9.5f)
                curveTo(9.5f, 8.12f, 10.62f, 7f, 12f, 7f)
                curveTo(13.38f, 7f, 14.5f, 8.12f, 14.5f, 9.5f)
                close()
            }
        }.build()
    }

    val Eye: ImageVector by lazy {
        ImageVector.Builder(
            name = "Eye",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.2f, 12f)
                curveTo(5f, 7f, 8.3f, 4.8f, 12f, 4.8f)
                curveTo(15.7f, 4.8f, 19f, 7f, 21.8f, 12f)
                curveTo(19f, 17f, 15.7f, 19.2f, 12f, 19.2f)
                curveTo(8.3f, 19.2f, 5f, 17f, 2.2f, 12f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                close()
            }
        }.build()
    }

    val EyeOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(2.2f, 12f)
                curveTo(5f, 7f, 8.3f, 4.8f, 12f, 4.8f)
                curveTo(15.7f, 4.8f, 19f, 7f, 21.8f, 12f)
                curveTo(19f, 17f, 15.7f, 19.2f, 12f, 19.2f)
                curveTo(8.3f, 19.2f, 5f, 17f, 2.2f, 12f)
                close()
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }.build()
    }

    // Navigation Destinations
    val TacticalMap: ImageVector by lazy {
        ImageVector.Builder(
            name = "TacticalMap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(20.5f, 3f)
                lineToRelative(-0.16f, 0.03f)
                lineTo(15f, 5.1f)
                lineTo(9f, 3f)
                lineTo(3.36f, 4.9f)
                curveToRelative(-0.21f, 0.07f, -0.36f, 0.25f, -0.36f, 0.48f)
                verticalLineTo(20.5f)
                curveToRelative(0f, 0.28f, 0.22f, 0.5f, 0.5f, 0.5f)
                lineToRelative(0.16f, -0.03f)
                lineTo(9f, 18.9f)
                lineToRelative(6f, 2.1f)
                lineToRelative(5.64f, -1.9f)
                curveToRelative(0.21f, -0.07f, 0.36f, -0.25f, 0.36f, -0.48f)
                verticalLineTo(3.5f)
                curveToRelative(0f, -0.28f, -0.22f, -0.5f, -0.5f, -0.5f)
                close()
                moveTo(15f, 19f)
                lineToRelative(-6f, -2.11f)
                verticalLineTo(5f)
                lineToRelative(6f, 2.11f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    val Overview: ImageVector by lazy {
        ImageVector.Builder(
            name = "Overview",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3f, 13f)
                horizontalLineToRelative(8f)
                verticalLineTo(3f)
                horizontalLineTo(3f)
                verticalLineToRelative(10f)
                close()
                moveTo(3f, 21f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-6f)
                horizontalLineTo(3f)
                verticalLineToRelative(6f)
                close()
                moveTo(13f, 21f)
                horizontalLineToRelative(8f)
                verticalLineToRelative(-10f)
                horizontalLineToRelative(-8f)
                verticalLineToRelative(10f)
                close()
                moveTo(13f, 3f)
                verticalLineToRelative(6f)
                horizontalLineToRelative(8f)
                verticalLineTo(3f)
                horizontalLineToRelative(-8f)
                close()
            }
        }.build()
    }

    val Teams: ImageVector by lazy {
        ImageVector.Builder(
            name = "Teams",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(16f, 11f)
                curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
                reflectiveCurveTo(17.66f, 5f, 16f, 5f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                close()
                moveTo(8f, 11f)
                curveToRelative(1.66f, 0f, 2.99f, -1.34f, 2.99f, -3f)
                reflectiveCurveTo(9.66f, 5f, 8f, 5f)
                curveToRelative(-1.66f, 0f, -3f, 1.34f, -3f, 3f)
                reflectiveCurveToRelative(1.34f, 3f, 3f, 3f)
                close()
                moveTo(8f, 13f)
                curveToRelative(-2.33f, 0f, -7f, 1.17f, -7f, 3.5f)
                verticalLineTo(19f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
                close()
                moveTo(16f, 13f)
                curveToRelative(-0.29f, 0f, -0.62f, 0.02f, -0.97f, 0.05f)
                curveToRelative(1.16f, 0.84f, 1.97f, 1.97f, 1.97f, 3.45f)
                verticalLineTo(19f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-2.5f)
                curveToRelative(0f, -2.33f, -4.67f, -3.5f, -7f, -3.5f)
                close()
            }
        }.build()
    }

    val Operations: ImageVector by lazy {
        ImageVector.Builder(
            name = "Operations",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(20f, 2f)
                horizontalLineTo(4f)
                curveToRelative(-1.1f, 0f, -1.99f, 0.9f, -1.99f, 2f)
                lineTo(2f, 22f)
                lineToRelative(4f, -4f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(4f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
                moveTo(18f, 14f)
                horizontalLineTo(6f)
                verticalLineToRelative(-2f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                close()
                moveTo(18f, 11f)
                horizontalLineTo(6f)
                verticalLineTo(9f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                close()
                moveTo(18f, 8f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(2f)
                close()
            }
        }.build()
    }

    val Profile: ImageVector by lazy {
        ImageVector.Builder(
            name = "Profile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 12f)
                curveToRelative(2.21f, 0f, 4f, -1.79f, 4f, -4f)
                reflectiveCurveToRelative(-1.79f, -4f, -4f, -4f)
                reflectiveCurveToRelative(-4f, 1.79f, -4f, 4f)
                reflectiveCurveToRelative(1.79f, 4f, 4f, 4f)
                close()
                moveTo(12f, 14f)
                curveToRelative(-2.67f, 0f, -8f, 1.34f, -8f, 4f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(-2f)
                curveToRelative(0f, -2.66f, -5.33f, -4f, -8f, -4f)
                close()
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.07f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.41f, 0.12f, -0.61f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.22f, -0.37f, -0.29f, -0.59f, -0.22f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineToRelative(-0.36f, -2.54f)
                curveToRelative(-0.04f, -0.24f, -0.24f, -0.41f, -0.48f, -0.41f)
                horizontalLineToRelative(-3.84f)
                curveToRelative(-0.24f, 0f, -0.43f, 0.17f, -0.47f, 0.41f)
                lineToRelative(-0.36f, 2.54f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.57f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.47f, 0f, -0.59f, 0.22f)
                lineTo(2.74f, 8.87f)
                curveToRelative(-0.12f, 0.21f, -0.08f, 0.47f, 0.12f, 0.61f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.05f, 0.3f, -0.09f, 0.63f, -0.09f, 0.94f)
                reflectiveCurveToRelative(0.02f, 0.64f, 0.07f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.41f, -0.12f, 0.61f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.22f, 0.37f, 0.29f, 0.59f, 0.22f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.05f, 0.24f, 0.24f, 0.41f, 0.48f, 0.41f)
                horizontalLineToRelative(3.84f)
                curveToRelative(0.24f, 0f, 0.44f, -0.17f, 0.47f, -0.41f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.47f, 0f, 0.59f, -0.22f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.22f, 0.07f, -0.47f, -0.12f, -0.61f)
                lineToRelative(-2.01f, -1.58f)
                close()
                moveTo(12f, 15.6f)
                curveToRelative(-1.98f, 0f, -3.6f, -1.62f, -3.6f, -3.6f)
                reflectiveCurveToRelative(1.62f, -3.6f, 3.6f, -3.6f)
                reflectiveCurveToRelative(3.6f, 1.62f, 3.6f, 3.6f)
                reflectiveCurveToRelative(-1.62f, 3.6f, -3.6f, 3.6f)
                close()
            }
        }.build()
    }

    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(13f, 17f)
                horizontalLineToRelative(-2f)
                verticalLineToRelative(-6f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(6f)
                close()
                moveTo(13f, 9f)
                horizontalLineToRelative(-2f)
                verticalLineTo(7f)
                horizontalLineToRelative(2f)
                verticalLineToRelative(2f)
                close()
            }
        }.build()
    }
}
