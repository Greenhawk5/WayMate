package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamMember

/**
 * Domain contract for accessing tactical team rosters and organizational unit metadata.
 */
interface TeamRepository {

    /**
     * Retrieves all active tactical teams visible to the authenticated user.
     * Accessible by OWNER and COMMANDER roles.
     *
     * @param fetchDetails Whether to also retrieve member rosters for each team.
     */
    suspend fun getTeams(fetchDetails: Boolean = true): Result<List<Team>>

    /**
     * Retrieves detailed metadata and personnel members for a specific team.
     */
    suspend fun getTeamDetails(teamId: String): Result<Team>

    /**
     * Creates a new tactical team and provisions its roster.
     * Accessible by OWNER and COMMANDER roles.
     */
    suspend fun createTeam(
        name: String,
        leadUserId: String,
        vehicleType: String,
        vehiclePlate: String,
        equipmentSummary: String,
        members: List<TeamMember> = emptyList()
    ): Result<Team>

    /**
     * Updates an existing tactical team's specifications or active status.
     * Accessible by OWNER and COMMANDER roles.
     */
    suspend fun updateTeam(
        teamId: String,
        name: String? = null,
        leadUserId: String? = null,
        vehicleType: String? = null,
        vehiclePlate: String? = null,
        equipmentSummary: String? = null,
        isActive: Boolean? = null
    ): Result<Team>
}
