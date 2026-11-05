package ir.alifaniani.waymate.data.local.model

import kotlinx.serialization.Serializable

/**
 * Tactical actions supported by the Store-and-Forward offline queue.
 */
enum class ActionType {
    SEND_MESSAGE,
    UPDATE_STATUS,
    ACK_MESSAGE,
    CREATE_TEAM,
    CREATE_POINT,
    UPDATE_POINT,
    DELETE_POINT
}


/**
 * Execution state of an item in the offline queue.
 */
enum class QueueStatus {
    PENDING,
    IN_FLIGHT,
    FAILED
}

/**
 * A persistent outgoing mutation record queued locally while network is disconnected.
 */
@Serializable
data class QueuedAction(
    val id: String,
    val actionType: ActionType,
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val lastError: String? = null,
    val status: QueueStatus = QueueStatus.PENDING
)
