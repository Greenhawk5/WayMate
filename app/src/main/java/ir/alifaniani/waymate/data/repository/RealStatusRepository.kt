package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.repository.StatusRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Production implementation of [StatusRepository] backed by Cloudflare Workers
 * and local offline store-and-forward caching.
 */
class RealStatusRepository(
    private val statusApiService: StatusApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val offlineStorage: OfflineStorage? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : StatusRepository {

    override suspend fun getLiveTeamStatuses(): Result<List<TeamLiveStatus>> =
        withContext(dispatcherProvider.io) {
            try {
                val response = statusApiService.getLiveUnits()
                if (response.isSuccessful) {
                    val body = response.body()
                    val dtos = body?.data
                    if (body != null && body.success && dtos != null) {
                        val statuses = dtos.map { it.toDomain() }
                        offlineStorage?.saveStatuses(statuses)
                        Result.Success(statuses)
                    } else {
                        val cached = offlineStorage?.getCachedStatuses()
                        if (!cached.isNullOrEmpty()) {
                            Result.Success(cached)
                        } else {
                            val errorMsg = body?.error?.message ?: "پاسخ نامعتبر از سرور وضعیت دریافت شد."
                            Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                        }
                    }
                } else {
                    val cached = offlineStorage?.getCachedStatuses()
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
                            else -> ApiException.NetworkUnavailable(errorMessage)
                        }
                        Result.Error(exception, errorMessage)
                    }
                }
            } catch (e: Exception) {
                val cached = offlineStorage?.getCachedStatuses()
                if (!cached.isNullOrEmpty()) {
                    Result.Success(cached)
                } else {
                    val errorMessage = NetworkErrorMapper.mapThrowable(e)
                    Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
                }
            }
        }

    override suspend fun updateOperationalStatus(status: OperationalStatus): Result<OperationalStatus> =
        withContext(dispatcherProvider.io) {
            offlineStorage?.saveMyOperationalStatus(status)
            val request = UpdateOperationalStatusRequestDto(operationalStatus = status.name)
            try {
                val response = statusApiService.updateOperationalStatus(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success) {
                        Result.Success(status)
                    } else {
                        val errorMsg = body?.error?.message ?: "خطا در به‌روزرسانی وضعیت در سرور."
                        Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                    }
                } else {
                    if (response.code() in 400..499) {
                        val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                        val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                        val exception = when (response.code()) {
                            401 -> ApiException.Unauthorized(errorMessage)
                            403 -> ApiException.Forbidden(errorMessage)
                            else -> ApiException.ServerError(response.code(), errorMessage)
                        }
                        Result.Error(exception, errorMessage)
                    } else {
                        enqueueOfflineStatus(request, status)
                    }
                }
            } catch (_: Exception) {
                enqueueOfflineStatus(request, status)
            }
        }

    override suspend fun getMyOperationalStatus(): OperationalStatus? =
        offlineStorage?.getMyOperationalStatus()

    private suspend fun enqueueOfflineStatus(
        request: UpdateOperationalStatusRequestDto,
        status: OperationalStatus
    ): Result<OperationalStatus> {
        return if (offlineStorage != null) {
            offlineStorage.enqueueAction(
                QueuedAction(
                    id = UUID.randomUUID().toString(),
                    actionType = ActionType.UPDATE_STATUS,
                    payloadJson = json.encodeToString(request),
                    createdAt = System.currentTimeMillis()
                )
            )
            Result.Success(status)
        } else {
            val msg = "عدم برقراری ارتباط با شبکه جهت به‌روزرسانی وضعیت"
            Result.Error(ApiException.NetworkUnavailable(msg), msg)
        }
    }
}
