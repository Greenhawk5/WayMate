package ir.alifaniani.waymate.ui.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateBorderSubtle
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateCyanContainer
import ir.alifaniani.waymate.ui.theme.WayMateElectricBlue
import ir.alifaniani.waymate.ui.theme.WayMateError
import ir.alifaniani.waymate.ui.theme.WayMateErrorContainer
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateOnCyan
import ir.alifaniani.waymate.ui.theme.WayMateSuccess
import ir.alifaniani.waymate.ui.theme.WayMateSuccessContainer
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceHighest
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextMuted
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary
import ir.alifaniani.waymate.ui.theme.WayMateTurquoise
import ir.alifaniani.waymate.ui.theme.WayMateWarning
import ir.alifaniani.waymate.ui.theme.WayMateWarningContainer

/**
 * Premium Tactical Card Container.
 * Deep midnight surface, subtle border, restrained elevation.
 */
@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = WayMateBorder,
    containerColor: Color = WayMateMidnightSurface,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Top Tactical Screen Header with Screen Title, Breadcrumb / Tagline,
 * and optional Trailing Action Controls.
 */
@Composable
fun TacticalScreenHeader(
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = WayMateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = WayMateTextSecondary,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailingContent()
        }
    }
}

/**
 * Status Badge Tone definitions.
 */
enum class TacticalTone {
    SUCCESS,
    WARNING,
    ALERT,
    INFO,
    NEUTRAL
}

/**
 * Tactical Status Pill Badge with illuminated indicator beacon.
 */
@Composable
fun TacticalStatusPill(
    text: String,
    tone: TacticalTone = TacticalTone.SUCCESS,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, textColor) = when (tone) {
        TacticalTone.SUCCESS -> Triple(WayMateSuccess, WayMateSuccessContainer, WayMateSuccess)
        TacticalTone.WARNING -> Triple(WayMateWarning, WayMateWarningContainer, WayMateWarning)
        TacticalTone.ALERT -> Triple(WayMateError, WayMateErrorContainer, WayMateError)
        TacticalTone.INFO -> Triple(WayMateCyan, WayMateCyanContainer, WayMateCyan)
        TacticalTone.NEUTRAL -> Triple(WayMateTextSecondary, WayMateSurfaceVariant, WayMateTextSecondary)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor.copy(alpha = 0.85f))
            .border(1.dp, dotColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Compact Tactical Metric Chip.
 */
@Composable
fun TacticalStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    highlight: Boolean = false
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight) WayMateCyanContainer.copy(alpha = 0.5f) else WayMateSurfaceVariant)
            .border(
                1.dp,
                if (highlight) WayMateCyan.copy(alpha = 0.5f) else WayMateBorderSubtle,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (highlight) WayMateCyan else WayMateTextSecondary
            )
        }
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = WayMateTextMuted
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (highlight) WayMateCyan else WayMateTextPrimary
            )
        }
    }
}

/**
 * Tactical Action Button (Primary Cyan, Secondary Dark Navy, Destructive Red).
 */
@Composable
fun TacticalActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    maxLines: Int = 1
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.97f else 1.0f, label = "buttonPressScale")

    if (isPrimary && !isDestructive) {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .scale(scale),
            enabled = enabled && !isLoading,
            shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = WayMateCyan,
                contentColor = WayMateOnCyan,
                disabledContainerColor = WayMateSurfaceVariant,
                disabledContentColor = WayMateTextMuted
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                ApertureLoader(
                    size = 22.dp,
                    color = WayMateOnCyan
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = maxLines,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 48.dp)
                .scale(scale),
            enabled = enabled && !isLoading,
            shape = RoundedCornerShape(12.dp),
            interactionSource = interactionSource,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isDestructive) WayMateError else WayMateTextPrimary,
                containerColor = if (isDestructive) WayMateErrorContainer.copy(alpha = 0.3f) else WayMateSurfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(
                1.dp,
                if (isDestructive) WayMateError.copy(alpha = 0.7f) else WayMateBorder
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (isLoading) {
                ApertureLoader(
                    size = 22.dp,
                    color = if (isDestructive) WayMateError else WayMateCyan
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = maxLines,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Section title with operational emphasis.
 */
@Composable
fun TacticalSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WayMateTextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (badgeText != null) {
                TacticalStatusPill(text = badgeText, tone = TacticalTone.INFO)
            }
        }

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    color = WayMateCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Standard Tactical Confirmation Dialog.
 */
@Composable
fun TacticalConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String = "انصراف",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = WayMateMidnightSurface,
            border = BorderStroke(1.dp, if (isDestructive) WayMateError.copy(alpha = 0.5f) else WayMateBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDestructive) WayMateError else WayMateTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WayMateTextSecondary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Equal weights + single-line labels previously truncated longer
                    // confirm labels (e.g. "ارسال فوری SOS"). The confirm action gets
                    // the larger share and may wrap to two lines; dismiss stays short.
                    TacticalActionButton(
                        text = dismissText,
                        onClick = onDismiss,
                        isPrimary = false,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    TacticalActionButton(
                        text = confirmText,
                        onClick = onConfirm,
                        isPrimary = !isDestructive,
                        isDestructive = isDestructive,
                        maxLines = 2,
                        modifier = Modifier.weight(1.6f)
                    )
                }
            }
        }
    }
}

