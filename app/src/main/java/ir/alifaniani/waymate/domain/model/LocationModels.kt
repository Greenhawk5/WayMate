package ir.alifaniani.waymate.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Raw geodetic fix captured from local device GPS or network sensors.
 */
data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val provider: String? = null
) {
    val isValid: Boolean
        get() = latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                accuracyMeters >= 0f &&
                timestamp > 0L

    val formattedCoordinates: String
        get() = String.format("%.4f°N, %.4f°E", latitude, longitude)
}

/**
 * Live position of another consenting member in the same hiking group.
 */
data class GroupMemberLocation(
    val userId: String,
    val callsign: String,
    val teamId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val altitudeMeters: Double? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timestamp: Long,
    val isSharing: Boolean = true
) {
    fun isStale(now: Long = System.currentTimeMillis(), staleThresholdMs: Long = 60_000L): Boolean {
        return (now - timestamp) > staleThresholdMs
    }

    val formattedCoordinates: String
        get() = String.format("%.4f°N, %.4f°E", latitude, longitude)
}

/**
 * Lifecycle states of the local consent-based location sharing session.
 */
enum class LocationSharingStatus(val displayNamePersian: String) {
    NOT_SHARING("اشتراک‌گذاری غیرفعال"),
    REQUESTING_PERMISSION("در حال درخواست مجوز"),
    STARTING("در حال اتصال و شروع"),
    SHARING("در حال اشتراک‌گذاری زنده"),
    RECONNECTING("در حال اتصال مجدد"),
    GPS_DISABLED("موقعیت‌یاب (GPS) خاموش است"),
    PERMISSION_DENIED("مجوز موقعیت مکانی داده نشد"),
    NETWORK_UNAVAILABLE("اینترنت در دسترس نیست"),
    STOPPING("در حال توقف"),
    STOPPED("متوقف شد"),
    ERROR("خطا در سرویس اشتراک‌گذاری")
}

/**
 * Complete immutable state of the location sharing system.
 */
data class LocationSharingState(
    val status: LocationSharingStatus = LocationSharingStatus.NOT_SHARING,
    val myLocation: UserLocation? = null,
    val lastPublishedTimestamp: Long = 0L,
    val activeSharers: List<GroupMemberLocation> = emptyList(),
    val errorMessage: String? = null
) {
    val isSharingActive: Boolean
        get() = status == LocationSharingStatus.SHARING ||
                status == LocationSharingStatus.STARTING ||
                status == LocationSharingStatus.RECONNECTING
}

// ---------------------------------------------------------------------------
// Network Protocol DTOs (matches Version 1 WebSocket contract)
// ---------------------------------------------------------------------------

@Serializable
data class ClientLocationUpdatePayload(
    val type: String = "LOCATION_UPDATE",
    val version: Int = 1,
    val seq: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long
)

@Serializable
data class ClientSharingStartedPayload(
    val type: String = "SHARING_STARTED",
    val version: Int = 1,
    val seq: Long,
    val timestamp: Long
)

@Serializable
data class ClientSharingStoppedPayload(
    val type: String = "SHARING_STOPPED",
    val version: Int = 1,
    val seq: Long,
    val timestamp: Long
)

@Serializable
data class ClientPingPayload(
    val type: String = "PING",
    val version: Int = 1,
    val timestamp: Long
)

@Serializable
data class ActiveSharerDto(
    val userId: String,
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long
)

@Serializable
data class ServerLocationEnvelope(
    val type: String,
    val version: Int = 1,
    val seq: Long? = null,
    val userId: String? = null,
    val callsign: String? = null,
    val teamId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val status: String? = null,
    val isSharing: Boolean? = null,
    val activeSharers: List<ActiveSharerDto>? = null,
    val code: String? = null,
    val message: String? = null,
    val timestamp: Long? = null,
    val serverTimestamp: Long? = null
)
