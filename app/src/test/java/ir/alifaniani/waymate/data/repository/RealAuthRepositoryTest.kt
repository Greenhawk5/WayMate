package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.network.AuthApiService
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import ir.alifaniani.waymate.data.session.SessionStorage
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RealAuthRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionStorage: SessionStorage
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var authApiService: AuthApiService
    private lateinit var repository: RealAuthRepository

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        sessionStorage = InMemorySessionStorage()
        testDispatcherProvider = TestDispatcherProvider()

        val retrofit = NetworkClient.createCustomClient(
            baseUrl = mockWebServer.url("/").toString(),
            sessionStorage = sessionStorage
        )
        authApiService = retrofit.create(AuthApiService::class.java)

        repository = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun login_withValidCredentials_persistsSessionAndReturnsSuccess() = runBlocking {
        val jsonResponse = """
            {
              "success": true,
              "data": {
                "token": "eyJhbGciOiJIUzI1NiJ9.demo.token",
                "token_type": "Bearer",
                "expires_at": ${System.currentTimeMillis() + 86400000},
                "user": {
                  "id": "usr_cmd_001",
                  "username": "commander_demo",
                  "role": "COMMANDER",
                  "callsign": "Eagle-1 (فرمانده)",
                  "is_active": true
                }
              },
              "meta": {
                "timestamp": 1729000000000,
                "request_id": "req_123"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.login("commander_demo", "demo123")

        assertTrue("Expected Result.Success", result is Result.Success)
        val user = (result as Result.Success).data
        assertEquals("commander_demo", user.username)
        assertEquals(UserRole.COMMANDER, user.role)
        assertEquals("Eagle-1 (فرمانده)", user.callsign)

        // Verify session persistence
        assertEquals("eyJhbGciOiJIUzI1NiJ9.demo.token", sessionStorage.getToken())
        assertEquals(user, repository.observeCurrentUser().value)
    }

    @Test
    fun login_withInvalidCredentials_returnsUnauthorizedError() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "UNAUTHORIZED",
                "message": "نام کاربری یا گذرواژه اشتباه است",
                "timestamp": 1729000000000,
                "request_id": "req_401"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login("commander_demo", "wrong_password")

        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertEquals("نام کاربری یا گذرواژه اشتباه است", error.message)
        assertNull(sessionStorage.getToken())
        assertNull(repository.observeCurrentUser().value)
    }

    @Test
    fun login_withServerValidationError_returnsValidationMessage() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "VALIDATION_FAILED",
                "message": "نام کاربری و گذرواژه الزامی هستند",
                "details": [
                  { "field": "password", "issue": "گذرواژه الزامی است" }
                ],
                "timestamp": 1729000000000,
                "request_id": "req_val"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.login("user_without_pass", "abc")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals("نام کاربری و گذرواژه الزامی هستند", error.message)
    }

    @Test
    fun login_withUnexpectedServerError_returnsInternalErrorMessage() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val result = repository.login("user", "pass")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.message?.contains("خطای موقت در سرور") == true)
    }

    @Test
    fun login_withNetworkFailure_returnsUserFriendlyNetworkError() = runBlocking {
        // Shutdown server immediately to simulate network unreachable
        mockWebServer.shutdown()

        val result = repository.login("user", "pass")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertNotNull(error.message)
        assertTrue(error.message?.contains("ارتباط با سرور") == true || error.message?.contains("شبکه") == true)
    }

    @Test
    fun restoreSession_onStartup_populatesCurrentUserIfValid() {
        val cachedUser = User("usr_stored", "cached_cmd", UserRole.COMMANDER, "Eagle-Saved")
        sessionStorage.saveSession("token_cached", cachedUser, System.currentTimeMillis() + 3600000)

        // Instantiate new repository to test startup restoration
        val newRepo = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider
        )

        assertEquals(cachedUser, newRepo.observeCurrentUser().value)
    }

    @Test
    fun restoreSession_onStartup_clearsExpiredSession() {
        val cachedUser = User("usr_expired", "expired_user", UserRole.TEAM_LEAD, "Falcon-Old")
        sessionStorage.saveSession("token_expired", cachedUser, System.currentTimeMillis() - 100000)

        val newRepo = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider
        )

        assertNull(newRepo.observeCurrentUser().value)
        assertFalse(sessionStorage.isSessionValid())
    }

    @Test
    fun logout_clearsRemoteSessionAndLocalState() = runBlocking {
        // 1. Setup active session
        val user = User("usr_logout", "user_out", UserRole.TEAM_LEAD, "Alpha-Lead")
        sessionStorage.saveSession("token_active", user, System.currentTimeMillis() + 60000)

        val repo = RealAuthRepository(
            authApiService = authApiService,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcherProvider
        )
        assertEquals(user, repo.observeCurrentUser().value)

        // 2. Enqueue logout response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""{"success":true,"data":{"message":"خروج با موفقیت انجام شد"}}""")
        )

        // 3. Logout
        val logoutResult = repo.logout()

        assertTrue(logoutResult is Result.Success)
        assertNull(sessionStorage.getToken())
        assertNull(repo.observeCurrentUser().value)
    }

    @Test
    fun roleMapping_unsupportedRole_fallsBackSafelyWithoutElevatedPrivileges() = runBlocking {
        val jsonWithUnknownRole = """
            {
              "success": true,
              "data": {
                "token": "token_unknown_role",
                "token_type": "Bearer",
                "expires_at": ${System.currentTimeMillis() + 60000},
                "user": {
                  "id": "usr_hacker",
                  "username": "suspicious_user",
                  "role": "SUPER_ADMIN_INVENTED",
                  "callsign": "Unknown"
                }
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonWithUnknownRole)
        )

        val result = repository.login("suspicious_user", "pass")
        assertTrue(result is Result.Success)
        val user = (result as Result.Success).data
        // Unknown role must NOT be OWNER or COMMANDER; must default safely to TEAM_LEAD
        assertEquals(UserRole.TEAM_LEAD, user.role)
    }
}
