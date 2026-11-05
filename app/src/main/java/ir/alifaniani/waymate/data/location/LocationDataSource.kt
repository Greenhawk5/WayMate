package ir.alifaniani.waymate.data.location

import ir.alifaniani.waymate.domain.model.UserLocation
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for geodetic coordinate acquisition from system sensors or synthetic sources.
 */
interface LocationDataSource {

    /**
     * Continuous stream of raw location fixes captured from the active provider.
     */
    fun getLocationUpdates(): Flow<UserLocation>

    /**
     * Checks if at least one location provider (GPS or Network) is enabled on the device.
     */
    fun isGpsEnabled(): Boolean

    /**
     * Checks if runtime location permissions (FINE or COARSE) are granted by the user.
     */
    fun hasLocationPermission(): Boolean

    /**
     * Checks if high-precision runtime location permission (ACCESS_FINE_LOCATION) is granted.
     */
    fun hasFineLocation(): Boolean = false

    /**
     * Checks if approximate runtime location permission (ACCESS_COARSE_LOCATION) is granted.
     */
    fun hasCoarseLocation(): Boolean = false
}
