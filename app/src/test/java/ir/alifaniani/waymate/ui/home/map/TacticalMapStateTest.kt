package ir.alifaniani.waymate.ui.home.map

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

class TacticalMapStateTest {

    @Test
    fun initialState_hasDefaultJahromCoordinatesAndSyntheticMarkers() {
        val state = TacticalMapUiState()

        assertEquals(28.5303, state.centerLatitude, 0.0001)
        assertEquals(53.6008, state.centerLongitude, 0.0001)
        assertEquals(14, state.zoomLevel)
        assertEquals(BaseMapPresentation.OFFLINE_TACTICAL, state.basePresentation)
        assertEquals(MapPresentationMode.TACTICAL_GRID, state.presentationMode)
        assertTrue(state.isOfflineSchematic)
        assertTrue(state.isSyntheticOnly)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertNull(state.selectedMarker)
        assertEquals(3, state.markers.size)
        assertTrue(state.layers.isNotEmpty())
    }

    @Test
    fun formattedCoordinates_formatsProperly() {
        val state = TacticalMapUiState(centerLatitude = 28.5303, centerLongitude = 53.6008)
        assertEquals("28.5303°N  53.6008°E", state.formattedCoordinates)
    }

    @Test
    fun syntheticMarkers_strictlyLabeledAsSimulated() {
        val markers = defaultJahromSyntheticMarkers
        assertTrue("Must have synthetic markers", markers.isNotEmpty())

        markers.forEach { marker ->
            assertTrue(
                "Marker ${marker.id} must be labeled as simulated",
                marker.callsign.contains("شبیه‌سازی‌شده")
            )
            assertTrue("Latitude must be valid WGS-84", marker.latitude in -90.0..90.0)
            assertTrue("Longitude must be valid WGS-84", marker.longitude in -180.0..180.0)
            assertNotNull("Tone must be defined", marker.tone)
        }
    }

    @Test
    fun markerSelection_updatesState() {
        var state = TacticalMapUiState()
        val targetMarker = state.markers.first()

        state = state.copy(selectedMarker = targetMarker)
        assertEquals(targetMarker.id, state.selectedMarker?.id)

        state = state.copy(selectedMarker = null)
        assertNull(state.selectedMarker)
    }

    @Test
    fun layerToggling_updatesIndividualLayerVisibility() {
        var state = TacticalMapUiState()

        assertTrue("Road layer should be enabled by default", state.isRoadOverlayEnabled)
        assertTrue("Waterway layer should be enabled by default", state.isWaterwayOverlayEnabled)
        assertTrue("Tactical grid should be enabled by default", state.isTacticalGridEnabled)
        assertTrue("Synthetic units should be enabled by default", state.isSyntheticUnitsEnabled)

        // Toggle road layer off
        state = state.toggleLayer("jahrom_road_network")
        assertFalse("Road layer should now be disabled", state.isRoadOverlayEnabled)
        assertTrue("Waterway layer should remain enabled", state.isWaterwayOverlayEnabled)

        // Toggle waterway layer off
        state = state.toggleLayer("jahrom_waterways")
        assertFalse("Waterway layer should now be disabled", state.isWaterwayOverlayEnabled)

        // Toggle road layer back on
        state = state.toggleLayer("jahrom_road_network")
        assertTrue("Road layer should be re-enabled", state.isRoadOverlayEnabled)
        assertFalse("Waterway layer should still be disabled", state.isWaterwayOverlayEnabled)

        // Toggle tactical grid off
        state = state.toggleLayer("tactical_grid")
        assertFalse("Grid should now be disabled", state.isTacticalGridEnabled)

        // Toggle synthetic units off
        state = state.toggleLayer("synthetic_units")
        assertFalse("Synthetic units should now be disabled", state.isSyntheticUnitsEnabled)
    }

    @Test
    fun unavailableLayers_cannotBeToggled() {
        var state = TacticalMapUiState()
        val initialCount = state.activeLayerCount

        // Attempt to toggle unavailable patrol_routes layer
        state = state.toggleLayer("patrol_routes")
        val patrolLayer = state.layers.find { it.id == "patrol_routes" }
        assertFalse("Unavailable layer must remain disabled", patrolLayer?.isEnabled == true)
        assertEquals("Active count must not change", initialCount, state.activeLayerCount)
    }

