package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.LocationSharingState
import ir.alifaniani.waymate.domain.model.LocationSharingStatus
import ir.alifaniani.waymate.domain.model.UserLocation
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing local location acquisition and group location distribution.
 */
interface LocationRepository {

    /**
     * Observable stream of overall sharing state, including local GPS fix and group member positions.
     */
    val sharingState: StateFlow<LocationSharingState>

    /**
     * Explicitly starts consent-based location sharing for the active group.
     */
    suspend fun startSharing(teamId: String? = null): Result<Unit>

    /**
     * Explicitly terminates location sharing and notifies group members immediately.
     */
    fun stopSharing()

    /**
     * Dispatches an acquired local GPS fix for publication over the active WebSocket link.
     */
    fun publishLocation(location: UserLocation)

    /**
     * Updates local status (e.g. from service lifecycle, permission loss, or provider errors).
     */
    fun updateStatus(status: LocationSharingStatus, errorMessage: String? = null)
}
