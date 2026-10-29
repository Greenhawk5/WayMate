package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.LoginRequestDto
import ir.alifaniani.waymate.data.dto.LoginResponseDto
import ir.alifaniani.waymate.data.dto.LogoutResponseDto
import ir.alifaniani.waymate.data.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit contract for Cloudflare Workers Authentication API endpoints.
 */
interface AuthApiService {

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponseDto<LoginResponseDto>>

    @GET("auth/me")
    suspend fun getProfile(): Response<ApiResponseDto<UserDto>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponseDto<LogoutResponseDto>>
}
