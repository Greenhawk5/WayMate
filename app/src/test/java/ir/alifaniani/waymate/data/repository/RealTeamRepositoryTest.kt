package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.core.network.ApiException
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import ir.alifaniani.waymate.data.session.SessionStorage
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class RealTeamRepositoryTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var sessionStorage: SessionStorage
    private lateinit var testDispatcherProvider: TestDispatcherProvider
    private lateinit var teamApiService: TeamApiService
    private lateinit var repository: RealTeamRepository

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
        teamApiService = retrofit.create(TeamApiService::class.java)

        repository = RealTeamRepository(
            teamApiService = teamApiService,
            dispatcherProvider = testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getTeams_success_returnsTeamListWithDetails() = runBlocking {
        val listJsonResponse = """
            {
              "success": true,
              "data": [
                {
                  "id": "team_alpha_001",
                  "name": "گشت آلفا (واکنش سریع)",
                  "lead_user_id": "usr_lead_001",
                  "vehicle_type": "تویوتا هایلوکس",
                  "vehicle_plate": "۴۴-ب-۱۲",
                  "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
                  "member_count": 2,
                  "is_active": true
                }
              ],
              "pagination": { "offset": 0, "limit": 50, "total": 1, "has_more": false },
              "meta": { "timestamp": 1729000000000, "request_id": "req_123" }
            }
        """.trimIndent()

        val detailJsonResponse = """
            {
              "success": true,
              "data": {
                "id": "team_alpha_001",
                "name": "گشت آلفا (واکنش سریع)",
                "lead_user_id": "usr_lead_001",
                "vehicle_type": "تویوتا هایلوکس",
                "vehicle_plate": "۴۴-ب-۱۲",
                "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
                "is_active": true,
                "members": [
                  {
                    "id": "mem_001",
                    "full_name": "رضا کریمی",
                    "callsign": "البرز ۱",
                    "role_in_team": "راننده و اپراتور فنی"
                  },
                  {
                    "id": "mem_002",
                    "full_name": "محمد حسینی",
                    "callsign": "سهند ۲",
                    "role_in_team": "دیده‌بان و سرتیم"
                  }
                ]
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(listJsonResponse)
        )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(detailJsonResponse)
        )

        val result = repository.getTeams(fetchDetails = true)

        assertTrue("Expected Result.Success", result is Result.Success)
        val teams = (result as Result.Success).data
        assertEquals(1, teams.size)
        val team = teams[0]
        assertEquals("team_alpha_001", team.id)
        assertEquals("گشت آلفا (واکنش سریع)", team.name)
        assertEquals("تویوتا هایلوکس", team.vehicleType)
        assertEquals(2, team.members.size)
        assertEquals("البرز ۱", team.members[0].callsign)
        assertEquals("محمد حسینی", team.members[1].fullName)

        val request = mockWebServer.takeRequest(5, TimeUnit.SECONDS)
        assertNotNull(request)
        assertEquals("/teams?offset=0&limit=50", request?.path)
    }

    @Test
    fun getTeams_emptyList_returnsEmptySuccess() = runBlocking {
        val jsonResponse = """
            {
              "success": true,
              "data": [],
              "pagination": { "offset": 0, "limit": 50, "total": 0, "has_more": false },
              "meta": { "timestamp": 1729000000000, "request_id": "req_124" }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse)
        )

        val result = repository.getTeams()

        assertTrue(result is Result.Success)
        val teams = (result as Result.Success).data
        assertTrue("Expected empty teams list", teams.isEmpty())
    }

    @Test
    fun getTeams_http401Unauthorized_returnsApiExceptionUnauthorized() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "UNAUTHORIZED",
                "message": "نشست کاربری نامعتبر است یا توکن ارسال نشده است",
                "timestamp": 1729000000000,
                "request_id": "req_err_401"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.getTeams()

        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Expected ApiException.Unauthorized", error.throwable is ApiException.Unauthorized)
        assertEquals("نشست کاربری نامعتبر است یا توکن ارسال نشده است", error.message)
    }

    @Test
    fun getTeams_http403Forbidden_returnsApiExceptionForbidden() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "FORBIDDEN",
                "message": "دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد",
                "timestamp": 1729000000000,
                "request_id": "req_err_403"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(403)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.getTeams()

        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Expected ApiException.Forbidden", error.throwable is ApiException.Forbidden)
        assertEquals("دسترسی به این عملیات برای نقش شما مجاز نمی‌باشد", error.message)
    }

    @Test
    fun getTeams_http500ServerError_returnsApiExceptionServerError() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "INTERNAL_ERROR",
                "message": "اتصال به پایگاه داده در دسترس نیست",
                "timestamp": 1729000000000,
                "request_id": "req_err_500"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.getTeams()

        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Expected ApiException.ServerError", error.throwable is ApiException.ServerError)
        assertEquals("اتصال به پایگاه داده در دسترس نیست", error.message)
    }

    @Test
    fun getTeams_networkTimeout_returnsNetworkUnavailable() = runBlocking {
        mockWebServer.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        )

        val result = repository.getTeams()

        assertTrue("Expected Result.Error", result is Result.Error)
        val error = result as Result.Error
        assertTrue("Expected ApiException.NetworkUnavailable", error.throwable is ApiException.NetworkUnavailable)
    }

    @Test
    fun getTeamDetails_success_returnsTeamWithMembers() = runBlocking {
        val detailJsonResponse = """
            {
              "success": true,
              "data": {
                "id": "team_alpha_001",
                "name": "گشت آلفا (واکنش سریع)",
                "lead_user_id": "usr_lead_001",
                "vehicle_type": "تویوتا هایلوکس",
                "vehicle_plate": "۴۴-ب-۱۲",
                "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
                "is_active": true,
                "members": [
                  {
                    "id": "mem_001",
                    "full_name": "رضا کریمی",
                    "callsign": "البرز ۱",
                    "role_in_team": "راننده و اپراتور فنی"
                  }
                ]
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(detailJsonResponse)
        )

        val result = repository.getTeamDetails("team_alpha_001")

        assertTrue(result is Result.Success)
        val team = (result as Result.Success).data
        assertEquals("team_alpha_001", team.id)
        assertEquals("گشت آلفا (واکنش سریع)", team.name)
        assertEquals(1, team.members.size)
        assertEquals("رضا کریمی", team.members[0].fullName)
    }

    @Test
    fun getTeamDetails_notFound_returnsApiExceptionNotFound() = runBlocking {
        val errorJson = """
            {
              "success": false,
              "error": {
                "code": "NOT_FOUND",
                "message": "تیم عملیاتی با شناسه 'non_existent' یافت نشد",
                "timestamp": 1729000000000,
                "request_id": "req_err_404"
              }
            }
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setHeader("Content-Type", "application/json")
                .setBody(errorJson)
        )

        val result = repository.getTeamDetails("non_existent")

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertTrue(error.throwable is ApiException.NotFound)
        assertEquals("تیم عملیاتی با شناسه 'non_existent' یافت نشد", error.message)
    }
}
