package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.BuildConfig

/**
 * Configuration for backend API connectivity.
 *
 * Supports switching or overriding base URLs for Android Emulator (10.0.2.2),
 * Physical Device over USB adb reverse (127.0.0.1), and local LAN IP.
 */
object ApiConfig {

    /**
     * Default base URL configured via buildConfig (deployed Cloudflare Worker).
     */
    val DEFAULT_BASE_URL: String = BuildConfig.DEFAULT_BASE_URL

    /**
     * Optional standard URL for Android Emulator accessing host computer locally.
     */
    const val EMULATOR_LOCAL_URL = "http://10.0.2.2:8787/api/v1/"

    /**
     * Optional standard URL for Physical Device connected via USB with `adb reverse tcp:8787 tcp:8787`.
     */
    const val PHYSICAL_DEVICE_LOCAL_URL = "http://127.0.0.1:8787/api/v1/"

    @Volatile
    private var customBaseUrl: String? = null

    val isEmulator: Boolean by lazy {
        android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
    }

    /**
     * Returns the effective API base URL.
     * By default, returns the deployed Cloudflare Worker HTTPS endpoint for all devices.
     */
    fun getBaseUrl(): String {
        return customBaseUrl ?: DEFAULT_BASE_URL
    }

    fun setCustomBaseUrl(url: String?) {
        customBaseUrl = if (url != null && !url.endsWith("/")) "$url/" else url
    }

    fun useLocalDevelopment(isEmulatorTarget: Boolean = isEmulator) {
        customBaseUrl = if (isEmulatorTarget) EMULATOR_LOCAL_URL else PHYSICAL_DEVICE_LOCAL_URL
    }

    fun resetToDefault() {
        customBaseUrl = null
    }
}
