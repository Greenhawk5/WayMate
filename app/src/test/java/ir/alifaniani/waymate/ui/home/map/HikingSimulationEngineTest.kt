package ir.alifaniani.waymate.ui.home.map

import ir.alifaniani.waymate.domain.model.HikingDifficulty
import ir.alifaniani.waymate.domain.model.HikingRoute
import ir.alifaniani.waymate.domain.model.HikingWaypoint
import ir.alifaniani.waymate.domain.model.WaypointType
import ir.alifaniani.waymate.domain.model.defaultJahromHikingRoutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for deterministic hiking simulation engine and coordinate calculations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HikingSimulationEngineTest {

    private val testRoute = HikingRoute(
        id = "test_trail",
        name = "مسیر آزمایشی کوهستان",
        description = "مسیر مستقیم برای ارزیابی محاسبات ژئودتیک",
        difficulty = HikingDifficulty.EASY,
        lengthKm = 1.0, // 1000 meters
        elevationGainMeters = 50,
        estimatedTimeMinutes = 20,
        coordinates = listOf(
            GeoPoint(53.6000, 28.5000), // Start (0m)
            GeoPoint(53.6000, 28.5045), // ~500m North
            GeoPoint(53.6000, 28.5090)  // ~1000m North
        ),
        waypoints = listOf(
            HikingWaypoint(
                id = "wp_0",
                routeId = "test_trail",
                title = "مبدأ",
                description = "شروع مسیر",
                type = WaypointType.TRAILHEAD,
                latitude = 28.5000,
                longitude = 53.6000,
                elevationMeters = 1000,
                orderIndex = 0
            ),
            HikingWaypoint(
                id = "wp_mid",
                routeId = "test_trail",
                title = "چشمه میانی",
                description = "آب آشامیدنی",
                type = WaypointType.WATER_SPRING,
                latitude = 28.5045,
                longitude = 53.6000,
                elevationMeters = 1025,
                orderIndex = 1
            ),
            HikingWaypoint(
                id = "wp_summit",
                routeId = "test_trail",
                title = "قله",
                description = "انتهای مسیر",
                type = WaypointType.SUMMIT,
                latitude = 28.5090,
                longitude = 53.6000,
                elevationMeters = 1050,
                orderIndex = 2
            )
        )
    )

    @Test
    fun initialState_beginsAtFirstCoordinateWithZeroProgress() {
        val engine = HikingSimulationEngine(initialRoute = testRoute)
        val state = engine.state.value

        assertEquals(testRoute.id, state.route.id)
        assertEquals(53.6000, state.currentPosition.longitude, 0.0001)
        assertEquals(28.5000, state.currentPosition.latitude, 0.0001)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertEquals(0.0, state.distanceCoveredKm, 0.001)
        assertFalse(state.isPlaying)
        assertFalse(state.isFinished)
        assertTrue(state.isSynthetic)
    }

    @Test
    fun distanceAndBearingCalculations_accurateForCardinalDirections() {
        val p1 = GeoPoint(53.6000, 28.5000)
        val pNorth = GeoPoint(53.6000, 28.5100)
        val pEast = GeoPoint(53.6100, 28.5000)

        // North bearing should be approximately 0 degrees
        val bearingNorth = HikingSimulationEngine.computeBearing(p1, pNorth)
        assertEquals(0.0, bearingNorth.toDouble(), 1.0)

        // East bearing should be approximately 90 degrees
        val bearingEast = HikingSimulationEngine.computeBearing(p1, pEast)
        assertEquals(90.0, bearingEast.toDouble(), 1.0)

        // Distance should be non-zero and reasonable
        val distNorth = HikingSimulationEngine.computeDistanceMeters(p1, pNorth)
        assertTrue("1 degree latitude is ~111km, so 0.01 deg is ~1110m", distNorth in 1000.0..1200.0)
    }

    @Test
    fun step_advancesProgressDeterministically() {
        val engine = HikingSimulationEngine(initialRoute = testRoute)

        // Walking baseline is 1.25 m/s. 100 seconds at 1x = 125 meters.
        engine.step(timeDeltaSeconds = 100f)

        val state = engine.state.value
        assertEquals(0.125, state.distanceCoveredKm, 0.005)
        assertEquals(0.125f, state.progressFraction, 0.005f)
        assertFalse(state.isFinished)
        // Position moved North along longitude 53.6000
        assertEquals(53.6000, state.currentPosition.longitude, 0.001)
        assertTrue("Latitude should have increased northward", state.currentPosition.latitude > 28.5000)
    }

    @Test
    fun speedMultiplier_scalesSteppingDistanceProportionately() {
        val engine1x = HikingSimulationEngine(initialRoute = testRoute)
        engine1x.setSpeedMultiplier(1.0f)
        engine1x.step(timeDeltaSeconds = 50f)

        val engine2x = HikingSimulationEngine(initialRoute = testRoute)
        engine2x.setSpeedMultiplier(2.0f)
        engine2x.step(timeDeltaSeconds = 50f)

        assertEquals(
            engine1x.state.value.distanceCoveredKm * 2.0,
            engine2x.state.value.distanceCoveredKm,
            0.002
        )
    }

    @Test
    fun step_reachesEndOfRouteAndSetsFinished() {
        val engine = HikingSimulationEngine(initialRoute = testRoute)

        // Advance 1000 seconds (1250 meters > 1000m route length)
        engine.step(timeDeltaSeconds = 1000f)

        val state = engine.state.value
        assertTrue("Must be marked finished when reaching end", state.isFinished)
        assertEquals(1.0f, state.progressFraction, 0.001f)
        assertEquals(1.0, state.distanceCoveredKm, 0.001)

        // Stepping further should not exceed route bounds
        engine.step(timeDeltaSeconds = 100f)
        assertEquals(1.0f, engine.state.value.progressFraction, 0.001f)
    }

    @Test
    fun reset_returnsToStart() {
        val engine = HikingSimulationEngine(initialRoute = testRoute)
        engine.step(timeDeltaSeconds = 200f)

        assertTrue(engine.state.value.distanceCoveredKm > 0.0)

        engine.reset()

        val state = engine.state.value
        assertEquals(0.0, state.distanceCoveredKm, 0.001)
        assertEquals(0f, state.progressFraction, 0.001f)
        assertEquals(28.5000, state.currentPosition.latitude, 0.0001)
        assertEquals(53.6000, state.currentPosition.longitude, 0.0001)
        assertFalse(state.isPlaying)
    }

    @Test
    fun selectRoute_switchesActiveRouteAndResetsProgress() {
        val secondRoute = defaultJahromHikingRoutes.last()
        val engine = HikingSimulationEngine(initialRoute = testRoute)
        engine.step(timeDeltaSeconds = 100f)

        engine.selectRoute(secondRoute)

        val state = engine.state.value
        assertEquals(secondRoute.id, state.route.id)
        assertEquals(0.0, state.distanceCoveredKm, 0.001)
        assertEquals(secondRoute.coordinates.first().longitude, state.currentPosition.longitude, 0.0001)
        assertEquals(secondRoute.coordinates.first().latitude, state.currentPosition.latitude, 0.0001)
    }

    @Test
    fun defaultJahromHikingRoutes_allRoutesHaveValidCoordinatesAndWaypoints() {
        val routes = defaultJahromHikingRoutes
        assertTrue("Must provide synthetic hiking routes", routes.isNotEmpty())

        routes.forEach { route ->
            assertTrue("Route must be synthetic", route.isSynthetic)
            assertTrue("Route length must be positive", route.lengthKm > 0.0)
            assertTrue("Route coordinates must form polyline", route.coordinates.size >= 2)
            assertTrue("Elevation gain must be non-negative", route.elevationGainMeters >= 0)

            route.coordinates.forEach { pt ->
                assertTrue("Longitude within Jahrom region", pt.longitude in 53.4..53.8)
                assertTrue("Latitude within Jahrom region", pt.latitude in 28.3..28.7)
            }

            route.waypoints.forEach { wp ->
                assertNotNull("Waypoint type must be specified", wp.type)
                assertTrue("Waypoint title must not be empty", wp.title.isNotBlank())
                assertTrue("Waypoint lat within bounds", wp.latitude in 28.3..28.7)
                assertTrue("Waypoint lon within bounds", wp.longitude in 53.4..53.8)
            }
        }
    }
}
