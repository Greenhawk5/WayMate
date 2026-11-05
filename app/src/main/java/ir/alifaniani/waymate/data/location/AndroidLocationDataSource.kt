package ir.alifaniani.waymate.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import ir.alifaniani.waymate.domain.model.UserLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Concrete Android [LocationDataSource] implementation using standard framework [LocationManager].
 *
 * Adheres to sovereign architectural constraints:
 * - Zero Google Play Services dependencies (runs on standard, de-Googled, or offline Android devices)
 * - Progressive permissions: safely handles fine vs. coarse runtime grants without throwing SecurityException
 * - Battery-conscious sampling interval: 5 seconds / 2 meters for outdoor hiking
 * - Graceful fallback between GPS and Network provider
 * - Dynamic provider state observation for GPS toggles during active sessions
 */
class AndroidLocationDataSource(
    private val context: Context,
    private val minTimeMs: Long = 5000L,
    private val minDistanceMeters: Float = 2.0f
) : LocationDataSource {

    private val locationManager by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun hasLocationPermission(): Boolean {
        return hasFineLocation() || hasCoarseLocation()
    }

    override fun hasFineLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun hasCoarseLocation(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun isGpsEnabled(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<UserLocation> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val fineGranted = hasFineLocation()
        val coarseGranted = hasCoarseLocation()

        // Safely emit cached last known location without throwing SecurityException
        try {
            val lastGps = if (fineGranted && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null

            val lastNet = if ((fineGranted || coarseGranted) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null

            val bestInitial = when {
                lastGps != null && lastNet != null -> if (lastGps.time >= lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            if (bestInitial != null) {
                trySend(bestInitial.toUserLocation())
            }
        } catch (_: Exception) {
            // Ignore initial cache read failure
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val userLocation = location.toUserLocation()
                if (userLocation.isValid) {
                    trySend(userLocation)
                }
            }

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {
                if (!isGpsEnabled()) {
                    close(IllegalStateException("All location providers disabled"))
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        val looper = Looper.myLooper() ?: Looper.getMainLooper()
        var registeredAny = false

        try {
            // Request GPS provider only if high-accuracy FINE permission is explicitly granted
            if (fineGranted && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTimeMs,
                    minDistanceMeters,
                    listener,
                    looper
                )
                registeredAny = true
            }

            // Fallback or companion Network provider (permitted with COARSE or FINE)
            if ((fineGranted || coarseGranted) && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    minTimeMs,
                    minDistanceMeters,
                    listener,
                    looper
                )
                registeredAny = true
            }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        if (!registeredAny) {
            close(IllegalStateException("No location provider available for current permissions and device settings"))
            return@callbackFlow
        }

        // Register broadcast receiver to detect user toggling GPS off in quick settings
        val providerReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                    if (!isGpsEnabled()) {
                        close(IllegalStateException("GPS disabled by user in device settings"))
                    }
                }
            }
        }

        try {
            val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
            context.registerReceiver(providerReceiver, filter)
        } catch (_: Exception) {
            // BroadcastReceiver registration failure should not kill updates
        }

        awaitClose {
            try {
                locationManager.removeUpdates(listener)
            } catch (_: Exception) {}
            try {
                context.unregisterReceiver(providerReceiver)
            } catch (_: Exception) {}
        }
    }

    private fun Location.toUserLocation(): UserLocation {
        return UserLocation(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracy,
            altitudeMeters = if (hasAltitude()) altitude else null,
            speedMps = if (hasSpeed()) speed else null,
            bearingDegrees = if (hasBearing()) bearing else null,
            timestamp = time,
            provider = provider
        )
    }
}
