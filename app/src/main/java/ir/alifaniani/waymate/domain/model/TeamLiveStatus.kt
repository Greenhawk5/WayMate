package ir.alifaniani.waymate.domain.model

/**
 * Real-time operational and connectivity status of an active field team.
 * Models authorized Blue Force Tracking presence for commanders according to REQ-F-006 and REQ-F-014.
 */
data class TeamLiveStatus(
    val teamId: String,
    val teamName: String,
    val callsign: String,
    val operationalStatus: OperationalStatus,
    val connectionStatus: ConnectionStatus,
    val lastSeenAt: Long,
    val isStale: Boolean
)
