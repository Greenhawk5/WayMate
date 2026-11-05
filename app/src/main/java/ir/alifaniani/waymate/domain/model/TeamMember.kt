package ir.alifaniani.waymate.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Individual member assigned to an operational patrol team.
 */
@Serializable
data class TeamMember(
    @SerialName("id") val id: String = "",
    @SerialName("full_name") val fullName: String = "",
    @SerialName("callsign") val callsign: String = "",
    @SerialName("role_in_team") val roleInTeam: String = ""
)
