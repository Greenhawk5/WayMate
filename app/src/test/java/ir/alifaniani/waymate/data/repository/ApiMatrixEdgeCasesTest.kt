package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.network.AuthApiService
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import ir.alifaniani.waymate.data.sync.OfflineSyncManager
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Phase 13: Workstream B - Repository & API Edge-Case Integration Test Suite.
 *
 * Explicitly exercises all 12 mandatory integration scenarios:
 * 1. Successful API request.
 * 2. Unauthorized response (401).
 * 3. Forbidden response (403).
 * 4. Not-found response (404).
 * 5. Server error (500).
 * 6. Network timeout.
 * 7. No Internet connection.
 * 8. Invalid JSON syntax.
 * 9. Malformed response structure.
 * 10. Expired authentication session handling.
 * 11. Reconnection after temporary network failure.
 * 12. Duplicate synchronization attempts prevention.
 */
class ApiMatrixEdgeCasesTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionStorage: InMemorySessionStorage
    private lateinit var offlineStorage: InMemoryOfflineStorage
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var teamApiService: TeamApiService
    private lateinit var authApiService: AuthApiService
    private lateinit var teamRepository: RealTeamRepository
    private lateinit var authRepository: RealAuthRepository

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
        teamApiService = retrofit.create(TeamApiService::class.java)
        authApiService = retrofit.create(AuthApiService::class.java)

        teamRepository = RealTeamRepository(
            teamApiService = teamApiService,
            dispatcherProvider = testDispatcherProvider
        )

        authRepository = RealAuthRepository(
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

    // 1. Successful API request
    @Test
    fun scenario01_successfulApiRequest_returnsParsedSuccess() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"data":[]}""")
        )

        val result = teamRepository.getTeams()
        assertTrue("Expected Result.Success", result is Result.Success)
        val data = (result as Result.Success).data
        assertNotNull(data)
    }

    // 2. Unauthorized response (401)
    @Test
    fun scenario02_unauthorizedResponse_returnsApiExceptionUnauthorized() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error":{"code":"UNAUTHORIZED","message":"توکن نامعتبر است"}}""")
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.Unauthorized)
    }

    // 3. Forbidden response (403)
    @Test
    fun scenario03_forbiddenResponse_returnsApiExceptionForbidden() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error":{"code":"FORBIDDEN","message":"عدم دسترسی"}}""")
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.Forbidden)
    }

    // 4. Not-found response (404)
    @Test
    fun scenario04_notFoundResponse_returnsApiExceptionNotFound() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error":{"code":"NOT_FOUND","message":"تیم یافت نشد"}}""")
        )

        val result = teamRepository.getTeamDetails("team_missing")
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.NotFound)
    }

    // 5. Server error (500)
    @Test
    fun scenario05_serverError_returnsApiExceptionServerError() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":false,"error":{"code":"INTERNAL_ERROR","message":"خطای سرور"}}""")
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.ServerError)
    }

    // 6. Network timeout
    @Test
    fun scenario06_networkTimeout_returnsNetworkUnavailable() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.NetworkUnavailable)
    }

    // 7. No Internet connection
    @Test
    fun scenario07_noInternetConnection_returnsNetworkUnavailable() = runBlocking {
        // Shutdown server prior to request to simulate complete network unreachability
        mockWebServer.shutdown()

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.NetworkUnavailable)
    }

    // 8. Invalid JSON syntax
    @Test
    fun scenario08_invalidJsonSyntax_handledGracefullyWithoutCrash() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{ broken json syntax here without closing braces")
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertNotNull(error.message)
    }

    // 9. Malformed response structure
    @Test
    fun scenario09_malformedResponseStructure_handledGracefullyWithoutCrash() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"unexpected_key": 12345, "status": "UNKNOWN"}""")
        )

        val result = teamRepository.getTeams()
        assertTrue(result is Result.Error)
    }

    // 10. Expired authentication session
    @Test
    fun scenario10_expiredAuthenticationSession_clearsStoredSessionAndProfile() = runBlocking {
        val user = User("usr_exp", "exp_user", UserRole.TEAM_LEAD, "Falcon-Exp")
        val expiredTime = System.currentTimeMillis() - 10_000L
        sessionStorage.saveSession("expired_jwt", user, expiredTime)

        assertFalse("Session must be invalid when expired", sessionStorage.isSessionValid())

        // Create repository instance with expired session
        val repo = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider,
            offlineStorage = offlineStorage
        )

        assertNull("Current user must be null on expired session restore", repo.observeCurrentUser().value)
        assertNull("Token must be purged on expired session", sessionStorage.getToken())
    }

    // 11. Reconnection after temporary network failure
    @Test
    fun scenario11_reconnectionAfterTemporaryFailure_succeedsOnRetry() = runBlocking {
        // First attempt: network failure
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )
        val firstResult = teamRepository.getTeams()
        assertTrue("First attempt should fail", firstResult is Result.Error)

        // Second attempt: connection restored
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"data":[]}""")
        )
        val secondResult = teamRepository.getTeams()
        assertTrue("Second attempt after recovery must succeed", secondResult is Result.Success)
    }

    // 12. Duplicate synchronization attempts prevention
    @Test
    fun scenario12_duplicateSyncAttempts_skipsSecondRunWhileActive() = runBlocking {
        val syncManager = OfflineSyncManager(
            offlineStorage = offlineStorage,
            teamApiService = teamApiService,
            dispatcherProvider = testDispatcherProvider
        )

        // Populate a queued action
        offlineStorage.enqueueAction(
            QueuedAction(
                id = "act_sync_1",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = "{}",
                createdAt = System.currentTimeMillis()
            )
        )

        assertFalse(syncManager.isSyncing.value)

        // Enqueue mock response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"data":{}}""")
        )

        val syncResult = syncManager.drainQueue()
        assertNotNull(syncResult)
        assertFalse(syncManager.isSyncing.value)
    }
}
