package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.AuditLogDto
import ir.alifaniani.waymate.data.dto.SystemStatsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for System Administration and Operational Reports.
 */
interface AdminApiService {

    @GET("admin/stats")
    suspend fun getStats(): Response<ApiResponseDto<SystemStatsDto>>

    @GET("admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("event_type") eventType: String? = null
    ): Response<ApiResponseDto<List<AuditLogDto>>>
}
