package ir.alifaniani.waymate.data.local.sqlite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Standard SQLite database helper for WayMate offline storage.
 *
 * Implements local persistence using the built-in Android SQLite engine
 * with zero external dependencies, zero KSP plugins, and full ACID transaction safety.
 */
class WayMateDatabaseHelper(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // 1. Offline Action Queue Table
            db.execSQL(
                """
                CREATE TABLE $TABLE_OFFLINE_QUEUE (
                    $COL_QUEUE_ID TEXT PRIMARY KEY,
                    $COL_QUEUE_ACTION_TYPE TEXT NOT NULL,
                    $COL_QUEUE_PAYLOAD_JSON TEXT NOT NULL,
                    $COL_QUEUE_CREATED_AT INTEGER NOT NULL,
                    $COL_QUEUE_RETRY_COUNT INTEGER NOT NULL DEFAULT 0,
                    $COL_QUEUE_LAST_ATTEMPT_AT INTEGER,
                    $COL_QUEUE_LAST_ERROR TEXT,
                    $COL_QUEUE_STATUS TEXT NOT NULL DEFAULT 'PENDING'
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_queue_created ON $TABLE_OFFLINE_QUEUE($COL_QUEUE_CREATED_AT ASC)")
            db.execSQL("CREATE INDEX idx_queue_status ON $TABLE_OFFLINE_QUEUE($COL_QUEUE_STATUS)")

            // 2. Cached Teams Table
            db.execSQL(
                """
                CREATE TABLE $TABLE_CACHED_TEAMS (
                    $COL_TEAM_ID TEXT PRIMARY KEY,
                    $COL_TEAM_NAME TEXT NOT NULL,
                    $COL_TEAM_LEAD_USER_ID TEXT NOT NULL,
                    $COL_TEAM_VEHICLE_TYPE TEXT,
                    $COL_TEAM_VEHICLE_PLATE TEXT,
                    $COL_TEAM_EQUIPMENT_SUMMARY TEXT,
                    $COL_TEAM_IS_ACTIVE INTEGER NOT NULL DEFAULT 1,
                    $COL_TEAM_MEMBERS_JSON TEXT,
                    $COL_TEAM_SYNCED_AT INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 3. Cached Tactical Messages Table
            db.execSQL(
                """
                CREATE TABLE $TABLE_CACHED_MESSAGES (
                    $COL_MSG_ID TEXT PRIMARY KEY,
                    $COL_MSG_SENDER_CALLSIGN TEXT NOT NULL,
                    $COL_MSG_BODY TEXT NOT NULL,
                    $COL_MSG_IS_URGENT INTEGER NOT NULL DEFAULT 0,
                    $COL_MSG_IS_ACKNOWLEDGED INTEGER NOT NULL DEFAULT 0,
                    $COL_MSG_CREATED_AT INTEGER NOT NULL,
                    $COL_MSG_IS_PENDING_SYNC INTEGER NOT NULL DEFAULT 0,
                    $COL_MSG_SYNCED_AT INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX idx_msg_created ON $TABLE_CACHED_MESSAGES($COL_MSG_CREATED_AT DESC)")

            // 4. Cached Unit Statuses Table
            db.execSQL(
                """
                CREATE TABLE $TABLE_CACHED_STATUSES (
                    $COL_STATUS_TEAM_ID TEXT PRIMARY KEY,
                    $COL_STATUS_TEAM_NAME TEXT NOT NULL,
                    $COL_STATUS_CALLSIGN TEXT NOT NULL,
                    $COL_STATUS_OPERATIONAL TEXT NOT NULL,
                    $COL_STATUS_CONNECTION TEXT NOT NULL,
                    $COL_STATUS_LAST_SEEN_AT INTEGER NOT NULL,
                    $COL_STATUS_IS_STALE INTEGER NOT NULL DEFAULT 0,
                    $COL_STATUS_SYNCED_AT INTEGER NOT NULL
                )
                """.trimIndent()
            )

            // 5. Metadata Table for synchronization timestamps
            db.execSQL(
                """
                CREATE TABLE $TABLE_METADATA (
                    $COL_META_KEY TEXT PRIMARY KEY,
                    $COL_META_VALUE INTEGER NOT NULL
                )
                """.trimIndent()
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Incremental migrations will be placed here in future phases.
        // For development v1, drop and recreate if schema increments.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_OFFLINE_QUEUE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_TEAMS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_MESSAGES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_STATUSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_METADATA")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "waymate_offline.db"
        const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_OFFLINE_QUEUE = "offline_queue"
        const val TABLE_CACHED_TEAMS = "cached_teams"
        const val TABLE_CACHED_MESSAGES = "cached_messages"
        const val TABLE_CACHED_STATUSES = "cached_statuses"
        const val TABLE_METADATA = "sync_metadata"

        // offline_queue columns
        const val COL_QUEUE_ID = "id"
        const val COL_QUEUE_ACTION_TYPE = "action_type"
        const val COL_QUEUE_PAYLOAD_JSON = "payload_json"
        const val COL_QUEUE_CREATED_AT = "created_at"
        const val COL_QUEUE_RETRY_COUNT = "retry_count"
        const val COL_QUEUE_LAST_ATTEMPT_AT = "last_attempt_at"
        const val COL_QUEUE_LAST_ERROR = "last_error"
        const val COL_QUEUE_STATUS = "status"

        // cached_teams columns
        const val COL_TEAM_ID = "id"
        const val COL_TEAM_NAME = "name"
        const val COL_TEAM_LEAD_USER_ID = "lead_user_id"
        const val COL_TEAM_VEHICLE_TYPE = "vehicle_type"
        const val COL_TEAM_VEHICLE_PLATE = "vehicle_plate"
        const val COL_TEAM_EQUIPMENT_SUMMARY = "equipment_summary"
        const val COL_TEAM_IS_ACTIVE = "is_active"
        const val COL_TEAM_MEMBERS_JSON = "members_json"
        const val COL_TEAM_SYNCED_AT = "synced_at"

        // cached_messages columns
        const val COL_MSG_ID = "id"
        const val COL_MSG_SENDER_CALLSIGN = "sender_callsign"
        const val COL_MSG_BODY = "body"
        const val COL_MSG_IS_URGENT = "is_urgent"
        const val COL_MSG_IS_ACKNOWLEDGED = "is_acknowledged"
        const val COL_MSG_CREATED_AT = "created_at"
        const val COL_MSG_IS_PENDING_SYNC = "is_pending_sync"
        const val COL_MSG_SYNCED_AT = "synced_at"

        // cached_statuses columns
        const val COL_STATUS_TEAM_ID = "team_id"
        const val COL_STATUS_TEAM_NAME = "team_name"
        const val COL_STATUS_CALLSIGN = "callsign"
        const val COL_STATUS_OPERATIONAL = "operational_status"
        const val COL_STATUS_CONNECTION = "connection_status"
        const val COL_STATUS_LAST_SEEN_AT = "last_seen_at"
        const val COL_STATUS_IS_STALE = "is_stale"
        const val COL_STATUS_SYNCED_AT = "synced_at"

        // metadata columns
        const val COL_META_KEY = "meta_key"
        const val COL_META_VALUE = "meta_value"

        const val META_KEY_TEAMS_SYNCED = "teams_last_synced"
        const val META_KEY_MESSAGES_SYNCED = "messages_last_synced"
        const val META_KEY_STATUSES_SYNCED = "statuses_last_synced"
    }
}
