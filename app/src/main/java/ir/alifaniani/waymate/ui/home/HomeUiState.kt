package ir.alifaniani.waymate.ui.home

import ir.alifaniani.waymate.domain.model.AuditLogEntry
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.SystemStats
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus

/**
 * UI State for the Home / Operations Center screen.
 * Tracks teams roster, incoming tactical messages, live unit statuses,
 * connection link health, operational status selection,
 * Phase 9 roles and management panel data,
 * and Phase 10 local offline storage & queue synchronization.
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val messages: List<TacticalMessage> = emptyList(),
    val liveStatuses: List<TeamLiveStatus> = emptyList(),
    val myOperationalStatus: OperationalStatus = OperationalStatus.NORMAL,
    val connectionStatus: ConnectionStatus = ConnectionStatus.ONLINE,
    val lastSyncTimestamp: Long = 0L,
    val isPollingActive: Boolean = false,
    val errorMessage: String? = null,
    val isSessionExpired: Boolean = false,
    val isForbidden: Boolean = false,
    val isSyntheticData: Boolean = true,
    // Phase 9 Management & Roles state
    val managedUsers: List<ManagedUser> = emptyList(),
    val systemStats: SystemStats? = null,
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isManagementLoading: Boolean = false,
    val managementFeedbackMessage: String? = null,
    val isManagementFeedbackError: Boolean = false,
    // Phase 10 Local & Offline Storage state
    val offlineQueueCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncFeedbackMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && !isForbidden && teams.isEmpty()

    val isSuccess: Boolean
        get() = !isLoading && errorMessage == null && !isForbidden && teams.isNotEmpty()

    val unacknowledgedMessagesCount: Int
        get() = messages.count { !it.isAcknowledged }
}
