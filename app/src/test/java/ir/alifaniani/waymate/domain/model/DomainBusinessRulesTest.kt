package ir.alifaniani.waymate.domain.model

import ir.alifaniani.waymate.ui.home.map.GeoPoint
import ir.alifaniani.waymate.ui.home.map.HikingSimulationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 13 Comprehensive Domain Unit Testing.
 *
 * Verifies business logic, security constraints, role boundaries,
 * geographic Haversine calculations, location freshness, and data validation rules.
 */
class DomainBusinessRulesTest {

    // =========================================================================
    // 1. User Roles & Operational Hierarchy Constraints
    // =========================================================================
    @Test
    fun userRole_hierarchy_isConsistent() {
        val owner = User(id = "usr_001", username = "owner_user", role = UserRole.OWNER, callsign = "Alpha-Base")
        val commander = User(id = "usr_002", username = "cmd_user", role = UserRole.COMMANDER, callsign = "Eagle-1")
        val teamLead = User(id = "usr_003", username = "lead_user", role = UserRole.TEAM_LEAD, callsign = "Falcon-Lead")

        assertEquals(UserRole.OWNER, owner.role)
        assertEquals(UserRole.COMMANDER, commander.role)
        assertEquals(UserRole.TEAM_LEAD, teamLead.role)
    }

    // =========================================================================
    // 2. Operational & Connection Status Transitions
    // =========================================================================
    @Test
    fun operationalStatus_enumEntries_areDefined() {
        val allStatuses = OperationalStatus.entries
        assertEquals(3, allStatuses.size)
        assertTrue(allStatuses.contains(OperationalStatus.NORMAL))
        assertTrue(allStatuses.contains(OperationalStatus.PURSUING))
        assertTrue(allStatuses.contains(OperationalStatus.ENGAGED))
    }

    @Test
    fun connectionStatus_hasCorrectPersianLabels() {
        assertEquals("برخط", ConnectionStatus.ONLINE.titlePersian)
        assertEquals("هشدار تأخیر", ConnectionStatus.STALE.titlePersian)
        assertEquals("قطع ارتباط", ConnectionStatus.OFFLINE.titlePersian)
    }

    // =========================================================================
    // 3. Location Validity & Expiration / Stale Cutoff
    // =========================================================================
    @Test
    fun userLocation_validation_detectsValidAndInvalidFixes() {
        // Valid geodetic coordinates
        val valid = UserLocation(
            latitude = 28.5020,
            longitude = 53.5650,
            accuracyMeters = 4.5f,
            timestamp = 1730000000000L
        )
        assertTrue(valid.isValid)

        // Invalid latitude (> 90.0)
        val badLat = valid.copy(latitude = 95.0)
        assertFalse(badLat.isValid)

        // Invalid latitude (< -90.0)
        val badLatNeg = valid.copy(latitude = -91.0)
        assertFalse(badLatNeg.isValid)

        // Invalid longitude (> 180.0)
        val badLon = valid.copy(longitude = 181.0)
        assertFalse(badLon.isValid)

        // Invalid longitude (< -180.0)
        val badLonNeg = valid.copy(longitude = -181.0)
        assertFalse(badLonNeg.isValid)

        // Invalid negative accuracy
        val badAcc = valid.copy(accuracyMeters = -1.0f)
        assertFalse(badAcc.isValid)

        // Invalid non-positive timestamp
        val badTime = valid.copy(timestamp = 0L)
        assertFalse(badTime.isValid)
    }

    @Test
    fun groupMemberLocation_freshnessAndStaleCutoff_behavePredictably() {
        val now = 1000_000L
        val freshLocation = GroupMemberLocation(
            userId = "usr_001",
            callsign = "Alborz-1",
            teamId = "team_alpha",
            latitude = 28.5100,
            longitude = 53.5700,
            accuracyMeters = 5f,
            timestamp = now - 30_000L // 30 seconds ago
        )
        // With 60s threshold, 30s is fresh
        assertFalse(freshLocation.isStale(now = now, staleThresholdMs = 60_000L))

        val staleLocation = freshLocation.copy(timestamp = now - 65_000L) // 65 seconds ago
        assertTrue(staleLocation.isStale(now = now, staleThresholdMs = 60_000L))

        // Boundary test: exactly at threshold (60_000ms ago) -> not stale until strictly greater
        val boundaryLocation = freshLocation.copy(timestamp = now - 60_000L)
        assertFalse(boundaryLocation.isStale(now = now, staleThresholdMs = 60_000L))
    }

