package ir.alifaniani.waymate.ui.home.map

/**
 * Base cartographic presentation types for the WayMate map engine.
 *
 * Defines the underlying base presentation layer, clearly distinguishing
 * between functional/offline engines and unconfigured/unavailable services
 * (such as standard tile servers and satellite imagery, which require external providers).
 */
enum class BaseMapPresentation(
    val id: String,
    val titlePersian: String,
    val subtitlePersian: String,
    val isConfigured: Boolean,
    val requiresExternalProvider: Boolean,
    val licenseNotice: String
) {
    /**
     * Pure Jetpack Compose hardware-accelerated vector engine.
     * Renders local GIS road vectors, waterways, coordinate grid, and tactical symbology.
     * Operates 100% offline with zero external network or tile server dependencies.
     */
    OFFLINE_TACTICAL(
        id = "offline_tactical",
        titlePersian = "شبکه تاکتیکی آفلاین (موتور محلی)",
        subtitlePersian = "رندر مستقیم برداری معابر و مختصات بدون نیاز به اینترنت یا سرور تایل",
        isConfigured = true,
        requiresExternalProvider = false,
        licenseNotice = "موتور بومی WayMate (Apache 2.0) با داده‌های وکتور جهرم"
    ),

    /**
     * Standard cartographic street tile presentation.
     * Unconfigured because public OSM tile servers prohibit offline bulk downloads
     * and no sovereign local tile server is currently deployed.
     */
    STANDARD_STREET(
        id = "standard_street",
        titlePersian = "نقشه خیابانی استاندارد (پیکربندی‌نشده)",
        subtitlePersian = "نیازمند سرور تایل محلی یا پکیج MBTiles — سرور عمومی OSM برای استفاده آفلاین مجاز نیست",
        isConfigured = false,
        requiresExternalProvider = true,
        licenseNotice = "نیازمند راه‌اندازی سرور تایل سازمانی بر پایه OpenStreetMap (ODbL)"
    ),

    /**
     * High-resolution satellite / aerial raster imagery.
     * Strictly marked as unconfigured because no commercial or public aerial tile service
     * is provisioned under the zero-budget prototype boundary.
     */
    SATELLITE_IMAGERY(
        id = "satellite_imagery",
        titlePersian = "تصویر ماهواره‌ای (پیکربندی‌نشده)",
        subtitlePersian = "غیرفعال — نیازمند منبع تصویربرداری مجزا و تأمین بودجه",
        isConfigured = false,
        requiresExternalProvider = true,
        licenseNotice = "نیازمند اشتراک تجاری یا تصاویر سنجش از دور (Sentinel/Landsat)"
    )
}
