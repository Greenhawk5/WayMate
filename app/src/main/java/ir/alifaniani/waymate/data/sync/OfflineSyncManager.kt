package ir.alifaniani.waymate.data.sync

import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.data.network.TeamApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    private val messageApiService: MessageApiService,
    private val statusApiService: StatusApiService,
    private val teamApiService: TeamApiService,
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
                    val request = json.decodeFromString<UpdateOperationalStatusRequestDto>(action.payloadJson)
                    val response = statusApiService.updateOperationalStatus(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در تغییر وضعیت")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
                    }
                }
                ActionType.ACK_MESSAGE -> {
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
                    val request = json.decodeFromString<CreateTeamRequestDto>(action.payloadJson)
                    val response = teamApiService.createTeam(request)
                    if (response.isSuccessful && response.body()?.success == true) {
                        ActionExecutionResult.Success
                    } else if (response.code() in 400..499) {
                        ActionExecutionResult.UnrecoverableError("خطای اعتبارسنجی (${response.code()}) در ایجاد تیم")
                    } else {
                        ActionExecutionResult.NetworkFailure("خطای سرور (${response.code()})")
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
