package ir.alifaniani.waymate.data.local

import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.TeamMember
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryOfflineStorageTest {

    private lateinit var storage: InMemoryOfflineStorage

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    @Test
    fun enqueueAction_persistsAndRetrievesInFifoOrder() = runBlocking {
        val action1 = QueuedAction(
            id = "act_1",
            actionType = ActionType.SEND_MESSAGE,
            payloadJson = "{\"body\":\"Order 1\"}",
            createdAt = 1000L
        )
        val action2 = QueuedAction(
            id = "act_2",
            actionType = ActionType.UPDATE_STATUS,
            payloadJson = "{\"status\":\"PURSUING\"}",
            createdAt = 2000L
        )

        // Insert in reverse order
        storage.enqueueAction(action2)
        storage.enqueueAction(action1)

        val pending = storage.getPendingActions()
        assertEquals(2, pending.size)
        // Must be sorted by createdAt ASC (FIFO)
        assertEquals("act_1", pending[0].id)
        assertEquals("act_2", pending[1].id)
        assertEquals(2, storage.getPendingActionCount())
    }

    @Test
    fun updateActionStatus_andRemoveAction() = runBlocking {
        val action = QueuedAction(
            id = "act_1",
            actionType = ActionType.SEND_MESSAGE,
            payloadJson = "{}",
            createdAt = 1000L
        )
        storage.enqueueAction(action)

        storage.updateActionStatus(
            id = "act_1",
            status = QueueStatus.FAILED,
            retryCount = 1,
            lastAttemptAt = 1500L,
            lastError = "Network error"
        )

        // FAILED actions are excluded from getPendingActions and count
        assertEquals(0, storage.getPendingActions().size)
        assertEquals(0, storage.getPendingActionCount())

        storage.removeAction("act_1")
        assertEquals(0, storage.getPendingActionCount())
    }

    @Test
    fun cachedTeams_saveAndRetrieve() = runBlocking {
        val team1 = Team(
            id = "team_1",
            name = "پشتوانه ۱",
            leadUserId = "lead_1",
            vehicleType = "تویوتا هایلوکس",
            vehiclePlate = "۱۲-الف-۳۴۵",
            equipmentSummary = "بیسیم تاکتیکی",
            members = listOf(
                TeamMember("m1", "علی حسینی", "آلفا-۱", "فرمانده دسته")
            )
        )

        storage.saveTeams(listOf(team1))

        val cached = storage.getCachedTeams()
        assertEquals(1, cached.size)
        assertEquals("team_1", cached[0].id)
        assertEquals("پشتوانه ۱", cached[0].name)
        assertEquals(1, cached[0].members.size)
        assertTrue(storage.getTeamsLastSynced() > 0)
    }

    @Test
    fun cachedMessages_saveLocalAndRemoteMessages() = runBlocking {
        val remoteMsg = TacticalMessage(
            id = "msg_remote",
            senderCallsign = "فرماندهی",
            body = "پیشروی به نقطه ب",
            isUrgent = true,
            createdAt = 1000L,
            isAcknowledged = false,
            isPendingSync = false
        )
        storage.saveMessages(listOf(remoteMsg))

        val localMsg = TacticalMessage(
            id = "msg_local",
            senderCallsign = "شما",
            body = "وضعیت منطقه امن است",
            isUrgent = false,
            createdAt = 2000L,
            isAcknowledged = false,
            isPendingSync = true
        )
        storage.saveLocalMessage(localMsg)

        val cached = storage.getCachedMessages()
        assertEquals(2, cached.size)
        // Ordered by createdAt DESC
        assertEquals("msg_local", cached[0].id)
        assertTrue(cached[0].isPendingSync)
        assertEquals("msg_remote", cached[1].id)
        assertFalse(cached[1].isPendingSync)

        // Acknowledge remote message
        storage.markMessageAcknowledged("msg_remote")
        val updated = storage.getCachedMessages()
        assertTrue(updated.first { it.id == "msg_remote" }.isAcknowledged)
    }

    @Test
    fun cachedStatuses_saveAndRetrieve() = runBlocking {
        val status = TeamLiveStatus(
            teamId = "t1",
            teamName = "تیم واکنش سریع",
            callsign = "عقاب",
            operationalStatus = OperationalStatus.PURSUING,
            connectionStatus = ConnectionStatus.ONLINE,
            lastSeenAt = 5000L,
            isStale = false
        )
        storage.saveStatuses(listOf(status))

        val cached = storage.getCachedStatuses()
        assertEquals(1, cached.size)
        assertEquals(OperationalStatus.PURSUING, cached[0].operationalStatus)
        assertEquals(ConnectionStatus.ONLINE, cached[0].connectionStatus)
    }

    @Test
    fun purgeExpiredData_cleansOldSyncedMessagesOnly() = runBlocking {
        val oldSyncedMsg = TacticalMessage(
            id = "old_synced",
            senderCallsign = "C",
            body = "Old",
            isUrgent = false,
            createdAt = 100L,
            isAcknowledged = true,
            isPendingSync = false
        )
        val oldPendingMsg = TacticalMessage(
            id = "old_pending",
            senderCallsign = "C",
            body = "Old Pending",
            isUrgent = false,
            createdAt = 100L,
            isAcknowledged = false,
            isPendingSync = true
        )
        storage.saveMessages(listOf(oldSyncedMsg))
        storage.saveLocalMessage(oldPendingMsg)

        // Purge with maxAge 1000ms
        storage.purgeExpiredData(1000L)

        val cached = storage.getCachedMessages()
        // oldSyncedMsg should be purged; oldPendingMsg MUST be preserved!
        assertEquals(1, cached.size)
        assertEquals("old_pending", cached[0].id)
    }

    @Test
    fun clearCacheAndQueue_purgesCompletely() = runBlocking {
        storage.enqueueAction(QueuedAction("a1", ActionType.SEND_MESSAGE, "{}", 100L))
        storage.saveTeams(listOf(Team("t1", "T", "l1", "", "", "")))

        assertEquals(1, storage.getPendingActionCount())
        assertEquals(1, storage.getCachedTeams().size)

        storage.clearAll()

        assertEquals(0, storage.getPendingActionCount())
        assertEquals(0, storage.getCachedTeams().size)
    }

    @Test
    fun operationalStatus_saveAndRetrieve() = runBlocking {
        assertEquals(null, storage.getMyOperationalStatus())

        storage.saveMyOperationalStatus(OperationalStatus.PURSUING)
        assertEquals(OperationalStatus.PURSUING, storage.getMyOperationalStatus())

        storage.saveMyOperationalStatus(OperationalStatus.ENGAGED)
        assertEquals(OperationalStatus.ENGAGED, storage.getMyOperationalStatus())
    }
}
