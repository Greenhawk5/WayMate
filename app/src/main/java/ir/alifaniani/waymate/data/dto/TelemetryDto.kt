package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationPingDto(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("accuracy_meters") val accuracyMeters: Float,
    @SerialName("speed_mps") val speedMps: Float = 0.0f,
    @SerialName("heading_degrees") val headingDegrees: Float = 0.0f,
    @SerialName("operational_status") val operationalStatus: String = "NORMAL",
    @SerialName("recorded_at") val recordedAt: Long
)

@Serializable
data class BatchTelemetryRequestDto(
    @SerialName("pings") val pings: List<LocationPingDto>
)

@Serializable
data class LiveUnitDto(
    @SerialName("team_id") val teamId: String,
    @SerialName("team_name") val teamName: String,
    @SerialName("callsign") val callsign: String,
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("accuracy_meters") val accuracyMeters: Float = 0.0f,
    @SerialName("speed_mps") val speedMps: Float = 0.0f,
    @SerialName("heading_degrees") val headingDegrees: Float = 0.0f,
    @SerialName("operational_status") val operationalStatus: String = "NORMAL",
    @SerialName("last_seen_at") val lastSeenAt: Long,
    @SerialName("connection_status") val connectionStatus: String = "ONLINE",
    @SerialName("is_stale") val isStale: Boolean = false
)

@Serializable
data class UpdateOperationalStatusRequestDto(
    @SerialName("operational_status") val operationalStatus: String
)

@Serializable
data class UpdateOperationalStatusResponseDto(
    @SerialName("ping_id") val pingId: String? = null,
    @SerialName("team_id") val teamId: String,
    @SerialName("operational_status") val operationalStatus: String,
    @SerialName("updated_at") val updatedAt: Long
)
