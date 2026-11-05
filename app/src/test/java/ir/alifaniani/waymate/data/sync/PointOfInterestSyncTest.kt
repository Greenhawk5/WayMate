package ir.alifaniani.waymate.data.sync

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreatePointRequestDto
import ir.alifaniani.waymate.data.dto.DeletePointResponseDto
import ir.alifaniani.waymate.data.dto.PointOfInterestDto
import ir.alifaniani.waymate.data.dto.UpdatePointRequestDto
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.local.model.QueuedAction
import ir.alifaniani.waymate.data.network.PointOfInterestApiService
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class PointOfInterestSyncTest {

    private lateinit var storage: InMemoryOfflineStorage
    private val testDispatcher = TestDispatcherProvider()
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    @Test
    fun drainQueue_withCreatePointAction_callsApiAndClearsQueue() = runBlocking {
        var createCalled = false

        val mockPointApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()

            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()

            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> {
                createCalled = true
                assertEquals("team_alpha_001", request.teamId)
                assertEquals("چشمه کوهستان", request.title)
                return Response.success(
                    ApiResponseDto(
                        success = true,
                        data = PointOfInterestDto(
                            id = "poi_srv_1",
                            teamId = request.teamId ?: "team_alpha_001",
                            title = request.title,
                            category = request.category,
                            description = request.description,
                            latitude = request.latitude,
                            longitude = request.longitude,
                            createdByUserId = "usr_01",
                            createdByCallsign = "راهنما",
                            updatedByUserId = "usr_01",
                            updatedByCallsign = "راهنما",
                            createdAt = 1000L,
                            updatedAt = 1000L,
                            version = 1
                        )
                    )
                )
            }

            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val manager = OfflineSyncManager(
            offlineStorage = storage,
            pointApiService = mockPointApi,
            dispatcherProvider = testDispatcher
        )

        val createPayload = CreatePointQueuePayload(
            localId = "local_poi_1",
            title = "چشمه کوهستان",
            description = "چشمه طبیعی",
            category = "WATER_SOURCE",
            latitude = 28.51,
            longitude = 53.56,
            teamId = "team_alpha_001"
        )

        storage.enqueueAction(
            QueuedAction(
                id = "action_poi_create_1",
                actionType = ActionType.CREATE_POINT,
                payloadJson = json.encodeToString(createPayload),
                createdAt = 1000L
            )
        )

        val result = manager.drainQueue()
        assertTrue(createCalled)
        assertEquals(1, result.syncedCount)
        assertEquals(0, result.failedCount)
        assertEquals(0, storage.getPendingActions().size)
    }

    @Test
    fun drainQueue_withUpdatePointAction_callsApiAndClearsQueue() = runBlocking {
        var updateCalled = false

        val mockPointApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()

            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()

            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> {
                updateCalled = true
                assertEquals("poi_srv_1", id)
                assertEquals("نام جدید", request.title)
                return Response.success(
                    ApiResponseDto(
                        success = true,
                        data = PointOfInterestDto(
                            id = id,
                            teamId = "team_alpha_001",
                            title = request.title ?: "",
                            category = request.category ?: "GENERAL",
                            description = request.description ?: "",
                            latitude = request.latitude ?: 28.5,
                            longitude = request.longitude ?: 53.5,
                            createdByUserId = "usr_01",
                            createdByCallsign = "راهنما",
                            updatedByUserId = "usr_02",
                            updatedByCallsign = "امدادگر",
                            createdAt = 1000L,
                            updatedAt = 2000L,
                            version = 2
                        )
                    )
                )
            }

            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val manager = OfflineSyncManager(
            offlineStorage = storage,
            pointApiService = mockPointApi,
            dispatcherProvider = testDispatcher
        )

        val updatePayload = UpdatePointQueuePayload(
            id = "poi_srv_1",
            title = "نام جدید",
            description = "توضیح جدید",
            category = "REST_AREA",
            latitude = 28.5,
            longitude = 53.5,
            version = 1
        )

        storage.enqueueAction(
            QueuedAction(
                id = "action_poi_update_1",
                actionType = ActionType.UPDATE_POINT,
                payloadJson = json.encodeToString(updatePayload),
                createdAt = 1000L
            )
        )

        val result = manager.drainQueue()
        assertTrue(updateCalled)
        assertEquals(1, result.syncedCount)
        assertEquals(0, storage.getPendingActions().size)
    }

    @Test
    fun drainQueue_withDeletePointAction_callsApiAndClearsQueue() = runBlocking {
        var deleteCalled = false

        val mockPointApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()

            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()

            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> {
                deleteCalled = true
                assertEquals("poi_to_delete", id)
                return Response.success(
                    ApiResponseDto(
                        success = true,
                        data = DeletePointResponseDto(id = id, deleted = true, version = 2)
                    )
                )
            }
        }

        val manager = OfflineSyncManager(
            offlineStorage = storage,
            pointApiService = mockPointApi,
            dispatcherProvider = testDispatcher
        )

        val deletePayload = DeletePointQueuePayload(id = "poi_to_delete")

        storage.enqueueAction(
            QueuedAction(
                id = "action_poi_delete_1",
                actionType = ActionType.DELETE_POINT,
                payloadJson = json.encodeToString(deletePayload),
                createdAt = 1000L
            )
        )

        val result = manager.drainQueue()
        assertTrue(deleteCalled)
        assertEquals(1, result.syncedCount)
        assertEquals(0, storage.getPendingActions().size)
    }
}
