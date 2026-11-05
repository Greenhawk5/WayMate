package ir.alifaniani.waymate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.ui.theme.LocalTacticalStatusColors

enum class BadgeTone {
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL
}

/**
 * Accessible status chip that pairs color with text and an indicator dot
 * to ensure state is never communicated through color alone.
 *
 * Guarantees horizontal integrity: maxLines = 1 and softWrap = false prevent
 * vertical character-by-character stacking even when placed in constrained containers.
 */
@Composable
fun TacticalStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.NEUTRAL
) {
    val statusColors = LocalTacticalStatusColors.current

    val (containerColor, contentColor, dotColor) = when (tone) {
        BadgeTone.SUCCESS -> Triple(
            statusColors.successContainer.copy(alpha = 0.5f),
            statusColors.onSuccess,
            statusColors.success
        )
        BadgeTone.WARNING -> Triple(
            statusColors.warningContainer.copy(alpha = 0.5f),
            statusColors.onWarning,
            statusColors.warning
        )
        BadgeTone.ERROR -> Triple(
            statusColors.errorContainer.copy(alpha = 0.5f),
            statusColors.onError,
            statusColors.error
        )
        BadgeTone.NEUTRAL -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.primary
        )
    }

    Row(
        modifier = modifier
            .wrapContentSize()
            .background(containerColor, RoundedCornerShape(12.dp))
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            softWrap = false
        )
    }
}
