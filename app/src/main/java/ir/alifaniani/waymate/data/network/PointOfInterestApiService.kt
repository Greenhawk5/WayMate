package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreatePointRequestDto
import ir.alifaniani.waymate.data.dto.DeletePointResponseDto
import ir.alifaniani.waymate.data.dto.PointOfInterestDto
import ir.alifaniani.waymate.data.dto.UpdatePointRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for Cloudflare Workers Group Points of Interest (POI) API endpoints.
 */
interface PointOfInterestApiService {

    @GET("points")
    suspend fun getPoints(
        @Query("team_id") teamId: String? = null,
        @Query("since") since: Long? = null,
        @Query("include_deleted") includeDeleted: Boolean? = null
    ): Response<ApiResponseDto<List<PointOfInterestDto>>>

    @GET("points/{id}")
    suspend fun getPointById(
        @Path("id") pointId: String
    ): Response<ApiResponseDto<PointOfInterestDto>>

    @POST("points")
    suspend fun createPoint(
        @Body request: CreatePointRequestDto
    ): Response<ApiResponseDto<PointOfInterestDto>>

    @PUT("points/{id}")
    suspend fun updatePoint(
        @Path("id") pointId: String,
        @Body request: UpdatePointRequestDto
    ): Response<ApiResponseDto<PointOfInterestDto>>

    @DELETE("points/{id}")
    suspend fun deletePoint(
        @Path("id") pointId: String
    ): Response<ApiResponseDto<DeletePointResponseDto>>
}
