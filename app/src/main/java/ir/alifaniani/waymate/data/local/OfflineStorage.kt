package ir.alifaniani.waymate.data.local

import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus

/**
 * Interface contract for local persistent storage and offline buffering in WayMate.
 *
 * Implements:
 * 1. Store-and-Forward Outgoing Queue (`offline_queue`)
 * 2. Cached Server Data (`cached_teams`, `cached_messages`, `cached_statuses`)
 * 3. Atomic Pruning and Purging for security and privacy.
 */
interface OfflineStorage {

    // ==========================================
    // Offline Store-and-Forward Queue
    // ==========================================

    suspend fun enqueueAction(action: QueuedAction)

    suspend fun getPendingActions(): List<QueuedAction>

    suspend fun updateActionStatus(
        id: String,
        status: QueueStatus,
        retryCount: Int,
        lastAttemptAt: Long?,
        lastError: String?
    )

    suspend fun removeAction(id: String)

    suspend fun getPendingActionCount(): Int

    // ==========================================
    // Cached Teams
    // ==========================================

    suspend fun saveTeams(teams: List<Team>)

    suspend fun getCachedTeams(): List<Team>

    suspend fun getTeamsLastSynced(): Long

    // ==========================================
    // Cached Tactical Messages
    // ==========================================

    suspend fun saveMessages(messages: List<TacticalMessage>)

    suspend fun getCachedMessages(): List<TacticalMessage>

    suspend fun saveLocalMessage(message: TacticalMessage)

    suspend fun markMessageAcknowledged(messageId: String)

    suspend fun getMessagesLastSynced(): Long

    // ==========================================
    // Cached Unit Statuses
    // ==========================================

    suspend fun saveStatuses(statuses: List<TeamLiveStatus>)

    suspend fun getCachedStatuses(): List<TeamLiveStatus>

    suspend fun getStatusesLastSynced(): Long

    // ==========================================
    // Retention, Pruning & Session Cleanup
    // ==========================================

    suspend fun purgeExpiredData(maxAgeMs: Long)

    suspend fun clearCache()

    suspend fun clearQueue()

    suspend fun clearAll()
}
