package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreatePointRequestDto
import ir.alifaniani.waymate.data.dto.DeletePointResponseDto
import ir.alifaniani.waymate.data.dto.PointOfInterestDto
import ir.alifaniani.waymate.data.dto.UpdatePointRequestDto
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.local.model.ActionType
import ir.alifaniani.waymate.data.network.PointOfInterestApiService
import ir.alifaniani.waymate.domain.model.PointCategory
import ir.alifaniani.waymate.domain.model.PointOfInterest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class PointOfInterestRepositoryTest {

    private lateinit var storage: InMemoryOfflineStorage
    private val testDispatcher = TestDispatcherProvider()

    @Before
    fun setUp() {
        storage = InMemoryOfflineStorage()
    }

    private val samplePointDto = PointOfInterestDto(
        id = "poi_101",
        teamId = "team_alpha_001",
        title = "قله شاه نشین",
        category = "DESTINATION",
        description = "ارتفاع ۲۳۵۰ متری با چشم‌انداز باز",
        latitude = 28.5150,
        longitude = 53.5720,
        createdByUserId = "usr_01",
        createdByCallsign = "علی اکبری",
        updatedByUserId = "usr_01",
        updatedByCallsign = "علی اکبری",
        createdAt = 1729000000000L,
        updatedAt = 1729000000000L,
        version = 1
    )

    @Test
    fun realRepository_getPoints_success_cachesAndReturnsList() = runBlocking {
        val successApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> {
                return Response.success(ApiResponseDto(success = true, data = listOf(samplePointDto)))
            }
            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val repository = RealPointOfInterestRepository(
            pointApiService = successApi,
            offlineStorage = storage,
            dispatcherProvider = testDispatcher
        )

        val result = repository.getPoints("team_alpha_001")
        assertTrue(result is Result.Success)
        val points = (result as Result.Success).data
        assertEquals(1, points.size)
        assertEquals("قله شاه نشین", points[0].title)
        assertEquals(PointCategory.DESTINATION, points[0].category)

        // Verify it was cached in offline storage
        val cached = storage.getCachedPoints("team_alpha_001")
        assertEquals(1, cached.size)
        assertEquals("poi_101", cached[0].id)
    }

    @Test
    fun realRepository_getPoints_networkFailure_fallsBackToCache() = runBlocking {
        storage.savePoints(
            listOf(
                PointOfInterest(
                    id = "poi_cached_01",
                    teamId = "team_alpha_001",
                    title = "چشمه علی‌آباد",
                    category = PointCategory.WATER_SOURCE,
                    description = "آب گوارا و مناسب برای آشامیدن",
                    latitude = 28.5200,
                    longitude = 53.5650,
                    createdByUserId = "usr_01",
                    createdByCallsign = "محمدرضا",
                    updatedByUserId = "usr_01",
                    updatedByCallsign = "محمدرضا",
                    createdAt = 1729000000000L,
                    updatedAt = 1729000000000L,
                    version = 1
                )
            )
        )

        val failingApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> {
                throw IOException("Network unavailable")
            }
            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val repository = RealPointOfInterestRepository(
            pointApiService = failingApi,
            offlineStorage = storage,
            dispatcherProvider = testDispatcher
        )

        val result = repository.getPoints("team_alpha_001")
        assertTrue(result is Result.Success)
        val points = (result as Result.Success).data
        assertEquals(1, points.size)
        assertEquals("چشمه علی‌آباد", points[0].title)
    }

    @Test
    fun realRepository_createPoint_networkOffline_createsOptimisticAndEnqueuesAction() = runBlocking {
        val failingApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()
            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> {
                throw IOException("No connectivity")
            }
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val repository = RealPointOfInterestRepository(
            pointApiService = failingApi,
            offlineStorage = storage,
            dispatcherProvider = testDispatcher
        )

        val result = repository.createPoint(
            title = "محل کمپ ۲",
            description = "محل مسطح مناسب برپایی چادر",
            category = PointCategory.CAMPSITE,
            latitude = 28.5300,
            longitude = 53.5800,
            teamId = "team_alpha_001"
        )

        assertTrue(result is Result.Success)
        val created = (result as Result.Success).data
        assertTrue(created.isPendingSync)
        assertEquals("محل کمپ ۲", created.title)
        assertEquals(PointCategory.CAMPSITE, created.category)

        // Verify stored in offline cache
        val cached = storage.getCachedPoints("team_alpha_001")
        assertEquals(1, cached.size)
        assertEquals(created.id, cached[0].id)

        // Verify queued for background sync
        val queue = storage.getPendingActions()
        assertEquals(1, queue.size)
        assertEquals(ActionType.CREATE_POINT, queue[0].actionType)
        assertTrue(queue[0].payloadJson.contains("محل کمپ ۲"))
    }

    @Test
    fun realRepository_updatePoint_networkOffline_updatesLocallyAndEnqueuesAction() = runBlocking {
        val initialPoint = PointOfInterest(
            id = "poi_101",
            teamId = "team_alpha_001",
            title = "نقطه اولیه",
            category = PointCategory.GENERAL,
            description = "",
            latitude = 28.5000,
            longitude = 53.5000,
            createdByUserId = "usr_01",
            createdByCallsign = "کاربر",
            updatedByUserId = "usr_01",
            updatedByCallsign = "کاربر",
            createdAt = 1000L,
            updatedAt = 1000L,
            version = 1
        )
        storage.savePoints(listOf(initialPoint))

        val failingApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()
            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> {
                throw IOException("Network timeout")
            }
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> = throw NotImplementedError()
        }

        val repository = RealPointOfInterestRepository(
            pointApiService = failingApi,
            offlineStorage = storage,
            dispatcherProvider = testDispatcher
        )

        val result = repository.updatePoint(
            id = "poi_101",
            title = "نقطه ویرایش شده آفلاین",
            description = "ویرایش در حالت بدون اینترنت",
            category = PointCategory.REST_AREA,
            latitude = 28.5010,
            longitude = 53.5010,
            version = 1
        )

        assertTrue(result is Result.Success)
        val updated = (result as Result.Success).data
        assertTrue(updated.isPendingSync)
        assertEquals("نقطه ویرایش شده آفلاین", updated.title)

        // Verify action queued
        val actions = storage.getPendingActions()
        assertEquals(1, actions.size)
        assertEquals(ActionType.UPDATE_POINT, actions[0].actionType)
    }

    @Test
    fun realRepository_deletePoint_networkOffline_deletesLocallyAndEnqueuesAction() = runBlocking {
        val initialPoint = PointOfInterest(
            id = "poi_to_delete",
            teamId = "team_alpha_001",
            title = "نقطه آزمایشی",
            category = PointCategory.GENERAL,
            description = "",
            latitude = 28.5000,
            longitude = 53.5000,
            createdByUserId = "usr_01",
            createdByCallsign = "کاربر",
            updatedByUserId = "usr_01",
            updatedByCallsign = "کاربر",
            createdAt = 1000L,
            updatedAt = 1000L,
            version = 1
        )
        storage.savePoints(listOf(initialPoint))

        val failingApi = object : PointOfInterestApiService {
            override suspend fun getPoints(
                teamId: String?,
                since: Long?,
                includeDeleted: Boolean?
            ): Response<ApiResponseDto<List<PointOfInterestDto>>> = throw NotImplementedError()
            override suspend fun getPointById(id: String): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun createPoint(request: CreatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun updatePoint(id: String, request: UpdatePointRequestDto): Response<ApiResponseDto<PointOfInterestDto>> = throw NotImplementedError()
            override suspend fun deletePoint(id: String): Response<ApiResponseDto<DeletePointResponseDto>> {
                throw IOException("Offline")
            }
        }

        val repository = RealPointOfInterestRepository(
            pointApiService = failingApi,
            offlineStorage = storage,
            dispatcherProvider = testDispatcher
        )

        val result = repository.deletePoint("poi_to_delete")
        assertTrue(result is Result.Success)

        // Point is removed from cache
        val cached = storage.getCachedPointById("poi_to_delete")
        assertEquals(null, cached)

        // Action queued
        val actions = storage.getPendingActions()
        assertEquals(1, actions.size)
        assertEquals(ActionType.DELETE_POINT, actions[0].actionType)
    }

    @Test
    fun fakeRepository_crudOperations_workCorrectly() = runBlocking {
        val fakeRepo = FakePointOfInterestRepository()

        // 1. Initial seeded points
        val initialResult = fakeRepo.getPoints("team_alpha_001")
        assertTrue(initialResult is Result.Success)
        val initialList = (initialResult as Result.Success).data
        assertEquals(3, initialList.size)

        // 2. Create new point
        val createResult = fakeRepo.createPoint(
            title = "نقطه جدید تستی",
            description = "محل آغاز برنامه پیمایش",
            category = PointCategory.START_POINT,
            latitude = 28.5050,
            longitude = 53.5600,
            teamId = "team_alpha_001"
        )
        assertTrue(createResult is Result.Success)
        val created = (createResult as Result.Success).data
        assertEquals("نقطه جدید تستی", created.title)
        assertEquals(1, created.version)

        // 3. Update point
        val updateResult = fakeRepo.updatePoint(
            id = created.id,
            title = "نقطه ویرایش شده",
            description = "توضیحات اصلاح شد",
            category = PointCategory.START_POINT,
            latitude = 28.5060,
            longitude = 53.5610,
            version = 1
        )
        assertTrue(updateResult is Result.Success)
        val updated = (updateResult as Result.Success).data
        assertEquals("نقطه ویرایش شده", updated.title)
        assertEquals(2, updated.version)

        // 4. Delete point
        val deleteResult = fakeRepo.deletePoint(created.id)
        assertTrue(deleteResult is Result.Success)

        // 5. Verify deleted point is gone
        val getDeleted = fakeRepo.getPointById(created.id)
        assertTrue(getDeleted is Result.Error)
    }
}
