package ir.alifaniani.waymate.data.local.sqlite

import android.content.ContentValues
import android.database.Cursor
import ir.alifaniani.waymate.core.common.DefaultDispatcherProvider
import ir.alifaniani.waymate.core.common.DispatcherProvider
import ir.alifaniani.waymate.data.local.OfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.TeamMember
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Production [OfflineStorage] implementation backed by Android SQLite.
 */
class SqliteOfflineStorage(
    private val dbHelper: WayMateDatabaseHelper,
    private val dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) : OfflineStorage {

    // ==========================================
    // Offline Store-and-Forward Queue
    // ==========================================

    override suspend fun enqueueAction(action: QueuedAction): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(WayMateDatabaseHelper.COL_QUEUE_ID, action.id)
                put(WayMateDatabaseHelper.COL_QUEUE_ACTION_TYPE, action.actionType.name)
                put(WayMateDatabaseHelper.COL_QUEUE_PAYLOAD_JSON, action.payloadJson)
                put(WayMateDatabaseHelper.COL_QUEUE_CREATED_AT, action.createdAt)
                put(WayMateDatabaseHelper.COL_QUEUE_RETRY_COUNT, action.retryCount)
                put(WayMateDatabaseHelper.COL_QUEUE_LAST_ATTEMPT_AT, action.lastAttemptAt)
                put(WayMateDatabaseHelper.COL_QUEUE_LAST_ERROR, action.lastError)
                put(WayMateDatabaseHelper.COL_QUEUE_STATUS, action.status.name)
            }
            db.insertWithOnConflict(
                WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
        }

    override suspend fun getPendingActions(): List<QueuedAction> =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.readableDatabase
            val actions = mutableListOf<QueuedAction>()
            val cursor = db.query(
                WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE,
                null,
                "${WayMateDatabaseHelper.COL_QUEUE_STATUS} != ?",
                arrayOf(QueueStatus.FAILED.name),
                null,
                null,
                "${WayMateDatabaseHelper.COL_QUEUE_CREATED_AT} ASC"
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    actions.add(cursorToQueuedAction(c))
                }
            }
            actions
        }

    override suspend fun updateActionStatus(
        id: String,
        status: QueueStatus,
        retryCount: Int,
        lastAttemptAt: Long?,
        lastError: String?
    ): Unit = withContext(dispatcherProvider.io) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(WayMateDatabaseHelper.COL_QUEUE_STATUS, status.name)
            put(WayMateDatabaseHelper.COL_QUEUE_RETRY_COUNT, retryCount)
            put(WayMateDatabaseHelper.COL_QUEUE_LAST_ATTEMPT_AT, lastAttemptAt)
            put(WayMateDatabaseHelper.COL_QUEUE_LAST_ERROR, lastError)
        }
        db.update(
            WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE,
            values,
            "${WayMateDatabaseHelper.COL_QUEUE_ID} = ?",
            arrayOf(id)
        )
    }

    override suspend fun removeAction(id: String): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.delete(
                WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE,
                "${WayMateDatabaseHelper.COL_QUEUE_ID} = ?",
                arrayOf(id)
            )
        }

    override suspend fun getPendingActionCount(): Int =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT COUNT(*) FROM ${WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE} WHERE ${WayMateDatabaseHelper.COL_QUEUE_STATUS} != ?",
                arrayOf(QueueStatus.FAILED.name)
            )
            cursor.use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        }

    // ==========================================
    // Cached Teams
    // ==========================================

    override suspend fun saveTeams(teams: List<Team>): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                // Clear old cache before writing fresh server snapshot
                db.delete(WayMateDatabaseHelper.TABLE_CACHED_TEAMS, null, null)
                val now = System.currentTimeMillis()
                for (team in teams) {
                    val membersJson = json.encodeToString(team.members)
                    val values = ContentValues().apply {
                        put(WayMateDatabaseHelper.COL_TEAM_ID, team.id)
                        put(WayMateDatabaseHelper.COL_TEAM_NAME, team.name)
                        put(WayMateDatabaseHelper.COL_TEAM_LEAD_USER_ID, team.leadUserId)
                        put(WayMateDatabaseHelper.COL_TEAM_VEHICLE_TYPE, team.vehicleType)
                        put(WayMateDatabaseHelper.COL_TEAM_VEHICLE_PLATE, team.vehiclePlate)
                        put(WayMateDatabaseHelper.COL_TEAM_EQUIPMENT_SUMMARY, team.equipmentSummary)
                        put(WayMateDatabaseHelper.COL_TEAM_IS_ACTIVE, if (team.isActive) 1 else 0)
                        put(WayMateDatabaseHelper.COL_TEAM_MEMBERS_JSON, membersJson)
                        put(WayMateDatabaseHelper.COL_TEAM_SYNCED_AT, now)
                    }
                    db.insert(WayMateDatabaseHelper.TABLE_CACHED_TEAMS, null, values)
                }
                setMetadata(db, WayMateDatabaseHelper.META_KEY_TEAMS_SYNCED, now)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun getCachedTeams(): List<Team> =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.readableDatabase
            val teams = mutableListOf<Team>()
            val cursor = db.query(
                WayMateDatabaseHelper.TABLE_CACHED_TEAMS,
                null,
                null,
                null,
                null,
                null,
                "${WayMateDatabaseHelper.COL_TEAM_NAME} ASC"
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_ID))
                    val name = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_NAME))
                    val leadUserId = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_LEAD_USER_ID))
                    val vehicleType = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_VEHICLE_TYPE)) ?: ""
                    val vehiclePlate = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_VEHICLE_PLATE)) ?: ""
                    val equipmentSummary = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_EQUIPMENT_SUMMARY)) ?: ""
                    val isActive = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_IS_ACTIVE)) == 1
                    val membersJson = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_TEAM_MEMBERS_JSON))

                    val members = try {
                        if (!membersJson.isNullOrBlank()) {
                            json.decodeFromString<List<TeamMember>>(membersJson)
                        } else emptyList()
                    } catch (_: Exception) {
                        emptyList()
                    }

                    teams.add(
                        Team(
                            id = id,
                            name = name,
                            leadUserId = leadUserId,
                            vehicleType = vehicleType,
                            vehiclePlate = vehiclePlate,
                            equipmentSummary = equipmentSummary,
                            members = members,
                            memberCount = members.size,
                            isActive = isActive
                        )
                    )
                }
            }
            teams
        }

    override suspend fun getTeamsLastSynced(): Long =
        withContext(dispatcherProvider.io) {
            getMetadata(WayMateDatabaseHelper.META_KEY_TEAMS_SYNCED)
        }

    // ==========================================
    // Cached Tactical Messages
    // ==========================================

    override suspend fun saveMessages(messages: List<TacticalMessage>): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                // Keep locally pending messages while replacing synced messages
                db.delete(
                    WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                    "${WayMateDatabaseHelper.COL_MSG_IS_PENDING_SYNC} = 0",
                    null
                )
                val now = System.currentTimeMillis()
                for (msg in messages) {
                    val values = ContentValues().apply {
                        put(WayMateDatabaseHelper.COL_MSG_ID, msg.id)
                        put(WayMateDatabaseHelper.COL_MSG_SENDER_CALLSIGN, msg.senderCallsign)
                        put(WayMateDatabaseHelper.COL_MSG_BODY, msg.body)
                        put(WayMateDatabaseHelper.COL_MSG_IS_URGENT, if (msg.isUrgent) 1 else 0)
                        put(WayMateDatabaseHelper.COL_MSG_IS_ACKNOWLEDGED, if (msg.isAcknowledged) 1 else 0)
                        put(WayMateDatabaseHelper.COL_MSG_CREATED_AT, msg.createdAt)
                        put(WayMateDatabaseHelper.COL_MSG_IS_PENDING_SYNC, if (msg.isPendingSync) 1 else 0)
                        put(WayMateDatabaseHelper.COL_MSG_SYNCED_AT, now)
                    }
                    db.insertWithOnConflict(
                        WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                        null,
                        values,
                        android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                setMetadata(db, WayMateDatabaseHelper.META_KEY_MESSAGES_SYNCED, now)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun saveLocalMessage(message: TacticalMessage): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(WayMateDatabaseHelper.COL_MSG_ID, message.id)
                put(WayMateDatabaseHelper.COL_MSG_SENDER_CALLSIGN, message.senderCallsign)
                put(WayMateDatabaseHelper.COL_MSG_BODY, message.body)
                put(WayMateDatabaseHelper.COL_MSG_IS_URGENT, if (message.isUrgent) 1 else 0)
                put(WayMateDatabaseHelper.COL_MSG_IS_ACKNOWLEDGED, if (message.isAcknowledged) 1 else 0)
                put(WayMateDatabaseHelper.COL_MSG_CREATED_AT, message.createdAt)
                put(WayMateDatabaseHelper.COL_MSG_IS_PENDING_SYNC, 1)
                put(WayMateDatabaseHelper.COL_MSG_SYNCED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                null,
                values,
                android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
            )
        }

    override suspend fun markMessageAcknowledged(messageId: String): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(WayMateDatabaseHelper.COL_MSG_IS_ACKNOWLEDGED, 1)
            }
            db.update(
                WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                values,
                "${WayMateDatabaseHelper.COL_MSG_ID} = ?",
                arrayOf(messageId)
            )
        }

    override suspend fun getCachedMessages(): List<TacticalMessage> =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.readableDatabase
            val messages = mutableListOf<TacticalMessage>()
            val cursor = db.query(
                WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                null,
                null,
                null,
                null,
                null,
                "${WayMateDatabaseHelper.COL_MSG_CREATED_AT} DESC"
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_ID))
                    val senderCallsign = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_SENDER_CALLSIGN))
                    val body = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_BODY))
                    val isUrgent = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_IS_URGENT)) == 1
                    val isAcknowledged = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_IS_ACKNOWLEDGED)) == 1
                    val createdAt = c.getLong(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_CREATED_AT))
                    val isPendingSync = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_MSG_IS_PENDING_SYNC)) == 1

                    messages.add(
                        TacticalMessage(
                            id = id,
                            senderCallsign = senderCallsign,
                            body = body,
                            isUrgent = isUrgent,
                            createdAt = createdAt,
                            isAcknowledged = isAcknowledged,
                            isPendingSync = isPendingSync
                        )
                    )
                }
            }
            messages
        }

    override suspend fun getMessagesLastSynced(): Long =
        withContext(dispatcherProvider.io) {
            getMetadata(WayMateDatabaseHelper.META_KEY_MESSAGES_SYNCED)
        }

    // ==========================================
    // Cached Unit Statuses
    // ==========================================

    override suspend fun saveStatuses(statuses: List<TeamLiveStatus>): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(WayMateDatabaseHelper.TABLE_CACHED_STATUSES, null, null)
                val now = System.currentTimeMillis()
                for (status in statuses) {
                    val values = ContentValues().apply {
                        put(WayMateDatabaseHelper.COL_STATUS_TEAM_ID, status.teamId)
                        put(WayMateDatabaseHelper.COL_STATUS_TEAM_NAME, status.teamName)
                        put(WayMateDatabaseHelper.COL_STATUS_CALLSIGN, status.callsign)
                        put(WayMateDatabaseHelper.COL_STATUS_OPERATIONAL, status.operationalStatus.name)
                        put(WayMateDatabaseHelper.COL_STATUS_CONNECTION, status.connectionStatus.name)
                        put(WayMateDatabaseHelper.COL_STATUS_LAST_SEEN_AT, status.lastSeenAt)
                        put(WayMateDatabaseHelper.COL_STATUS_IS_STALE, if (status.isStale) 1 else 0)
                        put(WayMateDatabaseHelper.COL_STATUS_SYNCED_AT, now)
                    }
                    db.insert(WayMateDatabaseHelper.TABLE_CACHED_STATUSES, null, values)
                }
                setMetadata(db, WayMateDatabaseHelper.META_KEY_STATUSES_SYNCED, now)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun getCachedStatuses(): List<TeamLiveStatus> =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.readableDatabase
            val statuses = mutableListOf<TeamLiveStatus>()
            val cursor = db.query(
                WayMateDatabaseHelper.TABLE_CACHED_STATUSES,
                null,
                null,
                null,
                null,
                null,
                "${WayMateDatabaseHelper.COL_STATUS_TEAM_NAME} ASC"
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val teamId = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_TEAM_ID))
                    val teamName = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_TEAM_NAME))
                    val callsign = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_CALLSIGN))
                    val opStatusStr = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_OPERATIONAL))
                    val connStatusStr = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_CONNECTION))
                    val lastSeenAt = c.getLong(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_LAST_SEEN_AT))
                    val isStale = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_STATUS_IS_STALE)) == 1

                    val opStatus = try {
                        OperationalStatus.valueOf(opStatusStr)
                    } catch (_: Exception) {
                        OperationalStatus.NORMAL
                    }

                    val connStatus = try {
                        ConnectionStatus.valueOf(connStatusStr)
                    } catch (_: Exception) {
                        ConnectionStatus.OFFLINE
                    }

                    statuses.add(
                        TeamLiveStatus(
                            teamId = teamId,
                            teamName = teamName,
                            callsign = callsign,
                            operationalStatus = opStatus,
                            connectionStatus = connStatus,
                            lastSeenAt = lastSeenAt,
                            isStale = isStale
                        )
                    )
                }
            }
            statuses
        }

    override suspend fun getStatusesLastSynced(): Long =
        withContext(dispatcherProvider.io) {
            getMetadata(WayMateDatabaseHelper.META_KEY_STATUSES_SYNCED)
        }

    // ==========================================
    // Retention, Pruning & Session Cleanup
    // ==========================================

    override suspend fun purgeExpiredData(maxAgeMs: Long): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            val threshold = System.currentTimeMillis() - maxAgeMs
            // Prune old messages that have already been synced
            db.delete(
                WayMateDatabaseHelper.TABLE_CACHED_MESSAGES,
                "${WayMateDatabaseHelper.COL_MSG_CREATED_AT} < ? AND ${WayMateDatabaseHelper.COL_MSG_IS_PENDING_SYNC} = 0",
                arrayOf(threshold.toString())
            )
        }

    override suspend fun clearCache(): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(WayMateDatabaseHelper.TABLE_CACHED_TEAMS, null, null)
                db.delete(WayMateDatabaseHelper.TABLE_CACHED_MESSAGES, null, null)
                db.delete(WayMateDatabaseHelper.TABLE_CACHED_STATUSES, null, null)
                db.delete(WayMateDatabaseHelper.TABLE_METADATA, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun clearQueue(): Unit =
        withContext(dispatcherProvider.io) {
            val db = dbHelper.writableDatabase
            db.delete(WayMateDatabaseHelper.TABLE_OFFLINE_QUEUE, null, null)
        }

    override suspend fun clearAll(): Unit =
        withContext(dispatcherProvider.io) {
            clearCache()
            clearQueue()
        }

    // ==========================================
    // Internal Helpers
    // ==========================================

    private fun cursorToQueuedAction(c: Cursor): QueuedAction {
        val id = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_ID))
        val actionTypeStr = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_ACTION_TYPE))
        val payloadJson = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_PAYLOAD_JSON))
        val createdAt = c.getLong(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_CREATED_AT))
        val retryCount = c.getInt(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_RETRY_COUNT))
        val lastAttemptIdx = c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_LAST_ATTEMPT_AT)
        val lastAttemptAt = if (c.isNull(lastAttemptIdx)) null else c.getLong(lastAttemptIdx)
        val lastErrorIdx = c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_LAST_ERROR)
        val lastError = if (c.isNull(lastErrorIdx)) null else c.getString(lastErrorIdx)
        val statusStr = c.getString(c.getColumnIndexOrThrow(WayMateDatabaseHelper.COL_QUEUE_STATUS))

        val actionType = try {
            ActionType.valueOf(actionTypeStr)
        } catch (_: Exception) {
            ActionType.SEND_MESSAGE
        }

        val status = try {
            QueueStatus.valueOf(statusStr)
        } catch (_: Exception) {
            QueueStatus.PENDING
        }

        return QueuedAction(
            id = id,
            actionType = actionType,
            payloadJson = payloadJson,
            createdAt = createdAt,
            retryCount = retryCount,
            lastAttemptAt = lastAttemptAt,
            lastError = lastError,
            status = status
        )
    }

    private fun setMetadata(db: android.database.sqlite.SQLiteDatabase, key: String, value: Long) {
        val values = ContentValues().apply {
            put(WayMateDatabaseHelper.COL_META_KEY, key)
            put(WayMateDatabaseHelper.COL_META_VALUE, value)
        }
        db.insertWithOnConflict(
            WayMateDatabaseHelper.TABLE_METADATA,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun getMetadata(key: String): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            WayMateDatabaseHelper.TABLE_METADATA,
            arrayOf(WayMateDatabaseHelper.COL_META_VALUE),
            "${WayMateDatabaseHelper.COL_META_KEY} = ?",
            arrayOf(key),
            null,
            null,
            null
        )
        return cursor.use { c ->
            if (c.moveToFirst()) c.getLong(0) else 0L
        }
    }
}
