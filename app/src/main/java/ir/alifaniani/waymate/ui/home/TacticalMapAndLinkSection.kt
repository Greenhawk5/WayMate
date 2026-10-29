package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.home.map.AssetGisLayerRepository
import ir.alifaniani.waymate.ui.home.map.BaseMapPresentation
import ir.alifaniani.waymate.ui.home.map.SyntheticMarker
import ir.alifaniani.waymate.ui.home.map.TacticalLayerControlSheet
import ir.alifaniani.waymate.ui.home.map.TacticalMapCanvas
import ir.alifaniani.waymate.ui.home.map.TacticalMapUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tactical map preview and connection health presentation component.
 *
 * Implements a safe, decoupled educational map experience:
 * - Decoupled base map presentations (Offline Tactical, Standard Street, and unconfigured Satellite)
 * - Independent vector overlays (Jahrom road network, Waterways, Tactical Grid, Synthetic Units)
 * - Non-blocking background loading of audited GIS vector assets
 * - Transparent status for unconfigured services (Satellite)
 * - Explicitly labeled synthetic demonstration markers (no real-world GPS tracking)
 * - Preserves Dark Tactical RTL layout and link health metrics
 */
@Composable
fun TacticalMapAndLinkSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gisRepository = remember { AssetGisLayerRepository(context) }
    var mapState by remember { mutableStateOf(TacticalMapUiState()) }

    // Asynchronously load real GIS vector features from assets without blocking UI
    LaunchedEffect(Unit) {
        try {
            mapState = mapState.copy(isLoading = true)
            val segments = gisRepository.loadJahromVectors()
            mapState = mapState.copy(
                vectorSegments = segments,
                isLoading = false
            )
        } catch (e: Exception) {
            mapState = mapState.copy(
                isLoading = false,
                errorMessage = "خطا در بارگذاری اولیه فایل‌های GIS"
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نقشه عملیاتی و لایه‌های اطلاعاتی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نمایش چندلایه‌ای داده‌های مکانی جهرم و وضعیت ارتباطی",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TacticalStatusBadge(
                    text = "${mapState.activeLayerCount} لایه فعال",
                    tone = BadgeTone.NEUTRAL
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Educational & Synthetic Boundary Banner
            TacticalFeedbackBanner(
                title = "محیط آزمایشی نقشه (Educational Prototype)",
                message = "کلیه نشانگرها داده‌های شبیه‌سازی‌شده هستند. داده‌های خطی معابر و مسیل‌ها از فایل‌های وکتور جهرم (EPSG:4326) استخراج و ممیزی شده‌اند.",
                type = BannerType.INFO
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Safe Multi-Layer Map Canvas
            TacticalMapCanvas(
                uiState = mapState,
                onMarkerSelected = { marker ->
                    mapState = mapState.copy(selectedMarker = marker)
                },
                onZoomIn = {
                    if (mapState.zoomLevel < 18) {
                        mapState = mapState.copy(zoomLevel = mapState.zoomLevel + 1)
                    }
                },
                onZoomOut = {
                    if (mapState.zoomLevel > 10) {
                        mapState = mapState.copy(zoomLevel = mapState.zoomLevel - 1)
                    }
                },
                onCenterReset = {
                    mapState = mapState.copy(
                        centerLatitude = 28.5303,
                        centerLongitude = 53.6008
                    )
                },
                onRetry = {
                    coroutineScope.launch {
                        mapState = mapState.copy(isLoading = true, errorMessage = null)
                        val segments = gisRepository.loadJahromVectors()
                        delay(300)
                        mapState = mapState.copy(
                            isLoading = false,
                            basePresentation = BaseMapPresentation.OFFLINE_TACTICAL,
                            vectorSegments = segments,
                            errorMessage = null
                        )
                    }
                }
            )

            // Selected Marker Tactical Inspection Card
            if (mapState.selectedMarker != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedMarkerCard(
                    marker = mapState.selectedMarker!!,
                    onDismiss = { mapState = mapState.copy(selectedMarker = null) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Layer Management Trigger & Base Presentation Quick Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TacticalOutlinedButton(
                    text = "تنظیم و مدیریت لایه‌ها (${mapState.activeLayerCount})",
                    onClick = {
                        mapState = mapState.copy(isLayerSheetVisible = !mapState.isLayerSheetVisible)
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                TacticalStatusBadge(
                    text = mapState.basePresentation.titlePersian,
                    tone = if (mapState.basePresentation.isConfigured) BadgeTone.SUCCESS else BadgeTone.WARNING
                )
            }

            // Layer Control Sheet (Inline Expandable / Modal View)
            if (mapState.isLayerSheetVisible) {
                Spacer(modifier = Modifier.height(14.dp))
                TacticalLayerControlSheet(
                    uiState = mapState,
                    onSelectBasePresentation = { presentation ->
                        mapState = mapState.selectBasePresentation(presentation)
                    },
                    onToggleLayer = { layerId ->
                        mapState = mapState.toggleLayer(layerId)
                    },
                    onDismiss = {
                        mapState = mapState.copy(isLayerSheetVisible = false)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Connection Link Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "نوع شبکه ارتباطی", value = "اینترانت امن تاکتیکی")
                MetricItem(label = "پوشش جغرافیایی", value = "جهرم (Fars, IRN)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem(label = "صف ذخیره و ارسال", value = "۰ پیام معوق (برخط)")
                MetricItem(label = "عوارض وکتور بارگذاری‌شده", value = "${mapState.vectorSegments.size} قطعه‌خط")
            }
        }
    }
}

@Composable
private fun SelectedMarkerCard(
    marker: SyntheticMarker,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = marker.callsign,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TacticalStatusBadge(
                    text = "نشانگر شبیه‌سازی‌شده",
                    tone = marker.tone
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = marker.unitDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "مختصات نمایشی: ${String.format("%.4f°N , %.4f°E", marker.latitude, marker.longitude)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            TacticalOutlinedButton(
                text = "بستن جزئیات نشانگر",
                onClick = onDismiss
            )
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
