package ir.alifaniani.waymate.ui.home.map

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton

/**
 * Dialog for creating a new Point of Interest or editing an existing one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PointOfInterestDialog(
    initialPoint: PointOfInterest? = null,
    defaultLatitude: Double = 28.5303,
    defaultLongitude: Double = 53.6008,
    onConfirmCreate: (title: String, description: String, category: PointCategory, latitude: Double, longitude: Double) -> Unit,
    onConfirmUpdate: (id: String, title: String, description: String, category: PointCategory, latitude: Double, longitude: Double, version: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = initialPoint != null

    var title by remember { mutableStateOf(initialPoint?.title ?: "") }
    var description by remember { mutableStateOf(initialPoint?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialPoint?.category ?: PointCategory.GENERAL) }
    var latitudeText by remember { mutableStateOf(initialPoint?.latitude?.toString() ?: defaultLatitude.toString()) }
    var longitudeText by remember { mutableStateOf(initialPoint?.longitude?.toString() ?: defaultLongitude.toString()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "ویرایش نقطه نشانه" else "افزودن نقطه نشانه جدید (POI)",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("عنوان نقطه") },
                    placeholder = { Text("مثلاً: چشمه آب، قله، محل کمپ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF30363D)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text(
                    text = "دسته‌بندی کارکردی",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PointCategory.values().forEach { cat ->
                        val isSelected = cat == selectedCategory
                        val chipColor = when (cat) {
                            PointCategory.START_POINT -> Color(0xFF00E676)
                            PointCategory.DESTINATION -> Color(0xFFFFD600)
                            PointCategory.CAMPSITE -> Color(0xFFFF9100)
                            PointCategory.REST_AREA -> Color(0xFF29B6F6)
                            PointCategory.WATER_SOURCE -> Color(0xFF00B0FF)
                            PointCategory.GENERAL -> Color(0xFFE040FB)
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) chipColor.copy(alpha = 0.25f) else Color(0xFF21262D),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) chipColor else Color(0xFF30363D),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat.displayNamePersian,
                                fontSize = 11.sp,
                                color = if (isSelected) chipColor else Color(0xFFC9D1D9),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات تکمیلی (اختیاری)") },
                    placeholder = { Text("مشخصات دسترسی، وضعیت آب، کیفیت استقرار...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color(0xFF30363D)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Coordinates Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latitudeText,
                        onValueChange = {
                            latitudeText = it
                            validationError = null
                        },
                        label = { Text("عرض جغرافیایی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF30363D)
                        )
                    )

                    OutlinedTextField(
                        value = longitudeText,
                        onValueChange = {
                            longitudeText = it
                            validationError = null
                        },
                        label = { Text("طول جغرافیایی") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0xFF30363D)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TacticalPrimaryButton(
                text = if (isEditing) "ذخیره تغییرات" else "ثبت نقطه",
                onClick = {
                    if (title.isBlank()) {
                        validationError = "لطفاً عنوان نقطه را وارد نمایید."
                        return@TacticalPrimaryButton
                    }
                    val lat = latitudeText.toDoubleOrNull()
                    if (lat == null || lat < -90.0 || lat > 90.0) {
                        validationError = "عرض جغرافیایی باید عددی بین ۹۰- و ۹۰+ باشد."
                        return@TacticalPrimaryButton
                    }
                    val lon = longitudeText.toDoubleOrNull()
                    if (lon == null || lon < -180.0 || lon > 180.0) {
                        validationError = "طول جغرافیایی باید عددی بین ۱۸۰- و ۱۸۰+ باشد."
                        return@TacticalPrimaryButton
                    }

                    if (isEditing) {
                        onConfirmUpdate(
                            initialPoint!!.id,
                            title.trim(),
                            description.trim(),
                            selectedCategory,
                            lat,
                            lon,
                            initialPoint.version
                        )
                    } else {
                        onConfirmCreate(
                            title.trim(),
                            description.trim(),
                            selectedCategory,
                            lat,
                            lon
                        )
                    }
                    onDismiss()
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "انصراف", color = Color(0xFF8B949E))
            }
        },
        containerColor = Color(0xFF161B22),
        shape = RoundedCornerShape(12.dp)
    )
}
