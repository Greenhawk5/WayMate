package ir.alifaniani.waymate.domain.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.AuditLogEntry
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.SystemStats
import ir.alifaniani.waymate.domain.model.UserRole

/**
 * Domain contract for administration, user provisioning, system statistics, and audit logs.
 */
interface AdminRepository {

    /**
     * Lists operational system users. Restricted to OWNER role.
     */
    suspend fun getUsers(roleFilter: UserRole? = null): Result<List<ManagedUser>>

    /**
     * Provisions a new operational user. Restricted to OWNER role.
     */
    suspend fun createUser(
        username: String,
        password: String,
        role: UserRole,
        callsign: String
    ): Result<ManagedUser>

    /**
     * Updates an existing user's role, callsign, or active status. Restricted to OWNER role.
     */
    suspend fun updateUser(
        userId: String,
        role: UserRole? = null,
        callsign: String? = null,
        isActive: Boolean? = null
    ): Result<ManagedUser>

    /**
     * Retrieves aggregated system health and operational metrics. Restricted to OWNER and COMMANDER.
     */
    suspend fun getSystemStats(): Result<SystemStats>

    /**
     * Retrieves immutable administrative audit event logs. Restricted to OWNER role.
     */
    suspend fun getAuditLogs(): Result<List<AuditLogEntry>>
}
