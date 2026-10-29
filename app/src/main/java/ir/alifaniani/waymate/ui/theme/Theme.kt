package ir.alifaniani.waymate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Tactical color extensions for status feedback (Online, Warning, Alert).
 */
@Immutable
data class TacticalStatusColors(
    val success: Color = TacticalSuccess,
    val onSuccess: Color = TacticalOnSuccess,
    val successContainer: Color = TacticalSuccessContainer,
    val warning: Color = TacticalWarning,
    val onWarning: Color = TacticalOnWarning,
    val warningContainer: Color = TacticalWarningContainer,
    val error: Color = TacticalError,
    val onError: Color = TacticalOnError,
    val errorContainer: Color = TacticalErrorContainer,
    val onErrorContainer: Color = TacticalOnErrorContainer
)

val LocalTacticalStatusColors = staticCompositionLocalOf { TacticalStatusColors() }

private val TacticalDarkColorScheme = darkColorScheme(
    primary = TacticalPrimary,
    onPrimary = TacticalOnPrimary,
    primaryContainer = TacticalPrimaryContainer,
    onPrimaryContainer = TacticalOnPrimaryContainer,

    secondary = TacticalSecondary,
    onSecondary = TacticalOnSecondary,
    secondaryContainer = TacticalSecondaryContainer,
    onSecondaryContainer = TacticalOnSecondaryContainer,

    background = TacticalBackground,
    onBackground = TacticalOnBackground,

    surface = TacticalSurface,
    onSurface = TacticalOnSurface,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TacticalOnSurfaceVariant,

    outline = TacticalOutline,
    outlineVariant = TacticalOutlineVariant,

    error = TacticalError,
    onError = TacticalOnError,
    errorContainer = TacticalErrorContainer,
    onErrorContainer = TacticalOnErrorContainer
)

/**
 * WayMate Theme.
 *
 * Enforces a dedicated Dark Tactical design system with high-contrast text,
 * OLED battery efficiency, zero facial glare, and native Persian RTL layout direction.
 */
@Composable
fun WayMateTheme(
    statusColors: TacticalStatusColors = TacticalStatusColors(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalTacticalStatusColors provides statusColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = TacticalDarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}