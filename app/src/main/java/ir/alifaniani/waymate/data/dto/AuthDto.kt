package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    @SerialName("username") val username: String,
    @SerialName("password") val password: String
)

@Serializable
data class LoginResponseDto(
    @SerialName("token") val token: String,
    @SerialName("token_type") val tokenType: String = "Bearer",
    @SerialName("expires_at") val expiresAt: Long = 0L,
    @SerialName("user") val user: UserDto
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("username") val username: String,
    @SerialName("role") val role: String,
    @SerialName("callsign") val callsign: String,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class LogoutResponseDto(
    @SerialName("message") val message: String = ""
)
