package ir.alifaniani.waymate.domain.model

/**
 * Domain representation of a system user managed by the Owner.
 */
data class ManagedUser(
    val id: String,
    val username: String,
    val role: UserRole,
    val callsign: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
