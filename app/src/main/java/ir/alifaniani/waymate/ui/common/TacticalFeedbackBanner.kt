package ir.alifaniani.waymate.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.ui.theme.LocalTacticalStatusColors

enum class BannerType {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

/**
 * Standard semantic feedback banner for operational notices, warnings, and errors.
 */
@Composable
fun TacticalFeedbackBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    type: BannerType = BannerType.INFO
) {
    val statusColors = LocalTacticalStatusColors.current

    val (containerColor, borderColor, contentColor, tagLabel) = when (type) {
        BannerType.ERROR -> Quadruple(
            statusColors.errorContainer.copy(alpha = 0.6f),
            statusColors.error,
            statusColors.onErrorContainer,
            "هشدار"
        )
        BannerType.WARNING -> Quadruple(
            statusColors.warningContainer.copy(alpha = 0.6f),
            statusColors.warning,
            statusColors.onWarning,
            "توجه"
        )
        BannerType.SUCCESS -> Quadruple(
            statusColors.successContainer.copy(alpha = 0.6f),
            statusColors.success,
            statusColors.onSuccess,
            "موفق"
        )
        BannerType.INFO -> Quadruple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "اطلاعیه"
        )
    }

    val tagTextColor = if (type == BannerType.INFO) {
        MaterialTheme.colorScheme.primary
    } else {
        borderColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[$tagLabel]",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = tagTextColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
            }
            if (message.isNotBlank()) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
