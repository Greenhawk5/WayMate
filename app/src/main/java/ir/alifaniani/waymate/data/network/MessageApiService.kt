package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.AcknowledgeResponseDto
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.SendMessageRequestDto
import ir.alifaniani.waymate.data.dto.TacticalMessageDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit contract for tactical orders and broadcast messaging endpoints.
 */
interface MessageApiService {

    @GET("messages")
    suspend fun getMessages(): Response<ApiResponseDto<List<TacticalMessageDto>>>

    @POST("messages/{id}/acknowledge")
    suspend fun acknowledgeMessage(
        @Path("id") id: String
    ): Response<ApiResponseDto<AcknowledgeResponseDto>>

    @POST("messages")
    suspend fun sendMessage(
        @Body request: SendMessageRequestDto
    ): Response<ApiResponseDto<TacticalMessageDto>>
}
