package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TeamDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("lead_user_id") val leadUserId: String = "",
    @SerialName("vehicle_type") val vehicleType: String = "",
    @SerialName("vehicle_plate") val vehiclePlate: String = "",
    @SerialName("equipment_summary") val equipmentSummary: String = "",
    @SerialName("members") val members: List<TeamMemberDto>? = null,
    @SerialName("member_count") val memberCount: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class TeamMemberDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("callsign") val callsign: String,
    @SerialName("role_in_team") val roleInTeam: String
)

