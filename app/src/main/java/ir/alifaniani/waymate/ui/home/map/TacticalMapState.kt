package ir.alifaniani.waymate.ui.home.map

import ir.alifaniani.waymate.ui.common.BadgeTone

/**
 * Representation of a synthetic demonstration marker for educational map preview.
 * Strictly non-operational and clearly labeled.
 */
data class SyntheticMarker(
    val id: String,
    val callsign: String,
    val unitDescription: String,
    val latitude: Double,
    val longitude: Double,
    val tone: BadgeTone = BadgeTone.SUCCESS,
    val isSelected: Boolean = false
)

/**
 * Legacy presentation mode for backward compatibility with earlier Phase 7 prototypes.
 */
enum class MapPresentationMode(val displayNamePersian: String) {
    TACTICAL_GRID("شبکه تاکتیکی (آفلاین)"),
    SCHEMATIC_VECTOR("نمای شماتیک وکتور"),
    SIMULATED_AERIAL("شبیه‌ساز هوایی")
}

/**
 * Immutable UI state for the extensible, layered tactical map experience.
 */
data class TacticalMapUiState(
    val centerLatitude: Double = 28.5303,
    val centerLongitude: Double = 53.6008,
    val zoomLevel: Int = 14,
    val basePresentation: BaseMapPresentation = BaseMapPresentation.OFFLINE_TACTICAL,
    val layers: List<MapOverlayLayer> = defaultMapLayers,
    val vectorSegments: List<GisLineSegment> = emptyList(),
    val markers: List<SyntheticMarker> = defaultJahromSyntheticMarkers,
    val selectedMarker: SyntheticMarker? = null,
    val isLayerSheetVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSyntheticOnly: Boolean = true
) {
    /**
     * Backward-compatible presentation mode accessor.
     */
    val presentationMode: MapPresentationMode
        get() = when (basePresentation) {
            BaseMapPresentation.OFFLINE_TACTICAL -> MapPresentationMode.TACTICAL_GRID
            BaseMapPresentation.STANDARD_STREET -> MapPresentationMode.SCHEMATIC_VECTOR
            BaseMapPresentation.SATELLITE_IMAGERY -> MapPresentationMode.SIMULATED_AERIAL
        }

    val isOfflineSchematic: Boolean
        get() = basePresentation == BaseMapPresentation.OFFLINE_TACTICAL

    val isSatelliteActive: Boolean
        get() = basePresentation == BaseMapPresentation.SATELLITE_IMAGERY

    val isSatelliteConfigured: Boolean
        get() = basePresentation.isConfigured

    val isRoadOverlayEnabled: Boolean
        get() = layers.find { it.id == "jahrom_road_network" }?.isEnabled == true

    val isWaterwayOverlayEnabled: Boolean
        get() = layers.find { it.id == "jahrom_waterways" }?.isEnabled == true

    val isTacticalGridEnabled: Boolean
        get() = layers.find { it.id == "tactical_grid" }?.isEnabled == true

    val isSyntheticUnitsEnabled: Boolean
        get() = layers.find { it.id == "synthetic_units" }?.isEnabled == true

    val activeLayerCount: Int
        get() = layers.count { it.isEnabled }

    val formattedCoordinates: String
        get() = String.format("%.4f°N  %.4f°E", centerLatitude, centerLongitude)

    /**
     * Toggles an individual layer's visibility if it is available.
     */
    fun toggleLayer(layerId: String): TacticalMapUiState {
        val updatedLayers = layers.map { layer ->
            if (layer.id == layerId && layer.isAvailable) {
                layer.copy(isEnabled = !layer.isEnabled)
            } else {
                layer
            }
        }
        return copy(layers = updatedLayers)
    }

    /**
     * Sets the base cartographic presentation.
     */
    fun selectBasePresentation(newPresentation: BaseMapPresentation): TacticalMapUiState {
        return copy(
            basePresentation = newPresentation,
            // If selecting an unconfigured presentation, set an informative message
            errorMessage = if (!newPresentation.isConfigured) {
                if (newPresentation == BaseMapPresentation.SATELLITE_IMAGERY) {
                    "سرویس تصویر ماهواره‌ای پیکربندی نشده است. نقشه روی لایه وکتور آفلاین محلی فعال باقی می‌ماند."
                } else {
                    "سرور کارتوگرافی خیابانی پیکربندی نشده است. نقشه روی لایه وکتور آفلاین محلی فعال باقی می‌ماند."
                }
            } else null
        )
    }
}

/**
 * Synthetic demonstration markers centered around Jahrom civic landmarks for demonstration only.
 * Kept strictly non-operational and distinctly marked as simulated.
 */
val defaultJahromSyntheticMarkers: List<SyntheticMarker> = listOf(
    SyntheticMarker(
        id = "synth_alpha_01",
        callsign = "گشت ۱ جهرم [شبیه‌سازی‌شده]",
        unitDescription = "واحد خودرویی فرضی — بلوار مطهری (داده نمایشی)",
        latitude = 28.5350,
        longitude = 53.6020,
        tone = BadgeTone.SUCCESS
    ),
    SyntheticMarker(
        id = "synth_alpha_02",
        callsign = "دیده‌بان ۲ [شبیه‌سازی‌شده]",
        unitDescription = "دیده‌بان پیاده فرضی — میدان شهدا (داده نمایشی)",
        latitude = 28.5280,
        longitude = 53.5980,
        tone = BadgeTone.WARNING
    ),
    SyntheticMarker(
        id = "synth_checkpoint_01",
        callsign = "پست دیده‌بانی فرضی [شبیه‌سازی‌شده]",
        unitDescription = "نقطه مرجع تاکتیکی فرضی — کمربندی جهرم",
        latitude = 28.5420,
        longitude = 53.6150,
        tone = BadgeTone.NEUTRAL
    )
)

/**
 * Alias for backward compatibility.
 */
val defaultSyntheticMarkers: List<SyntheticMarker> = defaultJahromSyntheticMarkers
