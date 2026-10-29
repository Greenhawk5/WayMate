package ir.alifaniani.waymate.ui.home

import ir.alifaniani.waymate.TestDispatcherProvider
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
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.data.repository.RealMessageRepository
import ir.alifaniani.waymate.data.repository.RealTeamRepository
import ir.alifaniani.waymate.data.sync.OfflineSyncManager
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class HomeViewModelOfflineTest {

    private lateinit var storage: InMemoryOfflineStorage
    private val testDispatcher = TestDispatcherProvider()

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    @Test
    fun processRestartOffline_loadsCachedDataIntoUiState() = runBlocking {
        // Pre-populate offline cache simulating data saved from earlier session
        storage.saveTeams(
            listOf(
                Team("t1", "پشتوانه عملیاتی", "lead1", "هایلوکس", "11", "بیسیم")
            )
        )
        storage.saveMessages(
            listOf(
                TacticalMessage("m1", "فرماندهی", "دستور آماده‌باش", false, 1000L, true)
            )
        )

        // API calls throw IOException (device offline)
        val offlineTeamApi = object : TeamApiService {
            override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> = throw IOException("Offline")
            override suspend fun getTeamById(teamId: String): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun updateTeam(teamId: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
        }

        val offlineMessageApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw IOException("Offline")
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> = throw NotImplementedError()
        }

        val teamRepo = RealTeamRepository(offlineTeamApi, testDispatcher, storage)
        val messageRepo = RealMessageRepository(offlineMessageApi, testDispatcher, storage)

        val viewModel = HomeViewModel(
            teamRepository = teamRepo,
            messageRepository = messageRepo,
            dispatcherProvider = testDispatcher
        )
        viewModel.stopPolling()

        val state = viewModel.uiState.value
        // Verify cached data is displayed despite network outage!
        assertEquals(1, state.teams.size)
        assertEquals("پشتوانه عملیاتی", state.teams[0].name)
        assertEquals(1, state.messages.size)
        assertEquals("دستور آماده‌باش", state.messages[0].body)
    }

    @Test
    fun offlineActions_incrementsOfflineQueueCountAndSyncsOnDemand() = runBlocking {
        var isNetworkAvailable = false

        val controllableMessageApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> {
                if (!isNetworkAvailable) throw IOException("Offline")
                return Response.success(ApiResponseDto(success = true, data = emptyList()))
            }
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                if (!isNetworkAvailable) throw IOException("Offline")
                return Response.success(
                    ApiResponseDto(
                        success = true,
                        data = TacticalMessageDto(
                            id = "msg_srv",
                            senderCallsign = "شما",
                            body = request.body,
                            isUrgent = request.isUrgent,
                            createdAt = 2000L,
                            acknowledged = false
                        )
                    )
                )
            }
        }

        val dummyTeamApi = object : TeamApiService {
            override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> {
                if (!isNetworkAvailable) throw IOException("Offline")
                return Response.success(ApiResponseDto(success = true, data = emptyList()))
            }
            override suspend fun getTeamById(teamId: String): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
            override suspend fun updateTeam(teamId: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
        }

        val dummyStatusApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> = Response.success(ApiResponseDto(success = true, data = emptyList()))
            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> = throw NotImplementedError()
        }

        val syncManager = OfflineSyncManager(
            offlineStorage = storage,
            messageApiService = controllableMessageApi,
            statusApiService = dummyStatusApi,
            teamApiService = dummyTeamApi,
            dispatcherProvider = testDispatcher
        )

        val teamRepo = RealTeamRepository(dummyTeamApi, testDispatcher, storage)
        val messageRepo = RealMessageRepository(controllableMessageApi, testDispatcher, storage)

        val viewModel = HomeViewModel(
            teamRepository = teamRepo,
            messageRepository = messageRepo,
            offlineSyncManager = syncManager,
            dispatcherProvider = testDispatcher
        )
        viewModel.stopPolling()

        assertEquals(0, viewModel.uiState.value.offlineQueueCount)

        // 1. Send message while offline
        viewModel.sendTacticalMessage("پیام ارسال‌شده در حالت قطع شبکه", isUrgent = false, targetTeamId = null)

        // Message should be added locally with isPendingSync = true
        val stateAfterSend = viewModel.uiState.value
        assertEquals(1, stateAfterSend.messages.size)
        assertTrue(stateAfterSend.messages[0].isPendingSync)
        assertEquals(1, stateAfterSend.offlineQueueCount)

        // 2. Restore connectivity and sync
        isNetworkAvailable = true
        viewModel.syncOfflineQueue()

        val stateAfterSync = viewModel.uiState.value
        assertEquals(0, stateAfterSync.offlineQueueCount)
        assertFalse(stateAfterSync.isSyncing)
        assertTrue(stateAfterSync.syncFeedbackMessage?.contains("با موفقیت") == true)
    }
}
