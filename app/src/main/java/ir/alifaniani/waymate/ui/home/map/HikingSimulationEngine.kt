package ir.alifaniani.waymate.ui.home.map

import ir.alifaniani.waymate.domain.model.HikingRoute
import ir.alifaniani.waymate.domain.model.HikingWaypoint
import ir.alifaniani.waymate.domain.model.defaultJahromHikingRoutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Immutable state of the deterministic hiking movement simulation.
 * Strictly educational and development-only.
 */
data class HikingSimulationState(
    val route: HikingRoute,
    val currentPosition: GeoPoint,
    val currentBearing: Float = 0f,
    val progressFraction: Float = 0f,
    val distanceCoveredKm: Double = 0.0,
    val isPlaying: Boolean = false,
    val speedMultiplier: Float = 1.0f,
    val nextWaypoint: HikingWaypoint? = null,
    val isFinished: Boolean = false,
    val isSynthetic: Boolean = true
) {
    val formattedProgress: String
        get() = String.format("%.1f / %.1f کیلومتر (%d٪)", distanceCoveredKm, route.lengthKm, (progressFraction * 100).toInt())
}

/**
 * Deterministic location simulation engine for hiking trails and outdoor exploration.
 *
 * Simulates continuous or stepped hiker movement along predefined route coordinates without
 * accessing hardware GPS or broadcasting coordinates over the network.
 */
