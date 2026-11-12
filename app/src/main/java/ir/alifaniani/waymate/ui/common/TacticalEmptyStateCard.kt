package ir.alifaniani.waymate.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Standard reusable empty-state card for the WayMate Dark Tactical UI,
 * cleanly integrating pre-designed illustration assets.
 */
@Composable
fun TacticalEmptyStateCard(
    @DrawableRes illustrationResId: Int,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onActionClicked: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = illustrationResId),
            contentDescription = title,
            modifier = Modifier
                .size(150.dp)
                .padding(bottom = 8.dp),
            contentScale = ContentScale.Fit
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        if (!actionText.isNullOrBlank() && onActionClicked != null) {
            Spacer(modifier = Modifier.height(14.dp))
            TacticalOutlinedButton(
                text = actionText,
                onClick = onActionClicked
            )
        }
    }
}

