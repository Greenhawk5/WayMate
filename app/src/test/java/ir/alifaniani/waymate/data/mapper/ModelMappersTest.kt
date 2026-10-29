package ir.alifaniani.waymate.data.mapper

import ir.alifaniani.waymate.data.dto.IncidentResponseDto
import ir.alifaniani.waymate.data.dto.LocationPingDto
import ir.alifaniani.waymate.data.dto.TacticalMessageDto
import ir.alifaniani.waymate.data.dto.TeamDto
import ir.alifaniani.waymate.data.dto.TeamMemberDto
import ir.alifaniani.waymate.data.dto.UserDto
import ir.alifaniani.waymate.domain.model.IncidentSeverity
import ir.alifaniani.waymate.domain.model.IncidentStatus
import ir.alifaniani.waymate.domain.model.IncidentType
import ir.alifaniani.waymate.domain.model.LocationPing
import ir.alifaniani.waymate.domain.model.OperationalStatus
import ir.alifaniani.waymate.domain.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelMappersTest {

    @Test
    fun userDto_maps_correctly_to_domain() {
        val dto = UserDto(
            id = "usr_01",
            username = "commander_test",
            role = "COMMANDER",
            callsign = "Eagle-1"
        )
        val domain = dto.toDomain()

        assertEquals("usr_01", domain.id)
        assertEquals("commander_test", domain.username)
        assertEquals(UserRole.COMMANDER, domain.role)
        assertEquals("Eagle-1", domain.callsign)
    }

    @Test
    fun userDto_unknown_role_defaults_to_team_lead() {
        val dto = UserDto("usr_02", "scout_test", "UNKNOWN_ROLE", "Scout-1")
        assertEquals(UserRole.TEAM_LEAD, dto.toDomain().role)
    }

    @Test
    fun teamDto_maps_with_members_to_domain() {
        val dto = TeamDto(
            id = "team_01",
            name = "گشت آلفا",
            leadUserId = "usr_lead_01",
            vehicleType = "تویوتا هایلوکس",
            vehiclePlate = "۴۴-ب-۱۲",
            equipmentSummary = "بی‌سیم، جعبه امداد",
            members = listOf(
                TeamMemberDto("mem_01", "رضا کریمی", "البرز ۱", "راننده"),
                TeamMemberDto("mem_02", "محمد حسینی", "سهند ۲", "دیده‌بان")
            )
        )
        val domain = dto.toDomain()

        assertEquals("team_01", domain.id)
        assertEquals(2, domain.members.size)
        assertEquals(2, domain.memberCount)
        assertEquals("البرز ۱", domain.members[0].callsign)
        assertEquals("محمد حسینی", domain.members[1].fullName)
    }

    @Test
    fun teamDto_maps_empty_members_and_memberCount_correctly() {
        val dto = TeamDto(
            id = "team_alpha_001",
            name = "گشت آلفا (واکنش سریع)",
            leadUserId = "usr_lead_001",
            vehicleType = "تویوتا هایلوکس",
            vehiclePlate = "۴۴-ب-۱۲",
            equipmentSummary = "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
            members = null,
            memberCount = 3,
            isActive = true
        )
        val domain = dto.toDomain()

        assertEquals("team_alpha_001", domain.id)
        assertTrue(domain.members.isEmpty())
        assertEquals(3, domain.memberCount)
        assertTrue(domain.isActive)
    }

    @Test
    fun teamDto_deserializes_from_backend_paginated_json() {
        val jsonString = """
            {
              "success": true,
              "data": [
                {
                  "id": "team_alpha_001",
                  "name": "گشت آلفا (واکنش سریع)",
                  "lead_user_id": "usr_lead_001",
                  "vehicle_type": "تویوتا هایلوکس",
                  "vehicle_plate": "۴۴-ب-۱۲",
                  "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
                  "member_count": 2,
                  "is_active": true
                }
              ],
              "pagination": { "offset": 0, "limit": 50, "total": 1, "has_more": false },
              "meta": { "timestamp": 1729000000000, "request_id": "req_test" }
            }
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val response = json.decodeFromString<ir.alifaniani.waymate.data.dto.ApiResponseDto<List<TeamDto>>>(jsonString)

        assertTrue(response.success)
        assertEquals(1, response.data?.size)
        val team = response.data!![0].toDomain()
        assertEquals("team_alpha_001", team.id)
        assertEquals("گشت آلفا (واکنش سریع)", team.name)
        assertEquals("usr_lead_001", team.leadUserId)
        assertEquals("تویوتا هایلوکس", team.vehicleType)
        assertEquals("۴۴-ب-۱۲", team.vehiclePlate)
        assertEquals(2, team.memberCount)
        assertTrue(team.isActive)
    }

    @Test
    fun teamDto_deserializes_from_backend_detail_json_with_members() {
        val jsonString = """
            {
              "success": true,
              "data": {
                "id": "team_alpha_001",
                "name": "گشت آلفا (واکنش سریع)",
                "lead_user_id": "usr_lead_001",
                "vehicle_type": "تویوتا هایلوکس",
                "vehicle_plate": "۴۴-ب-۱۲",
                "equipment_summary": "بی‌سیم فرکانس اختصاصی، جعبه امداد تاکتیکی",
                "is_active": true,
                "members": [
                  {
                    "id": "mem_001",
                    "full_name": "رضا کریمی",
                    "callsign": "البرز ۱",
                    "role_in_team": "راننده و اپراتور فنی"
                  },
                  {
                    "id": "mem_002",
                    "full_name": "محمد حسینی",
                    "callsign": "سهند ۲",
                    "role_in_team": "دیده‌بان و سرتیم"
                  }
                ]
              }
            }
        """.trimIndent()

        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }
        val response = json.decodeFromString<ir.alifaniani.waymate.data.dto.ApiResponseDto<TeamDto>>(jsonString)

        assertTrue(response.success)
        val team = response.data!!.toDomain()
        assertEquals("team_alpha_001", team.id)
        assertEquals(2, team.members.size)
        assertEquals(2, team.memberCount)
        assertEquals("رضا کریمی", team.members[0].fullName)
        assertEquals("البرز ۱", team.members[0].callsign)
        assertEquals("دیده‌بان و سرتیم", team.members[1].roleInTeam)
    }

    @Test
    fun locationPingDto_bidirectional_mapping() {
        val dto = LocationPingDto(
            latitude = 35.6961,
            longitude = 51.4231,
            accuracyMeters = 4.5f,
            speedMps = 12.0f,
            headingDegrees = 180.0f,
            operationalStatus = "PURSUING",
            recordedAt = 1729000000000L
        )

        val domain = dto.toDomain()
        assertEquals(35.6961, domain.latitude, 0.0001)
        assertEquals(51.4231, domain.longitude, 0.0001)
        assertEquals(4.5f, domain.accuracyMeters, 0.01f)
        assertEquals(OperationalStatus.PURSUING, domain.status)

        val backToDto = domain.toDto()
        assertEquals("PURSUING", backToDto.operationalStatus)
        assertEquals(1729000000000L, backToDto.recordedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun locationPing_rejects_invalid_latitude() {
        LocationPing(latitude = 95.0, longitude = 51.0, accuracyMeters = 5.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun locationPing_rejects_invalid_longitude() {
        LocationPing(latitude = 35.0, longitude = 185.0, accuracyMeters = 5.0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun locationPing_rejects_negative_accuracy() {
        LocationPing(latitude = 35.0, longitude = 51.0, accuracyMeters = -1.0f)
    }

    @Test
    fun incidentResponseDto_maps_correctly_to_domain() {
        val dto = IncidentResponseDto(
            id = "inc_01",
            teamId = "team_alpha",
            reporterUserId = "usr_01",
            title = "مشاهده مظنون",
            description = "فرد مشکوک در تقاطع شمالی",
            incidentType = "SUSPICIOUS_PERSON",
            severity = "URGENT",
            latitude = 35.7000,
            longitude = 51.4000,
            status = "OPEN",
            createdAt = 1729000100000L
        )

        val domain = dto.toDomain()
        assertEquals("inc_01", domain.id)
        assertEquals(IncidentType.SUSPICIOUS_PERSON, domain.type)
        assertEquals(IncidentSeverity.URGENT, domain.severity)
        assertEquals(IncidentStatus.OPEN, domain.status)
    }

    @Test
    fun tacticalMessageDto_maps_correctly_to_domain() {
        val dto = TacticalMessageDto(
            id = "msg_01",
            senderCallsign = "Eagle-1",
            body = "توقف عملیات",
            isUrgent = true,
            createdAt = 1729000200000L,
            acknowledged = true
        )

        val domain = dto.toDomain()
        assertEquals("msg_01", domain.id)
        assertTrue(domain.isUrgent)
        assertTrue(domain.isAcknowledged)
        assertEquals("Eagle-1", domain.senderCallsign)
    }
}
