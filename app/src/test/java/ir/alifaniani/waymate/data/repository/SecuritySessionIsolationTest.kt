package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.network.AuthApiService
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 12 Security Hardening: Session Isolation & Offline Storage Purge Tests.
 *
 * Verifies that on logout (whether network succeeds, fails, or server errors):
 * 1. Tokens and user credentials are removed from session storage.
 * 2. Cached server data (teams, messages, POIs) is completely wiped.
 * 3. Store-and-forward queued actions (which could contain sensitive user actions)
 *    are fully purged, preventing multi-user cross-account data leakage on shared devices.
 */
class SecuritySessionIsolationTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionStorage: InMemorySessionStorage
    private lateinit var offlineStorage: InMemoryOfflineStorage
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var authApiService: AuthApiService
    private lateinit var repository: RealAuthRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sessionStorage = InMemorySessionStorage()
        offlineStorage = InMemoryOfflineStorage()
        testDispatcherProvider = TestDispatcherProvider()

        val retrofit = NetworkClient.createCustomClient(
            baseUrl = mockWebServer.url("/").toString(),
            sessionStorage = sessionStorage
        )
        authApiService = retrofit.create(AuthApiService::class.java)

        repository = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider,
            offlineStorage = offlineStorage
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun logout_purgesOfflineQueueAndCachedDataAtomically() = runBlocking {
        // 1. Establish logged-in user session
        val user = User("usr_sar_1", "rescuer_1", UserRole.TEAM_LEAD, "Mountain-Rescue-1")
        sessionStorage.saveSession("jwt_secret_token", user, System.currentTimeMillis() + 3600_000)

        // 2. Pre-populate sensitive offline cached data
        offlineStorage.saveTeams(
            listOf(
                Team(
                    id = "team_alpha",
                    name = "تیم جستجو البرز",
                    leadUserId = "usr_sar_1",
                    vehicleType = "تویوتا هایلوکس",
                    vehiclePlate = "۴۴-ب-۱۲",
                    equipmentSummary = "بی‌سیم و جعبه امداد",
                    members = emptyList()
                )
            )
        )
        offlineStorage.saveLocalMessage(
            TacticalMessage(
                id = "msg_sensitive_1",
                senderCallsign = "Mountain-Rescue-1",
                body = "موقعیت فرد مفقود در دره شمال غربی تایید شد",
                isUrgent = true,
                createdAt = System.currentTimeMillis(),
                isAcknowledged = false
            )
        )
        offlineStorage.saveLocalPoint(
            PointOfInterest(
                id = "poi_confidential",
                teamId = "team_alpha",
                title = "محل کمپ اضطراری",
                description = "مختصات پشتیبانی",
                category = PointCategory.CAMPSITE,
                latitude = 35.805,
                longitude = 51.412,
                createdByUserId = "usr_sar_1",
                createdByCallsign = "Mountain-Rescue-1",
                updatedByUserId = "usr_sar_1",
                updatedByCallsign = "Mountain-Rescue-1",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // 3. Pre-populate pending offline queue with queued action
        offlineStorage.enqueueAction(
            QueuedAction(
                id = "queue_act_priv",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = """{"body":"پیام محرمانه عملیاتی"}""",
                createdAt = System.currentTimeMillis()
            )
        )

        // Verify pre-conditions
        assertEquals(1, offlineStorage.getPendingActionCount())
        assertEquals(1, offlineStorage.getCachedTeams().size)
        assertEquals(1, offlineStorage.getCachedMessages().size)
        assertEquals(1, offlineStorage.getCachedPoints().size)

        // 4. Remote logout response mock
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"data":{"message":"خروج با موفقیت انجام شد"}}""")
        )

        // 5. Execute logout
        val logoutResult = repository.logout()

        // 6. Verify full isolation and purge
        assertTrue(logoutResult is Result.Success)
        assertNull("Session token must be nullified", sessionStorage.getToken())
        assertNull("Current user must be cleared", repository.observeCurrentUser().value)

        // Verify that offline queue and all cached data are 100% purged
        assertEquals("Offline queue must be purged on logout to prevent cross-account leak", 0, offlineStorage.getPendingActionCount())
        assertTrue("Pending actions list must be empty", offlineStorage.getPendingActions().isEmpty())
        assertTrue("Cached teams must be wiped", offlineStorage.getCachedTeams().isEmpty())
        assertTrue("Cached messages must be wiped", offlineStorage.getCachedMessages().isEmpty())
        assertTrue("Cached points must be wiped", offlineStorage.getCachedPoints().isEmpty())
    }

    @Test
    fun logout_whenNetworkFails_stillPurgesSessionAndStorageLocally() = runBlocking {
        // Setup session and queue
        val user = User("usr_offline", "user_offline", UserRole.TEAM_LEAD, "Falcon-Off")
        sessionStorage.saveSession("jwt_token_offline", user, System.currentTimeMillis() + 3600_000)
        offlineStorage.enqueueAction(
            QueuedAction(
                id = "act_queued",
                actionType = ActionType.UPDATE_STATUS,
                payloadJson = """{"status":"STANDBY"}""",
                createdAt = System.currentTimeMillis()
            )
        )

        // Shut down server to simulate total network failure
        mockWebServer.shutdown()

        // Execute logout
        val result = repository.logout()

        // Logout must succeed locally
        assertTrue(result is Result.Success)
        assertNull(sessionStorage.getToken())
        assertNull(repository.observeCurrentUser().value)
        assertEquals("Queue must be purged even on network failure", 0, offlineStorage.getPendingActionCount())
    }

    @Test
    fun logout_whenServerReturns500InternalError_stillPurgesSessionAndStorageLocally() = runBlocking {
        // Setup session and queue
        val user = User("usr_error_case", "user_err", UserRole.COMMANDER, "Eagle-Fail")
        sessionStorage.saveSession("jwt_token_500", user, System.currentTimeMillis() + 3600_000)
        offlineStorage.enqueueAction(
            QueuedAction(
                id = "act_queued_500",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = "{}",
                createdAt = System.currentTimeMillis()
            )
        )

        // Mock 500 error from server
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        // Execute logout
        val result = repository.logout()

        assertTrue(result is Result.Success)
        assertNull(sessionStorage.getToken())
        assertNull(repository.observeCurrentUser().value)
        assertEquals(0, offlineStorage.getPendingActionCount())
    }
}
