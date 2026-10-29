package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.LiveUnitDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusRequestDto
import ir.alifaniani.waymate.data.dto.UpdateOperationalStatusResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for live Blue Force tracking presence and operational status endpoints.
 */
interface StatusApiService {

    @GET("telemetry/live")
    suspend fun getLiveUnits(): Response<ApiResponseDto<List<LiveUnitDto>>>

    @POST("telemetry/status")
    suspend fun updateOperationalStatus(
        @Body request: UpdateOperationalStatusRequestDto
    ): Response<ApiResponseDto<UpdateOperationalStatusResponseDto>>
}
