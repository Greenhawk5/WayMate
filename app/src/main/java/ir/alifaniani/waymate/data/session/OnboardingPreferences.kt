package ir.alifaniani.waymate.data.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Storage contract for first-launch onboarding walkthrough completion state.
 */
interface OnboardingStorage {
    var isOnboardingCompleted: Boolean
    fun reset()
}

/**
 * Android SharedPreferences-backed implementation of OnboardingStorage.
 */
class OnboardingPreferences(context: Context) : OnboardingStorage {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPLETED, value).apply()

    override fun reset() {
        prefs.edit().remove(KEY_COMPLETED).apply()
    }

    companion object {
        private const val PREFS_NAME = "waymate_onboarding_prefs"
        private const val KEY_COMPLETED = "is_onboarding_completed"
    }
}

/**
 * In-memory test implementation of OnboardingStorage for unit testing.
 */
class InMemoryOnboardingStorage(
    initialState: Boolean = false
) : OnboardingStorage {
    override var isOnboardingCompleted: Boolean = initialState

    override fun reset() {
        isOnboardingCompleted = false
    }
}
