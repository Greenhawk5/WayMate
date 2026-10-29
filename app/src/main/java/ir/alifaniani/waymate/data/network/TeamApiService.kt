package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreateTeamRequestDto
import ir.alifaniani.waymate.data.dto.TeamDto
import ir.alifaniani.waymate.data.dto.UpdateTeamRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for Cloudflare Workers Team and Organizational Roster API endpoints.
 */
interface TeamApiService {

    @GET("teams")
    suspend fun getTeams(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponseDto<List<TeamDto>>>

    @GET("teams/{id}")
    suspend fun getTeamById(
        @Path("id") teamId: String
    ): Response<ApiResponseDto<TeamDto>>

    @POST("teams")
    suspend fun createTeam(
        @Body request: CreateTeamRequestDto
    ): Response<ApiResponseDto<TeamDto>>

    @PUT("teams/{id}")
    suspend fun updateTeam(
        @Path("id") teamId: String,
        @Body request: UpdateTeamRequestDto
    ): Response<ApiResponseDto<TeamDto>>
}
