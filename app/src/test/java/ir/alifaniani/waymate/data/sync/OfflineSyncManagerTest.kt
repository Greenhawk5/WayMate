package ir.alifaniani.waymate.data.sync

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
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueueStatus
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.network.MessageApiService
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.data.network.TeamApiService
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class OfflineSyncManagerTest {

    private lateinit var storage: InMemoryOfflineStorage
    private val testDispatcher = TestDispatcherProvider()

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    private val dummyTeamApi = object : TeamApiService {
        override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> = throw NotImplementedError()
        override suspend fun getTeamById(id: String): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
        override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> {
            return Response.success(ApiResponseDto(success = true, data = TeamDto(id = "team_new", name = request.name)))
        }
        override suspend fun updateTeam(id: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> = throw NotImplementedError()
    }

    private val dummyStatusApi = object : StatusApiService {
        override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> = throw NotImplementedError()
        override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> {
            return Response.success(
                ApiResponseDto(
                    success = true,
                    data = UpdateOperationalStatusResponseDto(
                        teamId = "t1",
                        operationalStatus = request.operationalStatus,
                        updatedAt = 1000L
                    )
                )
            )
        }
    }

    private val dummyMessageApi = object : MessageApiService {
        override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw NotImplementedError()
        override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> {
            return Response.success(ApiResponseDto(success = true, data = AcknowledgeResponseDto(acknowledgedAt = 2000L)))
        }
        override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
            return Response.success(
                ApiResponseDto(
                    success = true,
                    data = TacticalMessageDto(
                        id = "msg_srv_1",
                        senderCallsign = "Sender",
                        body = request.body,
                        isUrgent = request.isUrgent,
                        createdAt = 3000L,
                        acknowledged = false
                    )
                )
            )
        }
    }

    @Test
    fun drainQueue_successfulExecution_clearsQueueAndIncrementsSyncedCount() = runBlocking {
        val manager = OfflineSyncManager(
            offlineStorage = storage,
            messageApiService = dummyMessageApi,
            statusApiService = dummyStatusApi,
            teamApiService = dummyTeamApi,
            dispatcherProvider = testDispatcher
        )

        storage.enqueueAction(
            QueuedAction(
                id = "act_1",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = "{\"body\":\"دستور الف\",\"is_urgent\":false}",
                createdAt = 1000L
            )
        )
        storage.enqueueAction(
            QueuedAction(
                id = "act_2",
                actionType = ActionType.UPDATE_STATUS,
                payloadJson = "{\"operational_status\":\"PURSUING\"}",
                createdAt = 2000L
            )
        )

        assertEquals(2, storage.getPendingActionCount())

        val result = manager.drainQueue()

        assertEquals(2, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, result.remainingCount)
        assertEquals(0, storage.getPendingActionCount())
        assertFalse(manager.isSyncing.value)
    }

    @Test
    fun drainQueue_networkError_keepsActionPendingAndIncrementsRetry() = runBlocking {
        val failingMessageApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw NotImplementedError()
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                throw IOException("عدم دسترسی به شبکه")
            }
        }

        val manager = OfflineSyncManager(
            offlineStorage = storage,
            messageApiService = failingMessageApi,
            statusApiService = dummyStatusApi,
            teamApiService = dummyTeamApi,
            dispatcherProvider = testDispatcher
        )

        storage.enqueueAction(
            QueuedAction(
                id = "act_net_fail",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = "{\"body\":\"پیام تست\",\"is_urgent\":false}",
                createdAt = 1000L,
                retryCount = 0
            )
        )

        val result = manager.drainQueue()

        assertEquals(0, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(1, result.remainingCount)

        val pending = storage.getPendingActions()
        assertEquals(1, pending.size)
        assertEquals("act_net_fail", pending[0].id)
        assertEquals(1, pending[0].retryCount)
        assertEquals(QueueStatus.PENDING, pending[0].status)
    }

    @Test
    fun drainQueue_clientValidationError4xx_marksActionFailed() = runBlocking {
        val error400MessageApi = object : MessageApiService {
            override suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>> = throw NotImplementedError()
            override suspend fun acknowledgeMessage(id: String): Response<ApiResponseDto<AcknowledgeResponseDto>> = throw NotImplementedError()
            override suspend fun sendMessage(request: SendMessageRequestDto): Response<ApiResponseDto<TacticalMessageDto>> {
                val errorBody = "{\"success\":false,\"error\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"متن پیام خالی است\"}}"
                    .toResponseBody("application/json".toMediaType())
                return Response.error(400, errorBody)
            }
        }

        val manager = OfflineSyncManager(
            offlineStorage = storage,
            messageApiService = error400MessageApi,
            statusApiService = dummyStatusApi,
            teamApiService = dummyTeamApi,
            dispatcherProvider = testDispatcher
        )

        storage.enqueueAction(
            QueuedAction(
                id = "act_val_fail",
                actionType = ActionType.SEND_MESSAGE,
                payloadJson = "{\"body\":\"\",\"is_urgent\":false}",
                createdAt = 1000L
            )
        )

        val result = manager.drainQueue()

        assertEquals(0, result.syncedCount)
        assertEquals(1, result.failedCount)
        assertEquals(0, result.remainingCount) // FAILED actions are not pending
    }

    @Test
    fun drainQueue_createTeamAndAckMessage() = runBlocking {
        val manager = OfflineSyncManager(
            offlineStorage = storage,
            messageApiService = dummyMessageApi,
            statusApiService = dummyStatusApi,
            teamApiService = dummyTeamApi,
            dispatcherProvider = testDispatcher
        )

        storage.enqueueAction(
            QueuedAction(
                id = "act_team",
                actionType = ActionType.CREATE_TEAM,
                payloadJson = "{\"name\":\"گروه صابرین\",\"lead_user_id\":\"u1\",\"vehicle_type\":\"وانت\",\"vehicle_plate\":\"11\",\"equipment_summary\":\"بیسیم\"}",
                createdAt = 1000L
            )
        )
        storage.enqueueAction(
            QueuedAction(
                id = "act_ack",
                actionType = ActionType.ACK_MESSAGE,
                payloadJson = "msg_123",
                createdAt = 2000L
            )
        )

        val result = manager.drainQueue()
        assertEquals(2, result.syncedCount)
        assertEquals(0, result.remainingCount)
    }
}
