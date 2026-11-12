package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.BuildConfig
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.ui.designsystem.TacticalCard
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.designsystem.TacticalScreenHeader
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateBorderSubtle
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextMuted
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary

/**
 * Professional About Screen for WayMate.
 * Displays application identity, version details, technical architecture, and privacy ethics.
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar with Back Navigation
            TacticalScreenHeader(
                title = "درباره وی‌میت",
                subtitle = "شناسه و معماری سامانه هماهنگی میدانی",
                trailingContent = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WayMateMidnightSurface)
                            .border(1.dp, WayMateBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = TacticalIcons.ArrowForward,
                            contentDescription = "بازگشت",
                            tint = WayMateCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Brand Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(WayMateMidnightSurface)
                    .border(1.dp, WayMateBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_waymate_logo),
                        contentDescription = "لوگوی وی‌میت",
                        modifier = Modifier
                            .size(80.dp)
                            .shadow(16.dp, CircleShape, spotColor = WayMateCyan),
                        contentScale = ContentScale.Fit
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "وی‌میت (WayMate)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WayMateTextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "سامانه هماهنگی موقعیت و عملیات میدانی تیم‌ها",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WayMateCyan,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(WayMateSurfaceVariant)
                            .border(1.dp, WayMateBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "نسخه ${BuildConfig.VERSION_NAME} (کد ساخت ${BuildConfig.VERSION_CODE})",
                            color = WayMateTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Technical Architecture Specification
            TacticalCard(
                shape = RoundedCornerShape(18.dp),
                containerColor = WayMateMidnightSurface,
                borderColor = WayMateBorder
            ) {
                Text(
                    text = "مشخصات فنی و زیرساخت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WayMateCyan
                )

                Spacer(modifier = Modifier.height(14.dp))

                ArchitectureRow(label = "کلاینت اندروید", value = "Kotlin Native + Jetpack Compose")
                ArchitectureRow(label = "موتور نقشه", value = "بوم برداری آفلاین شهرستان جهرم (EPSG:4326)")
                ArchitectureRow(label = "لایه‌های مکانی", value = "۵,۴۰۰+ عارضه معابر، آبراهه‌ها و خطوط مسیل")
                ArchitectureRow(label = "سرویس ابری API", value = "Cloudflare Workers + D1 Database")
                ArchitectureRow(label = "سرویس پس‌زمینه", value = "Foreground Service با اعلان اضطراری")
                ArchitectureRow(label = "پایش آنلاین", value = "SafePoller بلادرنگ با کنترل انقضای سشن")
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Privacy & Ethics Pledge
            TacticalCard(
                shape = RoundedCornerShape(18.dp),
                containerColor = WayMateMidnightSurface,
                borderColor = WayMateBorder
            ) {
                Text(
                    text = "تعهد به حریم خصوصی و امنیت عملیاتی",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WayMateTextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "اشتراک‌گذاری موقعیت مکانی در وی‌میت منحصراً با رضایت صریح و ارادی کاربر آغاز می‌شود. سرویس پس‌زمینه با اعلان پایدار در نوار وضعیت قابل مشاهده است و با یک لمس متوقف می‌گردد. کلیه اطلاعات مکانی فقط در محدوده گروه عملیاتی و با احراز هویت امن تبادل می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = WayMateTextSecondary,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArchitectureRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = WayMateTextMuted,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = WayMateTextPrimary,
            textAlign = TextAlign.End
        )
    }
}

