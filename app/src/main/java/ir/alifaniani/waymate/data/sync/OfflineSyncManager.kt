package ir.alifaniani.waymate.data.sync

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.data.dto.CreatePointRequestDto
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.dto.UpdatePointRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.mapper.toDomain
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.PointOfInterestApiService
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.data.network.TeamApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


/**
 * Result metrics returned after a queue synchronization run.
 */
data class SyncResult(
    val syncedCount: Int,
    val failedCount: Int,
    val remainingCount: Int
)

/**
 * Coordinates flushing and replaying the Store-and-Forward offline queue
 * when network connectivity is restored.
 */
class OfflineSyncManager(
    private val offlineStorage: OfflineStorage,
    private val messageApiService: MessageApiService? = null,
    private val statusApiService: StatusApiService? = null,
    private val teamApiService: TeamApiService? = null,
    private val pointApiService: PointOfInterestApiService? = null,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {


    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingQueueCount = MutableStateFlow(0)
    val pendingQueueCount: StateFlow<Int> = _pendingQueueCount.asStateFlow()

    suspend fun refreshPendingCount() {
        val count = offlineStorage.getPendingActionCount()
        _pendingQueueCount.value = count
    }

    /**
     * Iterates through pending queue items in FIFO order and replays them to the server.
     */
    suspend fun drainQueue(): SyncResult = withContext(dispatcherProvider.io) {
        if (_isSyncing.value) {
            return@withContext SyncResult(0, 0, _pendingQueueCount.value)
        }

        _isSyncing.value = true
        var synced = 0
        var failed = 0

        try {
            val pendingActions = offlineStorage.getPendingActions()
            for (action in pendingActions) {
                offlineStorage.updateActionStatus(
                    id = action.id,
                    status = QueueStatus.IN_FLIGHT,
                    retryCount = action.retryCount,
                    lastAttemptAt = System.currentTimeMillis(),
                    lastError = null
                )

                val outcome = executeAction(action)
                when (outcome) {
                    is ActionExecutionResult.Success -> {
                        offlineStorage.removeAction(action.id)
                        synced++
                    }
                    is ActionExecutionResult.UnrecoverableError -> {
                        offlineStorage.updateActionStatus(
                            id = action.id,
                            status = QueueStatus.FAILED,
                            retryCount = action.retryCount + 1,
                            lastAttemptAt = System.currentTimeMillis(),
                            lastError = outcome.errorMessage
                        )
                        failed++
                    }
                    is ActionExecutionResult.NetworkFailure -> {
                        offlineStorage.updateActionStatus(
                            id = action.id,
                            status = QueueStatus.PENDING,
                            retryCount = action.retryCount + 1,
                            lastAttemptAt = System.currentTimeMillis(),
                            lastError = outcome.errorMessage
                        )
                        // If network is completely unreachable, pause draining rest of queue
                        break
                    }
                }
            }
        } finally {
            val remaining = offlineStorage.getPendingActionCount()
            _pendingQueueCount.value = remaining
            _isSyncing.value = false
        }

        val remaining = offlineStorage.getPendingActionCount()
        SyncResult(synced, failed, remaining)
    }

    private suspend fun executeAction(action: QueuedAction): ActionExecutionResult {
        return try {
            when (action.actionType) {
                ActionType.SEND_MESSAGE -> {
                    if (messageApiService == null) return ActionExecutionResult.UnrecoverableError("سرویس پیام در دسترس نیست")
                    val request = json.decodeFromString<SendMessageRequestDto>(action.payloadJson)
                    val response = messageApiService.sendMessage(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای کلاینت (${response.code()}) در ارسال پیام")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                    }
                }
                ActionType.UPDATE_STATUS -> {
                    if (statusApiService == null) return ActionExecutionResult.UnrecoverableError("سرویس وضعیت در دسترس نیست")
                    val request = json.decodeFromString<UpdateOperationalStatusRequestDto>(action.payloadJson)
                    val response = statusApiService.updateOperationalStatus(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        ActionExecutionResult.Success
                    } else if (response.code() == 403) {
                        // Local status is already persisted for COMMANDER/OWNER roles
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در تغییر وضعیت")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                    }
                }
                ActionType.ACK_MESSAGE -> {
                    if (messageApiService == null) return ActionExecutionResult.UnrecoverableError("سرویس پیام در دسترس نیست")
                    val messageId = action.payloadJson
                    val response = messageApiService.acknowledgeMessage(messageId)
                    if (response.isSuccessful && response.body()?.success == true) {
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در تأیید دریافت")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                    }
                }
                ActionType.CREATE_TEAM -> {
                    if (teamApiService == null) return ActionExecutionResult.UnrecoverableError("سرویس تیم در دسترس نیست")
                    val request = json.decodeFromString<CreateTeamRequestDto>(action.payloadJson)
                    val response = teamApiService.createTeam(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        val createdTeam = response.body()?.data?.toDomain()
                        if (createdTeam != null) {
                            val existing = offlineStorage.getCachedTeams().toMutableList()
                            val optimisticIdx = existing.indexOfFirst {
                                it.id != createdTeam.id && it.name == createdTeam.name && it.vehiclePlate == createdTeam.vehiclePlate
                            }
                            if (optimisticIdx != -1) {
                                existing[optimisticIdx] = createdTeam
                            } else if (existing.none { it.id == createdTeam.id }) {
                                existing.add(createdTeam)
                            }
                            offlineStorage.saveTeams(existing)
                        }
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در ایجاد تیم")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                    }
                }
                ActionType.CREATE_POINT -> {
                    if (pointApiService == null) {
                        ActionExecutionResult.UnrecoverableError("سرویس نقاط در دسترس نیست")
                    } else {
                        val payload = json.decodeFromString<CreatePointQueuePayload>(action.payloadJson)
                        val request = CreatePointRequestDto(
                            title = payload.title,
                            description = payload.description,
                            category = payload.category,
                            latitude = payload.latitude,
                            longitude = payload.longitude,
                            teamId = payload.teamId
                        )
                        val response = pointApiService.createPoint(request)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val created = response.body()?.data?.toDomain()
                            if (created != null) {
                                offlineStorage.saveLocalPoint(created.copy(isPendingSync = false))
                            }
                            ActionExecutionResult.Success
                        } else if (response.code() in 400..499) {
                            ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در ثبت نقطه")
                        } else {
                            ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                        }
                    }
                }
                ActionType.UPDATE_POINT -> {
                    if (pointApiService == null) {
                        ActionExecutionResult.UnrecoverableError("سرویس نقاط در دسترس نیست")
                    } else {
                        val payload = json.decodeFromString<UpdatePointQueuePayload>(action.payloadJson)
                        val request = UpdatePointRequestDto(
                            title = payload.title,
                            description = payload.description,
                            category = payload.category,
                            latitude = payload.latitude,
                            longitude = payload.longitude,
                            version = payload.version
                        )
                        val response = pointApiService.updatePoint(payload.id, request)
                        if (response.isSuccessful && response.body()?.success == true) {
                            val updated = response.body()?.data?.toDomain()
                            if (updated != null) {
                                offlineStorage.saveLocalPoint(updated.copy(isPendingSync = false))
                            }
                            ActionExecutionResult.Success
                        } else if (response.code() == 409) {
                            ActionExecutionResult.UnrecoverableError("تعارض ویرایش همزمان (نسخه نقطه تغییر یافته است)")
                        } else if (response.code() in 400..499) {
                            ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در ویرایش نقطه")
                        } else {
                            ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                        }
                    }
                }
                ActionType.DELETE_POINT -> {
                    if (pointApiService == null) {
                        ActionExecutionResult.UnrecoverableError("سرویس نقاط در دسترس نیست")
                    } else {
                        val payload = json.decodeFromString<DeletePointQueuePayload>(action.payloadJson)
                        val response = pointApiService.deletePoint(payload.id)
                        if ((response.isSuccessful && response.body()?.success == true) || response.code() == 404) {
                            offlineStorage.markPointDeletedLocally(payload.id)
                            ActionExecutionResult.Success
                        } else if (response.code() in 400..499) {
                            ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در حذف نقطه")
                        } else {
                            ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            ActionExecutionResult.NetworkFailure(e.localizedMessage ?: "عدم برقراری ارتباط با شبکه")
        }
    }

    private sealed interface ActionExecutionResult {
        object Success : ActionExecutionResult
        data class UnrecoverableError(val errorMessage: String) : ActionExecutionResult
        data class NetworkFailure(val errorMessage: String) : ActionExecutionResult
    }
}

@Serializable
data class CreatePointQueuePayload(
    val localId: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val teamId: String? = null
)

@Serializable
data class UpdatePointQueuePayload(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val version: Int
)

@Serializable
data class DeletePointQueuePayload(
    val id: String
)
