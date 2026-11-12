package ir.alifaniani.waymate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WayMate Midnight Navy & Electric Cyan Tactical Design System Tokens.
 *
 * Designed from the official WayMate visual identity:
 * - Deep midnight navy foundations
 * - Refined dark blue surfaces with subtle edge contrast
 * - High-visibility cyan and turquoise brand accents
 * - Electric-blue highlights
 * - High-contrast cool-gray typography
 * - Semantic operational states (Online, Alert, SOS, Sync)
 */

// Foundations & Surfaces
val WayMateMidnightDark = Color(0xFF070B13)       // Deepest OLED midnight navy (Background)
val WayMateMidnightSurface = Color(0xFF0D1424)    // Primary elevated card surface
val WayMateSurfaceVariant = Color(0xFF141F35)     // Secondary elevated container
val WayMateSurfaceHighest = Color(0xFF1C2B49)     // Active / pressed / input container
val WayMateSurfaceHover = Color(0xFF223456)       // Hover & interactive highlight surface

// Brand Identity: Electric Cyan & Turquoise & Electric Blue
val WayMateCyan = Color(0xFF00E5FF)               // Core brand interactive accent
val WayMateOnCyan = Color(0xFF03141F)             // High-contrast dark navy on cyan
val WayMateCyanContainer = Color(0xFF043444)      // Subtle cyan-tinted active container
val WayMateOnCyanContainer = Color(0xFF8CF4FF)

val WayMateTurquoise = Color(0xFF00C4B4)          // Tactical supporting brand accent
val WayMateElectricBlue = Color(0xFF0088FF)       // Secondary operational accent
val WayMateElectricBlueContainer = Color(0xFF0A254A)

// Typography & Content Contrast (Cool Gray Palette)
val WayMateTextPrimary = Color(0xFFF1F5F9)        // High-emphasis text (95% contrast)
val WayMateTextSecondary = Color(0xFF94A3B8)      // Supporting text / labels
val WayMateTextMuted = Color(0xFF64748B)          // Captions / inactive indicators
val WayMateBorder = Color(0xFF1E2F50)             // Card borders and clean strokes
val WayMateBorderSubtle = Color(0xFF15223A)       // Dividers and soft lines

// Semantic Operational States
val WayMateSuccess = Color(0xFF00E676)            // Active / Online / Healthy
val WayMateSuccessContainer = Color(0xFF053322)
val WayMateOnSuccess = Color(0xFF021B12)

val WayMateWarning = Color(0xFFFFB300)            // Pursuing / Attention required / Stale
val WayMateWarningContainer = Color(0xFF382305)
val WayMateOnWarning = Color(0xFF211500)

val WayMateError = Color(0xFFFF3D71)              // SOS / Engaged / Error / Critical
val WayMateErrorContainer = Color(0xFF380C16)
val WayMateOnError = Color(0xFFFFFFFF)

// Legacy Aliases for Seamless Backward Compatibility
val TacticalBackground = WayMateMidnightDark
val TacticalSurface = WayMateMidnightSurface
val TacticalSurfaceVariant = WayMateSurfaceVariant
val TacticalSurfaceHighest = WayMateSurfaceHighest

val TacticalPrimary = WayMateCyan
val TacticalOnPrimary = WayMateOnCyan
val TacticalPrimaryContainer = WayMateCyanContainer
val TacticalOnPrimaryContainer = WayMateOnCyanContainer

val TacticalSecondary = WayMateElectricBlue
val TacticalOnSecondary = Color(0xFFFFFFFF)
val TacticalSecondaryContainer = WayMateElectricBlueContainer
val TacticalOnSecondaryContainer = Color(0xFFD0E4FF)

val TacticalOnBackground = WayMateTextPrimary
val TacticalOnSurface = WayMateTextPrimary
val TacticalOnSurfaceVariant = WayMateTextSecondary
val TacticalOutline = WayMateBorder
val TacticalOutlineVariant = WayMateBorderSubtle

val TacticalSuccess = WayMateSuccess
val TacticalSuccessContainer = WayMateSuccessContainer
val TacticalOnSuccess = Color(0xFFFFFFFF)

val TacticalWarning = WayMateWarning
val TacticalWarningContainer = WayMateWarningContainer
val TacticalOnWarning = Color(0xFFFFFFFF)

val TacticalError = WayMateError
val TacticalErrorContainer = WayMateErrorContainer
val TacticalOnError = WayMateOnError
val TacticalOnErrorContainer = Color(0xFFFFB4AB)
