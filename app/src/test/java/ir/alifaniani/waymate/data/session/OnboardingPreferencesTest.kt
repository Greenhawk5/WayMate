package ir.alifaniani.waymate.data.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying OnboardingStorage contracts and state persistence.
 */
class OnboardingPreferencesTest {

    private lateinit var storage: OnboardingStorage

    @Before
    fun setup() {
        storage = InMemoryOnboardingStorage()
    }

    @Test
    fun initialState_isNotCompleted() {
        assertFalse("New install must default to onboarding not completed", storage.isOnboardingCompleted)
    }

    @Test
    fun completeOnboarding_persistsTrue() {
        storage.isOnboardingCompleted = true
        assertTrue(storage.isOnboardingCompleted)
    }

    @Test
    fun reset_clearsCompletionState() {
        storage.isOnboardingCompleted = true
        assertTrue(storage.isOnboardingCompleted)

        storage.reset()
        assertFalse(storage.isOnboardingCompleted)
    }
}