    @Test
    fun basePresentation_offlineTacticalVsUnconfiguredProviders() {
        var state = TacticalMapUiState()
        assertEquals(BaseMapPresentation.OFFLINE_TACTICAL, state.basePresentation)
        assertTrue(state.isOfflineSchematic)
        assertTrue(state.basePresentation.isConfigured)
        assertFalse(state.isSatelliteActive)
        assertNull(state.errorMessage)

        // Select Satellite Imagery (Unconfigured)
        state = state.selectBasePresentation(BaseMapPresentation.SATELLITE_IMAGERY)
        assertTrue(state.isSatelliteActive)
        assertFalse(state.isSatelliteConfigured)
        assertFalse(state.isOfflineSchematic)
        assertNotNull("Must set informative unconfigured message for satellite", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("ماهواره‌ای"))

        // Select Standard Street (Unconfigured)
        state = state.selectBasePresentation(BaseMapPresentation.STANDARD_STREET)
        assertEquals(BaseMapPresentation.STANDARD_STREET, state.basePresentation)
        assertFalse("Standard street must be unconfigured without local tile server", state.basePresentation.isConfigured)
        assertNotNull("Must set informative unconfigured message for street", state.errorMessage)
        assertTrue(state.errorMessage!!.contains("خیابانی"))

        // Switch back to Offline Tactical (Configured)
        state = state.selectBasePresentation(BaseMapPresentation.OFFLINE_TACTICAL)
        assertEquals(BaseMapPresentation.OFFLINE_TACTICAL, state.basePresentation)
        assertTrue(state.basePresentation.isConfigured)
        assertNull(state.errorMessage)
    }

    @Test
    fun errorState_allowsRecoveryToOfflineGrid() {
        var state = TacticalMapUiState(
            basePresentation = BaseMapPresentation.SATELLITE_IMAGERY,
            errorMessage = "سرویس ماهواره‌ای فعال نیست"
        )
        assertNotNull(state.errorMessage)

        // Recover to baseline
        state = state.copy(
            basePresentation = BaseMapPresentation.OFFLINE_TACTICAL,
            errorMessage = null
        )
        assertNull(state.errorMessage)
        assertTrue(state.isOfflineSchematic)
    }

    @Test
    fun fakeGisLayerRepository_loadsAuditedSegments() = runBlocking {
        val repository = FakeGisLayerRepository()
        val segments = repository.loadJahromVectors()

        assertTrue("Must load test segments", segments.isNotEmpty())
        assertEquals(3, segments.size)

        val roadSegment = segments.find { it.roadType == "جاده اصلی" }
        assertNotNull(roadSegment)
        assertFalse(roadSegment!!.isWaterway)

        val waterwaySegment = segments.find { it.isWaterway }
        assertNotNull(waterwaySegment)
        assertEquals("رودخانه", waterwaySegment!!.roadType)

        val bounds = repository.getBoundingBox()
        assertEquals(53.4523, bounds.minLongitude, 0.001)
        assertEquals(28.4391, bounds.minLatitude, 0.001)
        assertEquals(53.7494, bounds.maxLongitude, 0.001)
        assertEquals(28.6215, bounds.maxLatitude, 0.001)
    }

