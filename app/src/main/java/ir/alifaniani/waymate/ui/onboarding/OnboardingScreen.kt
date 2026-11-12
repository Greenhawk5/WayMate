package ir.alifaniani.waymate.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateOnCyan
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary
import kotlinx.coroutines.launch

/**
 * Data model for onboarding feature presentation slides.
 * Preserved for functional and test parity.
 */
data class OnboardingStep(
    val illustrationRes: Int,
    val title: String,
    val description: String
)

/**
 * Refined WayMate Onboarding Experience.
 *
 * Major UX Corrections:
 * 1. Fixed Illustration Card Framing: Removed outer artificial borders, shadows, and letterbox gaps.
 *    The artwork renders cleanly as a self-contained card with embedded frame and indicators.
 * 2. High-Visibility Skip Button: Styled with a restrained crimson tint and refined border for optimal contrast.
 * 3. Correct Slide Ordering:
 *    - Slide 1: همراه مطمئن (Compass, radio, tactical map) -> rightmost dot glowing in RTL
 *    - Slide 2: هماهنگی و موقعیت زنده (Team location markers) -> middle dot glowing in RTL
 *    - Slide 3: نقشه همیشه در دسترس (Offline map download) -> leftmost dot glowing in RTL
 * 4. Native HorizontalPager touch navigation supporting fluid swipe gestures and button navigation.
 */
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_compass_navigation,
                title = "همراه مطمئن در شرایط عملیاتی",
                description = "هدایت مسیر با محاسبهٔ دقیق فاصله، سرعت و سمت جغرافیایی روی بوم تاکتیکی نقشه به همراه علامت‌گذاری نشانه‌های کلیدی (POI)."
            ),
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_live_location,
                title = "هماهنگی و موقعیت زنده در گروه",
                description = "نمایش بلادرنگ و با رضایت صریح موقعیت مکانی اعضای گروه، مجهز به سرویس پس‌زمینهٔ ایمن و دکمهٔ توقف فوری در اعلان‌های گوشی."
            ),
            OnboardingStep(
                illustrationRes = R.drawable.img_onboarding_map_download,
                title = "نقشه‌های آفلاین و همیشه در دسترس",
                description = "دسترسی پایدار به بیش از ۵,۴۰۰ عارضهٔ جغرافیایی، معابر شهری و خطوط مسیل شهرستان جهرم، بدون نیاز به اینترنت در ارتفاعات."
            )
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val isLastStep = pagerState.currentPage == steps.size - 1

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Skip Button with refined, tactical red accent for clear visibility
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onFinishOnboarding,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color(0x24EF4444),
                        contentColor = Color(0xFFFF808F)
                    ),
                    border = BorderStroke(1.dp, Color(0x66EF4444)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "رد شدن",
                            color = Color(0xFFFF808F),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = TacticalIcons.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = Color(0xFFFF808F)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Hero: Clean illustration presentation with native HorizontalPager swipe
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                contentAlignment = Alignment.Center
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val step = steps[pageIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Render artwork cleanly without artificial outer double-border or letterbox
                        Image(
                            painter = painterResource(id = step.illustrationRes),
                            contentDescription = step.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Refined Tactical Navigation Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Step Button (Visible when page > 0)
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    modifier = Modifier.weight(1f),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WayMateTextPrimary
                        ),
                        border = BorderStroke(1.dp, WayMateBorder),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = TacticalIcons.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = WayMateTextSecondary
                            )
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "مرحله قبلی",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Primary Next / Finish Button
                Button(
                    onClick = {
                        if (isLastStep) {
                            onFinishOnboarding()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(if (pagerState.currentPage > 0) 1.2f else 1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WayMateCyan,
                        contentColor = WayMateOnCyan
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isLastStep) "ورود به سامانه" else "مرحله بعدی",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Icon(
                            imageVector = if (isLastStep) TacticalIcons.Check else TacticalIcons.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = WayMateOnCyan
                        )
                    }
                }
            }
        }
    }
}
