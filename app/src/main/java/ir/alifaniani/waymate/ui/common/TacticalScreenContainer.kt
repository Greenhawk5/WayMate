package ir.alifaniani.waymate.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard screen container providing:
 * - OLED-friendly tactical background
 * - Responsive horizontal constraint (centered max 540dp on tablets/landscape)
 * - Automatic keyboard avoidance via [imePadding]
 * - Smooth vertical scrolling for small screens and landscape orientation
 */
@Composable
fun TacticalScreenContainer(
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .then(
                    if (scrollable) Modifier.verticalScroll(scrollState)
                    else Modifier
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}
