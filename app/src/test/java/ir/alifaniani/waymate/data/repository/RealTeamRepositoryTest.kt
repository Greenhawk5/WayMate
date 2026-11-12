package ir.alifaniani.waymate.data.repository

import ir.alifaniani.waymate.TestDispatcherProvider
import ir.alifaniani.waymate.core.common.Result
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreateMemberDto
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.TeamDto
import ir.alifaniani.waymate.data.dto.TeamMemberDto
import ir.alifaniani.waymate.data.dto.UpdateTeamRequestDto
import ir.alifaniani.waymate.data.local.InMemoryOfflineStorage
import ir.alifaniani.waymate.data.network.NetworkClient
import ir.alifaniani.waymate.data.network.TeamApiService
import ir.alifaniani.waymate.data.session.InMemorySessionStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RealTeamRepositoryTest {

    private val testDispatcher = TestDispatcherProvider()

    @Test
    fun networkClientJson_omitsExplicitNullsForPartialTeamUpdate() {
        val client = NetworkClient(InMemorySessionStorage())
        val payload = client.json.encodeToString(UpdateTeamRequestDto(isActive = false))

        assertEquals("{\"is_active\":false}", payload)
        assertFalse("Partial update payload must not contain null fields", payload.contains("null"))
    }

    @Test
    fun createMultipleTeamsAndDeactivate_preservesAllTeamsAndUpdatesActiveStatus() = runBlocking {
        val storage = InMemoryOfflineStorage()
        val serverTeams = mutableListOf(
            TeamDto(
                id = "team_alpha_001",
                name = "گشت آلفا",
                leadUserId = "usr_lead_001",
                vehicleType = "هایلوکس",
                vehiclePlate = "۱۱-الف-۲۲",
                equipmentSummary = "بی‌سیم",
                isActive = true,
                memberCount = 2
            )
        )

        val fakeApi = object : TeamApiService {
            override suspend fun getTeams(offset: Int, limit: Int): Response<ApiResponseDto<List<TeamDto>>> {
                // Simulate backend behavior: GET /api/v1/teams only returns active teams (WHERE is_active = 1)
                return Response.success(
                    ApiResponseDto(
                        success = true,
                        data = serverTeams.filter { it.isActive }
                    )
                )
            }

            override suspend fun getTeamById(id: String): Response<ApiResponseDto<TeamDto>> {
                val found = serverTeams.firstOrNull { it.id == id }
                return Response.success(ApiResponseDto(success = found != null, data = found))
            }

            override suspend fun createTeam(request: CreateTeamRequestDto): Response<ApiResponseDto<TeamDto>> {
                val created = TeamDto(
                    id = "team_${serverTeams.size + 1}",
                    name = request.name,
                    leadUserId = request.leadUserId,
                    vehicleType = request.vehicleType,
                    vehiclePlate = request.vehiclePlate,
                    equipmentSummary = request.equipmentSummary,
                    isActive = true,
                    memberCount = request.members.size
                )
                serverTeams.add(created)
                return Response.success(ApiResponseDto(success = true, data = created))
            }

            override suspend fun updateTeam(id: String, request: UpdateTeamRequestDto): Response<ApiResponseDto<TeamDto>> {
                val index = serverTeams.indexOfFirst { it.id == id }
                val current = serverTeams[index]
                val updated = current.copy(
                    name = request.name ?: current.name,
                    isActive = request.isActive ?: current.isActive
                )
                serverTeams[index] = updated
                return Response.success(ApiResponseDto(success = true, data = updated))
            }

            override suspend fun addMember(teamId: String, request: CreateMemberDto): Response<ApiResponseDto<TeamMemberDto>> {
                throw NotImplementedError()
            }

            override suspend fun removeMember(teamId: String, memberId: String): Response<ApiResponseDto<Map<String, String>>> {
                throw NotImplementedError()
            }
        }

        val repository = RealTeamRepository(
            teamApiService = fakeApi,
            dispatcherProvider = testDispatcher,
            offlineStorage = storage
        )

        // Create two additional teams sequentially
        val create1 = repository.createTeam("گروه براوو", "usr_lead_001", "پاترول", "۲۲-ب-۳۳", "کیت امداد", emptyList())
        val create2 = repository.createTeam("گروه چارلی", "usr_lead_001", "رونیز", "۴۴-ج-۵۵", "پهپاد", emptyList())
        assertTrue(create1 is Result.Success)
        assertTrue(create2 is Result.Success)

        val listAfterCreation = repository.getTeams()
        assertTrue(listAfterCreation is Result.Success)
        assertEquals(3, (listAfterCreation as Result.Success).data.size)

        // Deactivate "گروه براوو"
        val bravoId = (create1 as Result.Success).data.id
        val deactivateResult = repository.updateTeam(bravoId, isActive = false)
        assertTrue(deactivateResult is Result.Success)
        assertFalse((deactivateResult as Result.Success).data.isActive)

        // Reload teams: deactivated team must remain preserved with isActive = false
        val listAfterDeactivation = repository.getTeams()
        assertTrue(listAfterDeactivation is Result.Success)
        val allTeams = (listAfterDeactivation as Result.Success).data
        assertEquals(3, allTeams.size)
        val bravoAfterReload = allTeams.first { it.id == bravoId }
        assertFalse(bravoAfterReload.isActive)
    }
}
