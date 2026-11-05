package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.dto.CreateMemberDto
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.UpdateTeamRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.domain.repository.TeamRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Production implementation of [TeamRepository] communicating with Cloudflare Workers backend
 * with local offline caching and fallback.
 */
class RealTeamRepository(
    private val teamApiService: TeamApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val offlineStorage: OfflineStorage? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TeamRepository {

    override suspend fun getTeams(fetchDetails: Boolean): Result<List<Team>> =
        withContext(dispatcherProvider.io) {
            try {
                val response = teamApiService.getTeams()
                if (response.isSuccessful) {
                    val body = response.body()
                    val teamDtos = body?.data
                    if (body != null && body.success && teamDtos != null) {
                        val teams = if (fetchDetails && teamDtos.isNotEmpty()) {
                            teamDtos.map { dto ->
                                try {
                                    val detailResponse = teamApiService.getTeamById(dto.id)
                                    val detailBody = detailResponse.body()
                                    if (detailResponse.isSuccessful && detailBody != null && detailBody.success && detailBody.data != null) {
                                        detailBody.data.toDomain()
                                    } else {
                                        dto.toDomain()
                                    }
                                } catch (_: Exception) {
                                    dto.toDomain()
                                }
                            }
                        } else {
                            teamDtos.map { it.toDomain() }
                        }
                        offlineStorage?.saveTeams(teams)
                        Result.Success(teams)
                    } else {
                        val cached = offlineStorage?.getCachedTeams()
                        if (!cached.isNullOrEmpty()) {
                            Result.Success(cached)
                        } else {
                            val errorMsg = body?.error?.message ?: "پاسخ نامعتبر از سرور دریافت شد."
                            Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                        }
                    }
                } else {
                    val cached = offlineStorage?.getCachedTeams()
                    if (!cached.isNullOrEmpty()) {
                        Result.Success(cached)
                    } else {
                        val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                        val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                        val exception = when (response.code()) {
                            401 -> ApiException.Unauthorized(errorMessage)
                            403 -> ApiException.Forbidden(errorMessage)
                            404 -> ApiException.NotFound(errorMessage)
                            429 -> ApiException.RateLimited(errorMessage)
                            in 500..599 -> ApiException.ServerError(response.code(), errorMessage)
                            else -> Exception(errorMessage)
                        }
                        Result.Error(exception, errorMessage)
                    }
                }
            } catch (e: Exception) {
                val cached = offlineStorage?.getCachedTeams()
                if (!cached.isNullOrEmpty()) {
                    Result.Success(cached)
                } else {
                    val errorMessage = NetworkErrorMapper.mapThrowable(e)
                    Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
                }
            }
        }

    override suspend fun getTeamDetails(teamId: String): Result<Team> =
        withContext(dispatcherProvider.io) {
            try {
                val response = teamApiService.getTeamById(teamId)
                if (response.isSuccessful) {
                    val body = response.body()
                    val teamDto = body?.data
                    if (body != null && body.success && teamDto != null) {
                        Result.Success(teamDto.toDomain())
                    } else {
                        val errorMsg = body?.error?.message ?: "پاسخ نامعتبر از سرور دریافت شد."
                        Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                    }
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    val exception = when (response.code()) {
                        401 -> ApiException.Unauthorized(errorMessage)
                        403 -> ApiException.Forbidden(errorMessage)
                        404 -> ApiException.NotFound(errorMessage)
                        429 -> ApiException.RateLimited(errorMessage)
                        in 500..599 -> ApiException.ServerError(response.code(), errorMessage)
                        else -> Exception(errorMessage)
                    }
                    Result.Error(exception, errorMessage)
                }
            } catch (e: Exception) {
                val cached = offlineStorage?.getCachedTeams()?.firstOrNull { it.id == teamId }
                if (cached != null) {
                    Result.Success(cached)
                } else {
                    val errorMessage = NetworkErrorMapper.mapThrowable(e)
                    Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
                }
            }
        }

    override suspend fun createTeam(
        name: String,
        leadUserId: String,
        vehicleType: String,
        vehiclePlate: String,
        equipmentSummary: String,
        members: List<TeamMember>
    ): Result<Team> = withContext(dispatcherProvider.io) {
        val request = CreateTeamRequestDto(
            name = name,
            leadUserId = leadUserId,
            vehicleType = vehicleType,
            vehiclePlate = vehiclePlate,
            equipmentSummary = equipmentSummary,
            members = members.map { CreateMemberDto(it.fullName, it.callsign, it.roleInTeam) }
        )
        try {
            val response = teamApiService.createTeam(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.Success(body.data.toDomain())
                } else {
                    val errorMsg = body?.error?.message ?: "خطا در ایجاد گروه عملیاتی"
                    Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                }
            } else {
                val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                Result.Error(Exception(errorMessage), errorMessage)
            }
        } catch (e: Exception) {
            if (offlineStorage != null) {
                val localId = "team_" + UUID.randomUUID().toString().take(8)
                val optimisticTeam = Team(
                    id = localId,
                    name = name,
                    leadUserId = leadUserId,
                    vehicleType = vehicleType,
                    vehiclePlate = vehiclePlate,
                    equipmentSummary = equipmentSummary,
                    members = members,
                    memberCount = members.size,
                    isActive = true
                )
                offlineStorage.enqueueAction(
                    QueuedAction(
                        id = UUID.randomUUID().toString(),
                        actionType = ActionType.CREATE_TEAM,
                        payloadJson = json.encodeToString(request),
                        createdAt = System.currentTimeMillis()
                    )
                )
                Result.Success(optimisticTeam)
            } else {
                val errorMessage = NetworkErrorMapper.mapThrowable(e)
                Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
            }
        }
    }

    override suspend fun updateTeam(
        teamId: String,
        name: String?,
        leadUserId: String?,
        vehicleType: String?,
        vehiclePlate: String?,
        equipmentSummary: String?,
        isActive: Boolean?
    ): Result<Team> = withContext(dispatcherProvider.io) {
        try {
            val request = UpdateTeamRequestDto(
                name = name,
                leadUserId = leadUserId,
                vehicleType = vehicleType,
                vehiclePlate = vehiclePlate,
                equipmentSummary = equipmentSummary,
                isActive = isActive
            )
            val response = teamApiService.updateTeam(teamId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.Success(body.data.toDomain())
                } else {
                    val errorMsg = body?.error?.message ?: "خطا در به‌روزرسانی گروه عملیاتی"
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

    override suspend fun addMember(
        teamId: String,
        fullName: String,
        callsign: String,
        roleInTeam: String
    ): Result<TeamMember> = withContext(dispatcherProvider.io) {
        try {
            val request = CreateMemberDto(
                fullName = fullName,
                callsign = callsign,
                roleInTeam = roleInTeam
            )
            val response = teamApiService.addMember(teamId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    val newMember = body.data.toDomain()
                    offlineStorage?.let { storage ->
                        val cachedTeams = storage.getCachedTeams().toMutableList()
                        val idx = cachedTeams.indexOfFirst { it.id == teamId }
                        if (idx != -1) {
                            val t = cachedTeams[idx]
                            val updatedMembers = t.members + newMember
                            cachedTeams[idx] = t.copy(members = updatedMembers, memberCount = updatedMembers.size)
                            storage.saveTeams(cachedTeams)
                        }
                    }
                    Result.Success(newMember)
                } else {
                    val errorMsg = body?.error?.message ?: "خطا در افزودن عضو به گروه عملیاتی"
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

    override suspend fun removeMember(
        teamId: String,
        memberId: String
    ): Result<Unit> = withContext(dispatcherProvider.io) {
        try {
            val response = teamApiService.removeMember(teamId, memberId)
            if (response.isSuccessful) {
                offlineStorage?.let { storage ->
                    val cachedTeams = storage.getCachedTeams().toMutableList()
                    val idx = cachedTeams.indexOfFirst { it.id == teamId }
                    if (idx != -1) {
                        val t = cachedTeams[idx]
                        val updatedMembers = t.members.filterNot { it.id == memberId }
                        cachedTeams[idx] = t.copy(members = updatedMembers, memberCount = updatedMembers.size)
                        storage.saveTeams(cachedTeams)
                    }
                }
                Result.Success(Unit)
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
