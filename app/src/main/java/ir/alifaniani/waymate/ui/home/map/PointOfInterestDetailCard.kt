package ir.alifaniani.waymate.ui.home.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Inspection and action card for a selected Point of Interest.
 * Provides detailed metadata, creator identity, sync status, and edit/delete actions.
 */
@Composable
fun PointOfInterestDetailCard(
    point: PointOfInterest,
    onEdit: (PointOfInterest) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val categoryColor = when (point.category) {
        PointCategory.START_POINT -> Color(0xFF00E676)
        PointCategory.DESTINATION -> Color(0xFFFFD600)
        PointCategory.CAMPSITE -> Color(0xFFFF9100)
        PointCategory.REST_AREA -> Color(0xFF29B6F6)
        PointCategory.WATER_SOURCE -> Color(0xFF00B0FF)
        PointCategory.GENERAL -> Color(0xFFE040FB)
    }

    val dateFormatter = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = primaryColor, shape = RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151B22)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title & Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = point.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = point.category.displayNamePersian,
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (point.isPendingSync) {
                        TacticalStatusBadge(
                            text = "در صف ارسال",
                            tone = BadgeTone.WARNING
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    TacticalStatusBadge(
                        text = "نسخه ${point.version}",
                        tone = BadgeTone.NEUTRAL
                    )
                }
            }

            if (point.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = point.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC9D1D9),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Metadata Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "مختصات WGS-84", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
                    Text(text = point.formattedCoordinates, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "ثبت‌کننده", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
                    Text(
                        text = point.createdByCallsign.ifBlank { "کاربر سیستم" },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "زمان ایجاد", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
                    Text(
                        text = if (point.createdAt > 0) dateFormatter.format(Date(point.createdAt)) else "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF8B949E)
                    )
                }
                if (point.updatedAt > point.createdAt) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "آخرین ویرایش", style = MaterialTheme.typography.labelSmall, color = Color(0xFF8B949E))
                        Text(
                            text = "${point.updatedByCallsign.ifBlank { "کاربر" }} (${dateFormatter.format(Date(point.updatedAt))})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B949E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TacticalOutlinedButton(
                    text = "ویرایش نقطه",
                    onClick = { onEdit(point) },
                    modifier = Modifier.weight(1f)
                )

                TacticalOutlinedButton(
                    text = "حذف نقطه",
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f)
                )

                TacticalOutlinedButton(
                    text = "بستن",
                    onClick = onDismiss,
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = {
                Text(
                    text = "تأیید حذف نقطه",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "آیا از حذف نقطه «${point.title}» اطمینان دارید؟ این عمل نقطه را از نقشه گروه پاک خواهد کرد.",
                    color = Color(0xFFC9D1D9)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete(point.id)
                    }
                ) {
                    Text(text = "بله، حذف شود", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(text = "انصراف", color = Color(0xFF8B949E))
                }
            },
            containerColor = Color(0xFF161B22),
            shape = RoundedCornerShape(12.dp)
        )
    }
}
