package ir.alifaniani.waymate.data.local

import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coroutine-safe In-Memory implementation of [OfflineStorage] for deterministic unit tests.
 */
class InMemoryOfflineStorage : OfflineStorage {

    private val mutex = Mutex()
    private val queue = mutableListOf<QueuedAction>()
    private val teams = mutableListOf<Team>()
    private val messages = mutableListOf<TacticalMessage>()
    private val statuses = mutableListOf<TeamLiveStatus>()

    private var teamsLastSynced: Long = 0L
    private var messagesLastSynced: Long = 0L
    private var statusesLastSynced: Long = 0L

    override suspend fun enqueueAction(action: QueuedAction): Unit = mutex.withLock {
        val existingIndex = queue.indexOfFirst { it.id == action.id }
        if (existingIndex >= 0) {
            queue[existingIndex] = action
        } else {
            queue.add(action)
        }
    }

    override suspend fun getPendingActions(): List<QueuedAction> = mutex.withLock {
        queue.filter { it.status != QueueStatus.FAILED }
            .sortedBy { it.createdAt }
            .toList()
    }

    override suspend fun updateActionStatus(
        id: String,
        status: QueueStatus,
        retryCount: Int,
        lastAttemptAt: Long?,
        lastError: String?
    ): Unit = mutex.withLock {
        val index = queue.indexOfFirst { it.id == id }
        if (index >= 0) {
            queue[index] = queue[index].copy(
                status = status,
                retryCount = retryCount,
                lastAttemptAt = lastAttemptAt,
                lastError = lastError
            )
        }
    }

    override suspend fun removeAction(id: String): Unit = mutex.withLock {
        queue.removeAll { it.id == id }
    }

    override suspend fun getPendingActionCount(): Int = mutex.withLock {
        queue.count { it.status != QueueStatus.FAILED }
    }

    override suspend fun saveTeams(teams: List<Team>): Unit = mutex.withLock {
        this.teams.clear()
        this.teams.addAll(teams)
        this.teamsLastSynced = System.currentTimeMillis()
    }

    override suspend fun getCachedTeams(): List<Team> = mutex.withLock {
        teams.toList()
    }

    override suspend fun getTeamsLastSynced(): Long = mutex.withLock {
        teamsLastSynced
    }

    override suspend fun saveMessages(messages: List<TacticalMessage>): Unit = mutex.withLock {
        // Keep messages that are pending sync
        val pending = this.messages.filter { it.isPendingSync }
        this.messages.clear()
        this.messages.addAll(pending)
        for (msg in messages) {
            val idx = this.messages.indexOfFirst { it.id == msg.id }
            if (idx >= 0) {
                this.messages[idx] = msg
            } else {
                this.messages.add(msg)
            }
        }
        this.messagesLastSynced = System.currentTimeMillis()
    }

    override suspend fun saveLocalMessage(message: TacticalMessage): Unit = mutex.withLock {
        val idx = messages.indexOfFirst { it.id == message.id }
        if (idx >= 0) {
            messages[idx] = message.copy(isPendingSync = true)
        } else {
            messages.add(0, message.copy(isPendingSync = true))
        }
    }

    override suspend fun markMessageAcknowledged(messageId: String): Unit = mutex.withLock {
        val idx = messages.indexOfFirst { it.id == messageId }
        if (idx >= 0) {
            messages[idx] = messages[idx].copy(isAcknowledged = true)
        }
    }

    override suspend fun getCachedMessages(): List<TacticalMessage> = mutex.withLock {
        messages.sortedByDescending { it.createdAt }
    }

    override suspend fun getMessagesLastSynced(): Long = mutex.withLock {
        messagesLastSynced
    }

    override suspend fun saveStatuses(statuses: List<TeamLiveStatus>): Unit = mutex.withLock {
        this.statuses.clear()
        this.statuses.addAll(statuses)
        this.statusesLastSynced = System.currentTimeMillis()
    }

    override suspend fun getCachedStatuses(): List<TeamLiveStatus> = mutex.withLock {
        statuses.toList()
    }

    override suspend fun getStatusesLastSynced(): Long = mutex.withLock {
        statusesLastSynced
    }

    override suspend fun purgeExpiredData(maxAgeMs: Long): Unit = mutex.withLock {
        val threshold = System.currentTimeMillis() - maxAgeMs
        messages.removeAll { it.createdAt < threshold && !it.isPendingSync }
    }

    override suspend fun clearCache(): Unit = mutex.withLock {
        teams.clear()
        messages.clear()
        statuses.clear()
        teamsLastSynced = 0L
        messagesLastSynced = 0L
        statusesLastSynced = 0L
    }

    override suspend fun clearQueue(): Unit = mutex.withLock {
        queue.clear()
    }

    override suspend fun clearAll(): Unit = mutex.withLock {
        teams.clear()
        messages.clear()
        statuses.clear()
        teamsLastSynced = 0L
        messagesLastSynced = 0L
        statusesLastSynced = 0L
        queue.clear()
    }
}