    @Test
    fun assetGisLayerRepository_loadsCompleteDatasetFromJsonStream() {
        val assetFile = File("src/main/assets/gis/jahrom/jahrom_vectors.json")
        assertTrue("Complete dataset asset file must exist", assetFile.exists())

        val segments = FileInputStream(assetFile).use { stream ->
            AssetGisLayerRepository.parseJsonStream(stream)
        }

        // Verify total feature count
        assertEquals("Must load all 5,459 audited features", 5459, segments.size)

        // Verify road and waterway counts
        val roadSegments = segments.filter { !it.isWaterway }
        val waterwaySegments = segments.filter { it.isWaterway }

        assertEquals("Must have exactly 5,351 road features", 5351, roadSegments.size)
        assertEquals("Must have exactly 108 waterway features", 108, waterwaySegments.size)

        // Verify coordinate bounds are strictly within Jahrom geographic envelope
        segments.forEach { segment ->
            assertTrue("Segment ${segment.id} must have at least 2 points", segment.points.size >= 2)
            segment.points.forEach { pt ->
                assertTrue(
                    "Longitude ${pt.longitude} must fall within Jahrom extent",
                    pt.longitude in 53.45..53.75
                )
                assertTrue(
                    "Latitude ${pt.latitude} must fall within Jahrom extent",
                    pt.latitude in 28.43..28.63
                )
            }
        }

        // Verify Persian names and UTF-8 encoding integrity
        val ringRoad = roadSegments.find { it.name == "کمربندی جهرم" }
        assertNotNull("Must contain Jahrom Ring Road with correct Persian encoding", ringRoad)

        val riverFeature = waterwaySegments.find { it.name == "رودخانه محله صحرا" }
        assertNotNull("Must contain Sahra waterway with correct Persian encoding", riverFeature)
    }

    @Test
    fun assetGisLayerRepository_handlesEmptyAndMalformedStreamGracefully() {
        // Empty stream
        val emptyStream = ByteArrayInputStream("".toByteArray())
        val emptyResult = AssetGisLayerRepository.parseJsonStream(emptyStream)
        assertTrue("Empty stream should return empty list", emptyResult.isEmpty())

        // Blank stream
        val blankStream = ByteArrayInputStream("   ".toByteArray())
        val blankResult = AssetGisLayerRepository.parseJsonStream(blankStream)
        assertTrue("Blank stream should return empty list", blankResult.isEmpty())
    }

    @Test
    fun hikingRoutesLayer_enabledByDefaultAndCanBeToggled() {
        var state = TacticalMapUiState()
        assertTrue("Hiking routes layer should be enabled by default", state.isHikingRoutesEnabled)
        assertTrue("Hiking routes should be loaded", state.hikingRoutes.isNotEmpty())

        state = state.toggleLayer("hiking_routes")
        assertFalse("Hiking routes layer should be disabled after toggle", state.isHikingRoutesEnabled)

        state = state.toggleLayer("hiking_routes")
        assertTrue("Hiking routes layer should be re-enabled after second toggle", state.isHikingRoutesEnabled)
    }

    @Test
    fun simulatedHikerState_storesCoordinatesAndBearing() {
        var state = TacticalMapUiState()
        assertNull(state.simulatedHikerPosition)
        assertEquals(0f, state.simulatedHikerBearing, 0.001f)

        state = state.copy(
            simulatedHikerPosition = GeoPoint(53.6008, 28.5303),
            simulatedHikerBearing = 145f
        )
        assertNotNull(state.simulatedHikerPosition)
        assertEquals(53.6008, state.simulatedHikerPosition!!.longitude, 0.0001)
        assertEquals(28.5303, state.simulatedHikerPosition!!.latitude, 0.0001)
        assertEquals(145f, state.simulatedHikerBearing, 0.001f)
    }

