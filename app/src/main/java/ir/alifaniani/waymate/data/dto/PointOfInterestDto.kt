package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PointOfInterestDto(
    @SerialName("id") val id: String,
    @SerialName("team_id") val teamId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("created_by_user_id") val createdByUserId: String,
    @SerialName("created_by_callsign") val createdByCallsign: String,
    @SerialName("updated_by_user_id") val updatedByUserId: String,
    @SerialName("updated_by_callsign") val updatedByCallsign: String,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("version") val version: Int = 1,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class CreatePointRequestDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("team_id") val teamId: String? = null
)

@Serializable
data class UpdatePointRequestDto(
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("version") val version: Int? = null
)

@Serializable
data class DeletePointResponseDto(
    @SerialName("deleted") val deleted: Boolean,
    @SerialName("id") val id: String,
    @SerialName("version") val version: Int
)
