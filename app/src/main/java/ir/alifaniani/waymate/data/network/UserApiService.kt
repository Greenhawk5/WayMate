package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiResponseDto
import ir.alifaniani.waymate.data.dto.CreateUserRequestDto
import ir.alifaniani.waymate.data.dto.ManagedUserDto
import ir.alifaniani.waymate.data.dto.UpdateUserRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for User Management endpoints (Restricted to OWNER).
 */
interface UserApiService {

    @GET("users")
    suspend fun getUsers(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 50,
        @Query("role") role: String? = null
    ): Response<ApiResponseDto<List<ManagedUserDto>>>

    @POST("users")
    suspend fun createUser(
        @Body request: CreateUserRequestDto
    ): Response<ApiResponseDto<ManagedUserDto>>

    @PUT("users/{id}")
    suspend fun updateUser(
        @Path("id") id: String,
        @Body request: UpdateUserRequestDto
    ): Response<ApiResponseDto<ManagedUserDto>>
}
