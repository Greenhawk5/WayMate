package ir.alifaniani.waymate.domain.model

/**
 * Tactical patrol team entity within the command hierarchy.
 */
data class Team(
    val id: String,
    val name: String,
    val leadUserId: String,
    val vehicleType: String,
    val vehiclePlate: String,
    val equipmentSummary: String,
    val members: List<TeamMember> = emptyList(),
    val memberCount: Int = members.size,
    val isActive: Boolean = true
)
