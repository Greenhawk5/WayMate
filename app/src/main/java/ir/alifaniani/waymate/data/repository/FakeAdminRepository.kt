package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.domain.model.AuditLogEntry
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.SystemStats
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.domain.repository.AdminRepository

/**
 * DEVELOPMENT AND TESTING ONLY.
 *
 * In-memory fake implementation of [AdminRepository] providing synthetic management entities.
 */
class FakeAdminRepository : AdminRepository {

    var shouldFail: Boolean = false
    var failureError: Throwable = Exception("خطای ساختگی مدیریت")
    var failureMessage: String = "خطای ساختگی مدیریت"

    val syntheticUsers: MutableList<ManagedUser> = mutableListOf(
        ManagedUser(
            id = "usr_owner_001",
            username = "owner_demo",
            role = UserRole.OWNER,
            callsign = "فرماندهی ارشد",
            isActive = true,
            createdAt = 1700000000000L,
            updatedAt = 1700000000000L
        ),
        ManagedUser(
            id = "usr_cmd_001",
            username = "commander_demo",
            role = UserRole.COMMANDER,
            callsign = "فرمانده ۱",
            isActive = true,
            createdAt = 1700000100000L,
            updatedAt = 1700000100000L
        ),
        ManagedUser(
            id = "usr_lead_001",
            username = "patrol_lead_demo",
            role = UserRole.TEAM_LEAD,
            callsign = "سرتیم گشت ۱",
            isActive = true,
            createdAt = 1700000200000L,
            updatedAt = 1700000200000L
        )
    )

    var syntheticStats: SystemStats = SystemStats(
        systemStatus = "NORMAL",
        totalUsers = 3,
        activeUsers = 3,
        ownersCount = 1,
        commandersCount = 1,
        teamLeadsCount = 1,
        totalTeams = 1,
        activeTeams = 1,
        totalIncidents = 2,
        openIncidents = 1,
        totalMessages = 5,
        recentAuditEvents = 4,
        timestamp = 1700000000000L
    )

    val syntheticLogs: MutableList<AuditLogEntry> = mutableListOf(
        AuditLogEntry(
            id = "audit_001",
            actorUserId = "usr_owner_001",
            eventType = "USER_LOGIN_SUCCESS",
            resourceType = "AUTH",
            resourceId = "usr_owner_001",
            ipAddress = "127.0.0.1",
            details = "{}",
            createdAt = 1700000300000L
        )
    )

    override suspend fun getUsers(roleFilter: UserRole?): Result<List<ManagedUser>> {
        if (shouldFail) return Result.Error(failureError, failureMessage)
        val filtered = if (roleFilter != null) {
            syntheticUsers.filter { it.role == roleFilter }
        } else {
            syntheticUsers
        }
        return Result.Success(filtered.toList())
    }

    override suspend fun createUser(
        username: String,
        password: String,
        role: UserRole,
        callsign: String
    ): Result<ManagedUser> {
        if (shouldFail) return Result.Error(failureError, failureMessage)
        val now = System.currentTimeMillis()
        val newUser = ManagedUser(
            id = "usr_$now",
            username = username,
            role = role,
            callsign = callsign,
            isActive = true,
            createdAt = now,
            updatedAt = now
        )
        syntheticUsers.add(newUser)
        return Result.Success(newUser)
    }

    override suspend fun updateUser(
        userId: String,
        role: UserRole?,
        callsign: String?,
        isActive: Boolean?
    ): Result<ManagedUser> {
        if (shouldFail) return Result.Error(failureError, failureMessage)
        val index = syntheticUsers.indexOfFirst { it.id == userId }
        if (index == -1) {
            return Result.Error(Exception("کاربر یافت نشد"), "کاربر یافت نشد")
        }
        val current = syntheticUsers[index]
        val updated = current.copy(
            role = role ?: current.role,
            callsign = callsign ?: current.callsign,
            isActive = isActive ?: current.isActive,
            updatedAt = System.currentTimeMillis()
        )
        syntheticUsers[index] = updated
        return Result.Success(updated)
    }

    override suspend fun getSystemStats(): Result<SystemStats> {
        if (shouldFail) return Result.Error(failureError, failureMessage)
        return Result.Success(syntheticStats)
    }

    override suspend fun getAuditLogs(): Result<List<AuditLogEntry>> {
        if (shouldFail) return Result.Error(failureError, failureMessage)
        return Result.Success(syntheticLogs.toList())
    }
}