class HikingSimulationEngine(
    initialRoute: HikingRoute = defaultJahromHikingRoutes.first()
) {

    private val _state = MutableStateFlow(createInitialState(initialRoute))
    val state: StateFlow<HikingSimulationState> = _state.asStateFlow()

    private var tickerJob: Job? = null

    /**
     * Starts continuous background simulation stepping at 500ms intervals.
     */
    fun start(scope: CoroutineScope) {
        if (_state.value.isPlaying) return
        _state.update { it.copy(isPlaying = true) }

        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _state.value.isPlaying) {
                delay(500L)
                step(timeDeltaSeconds = 0.5f)
            }
        }
    }

    /**
     * Pauses the simulation while preserving current progress and position.
     */
    fun pause() {
        tickerJob?.cancel()
        tickerJob = null
        _state.update { it.copy(isPlaying = false) }
    }

    /**
     * Resets the simulation to the beginning of the active route.
     */
    fun reset() {
        pause()
        _state.update { createInitialState(it.route) }
    }

    /**
     * Sets the active hiking route and resets progress.
     */
    fun selectRoute(route: HikingRoute) {
        pause()
        _state.update { createInitialState(route) }
    }

    /**
     * Configures the speed multiplier (e.g. 1.0x, 2.0x, 5.0x).
     */
    fun setSpeedMultiplier(multiplier: Float) {
        _state.update { it.copy(speedMultiplier = multiplier.coerceIn(0.5f, 10.0f)) }
    }

    /**
     * Advances the simulation by a discrete time delta.
     * Accessible directly for deterministic, synchronous unit testing without coroutine delays.
     */
    fun step(timeDeltaSeconds: Float = 1.0f) {
        val current = _state.value
        if (current.isFinished) return

        val coords = current.route.coordinates
        if (coords.size < 2) return

        // Baseline walking speed: 4.5 km/h = 1.25 m/s
        val baseSpeedMetersPerSecond = 1.25
        val stepDistanceMeters = baseSpeedMetersPerSecond * current.speedMultiplier * timeDeltaSeconds

        val totalRouteMeters = current.route.lengthKm * 1000.0
        val currentDistanceMeters = current.distanceCoveredKm * 1000.0
        val newDistanceMeters = (currentDistanceMeters + stepDistanceMeters).coerceAtMost(totalRouteMeters)

        val isFinished = newDistanceMeters >= totalRouteMeters
        val progress = if (totalRouteMeters > 0) (newDistanceMeters / totalRouteMeters).toFloat() else 1f

        // Compute new interpolated coordinate and bearing along polyline
        val (newPos, bearing) = interpolatePositionAlongRoute(coords, newDistanceMeters)

        // Find upcoming waypoint
        val nextWp = current.route.waypoints.firstOrNull { wp ->
            computeDistanceMeters(newPos, GeoPoint(wp.longitude, wp.latitude)) > 15.0 &&
                    isWaypointAhead(coords, newPos, GeoPoint(wp.longitude, wp.latitude))
        }

        _state.update {
            it.copy(
                currentPosition = newPos,
                currentBearing = bearing,
                progressFraction = progress,
                distanceCoveredKm = newDistanceMeters / 1000.0,
                nextWaypoint = nextWp,
                isFinished = isFinished,
                isPlaying = if (isFinished) false else it.isPlaying
            )
        }

        if (isFinished) {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun createInitialState(route: HikingRoute): HikingSimulationState {
        val startPos = route.coordinates.firstOrNull() ?: GeoPoint(53.6008, 28.5303)
        val initialBearing = if (route.coordinates.size >= 2) {
            computeBearing(route.coordinates[0], route.coordinates[1])
        } else 0f

        return HikingSimulationState(
            route = route,
            currentPosition = startPos,
            currentBearing = initialBearing,
            progressFraction = 0f,
            distanceCoveredKm = 0.0,
            isPlaying = false,
            speedMultiplier = 1.0f,
            nextWaypoint = route.waypoints.getOrNull(0),
            isFinished = false
        )
    }

    companion object {
        /**
         * Calculates great-circle distance between two coordinates in meters using the Haversine formula.
         */
        fun computeDistanceMeters(p1: GeoPoint, p2: GeoPoint): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(p2.latitude - p1.latitude)
            val dLon = Math.toRadians(p2.longitude - p1.longitude)
            val lat1 = Math.toRadians(p1.latitude)
            val lat2 = Math.toRadians(p2.latitude)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                    sin(dLon / 2) * sin(dLon / 2) * cos(lat1) * cos(lat2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        /**
         * Calculates the forward azimuth / bearing in degrees (0..360) from p1 to p2.
         */
        fun computeBearing(p1: GeoPoint, p2: GeoPoint): Float {
            val lat1 = Math.toRadians(p1.latitude)
            val lat2 = Math.toRadians(p2.latitude)
            val dLon = Math.toRadians(p2.longitude - p1.longitude)

            val y = sin(dLon) * cos(lat2)
            val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
            val initialBearing = Math.toDegrees(atan2(y, x))
            return ((initialBearing + 360.0) % 360.0).toFloat()
        }

        /**
         * Finds position and bearing at a given distance along a polyline.
         */
        fun interpolatePositionAlongRoute(
            coords: List<GeoPoint>,
            targetDistanceMeters: Double
        ): Pair<GeoPoint, Float> {
            if (coords.isEmpty()) return Pair(GeoPoint(0.0, 0.0), 0f)
            if (coords.size == 1 || targetDistanceMeters <= 0.0) {
                val bearing = if (coords.size >= 2) computeBearing(coords[0], coords[1]) else 0f
                return Pair(coords[0], bearing)
            }

            var accumulatedDistance = 0.0
            for (i in 0 until coords.size - 1) {
                val segStart = coords[i]
                val segEnd = coords[i + 1]
                val segDistance = computeDistanceMeters(segStart, segEnd)

                if (accumulatedDistance + segDistance >= targetDistanceMeters) {
                    val remaining = targetDistanceMeters - accumulatedDistance
                    val fraction = if (segDistance > 0.0) (remaining / segDistance).coerceIn(0.0, 1.0) else 0.0
                    val lat = segStart.latitude + fraction * (segEnd.latitude - segStart.latitude)
                    val lon = segStart.longitude + fraction * (segEnd.longitude - segStart.longitude)
                    val bearing = computeBearing(segStart, segEnd)
                    return Pair(GeoPoint(lon, lat), bearing)
                }
                accumulatedDistance += segDistance
            }

            val lastIdx = coords.size - 1
            val lastBearing = computeBearing(coords[lastIdx - 1], coords[lastIdx])
            return Pair(coords[lastIdx], lastBearing)
        }

        private fun isWaypointAhead(coords: List<GeoPoint>, currentPos: GeoPoint, waypointPos: GeoPoint): Boolean {
            // Find closest segment to currentPos and waypointPos
            val currentIdx = coords.indexOfFirst { computeDistanceMeters(it, currentPos) < 200.0 }
            val waypointIdx = coords.indexOfFirst { computeDistanceMeters(it, waypointPos) < 200.0 }
            return waypointIdx >= currentIdx
        }
    }
}
