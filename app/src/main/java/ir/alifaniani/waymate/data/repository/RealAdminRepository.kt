package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.dto.CreateUserRequestDto
import ir.alifaniani.waymate.data.dto.UpdateUserRequestDto
import ir.alifaniani.waymate.data.network.AdminApiService
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.data.network.UserApiService
import ir.alifaniani.waymate.domain.model.AuditLogEntry
import ir.alifaniani.waymate.domain.model.ManagedUser
import ir.alifaniani.waymate.domain.model.SystemStats
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.withContext

/**
 * Production implementation of [ir.alifaniani.waymate.domain.repository.AdminRepository] communicating with Cloudflare Workers.
 */
class RealAdminRepository(
    private val userApiService: UserApiService,
    private val adminApiService: AdminApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
) : ir.alifaniani.waymate.domain.repository.AdminRepository {

    override suspend fun getUsers(roleFilter: UserRole?): Result<List<ManagedUser>> =
        withContext(dispatcherProvider.io) {
            try {
                val response = userApiService.getUsers(role = roleFilter?.name)
                if (response.isSuccessful) {
                    val body = response.body()
                    val dtos = body?.data
                    if (body != null && body.success && dtos != null) {
                        val users = dtos.map { dto ->
                            ManagedUser(
                                id = dto.id,
                                username = dto.username,
                                role = runCatching { UserRole.valueOf(dto.role) }.getOrDefault(UserRole.TEAM_LEAD),
                                callsign = dto.callsign,
                                isActive = dto.isActive,
                                createdAt = dto.createdAt,
                                updatedAt = dto.updatedAt
                            )
                        }
                        Result.Success(users)
                    } else {
                        val errorMsg = body?.error?.message ?: "خطا در دریافت فهرست کاربران"
                        Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                    }
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    Result.Error(Exception(errorMessage), errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorMapper.mapThrowable(e)
                Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
            }
        }

    override suspend fun createUser(
        username: String,
        password: String,
        role: UserRole,
        callsign: String
    ): Result<ManagedUser> = withContext(dispatcherProvider.io) {
        try {
            val request = CreateUserRequestDto(
                username = username,
                password = password,
                role = role.name,
                callsign = callsign
            )
            val response = userApiService.createUser(request)
            if (response.isSuccessful) {
                val body = response.body()
                val dto = body?.data
                if (body != null && body.success && dto != null) {
                    Result.Success(
                        ManagedUser(
                            id = dto.id,
                            username = dto.username,
                            role = runCatching { UserRole.valueOf(dto.role) }.getOrDefault(UserRole.TEAM_LEAD),
                            callsign = dto.callsign,
                            isActive = dto.isActive,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )
                    )
                } else {
                    val errorMsg = body?.error?.message ?: "خطا در ایجاد کاربر"
                    Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                }
            } else {
                val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                Result.Error(Exception(errorMessage), errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = NetworkErrorMapper.mapThrowable(e)
            Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
        }
    }

    override suspend fun updateUser(
        userId: String,
        role: UserRole?,
        callsign: String?,
        isActive: Boolean?
    ): Result<ManagedUser> = withContext(dispatcherProvider.io) {
        try {
            val request = UpdateUserRequestDto(
                role = role?.name,
                callsign = callsign,
                isActive = isActive
            )
            val response = userApiService.updateUser(userId, request)
            if (response.isSuccessful) {
                val body = response.body()
                val dto = body?.data
                if (body != null && body.success && dto != null) {
                    Result.Success(
                        ManagedUser(
                            id = dto.id,
                            username = dto.username,
                            role = runCatching { UserRole.valueOf(dto.role) }.getOrDefault(UserRole.TEAM_LEAD),
                            callsign = dto.callsign,
                            isActive = dto.isActive,
                            createdAt = dto.createdAt,
                            updatedAt = dto.updatedAt
                        )
                    )
                } else {
                    val errorMsg = body?.error?.message ?: "خطا در ویرایش کاربر"
                    Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                }
            } else {
                val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                Result.Error(Exception(errorMessage), errorMessage)
            }
        } catch (e: Exception) {
            val errorMessage = NetworkErrorMapper.mapThrowable(e)
            Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
        }
    }

    override suspend fun getSystemStats(): Result<SystemStats> =
        withContext(dispatcherProvider.io) {
            try {
                val response = adminApiService.getStats()
                if (response.isSuccessful) {
                    val body = response.body()
                    val dto = body?.data
                    if (body != null && body.success && dto != null) {
                        Result.Success(
                            SystemStats(
                                systemStatus = dto.systemStatus,
                                totalUsers = dto.users.total,
                                activeUsers = dto.users.active,
                                ownersCount = dto.users.byRole.owners,
                                commandersCount = dto.users.byRole.commanders,
                                teamLeadsCount = dto.users.byRole.teamLeads,
                                totalTeams = dto.teams.total,
                                activeTeams = dto.teams.active,
                                totalIncidents = dto.incidents.total,
                                openIncidents = dto.incidents.open,
                                totalMessages = dto.messages.total,
                                recentAuditEvents = dto.audit.recent24hEvents,
                                timestamp = dto.timestamp
                            )
                        )
                    } else {
                        val errorMsg = body?.error?.message ?: "خطا در دریافت وضعیت سامانه"
                        Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                    }
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    Result.Error(Exception(errorMessage), errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorMapper.mapThrowable(e)
                Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
            }
        }

    override suspend fun getAuditLogs(): Result<List<AuditLogEntry>> =
        withContext(dispatcherProvider.io) {
            try {
                val response = adminApiService.getAuditLogs()
                if (response.isSuccessful) {
                    val body = response.body()
                    val dtos = body?.data
                    if (body != null && body.success && dtos != null) {
                        val logs = dtos.map { dto ->
                            AuditLogEntry(
                                id = dto.id,
                                actorUserId = dto.actorUserId,
                                eventType = dto.eventType,
                                resourceType = dto.resourceType,
                                resourceId = dto.resourceId,
                                ipAddress = dto.ipAddress,
                                details = dto.details?.toString() ?: "{}",
                                createdAt = dto.createdAt
                            )
                        }
                        Result.Success(logs)
                    } else {
                        val errorMsg = body?.error?.message ?: "خطا در دریافت لاگ‌های مدیریتی"
                        Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                    }
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    Result.Error(Exception(errorMessage), errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = NetworkErrorMapper.mapThrowable(e)
                Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
            }
        }
}
