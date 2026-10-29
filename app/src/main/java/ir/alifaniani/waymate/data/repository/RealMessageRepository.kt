package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.NetworkErrorMapper
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.repository.MessageRepository
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Production implementation of [MessageRepository] backed by Cloudflare Workers
 * and local offline store-and-forward caching.
 */
class RealMessageRepository(
    private val messageApiService: MessageApiService,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val offlineStorage: OfflineStorage? = null,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : MessageRepository {

    override suspend fun getMessages(): Result<List<TacticalMessage>> =
        withContext(dispatcherProvider.io) {
            try {
                val response = messageApiService.getMessages()
                if (response.isSuccessful) {
                    val body = response.body()
                    val dtos = body?.data
                    if (body != null && body.success && dtos != null) {
                        val messages = dtos.map { it.toDomain() }
                        offlineStorage?.saveMessages(messages)
                        Result.Success(messages)
                    } else {
                        val cached = offlineStorage?.getCachedMessages()
                        if (!cached.isNullOrEmpty()) {
                            Result.Success(cached)
                        } else {
                            val errorMsg = body?.error?.message ?: "پاسخ نامعتبر از سرور پیام‌ها دریافت شد."
                            Result.Error(ApiException.ServerError(response.code(), errorMsg), errorMsg)
                        }
                    }
                } else {
                    val cached = offlineStorage?.getCachedMessages()
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
                val cached = offlineStorage?.getCachedMessages()
                if (!cached.isNullOrEmpty()) {
                    Result.Success(cached)
                } else {
                    val errorMessage = NetworkErrorMapper.mapThrowable(e)
                    Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
                }
            }
        }

    override suspend fun acknowledgeMessage(messageId: String): Result<Boolean> =
        withContext(dispatcherProvider.io) {
            try {
                val response = messageApiService.acknowledgeMessage(messageId)
                if (response.isSuccessful && response.body()?.success == true) {
                    offlineStorage?.markMessageAcknowledged(messageId)
                    Result.Success(true)
                } else {
                    val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                    val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                    Result.Error(ApiException.ServerError(response.code(), errorMessage), errorMessage)
                }
            } catch (e: Exception) {
                // If offline, queue the ACK action and mark locally acknowledged
                if (offlineStorage != null) {
                    offlineStorage.enqueueAction(
                        QueuedAction(
                            id = UUID.randomUUID().toString(),
                            actionType = ActionType.ACK_MESSAGE,
                            payloadJson = messageId,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    offlineStorage.markMessageAcknowledged(messageId)
                    Result.Success(true)
                } else {
                    val errorMessage = NetworkErrorMapper.mapThrowable(e)
                    Result.Error(ApiException.NetworkUnavailable(errorMessage), errorMessage)
                }
            }
        }

    override suspend fun sendMessage(
        body: String,
        isUrgent: Boolean,
        targetTeamId: String?
    ): Result<TacticalMessage> =
        withContext(dispatcherProvider.io) {
            val request = SendMessageRequestDto(
                body = body,
                isUrgent = isUrgent,
                targetTeamId = targetTeamId
            )
            try {
                val response = messageApiService.sendMessage(request)
                if (response.isSuccessful) {
                    val resBody = response.body()
                    val dto = resBody?.data
                    if (resBody != null && resBody.success && dto != null) {
                        val message = dto.toDomain()
                        offlineStorage?.saveLocalMessage(message)
                        Result.Success(message)
                    } else {
                        enqueueOfflineMessage(request)
                    }
                } else {
                    if (response.code() in 400..499) {
                        val apiError = NetworkErrorMapper.parseApiError(response.errorBody())
                        val errorMessage = NetworkErrorMapper.mapHttpError(response.code(), apiError)
                        val exception = when (response.code()) {
                            401 -> ApiException.Unauthorized(errorMessage)
                            403 -> ApiException.Forbidden(errorMessage)
                            404 -> ApiException.NotFound(errorMessage)
                            else -> ApiException.ServerError(response.code(), errorMessage)
                        }
                        Result.Error(exception, errorMessage)
                    } else {
                        // 5xx or server gateway error: buffer offline
                        enqueueOfflineMessage(request)
                    }
                }
            } catch (_: Exception) {
                // Network unreachable: buffer offline
                enqueueOfflineMessage(request)
            }
        }

    private suspend fun enqueueOfflineMessage(request: SendMessageRequestDto): Result<TacticalMessage> {
        return if (offlineStorage != null) {
            val localId = "pending_" + UUID.randomUUID().toString().take(8)
            val now = System.currentTimeMillis()
            val localMessage = TacticalMessage(
                id = localId,
                senderCallsign = "شما",
                body = request.body,
                isUrgent = request.isUrgent,
                createdAt = now,
                isAcknowledged = false,
                isPendingSync = true
            )
            offlineStorage.enqueueAction(
                QueuedAction(
                    id = UUID.randomUUID().toString(),
                    actionType = ActionType.SEND_MESSAGE,
                    payloadJson = json.encodeToString(request),
                    createdAt = now
                )
            )
            offlineStorage.saveLocalMessage(localMessage)
            Result.Success(localMessage)
        } else {
            val msg = "عدم برقراری ارتباط با شبکه جهت ارسال پیام"
            Result.Error(ApiException.NetworkUnavailable(msg), msg)
        }
    }
}
