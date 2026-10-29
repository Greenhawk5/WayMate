package ir.alifaniani.waymate.data.mapper

import ir.alifaniani.waymate.data.dto.IncidentResponseDto
import ir.alifaniani.waymate.data.dto.LiveUnitDto
import ir.alifaniani.waymate.data.dto.LocationPingDto
import ir.alifaniani.waymate.data.dto.TacticalMessageDto
import ir.alifaniani.waymate.data.dto.TeamDto
import ir.alifaniani.waymate.data.dto.TeamMemberDto
import ir.alifaniani.waymate.data.dto.UserDto
import ir.alifaniani.waymate.domain.model.ConnectionStatus
import ir.alifaniani.waymate.domain.model.IncidentReport
import ir.alifaniani.waymate.domain.model.IncidentSeverity
import ir.alifaniani.waymate.domain.model.IncidentStatus
import ir.alifaniani.waymate.domain.model.IncidentType
import ir.alifaniani.waymate.domain.model.LocationPing
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.TacticalMessage
import ir.alifaniani.waymate.domain.model.Team
import ir.alifaniani.waymate.domain.model.TeamLiveStatus
import ir.alifaniani.waymate.domain.model.TeamMember
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole

fun UserDto.toDomain(): User = User(
    id = id,
    username = username,
    role = when (role.uppercase()) {
        "OWNER" -> UserRole.OWNER
        "COMMANDER" -> UserRole.COMMANDER
        else -> UserRole.TEAM_LEAD
    },
    callsign = callsign
)

fun TeamMemberDto.toDomain(): TeamMember = TeamMember(
    id = id,
    fullName = fullName,
    callsign = callsign,
    roleInTeam = roleInTeam
)

fun TeamDto.toDomain(): Team = Team(
    id = id,
    name = name,
    leadUserId = leadUserId,
    vehicleType = vehicleType,
    vehiclePlate = vehiclePlate,
    equipmentSummary = equipmentSummary,
    members = members?.map { it.toDomain() } ?: emptyList(),
    memberCount = memberCount ?: members?.size ?: 0,
    isActive = isActive
)

fun LocationPingDto.toDomain(): LocationPing = LocationPing(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    speedMps = speedMps,
    headingDegrees = headingDegrees,
    status = when (operationalStatus.uppercase()) {
        "PURSUING" -> OperationalStatus.PURSUING
        "ENGAGED" -> OperationalStatus.ENGAGED
        else -> OperationalStatus.NORMAL
    },
    recordedAt = recordedAt
)

fun LocationPing.toDto(): LocationPingDto = LocationPingDto(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
    speedMps = speedMps,
    headingDegrees = headingDegrees,
    operationalStatus = status.name,
    recordedAt = recordedAt
)

fun IncidentResponseDto.toDomain(): IncidentReport = IncidentReport(
    id = id,
    teamId = teamId,
    title = title,
    description = description,
    type = when (incidentType.uppercase()) {
        "SUSPICIOUS_PERSON" -> IncidentType.SUSPICIOUS_PERSON
        "VEHICLE_TRESPASS" -> IncidentType.VEHICLE_TRESPASS
        "ROAD_HAZARD" -> IncidentType.ROAD_HAZARD
        "SECURITY_BREACH" -> IncidentType.SECURITY_BREACH
        "QUICK_ALERT" -> IncidentType.QUICK_ALERT
        else -> IncidentType.OTHER
    },
    severity = when (severity.uppercase()) {
        "URGENT" -> IncidentSeverity.URGENT
        "WARNING" -> IncidentSeverity.WARNING
        else -> IncidentSeverity.ROUTINE
    },
    latitude = latitude,
    longitude = longitude,
    status = when (status.uppercase()) {
        "IN_PROGRESS" -> IncidentStatus.IN_PROGRESS
        "RESOLVED" -> IncidentStatus.RESOLVED
        else -> IncidentStatus.OPEN
    },
    createdAt = createdAt
)

fun TacticalMessageDto.toDomain(): TacticalMessage = TacticalMessage(
    id = id,
    senderCallsign = senderCallsign,
    body = body,
    isUrgent = isUrgent,
    createdAt = createdAt,
    isAcknowledged = acknowledged
)

fun LiveUnitDto.toDomain(): TeamLiveStatus = TeamLiveStatus(
    teamId = teamId,
    teamName = teamName,
    callsign = callsign,
    operationalStatus = when (operationalStatus.uppercase()) {
        "PURSUING" -> OperationalStatus.PURSUING
        "ENGAGED" -> OperationalStatus.ENGAGED
        else -> OperationalStatus.NORMAL
    },
    connectionStatus = when (connectionStatus.uppercase()) {
        "ONLINE" -> ConnectionStatus.ONLINE
        "STALE" -> ConnectionStatus.STALE
        else -> ConnectionStatus.OFFLINE
    },
    lastSeenAt = lastSeenAt,
    isStale = isStale
)
