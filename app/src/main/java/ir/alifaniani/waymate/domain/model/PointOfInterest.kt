package ir.alifaniani.waymate.domain.model

/**
 * Functional category for hiking and outdoor points of interest.
 */
enum class PointCategory(val displayNamePersian: String) {
    START_POINT("نقطه شروع پیمایش"),
    DESTINATION("مقصد نهایی"),
    CAMPSITE("محل کمپ و اقامت"),
    REST_AREA("محل استراحت"),
    WATER_SOURCE("منبع آب آشامیدنی"),
    GENERAL("نقطه عمومی و نشانه")
}

/**
 * Domain representation of a shared Point of Interest (POI) for a group.
 */
data class PointOfInterest(
    val id: String,
    val teamId: String,
    val title: String,
    val description: String,
    val category: PointCategory,
    val latitude: Double,
    val longitude: Double,
    val createdByUserId: String,
    val createdByCallsign: String,
    val updatedByUserId: String,
    val updatedByCallsign: String,
    val isDeleted: Boolean = false,
    val version: Int = 1,
    val createdAt: Long,
    val updatedAt: Long,
    val isPendingSync: Boolean = false
) {
    val formattedCoordinates: String
        get() = String.format("%.4f°N, %.4f°E", latitude, longitude)
}
