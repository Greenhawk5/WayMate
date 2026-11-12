package ir.alifaniani.waymate.ui.onboarding

import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying Onboarding data models and navigation destinations.
 */
class OnboardingNavigationTest {

    @Test
    fun appDestination_supportsOnboardingLoginAndHome() {
        val onboarding: AppDestination = AppDestination.Onboarding
        val login: AppDestination = AppDestination.Login
        val dummyUser = User(
            id = "u-test",
            username = "test_user",
            role = UserRole.COMMANDER,
            callsign = "EAGLE-1"
        )
        val home: AppDestination = AppDestination.Home(dummyUser)

        assertEquals(AppDestination.Onboarding, onboarding)
        assertEquals(AppDestination.Login, login)
        assertTrue(home is AppDestination.Home)
        assertEquals("test_user", (home as AppDestination.Home).user.username)
    }

    @Test
    fun onboardingSteps_haveValidContentAndDrawables() {
        val steps = listOf(
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_map_download,
                title = "نقشه‌های آفلاین و عوارض جهرم",
                description = "دسترسی پایدار به بیش از ۵,۴۰۰ عارضهٔ جغرافیایی، معابر شهری و خطوط مسیل شهرستان جهرم، بدون نیاز به اینترنت در ارتفاعات."
            ),
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_live_location,
                title = "اشتراک‌گذاری زنده موقعیت در گروه",
                description = "نمایش بلادرنگ و با رضایت صریح موقعیت مکانی اعضای گروه، مجهز به سرویس پس‌زمینهٔ ایمن و دکمهٔ توقف فوری در اعلان‌های گوشی."
            ),
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_compass_navigation,
                title = "جهت‌یابی قطب‌نما و هدایت هوشمند",
                description = "هدایت مسیر با محاسبهٔ دقیق فاصله، سرعت و سمت جغرافیایی روی بوم تاکتیکی نقشه به همراه علامت‌گذاری نشانه‌های کلیدی (POI)."
            )
        )

        assertEquals(3, steps.size)
        steps.forEach { step ->
            assertTrue("Title must not be empty", step.title.isNotBlank())
            assertTrue("Description must not be empty", step.description.isNotBlank())
            assertTrue("Drawable resource must be non-zero", step.illustrationRes != 0)
        }
    }
}

