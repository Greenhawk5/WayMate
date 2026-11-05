package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TeamLiveStatus

/**
 * Domain repository contract for operational status and link health monitoring.
 */
interface StatusRepository {
    /**
     * Polls the live operational presence and link connectivity of active field units.
     * Restricted to Commander / Owner roles.
     */
    suspend fun getLiveTeamStatuses(): Result<List<TeamLiveStatus>>

    /**
     * Updates the operational status of the authenticated team lead's unit.
     */
    suspend fun updateOperationalStatus(status: OperationalStatus): Result<OperationalStatus>

    /**
     * Retrieves the locally persisted operational status of this unit.
     */
    suspend fun getMyOperationalStatus(): OperationalStatus?
}
