package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.AcknowledgeResponseDto
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.LiveUnitDto
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.dto.TacticalMessageDto
import ir.alifaniani.waymate.data.dto.TeamDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusResponseDto
import ir.alifaniani.waymate.data.dto.UpdateTeamRequestDto
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.network.AuthApiService
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class OfflineRepositoryIntegrationTest {

    private lateinit var storage: InMemoryOfflineStorage
    private val testDispatcher = TestDispatcherProvider()

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    // ==========================================
    // RealMessageRepository Offline Tests
    // ==========================================

    @Test
    fun realMessageRepository_offline_getMessages_returnsCachedMessages() = runBlocking {
        // Pre-seed storage with cached messages
        storage.saveMessages(
            listOf(
                TacticalMessage(
                    id = "msg_cached_1",
                    senderCallsign = "Commander",
                    body = "دستور ذخیره‌شده محلی",
                    isUrgent = false,
                    createdAt = 5000L,
                    isAcknowledged = true
                )
            )
        )

        val failingApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> {
                throw IOException("Network offline")
            }
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> = throw NotImplementedError()
        }

        val repository = RealMessageRepository(
            messageApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.getMessages()
        assertTrue(result is Result.Success)
        val messages = (result as Result.Success).data
        assertEquals(1, messages.size)
        assertEquals("msg_cached_1", messages[0].id)
    }

    @Test
    fun realMessageRepository_offline_sendMessage_enqueuesActionAndReturnsOptimisticMessage() = runBlocking {
        val failingApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw NotImplementedError()
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                throw IOException("Network offline")
            }
        }

        val repository = RealMessageRepository(
            messageApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.sendMessage("گزارش موقعیت در غیاب شبکه", isUrgent = true)
        assertTrue(result is Result.Success)
        val msg = (result as Result.Success).data
        assertEquals("گزارش موقعیت در غیاب شبکه", msg.body)
        assertTrue(msg.isUrgent)
        assertTrue(msg.isPendingSync)

        // Verify action is queued in offline_queue
        assertEquals(1, storage.getPendingActionCount())
        val pending = storage.getPendingActions()
        assertEquals(1, pending.size)
        assertTrue(pending[0].payloadJson.contains("گزارش موقعیت در غیاب شبکه"))

        // Verify message is saved in local cache
        val cached = storage.getCachedMessages()
        assertEquals(1, cached.size)
        assertTrue(cached[0].isPendingSync)
    }

    @Test
    fun realMessageRepository_offline_acknowledgeMessage_queuesAckActionAndUpdatesCache() = runBlocking {
        storage.saveMessages(
            listOf(
                TacticalMessage(
                    id = "msg_to_ack",
                    senderCallsign = "HQ",
                    body = "پیام",
                    isUrgent = false,
                    createdAt = 1000L,
                    isAcknowledged = false
                )
            )
        )

        val failingApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw NotImplementedError()
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> {
                throw IOException("Network offline")
            }
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> = throw NotImplementedError()
        }

        val repository = RealMessageRepository(
            messageApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.acknowledgeMessage("msg_to_ack")
        assertTrue(result is Result.Success)

        // Queued in offline_queue
        assertEquals(1, storage.getPendingActionCount())

        // Locally acknowledged in cache
        val cached = storage.getCachedMessages()
        assertTrue(cached[0].isAcknowledged)
    }

    // ==========================================
    // RealTeamRepository Offline Tests
    // ==========================================

    @Test
    fun realTeamRepository_offline_getTeams_returnsCachedTeams() = runBlocking {
        storage.saveTeams(
            listOf(
                Team("t1", "تیم الف", "lead1", "هایلوکس", "11", "بیسیم")
            )
        )

        val failingApi = object : TeamApiService {
            override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> {
                throw IOException("Network offline")
            }
            override suspend fun getTeamById(id: String): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun updateTeam(id: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
        }

        val repository = RealTeamRepository(
            teamApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.getTeams()
        assertTrue(result is Result.Success)
        val teams = (result as Result.Success).data
        assertEquals(1, teams.size)
        assertEquals("تیم الف", teams[0].name)
    }

    @Test
    fun realTeamRepository_offline_createTeam_enqueuesActionAndReturnsOptimisticTeam() = runBlocking {
        val failingApi = object : TeamApiService {
            override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> = throw NotImplementedError()
            override suspend fun getTeamById(id: String): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> {
                throw IOException("Network offline")
            }
            override suspend fun updateTeam(id: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
        }

        val repository = RealTeamRepository(
            teamApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.createTeam(
            name = "تیم واکنش اضطراری",
            leadUserId = "usr_lead",
            vehicleType = "موتورسیکلت",
            vehiclePlate = "99",
            equipmentSummary = "کیت امداد",
            members = emptyList()
        )

        assertTrue(result is Result.Success)
        val team = (result as Result.Success).data
        assertEquals("تیم واکنش اضطراری", team.name)
        assertEquals(1, storage.getPendingActionCount())
    }

    // ==========================================
    // RealStatusRepository Offline Tests
    // ==========================================

    @Test
    fun realStatusRepository_offline_updateOperationalStatus_enqueuesAction() = runBlocking {
        val failingApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> = throw NotImplementedError()
            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> {
                throw IOException("Network offline")
            }
        }

        val repository = RealStatusRepository(
            statusApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.updateOperationalStatus(OperationalStatus.PURSUING)
        assertTrue(result is Result.Success)
        assertEquals(OperationalStatus.PURSUING, (result as Result.Success).data)

        // Action queued in offline storage
        assertEquals(1, storage.getPendingActionCount())
        val pending = storage.getPendingActions()
        assertTrue(pending[0].payloadJson.contains("PURSUING"))
    }

    @Test
    fun realStatusRepository_offline_getLiveStatuses_returnsCached() = runBlocking {
        storage.saveStatuses(
            listOf(
                TeamLiveStatus(
                    teamId = "t1",
                    teamName = "تیم ۱",
                    callsign = "شاهین",
                    operationalStatus = OperationalStatus.NORMAL,
                    connectionStatus = ConnectionStatus.ONLINE,
                    lastSeenAt = 1000L,
                    isStale = false
                )
            )
        )

        val failingApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> {
                throw IOException("Network offline")
            }
            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> = throw NotImplementedError()
        }

        val repository = RealStatusRepository(
            statusApiService = failingApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        val result = repository.getLiveTeamStatuses()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals("شاهین", list[0].callsign)
    }

    // ==========================================
    // RealAuthRepository Logout Cache Purge Test
    // ==========================================

    @Test
    fun realAuthRepository_logout_purgesOfflineCache() = runBlocking {
        storage.saveTeams(listOf(Team("t1", "T", "l1", "", "", "")))
        assertEquals(1, storage.getCachedTeams().size)

        val sessionStorage = InMemorySessionStorage()
        sessionStorage.saveSession("token", User("u1", "user", UserRole.TEAM_LEAD, "Call"), 999999L)

        val dummyAuthApi = object : AuthApiService {
            override suspend fun login(request: ir.alifaniani.waymate.data.dto.LoginRequestDto): Response<ApiResponseDto<ir.alifaniani.waymate.data.dto.LoginResponseDto>> = throw NotImplementedError()
            override suspend fun logout(): Response<ApiResponseDto<ir.alifaniani.waymate.data.dto.LogoutResponseDto>> {
                return Response.success(ApiResponseDto(success = true, data = ir.alifaniani.waymate.data.dto.LogoutResponseDto(message = "موفق")))
            }
            override suspend fun getProfile(): Response<ApiResponseDto<ir.alifaniani.waymate.data.dto.UserDto>> = throw NotImplementedError()
        }

        val authRepo = RealAuthRepository(
            authApiService = dummyAuthApi,
            sessionStorage = sessionStorage,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        authRepo.logout()

        assertFalse(sessionStorage.isSessionValid())
        assertEquals(0, storage.getCachedTeams().size)
    }
}
