package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ManagedUserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("role") val role: String,
    @SerialName("callsign") val callsign: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: Long = 0L,
    @SerialName("updated_at") val updatedAt: Long = 0L
)

@Serializable
data class CreateUserRequestDto(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String,
    @SerialName("role") val role: String,
    @SerialName("callsign") val callsign: String
)

@Serializable
data class UpdateUserRequestDto(
    @SerialName("role") val role: String? = null,
    @SerialName("callsign") val callsign: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class CreateTeamRequestDto(
    @SerialName("name") val name: String,
    @SerialName("lead_user_id") val leadUserId: String,
    @SerialName("vehicle_type") val vehicleType: String,
    @SerialName("vehicle_plate") val vehiclePlate: String,
    @SerialName("equipment_summary") val equipmentSummary: String = "",
    @SerialName("members") val members: List<CreateMemberDto> = emptyList()
)

@Serializable
data class CreateMemberDto(
    @SerialName("full_name") val fullName: String,
    @SerialName("callsign") val callsign: String,
    @SerialName("role_in_team") val roleInTeam: String
)

@Serializable
data class UpdateTeamRequestDto(
    @SerialName("name") val name: String? = null,
    @SerialName("lead_user_id") val leadUserId: String? = null,
    @SerialName("vehicle_type") val vehicleType: String? = null,
    @SerialName("vehicle_plate") val vehiclePlate: String? = null,
    @SerialName("equipment_summary") val equipmentSummary: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

@Serializable
data class SystemStatsDto(
    @SerialName("system_status") val systemStatus: String = "OPERATIONAL",
    @SerialName("environment") val environment: String = "development",
    @SerialName("users") val users: UserStatsDto = UserStatsDto(),
    @SerialName("teams") val teams: TeamStatsDto = TeamStatsDto(),
    @SerialName("incidents") val incidents: IncidentStatsDto = IncidentStatsDto(),
    @SerialName("messages") val messages: MessageStatsDto = MessageStatsDto(),
    @SerialName("audit") val audit: AuditStatsDto = AuditStatsDto(),
    @SerialName("timestamp") val timestamp: Long = 0L
)

@Serializable
data class UserStatsDto(
    @SerialName("total") val total: Int = 0,
    @SerialName("active") val active: Int = 0,
    @SerialName("by_role") val byRole: UserRoleStatsDto = UserRoleStatsDto()
)

@Serializable
data class UserRoleStatsDto(
    @SerialName("owners") val owners: Int = 0,
    @SerialName("commanders") val commanders: Int = 0,
    @SerialName("team_leads") val teamLeads: Int = 0
)

@Serializable
data class TeamStatsDto(
    @SerialName("total") val total: Int = 0,
    @SerialName("active") val active: Int = 0
)

@Serializable
data class IncidentStatsDto(
    @SerialName("total") val total: Int = 0,
    @SerialName("open") val open: Int = 0
)

@Serializable
data class MessageStatsDto(
    @SerialName("total") val total: Int = 0
)

@Serializable
data class AuditStatsDto(
    @SerialName("recent_24h_events") val recent24hEvents: Int = 0
)

@Serializable
data class AuditLogDto(
    @SerialName("id") val id: String,
    @SerialName("actor_user_id") val actorUserId: String? = null,
    @SerialName("event_type") val eventType: String,
    @SerialName("resource_type") val resourceType: String,
    @SerialName("resource_id") val resourceId: String,
    @SerialName("ip_address") val ipAddress: String,
    @SerialName("details") val details: JsonObject? = null,
    @SerialName("created_at") val createdAt: Long
)
