package ir.alifaniani.waymate.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TacticalMessageDto(
    @SerialName("id") val id: String,
    @SerialName("sender_callsign") val senderCallsign: String = "",
    @SerialName("target_team_id") val targetTeamId: String? = null,
    @SerialName("body") val body: String,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("acknowledged") val acknowledged: Boolean = false
)

@Serializable
data class SendMessageRequestDto(
    @SerialName("body") val body: String,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    @SerialName("target_team_id") val targetTeamId: String? = null
)

@Serializable
data class AcknowledgeResponseDto(
    @SerialName("acknowledged_at") val acknowledgedAt: Long
)
