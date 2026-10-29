package ir.alifaniani.waymate.domain.model

/**
 * Domain representation of an authenticated user.
 */
data class User(
    val id: String,
    val username: String,
    val role: UserRole,
    val callsign: String
)
