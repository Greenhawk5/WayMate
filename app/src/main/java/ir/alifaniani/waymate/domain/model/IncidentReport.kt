package ir.alifaniani.waymate.domain.model

enum class IncidentType {
    SUSPICIOUS_PERSON,
    VEHICLE_TRESPASS,
    ROAD_HAZARD,
    SECURITY_BREACH,
    QUICK_ALERT,
    OTHER
}

enum class IncidentStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED
}

/**
 * Structured tactical observation or quick emergency alert record.
 */
data class IncidentReport(
    val id: String,
    val teamId: String,
    val title: String,
    val description: String,
    val type: IncidentType,
    val severity: IncidentSeverity,
    val latitude: Double,
    val longitude: Double,
    val status: IncidentStatus = IncidentStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis()
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90: $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180: $longitude" }
        require(title.isNotBlank()) { "Title must not be blank" }
    }
}
