package ir.alifaniani.waymate.domain.model

/**
 * Individual member assigned to an operational patrol team.
 */
data class TeamMember(
    val id: String,
    val fullName: String,
    val callsign: String,
    val roleInTeam: String
)
