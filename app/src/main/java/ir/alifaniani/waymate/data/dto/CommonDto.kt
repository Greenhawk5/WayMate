package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard API envelope wrapper for JSON responses.
 */
@Serializable
data class ApiResponseDto<T>(
    @SerialName("success") val success: Boolean,
    @SerialName("data") val data: T? = null,
    @SerialName("error") val error: ApiErrorDto? = null,
    @SerialName("pagination") val pagination: PaginationDto? = null,
    @SerialName("meta") val meta: ApiMetaDto? = null
)

@Serializable
data class ApiErrorDto(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String,
    @SerialName("details") val details: List<FieldErrorDto>? = null,
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("request_id") val requestId: String? = null
)

@Serializable
data class FieldErrorDto(
    @SerialName("field") val field: String,
    @SerialName("issue") val issue: String
)

@Serializable
data class PaginationDto(
    @SerialName("offset") val offset: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("total") val total: Int,
    @SerialName("has_more") val hasMore: Boolean
)

@Serializable
data class ApiMetaDto(
    @SerialName("timestamp") val timestamp: Long? = null,
    @SerialName("request_id") val requestId: String? = null
)
