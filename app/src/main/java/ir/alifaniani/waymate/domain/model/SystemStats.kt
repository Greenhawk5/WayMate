package ir.alifaniani.waymate.domain.model

/**
 * Domain representation of basic system and operational status metrics.
 */
data class SystemStats(
    val systemStatus: String,
    val totalUsers: Int,
    val activeUsers: Int,
    val ownersCount: Int,
    val commandersCount: Int,
    val teamLeadsCount: Int,
    val totalTeams: Int,
    val activeTeams: Int,
    val totalIncidents: Int,
    val openIncidents: Int,
    val totalMessages: Int,
    val recentAuditEvents: Int,
    val timestamp: Long
)