    @Test
    fun pointOfInterestSelection_updatesSelectedPointAndCoordinates() {
        val testPoint = ir.alifaniani.waymate.domain.model.PointOfInterest(
            id = "poi_test",
            teamId = "team_alpha",
            title = "چشمه سنگی",
            description = "محل استراحت",
            category = ir.alifaniani.waymate.domain.model.PointCategory.WATER_SOURCE,
            latitude = 28.5300,
            longitude = 53.6000,
            createdByUserId = "u1",
            createdByCallsign = "راهنما ۱",
            updatedByUserId = "u1",
            updatedByCallsign = "راهنما ۱",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        var state = TacticalMapUiState(pointsOfInterest = listOf(testPoint))
        assertNull(state.selectedPointOfInterest)

        state = state.copy(selectedPointOfInterest = testPoint)
        assertEquals("poi_test", state.selectedPointOfInterest?.id)
        assertEquals("چشمه سنگی", state.selectedPointOfInterest?.title)
    }

    @Test
    fun groupMemberLocations_layerTogglingAndSelection() {
        val member = ir.alifaniani.waymate.domain.model.GroupMemberLocation(
            userId = "usr_42",
            callsign = "زاگرس",
            teamId = "team_alpha",
            latitude = 28.5350,
            longitude = 53.6050,
            accuracyMeters = 4.5f,
            timestamp = System.currentTimeMillis()
        )

        var state = TacticalMapUiState(groupMemberLocations = listOf(member))
        assertTrue("Group locations layer should be enabled by default", state.isGroupLocationsEnabled)
        assertNull("No member selected initially", state.selectedGroupMember)
        assertEquals(1, state.groupMemberLocations.size)

        // Select member
        state = state.copy(selectedGroupMember = member)
        assertEquals("usr_42", state.selectedGroupMember?.userId)
        assertEquals("زاگرس", state.selectedGroupMember?.callsign)

        // Toggle layer off
        state = state.toggleLayer("live_group_locations")
        assertFalse("Group locations layer should now be disabled", state.isGroupLocationsEnabled)

        // Toggle layer back on
        state = state.toggleLayer("live_group_locations")
        assertTrue("Group locations layer should be re-enabled", state.isGroupLocationsEnabled)
    }

    @Test
    fun mapCoordinateTransformation_rotationInvariance() {
        val centerLat = 28.5303
        val centerLon = 53.6008
        val zoomLevel = 14
        val userZoomScale = 1.5f
        val effectiveZoom = (zoomLevel.toFloat() / 14f) * userZoomScale
        val scale = 35000f * effectiveZoom
        val cosLat = kotlin.math.cos(Math.toRadians(centerLat)).toFloat()

        val targetLat = 28.5350
        val targetLon = 53.6050

        val dx = (targetLon - centerLon).toFloat() * scale * cosLat
        val dy = -(targetLat - centerLat).toFloat() * scale

        // Test with different rotation angles: 0, 45, 90, 180, 270 degrees
        val testAngles = listOf(0f, 45f, 90f, 135f, 180f, 270f, 315f)
        for (rotationDegrees in testAngles) {
            val rad = Math.toRadians(rotationDegrees.toDouble())
            // Forward rotation
            val rotX = (dx * kotlin.math.cos(rad) - dy * kotlin.math.sin(rad)).toFloat()
            val rotY = (dx * kotlin.math.sin(rad) + dy * kotlin.math.cos(rad)).toFloat()

            // Inverse (unrotation)
            val unrotX = (rotX * kotlin.math.cos(rad) + rotY * kotlin.math.sin(rad)).toFloat()
            val unrotY = (-rotX * kotlin.math.sin(rad) + rotY * kotlin.math.cos(rad)).toFloat()

            assertEquals("Unrotated X must match original dx for angle $rotationDegrees", dx, unrotX, 0.01f)
            assertEquals("Unrotated Y must match original dy for angle $rotationDegrees", dy, unrotY, 0.01f)
        }
    }

    @Test
    fun mapCoordinateTransformation_scaleAndZoomBounds() {
        val baseZoom = 14
        val minScale = 0.4f
        val maxScale = 3.5f

        val effectiveMinZoom = (baseZoom.toFloat() / 14f) * minScale
        val effectiveMaxZoom = (baseZoom.toFloat() / 14f) * maxScale

        assertEquals(0.4f, effectiveMinZoom, 0.001f)
        assertEquals(3.5f, effectiveMaxZoom, 0.001f)

        val baseScale = 35000f * (baseZoom.toFloat() / 14f)
        assertEquals(35000f, baseScale, 0.01f)
    }

    @Test
    fun groupMemberLocation_stalenessCalculation() {
        val now = 1700000100000L
        val freshMember = ir.alifaniani.waymate.domain.model.GroupMemberLocation(
            userId = "usr_01",
            callsign = "شاهین",
            teamId = "team_alpha",
            latitude = 28.53,
            longitude = 53.60,
            accuracyMeters = 5f,
            timestamp = now - 30_000L
        )
        assertFalse("30s old fix should not be stale with 60s threshold", freshMember.isStale(now, 60_000L))

        val staleMember = freshMember.copy(timestamp = now - 75_000L)
        assertTrue("75s old fix must be marked stale with 60s threshold", staleMember.isStale(now, 60_000L))
    }
}
