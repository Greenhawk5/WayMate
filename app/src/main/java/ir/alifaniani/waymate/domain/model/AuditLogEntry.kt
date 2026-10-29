package ir.alifaniani.waymate.domain.model

/**
 * Domain representation of an immutable administrative audit event.
 */
data class AuditLogEntry(
    val id: String,
    val actorUserId: String?,
    val eventType: String,
    val resourceType: String,
    val resourceId: String,
    val ipAddress: String,
    val details: String,
    val createdAt: Long
)
