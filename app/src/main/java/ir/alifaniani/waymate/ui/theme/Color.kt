package ir.alifaniani.waymate.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * WayMate Dark Tactical Design System Tokens.
 *
 * Designed specifically for low-light night operations, high readability,
 * OLED battery conservation, and zero facial glare.
 */

// Backgrounds & Surface Hierarchy
val TacticalBackground = Color(0xFF0B0F14)       // Deep OLED charcoal black
val TacticalSurface = Color(0xFF131920)          // Primary elevated card surface
val TacticalSurfaceVariant = Color(0xFF1A222C)   // Secondary elevated container
val TacticalSurfaceHighest = Color(0xFF242E3B)   // Input fields & active selection containers

// Primary Action & Branding (Restrained Tactical Cyan-Blue)
val TacticalPrimary = Color(0xFF3B82F6)          // Primary interactive accent
val TacticalOnPrimary = Color(0xFFFFFFFF)        // High-contrast text on primary buttons
val TacticalPrimaryContainer = Color(0xFF1E3A8A) // Subtle active highlight container
val TacticalOnPrimaryContainer = Color(0xFFBFDBFE)

// Secondary & Auxiliary
val TacticalSecondary = Color(0xFF64748B)        // Muted slate for secondary actions
val TacticalOnSecondary = Color(0xFFF8FAFC)
val TacticalSecondaryContainer = Color(0xFF1E293B)
val TacticalOnSecondaryContainer = Color(0xFFCBD5E1)

// Typography & Content Contrast
val TacticalOnBackground = Color(0xFFF1F5F9)     // Primary high-emphasis text (~95% contrast)
val TacticalOnSurface = Color(0xFFE2E8F0)        // Card body text
val TacticalOnSurfaceVariant = Color(0xFF94A3B8) // Secondary / helper text / timestamps
val TacticalOutline = Color(0xFF334155)          // Card borders and field outlines
val TacticalOutlineVariant = Color(0xFF1E293B)   // Subtle dividers

// Operational Status & Semantic Feedback
val TacticalSuccess = Color(0xFF10B981)          // Operational: Normal / Active / Online
val TacticalSuccessContainer = Color(0xFF064E3B)
val TacticalOnSuccess = Color(0xFFFFFFFF)

val TacticalWarning = Color(0xFFF59E0B)          // Operational: Pursuing / Stale connection
val TacticalWarningContainer = Color(0xFF78350F)
val TacticalOnWarning = Color(0xFFFFFFFF)

val TacticalError = Color(0xFFEF4444)            // Operational: Engaged / Emergency alert / Error
val TacticalErrorContainer = Color(0xFF450A0A)
val TacticalOnError = Color(0xFFFFFFFF)
val TacticalOnErrorContainer = Color(0xFFFCA5A5)