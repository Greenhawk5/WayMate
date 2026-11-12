package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.LiveUnitDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusResponseDto
import ir.alifaniani.waymate.data.network.StatusApiService
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class StatusRepositoryTest {

    private val testDispatcherProvider = TestDispatcherProvider()

    @Test
    fun fakeStatusRepository_getLiveTeamStatuses_returnsSeededStatuses() = runBlocking {
        val seeded = listOf(
            TeamLiveStatus(
                teamId = "team_alpha_001",
                teamName = "گشت آلفا",
                callsign = "Falcon",
                operationalStatus = OperationalStatus.NORMAL,
                connectionStatus = ConnectionStatus.ONLINE,
                lastSeenAt = 5000L,
                isStale = false
            )
        )
        val repository = FakeStatusRepository(seeded)

        val result = repository.getLiveTeamStatuses()
        assertTrue(result is Result.Success)
        val statuses = (result as Result.Success).data
        assertEquals(1, statuses.size)
        assertEquals("team_alpha_001", statuses[0].teamId)
        assertEquals(OperationalStatus.NORMAL, statuses[0].operationalStatus)
        assertEquals(ConnectionStatus.ONLINE, statuses[0].connectionStatus)
    }

    @Test
    fun fakeStatusRepository_updateOperationalStatus_updatesStatus() = runBlocking {
        val repository = FakeStatusRepository()

        val updateResult = repository.updateOperationalStatus(OperationalStatus.PURSUING)
        assertTrue(updateResult is Result.Success)
        assertEquals(OperationalStatus.PURSUING, repository.currentOperationalStatus)
    }

    @Test
    fun realStatusRepository_getLiveUnits_success() = runBlocking {
        val fakeApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> {
                val list = listOf(
                    LiveUnitDto(
                        teamId = "t1",
                        teamName = "Alpha",
                        callsign = "F1",
                        operationalStatus = "PURSUING",
                        lastSeenAt = 1000L,
                        connectionStatus = "ONLINE",
                        isStale = false
                    )
                )
                return Response.success(ApiResponseDto(success = true, data = list))
            }

            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> {
                val res = UpdateOperationalStatusResponseDto(
                    teamId = "t1",
                    operationalStatus = request.operationalStatus,
                    updatedAt = 2000L
                )
                return Response.success(ApiResponseDto(success = true, data = res))
            }
        }

        val repository = RealStatusRepository(fakeApi, testDispatcherProvider)
        val result = repository.getLiveTeamStatuses()
        assertTrue(result is Result.Success)
        val list = (result as Result.Success).data
        assertEquals(1, list.size)
        assertEquals(OperationalStatus.PURSUING, list[0].operationalStatus)
        assertEquals(ConnectionStatus.ONLINE, list[0].connectionStatus)
    }

    @Test
    fun realStatusRepository_updateOperationalStatus_error() = runBlocking {
        val fakeApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> {
                throw NotImplementedError()
            }

            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> {
                val errorBody = "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"دسترسی غیرمجاز\"}}"
                    .toResponseBody("application/json".toMediaType())
                return Response.error(403, errorBody)
            }
        }

        val repository = RealStatusRepository(fakeApi, testDispatcherProvider)
        val result = repository.updateOperationalStatus(OperationalStatus.ENGAGED)
        assertTrue(result is Result.Error)
    }

    @Test
    fun realStatusRepository_updateOperationalStatus_withOfflineStorage_persistsAndSyncsLiveStatuses() = runBlocking {
        val storage = ir.alifaniani.waymate.data.local.InMemoryOfflineStorage()
        storage.saveStatuses(
            listOf(
                TeamLiveStatus(
                    teamId = "team_alpha_001",
                    teamName = "گشت آلفا",
                    callsign = "Falcon-Lead",
                    operationalStatus = OperationalStatus.NORMAL,
                    connectionStatus = ConnectionStatus.ONLINE,
                    lastSeenAt = 1000L,
                    isStale = false
                )
            )
        )

        val fakeApi = object : StatusApiService {
            override suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>> {
                val list = listOf(
                    LiveUnitDto(
                        teamId = "team_alpha_001",
                        teamName = "گشت آلفا",
                        callsign = "Falcon-Lead",
                        operationalStatus = "NORMAL",
                        lastSeenAt = 2000L,
                        connectionStatus = "ONLINE",
                        isStale = false
                    )
                )
                return Response.success(ApiResponseDto(success = true, data = list))
            }

            override suspend fun updateOperationalStatus(request: UpdateOperationalStatusRequestDto): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>> {
                val errorBody = "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"دسترسی غیرمجاز\"}}"
                    .toResponseBody("application/json".toMediaType())
                return Response.error(403, errorBody)
            }
        }

        val repository = RealStatusRepository(fakeApi, testDispatcherProvider, storage)
        val updateResult = repository.updateOperationalStatus(OperationalStatus.PURSUING)
        assertTrue(updateResult is Result.Success)
        assertEquals(OperationalStatus.PURSUING, repository.getMyOperationalStatus())

        val liveResult = repository.getLiveTeamStatuses()
        assertTrue(liveResult is Result.Success)
        assertEquals(OperationalStatus.PURSUING, (liveResult as Result.Success).data.first().operationalStatus)
    }
}
