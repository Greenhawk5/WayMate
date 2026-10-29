package ir.alifaniani.waymate.domain.model

/**
 * Pure domain model representing a tactical GPS location acquisition point.
 */
data class LocationPing(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val speedMps: Float = 0.0f,
    val headingDegrees: Float = 0.0f,
    val status: OperationalStatus = OperationalStatus.NORMAL,
    val recordedAt: Long = System.currentTimeMillis()
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90 degrees: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180 degrees: $longitude" }
        require(accuracyMeters >= 0.0f) { "Accuracy must be non-negative: $accuracyMeters" }
        require(speedMps >= 0.0f) { "Speed must be non-negative: $speedMps" }
        require(headingDegrees in 0.0f..360.0f) { "Heading must be between 0 and 360 degrees: $headingDegrees" }
    }
}
