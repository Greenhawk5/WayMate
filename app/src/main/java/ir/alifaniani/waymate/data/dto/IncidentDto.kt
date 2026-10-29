package ir.alifaniani.waymate.data.dto

data class IncidentRequestDto(
    val title: String,
    val description: String,
    val incidentType: String,
    val severity: String,
    val latitude: Double,
    val longitude: Double
)

data class QuickAlertRequestDto(
    val latitude: Double,
    val longitude: Double,
    val presetReason: String = "مورد مشکوک تاکتیکی رویت شد"
)

data class IncidentResponseDto(
    val id: String,
    val teamId: String,
    val reporterUserId: String,
    val title: String,
    val description: String,
    val incidentType: String,
    val severity: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val createdAt: Long,
    val resolvedAt: Long? = null
)