    // =========================================================================
    // 4. Point of Interest (POI) Validation & Categorization
    // =========================================================================
    @Test
    fun pointOfInterest_formattingAndCategories() {
        val poi = PointOfInterest(
            id = "poi_001",
            teamId = "team_alpha",
            title = "چشمه قلات جهرم",
            description = "آب گوارا و سرد",
            category = PointCategory.WATER_SOURCE,
            latitude = 28.512045,
            longitude = 53.583012,
            createdByUserId = "usr_001",
            createdByCallsign = "Leader-1",
            updatedByUserId = "usr_001",
            updatedByCallsign = "Leader-1",
            createdAt = 1730000000000L,
            updatedAt = 1730000000000L
        )

        assertEquals("منبع آب آشامیدنی", poi.category.displayNamePersian)
        assertEquals("28.5120°N, 53.5830°E", poi.formattedCoordinates)
        assertFalse(poi.isDeleted)
        assertEquals(1, poi.version)
    }

    // =========================================================================
    // 5. Geographic Math Calculations (Haversine & Bearing)
    // =========================================================================
    @Test
    fun geographicCalculations_distanceAndBearing_areAccurate() {
        // Point A: Trailhead in Jahrom (53.5650°E, 28.5020°N)
        // Point B: Water Spring (53.5830°E, 28.5120°N)
        val p1 = GeoPoint(53.5650, 28.5020)
        val p2 = GeoPoint(53.5830, 28.5120)

        val distance = HikingSimulationEngine.computeDistanceMeters(p1, p2)
        // Expected straight line distance ~2090 to ~2130 meters
        assertTrue("Distance should be ~2100m, was $distance", distance in 2050.0..2150.0)

        val bearing = HikingSimulationEngine.computeBearing(p1, p2)
        // Moving North-East: bearing must be between 40° and 75°
        assertTrue("Bearing should be North-East (~55°), was $bearing", bearing in 40f..75f)

        // Reverse bearing from p2 to p1 should be approximately bearing + 180°
        val reverseBearing = HikingSimulationEngine.computeBearing(p2, p1)
        val diff = (reverseBearing - bearing + 360f) % 360f
        assertTrue("Reverse bearing difference should be ~180°, was $diff", diff in 178f..182f)
    }

    @Test
    fun geographicInterpolation_alongRoutePolyline() {
        val coords = listOf(
            GeoPoint(53.0, 28.0),
            GeoPoint(53.0, 28.1), // 0.1 deg north (~11.1 km)
            GeoPoint(53.1, 28.1)  // 0.1 deg east
        )

        // Target distance 0: should return start
        val (startPos, startBearing) = HikingSimulationEngine.interpolatePositionAlongRoute(coords, 0.0)
        assertEquals(28.0, startPos.latitude, 0.0001)
        assertEquals(53.0, startPos.longitude, 0.0001)

        // Target distance half of segment 1 (~5550m): should be at ~28.05
        val seg1Dist = HikingSimulationEngine.computeDistanceMeters(coords[0], coords[1])
        val (midPos, _) = HikingSimulationEngine.interpolatePositionAlongRoute(coords, seg1Dist / 2.0)
        assertEquals(28.05, midPos.latitude, 0.005)

        // Target distance exceeding total polyline: should return last point
        val (endPos, _) = HikingSimulationEngine.interpolatePositionAlongRoute(coords, 999_999.0)
        assertEquals(coords.last().latitude, endPos.latitude, 0.0001)
        assertEquals(coords.last().longitude, endPos.longitude, 0.0001)
    }

    // =========================================================================
    // 6. Hiking Route & Waypoints Display Formatting
    // =========================================================================
    @Test
    fun hikingRoute_formattingDurationsAndWaypoints() {
        val routeLong = HikingRoute(
            id = "route_1",
            name = "مسیر کوهستانی",
            description = "مسیر آزمایشی",
            difficulty = HikingDifficulty.DIFFICULT,
            lengthKm = 12.5,
            elevationGainMeters = 850,
            estimatedTimeMinutes = 270, // 4 hours 30 mins
            coordinates = listOf(GeoPoint(53.5, 28.5))
        )
        assertEquals("4 ساعت و 30 دقیقه", routeLong.formattedDuration)

        val routeShort = routeLong.copy(estimatedTimeMinutes = 45)
        assertEquals("45 دقیقه", routeShort.formattedDuration)
    }

    // =========================================================================
    // 7. Team & Member Integrity Rules
    // =========================================================================
    @Test
    fun team_defaultsAndMemberCount() {
        val member1 = TeamMember(id = "m1", fullName = "علی کریمی", callsign = "البرز-۱", roleInTeam = "پیشرو")
        val member2 = TeamMember(id = "m2", fullName = "رضا احمدی", callsign = "البرز-۲", roleInTeam = "دیده‌بان")

        val team = Team(
            id = "team_1",
            name = "گشت امداد کوهستان",
            leadUserId = "usr_lead",
            vehicleType = "هایلوکس",
            vehiclePlate = "۴۴-ب-۱۲",
            equipmentSummary = "کیت تروما و بی‌سیم",
            members = listOf(member1, member2)
        )

        assertEquals(2, team.memberCount)
        assertTrue(team.isActive)
    }
}
