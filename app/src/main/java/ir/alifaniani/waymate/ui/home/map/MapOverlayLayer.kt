package ir.alifaniani.waymate.ui.home.map

/**
 * Functional category for grouping map overlay layers in the layer management interface.
 */
enum class LayerCategory(val titlePersian: String) {
    INFRASTRUCTURE("زیرساخت و راه‌ها"),
    HYDROLOGY("هیدرولوژی و موانع طبیعی"),
    TACTICAL("اطلاعات تاکتیکی فضایی"),
    OPERATIONAL("واحدهای میدانی و مأموریت")
}

/**
 * Immutable representation of a toggleable map overlay layer.
 *
 * Supports typed identifiers, user-visible Persian descriptions, feature counts,
 * attribution requirements, and independent visibility state.
 */
data class MapOverlayLayer(
    val id: String,
    val titlePersian: String,
    val descriptionPersian: String,
    val category: LayerCategory,
    val isEnabled: Boolean = true,
    val isAvailable: Boolean = true,
    val featureCount: Int? = null,
    val attribution: String? = null,
    val isImmutableSource: Boolean = true
)

/**
 * Default registered map overlay layers for WayMate.
 * Reflects the audited Jahrom GIS dataset, tactical coordinate elements,
 * synthetic demonstration markers, and future extensible slots.
 */
val defaultMapLayers: List<MapOverlayLayer> = listOf(
    MapOverlayLayer(
        id = "jahrom_road_network",
        titlePersian = "شبکه معابر جهرم (داده وکتور GIS)",
        descriptionPersian = "۵,۳۵۱ قطعه‌خط معابر شامل جاده‌های اصلی، خیابان‌ها و کوچه‌های جهرم",
        category = LayerCategory.INFRASTRUCTURE,
        isEnabled = true,
        isAvailable = true,
        featureCount = 5351,
        attribution = "پایگاه داده وکتور معابر شهری جهرم (ممیزی شده)"
    ),
    MapOverlayLayer(
        id = "jahrom_waterways",
        titlePersian = "مسیل‌ها و حریم رودخانه‌ها",
        descriptionPersian = "۱۰۸ عارضه آبراهه و رودخانه‌های فصلی تفکیک‌شده از شبکه معابر",
        category = LayerCategory.HYDROLOGY,
        isEnabled = true,
        isAvailable = true,
        featureCount = 108,
        attribution = "داده‌های طبیعی و هیدروگرافی جهرم"
    ),
    MapOverlayLayer(
        id = "tactical_grid",
        titlePersian = "شبکه مختصات و حلقه‌های برد",
        descriptionPersian = "خطوط شطرنجی WGS-84، جهت‌نمای شمال و حلقه‌های فواصل تاکتیکی",
        category = LayerCategory.TACTICAL,
        isEnabled = true,
        isAvailable = true,
        attribution = "سامانه موقعیت‌یابی بومی WayMate"
    ),
    MapOverlayLayer(
        id = "synthetic_units",
        titlePersian = "واحدهای شبیه‌سازی‌شده (آموزشی)",
        descriptionPersian = "موقعیت‌های فرضی خودرویی و پیاده جهت نمایش و آزمون عملکرد",
        category = LayerCategory.OPERATIONAL,
        isEnabled = true,
        isAvailable = true,
        attribution = "داده‌های ساختگی صرفاً جهت توسعه و آموزش"
    ),
    MapOverlayLayer(
        id = "patrol_routes",
        titlePersian = "مسیرهای گشتی مصوب",
        descriptionPersian = "طرح‌ریزی مسیرهای ایست‌وبازرسی و گشت‌زنی عملیاتی (در دست توسعه)",
        category = LayerCategory.OPERATIONAL,
        isEnabled = false,
        isAvailable = false,
        attribution = "نیازمند فعال‌سازی پنل مدیریت مأموریت"
    ),
    MapOverlayLayer(
        id = "sensitive_zones",
        titlePersian = "مراکز و ابنیه حساس",
        category = LayerCategory.TACTICAL,
        descriptionPersian = "نقاط کلیدی و زیرساخت‌های حیاتی (طرح فاز آینده)",
        isEnabled = false,
        isAvailable = false,
        attribution = "سامانه سطوح دسترسی فرماندهی"
    )
)
