package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.domain.model.User
import ir.alifaniani.waymate.domain.model.UserRole
import ir.alifaniani.waymate.ui.designsystem.TacticalActionButton
import ir.alifaniani.waymate.ui.designsystem.TacticalCard
import ir.alifaniani.waymate.ui.designsystem.TacticalConfirmDialog
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons
import ir.alifaniani.waymate.ui.designsystem.TacticalScreenHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalSectionHeader
import ir.alifaniani.waymate.ui.designsystem.TacticalStatusPill
import ir.alifaniani.waymate.ui.designsystem.TacticalTone
import ir.alifaniani.waymate.ui.theme.WayMateBorder
import ir.alifaniani.waymate.ui.theme.WayMateBorderSubtle
import ir.alifaniani.waymate.ui.theme.WayMateCyan
import ir.alifaniani.waymate.ui.theme.WayMateCyanContainer
import ir.alifaniani.waymate.ui.theme.WayMateError
import ir.alifaniani.waymate.ui.theme.WayMateMidnightDark
import ir.alifaniani.waymate.ui.theme.WayMateMidnightSurface
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceHighest
import ir.alifaniani.waymate.ui.theme.WayMateSurfaceVariant
import ir.alifaniani.waymate.ui.theme.WayMateTextMuted
import ir.alifaniani.waymate.ui.theme.WayMateTextPrimary
import ir.alifaniani.waymate.ui.theme.WayMateTextSecondary

/**
 * Dedicated Destination E: Profile & Operational Identity.
 *
 * Displays:
 * - Verified tactical callsign and account identity
 * - Explicit role designation (Owner / Commander / Team Lead)
 * - Consent-based location sharing status
 * - Quick secondary links to Settings and About screens
 * - Confirmation-safeguarded Logout action
 */
@Composable
fun ProfileScreen(
    user: User,
    onLogoutClicked: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = WayMateMidnightDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Screen Header
            TacticalScreenHeader(
                title = "مشخصات و هویت کاربری",
                subtitle = "شناسه تماس و سطح دسترسی عملیاتی",
                trailingContent = {
                    TacticalStatusPill(
                        text = "احراز شده",
                        tone = TacticalTone.SUCCESS
                    )
                }
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Tactical Identity Avatar Card
            TacticalCard(shape = RoundedCornerShape(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Callsign Monogram Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(12.dp, CircleShape, spotColor = WayMateCyan)
                            .clip(CircleShape)
                            .background(WayMateCyanContainer)
                            .border(1.5.dp, WayMateCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.callsign.take(2).uppercase(),
                            color = WayMateCyan,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.callsign,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WayMateTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (user.role) {
                                UserRole.OWNER -> "متولی اصلی سامانه (OWNER)"
                                UserRole.COMMANDER -> "فرمانده عملیات (COMMANDER)"
                                UserRole.TEAM_LEAD -> "سرتیم گشت میدانی (TEAM_LEAD)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = WayMateCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "نام کاربری: ${user.username}",
                            fontSize = 12.sp,
                            color = WayMateTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(WayMateSurfaceVariant)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "شناسه رکورد امنیتی",
                            fontSize = 11.sp,
                            color = WayMateTextMuted
                        )
                        Text(
                            text = user.id,
                            fontSize = 11.sp,
                            color = WayMateTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Navigation to Secondary Destinations
            TacticalSectionHeader(title = "پیکربندی و اطلاعات سامانه")

            TacticalCard(shape = RoundedCornerShape(16.dp)) {
                // Settings Destination Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToSettings)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WayMateSurfaceHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = TacticalIcons.Settings,
                                contentDescription = null,
                                tint = WayMateCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تنظیمات سامانه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WayMateTextPrimary
                            )
                            Text(
                                text = "پیکربندی GPS، نقشه، هشدارها و صف آفلاین",
                                fontSize = 11.sp,
                                color = WayMateTextMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = TacticalIcons.ArrowBack,
                        contentDescription = "ورود",
                        tint = WayMateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // About Destination Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNavigateToAbout)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(WayMateSurfaceHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = TacticalIcons.Info,
                                contentDescription = null,
                                tint = WayMateCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "درباره وی‌میت",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = WayMateTextPrimary
                            )
                            Text(
                                text = "شناسه نسخه، مشخصات معماری و حریم خصوصی",
                                fontSize = 11.sp,
                                color = WayMateTextMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = TacticalIcons.ArrowBack,
                        contentDescription = "ورود",
                        tint = WayMateTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            TacticalActionButton(
                text = "خروج از حساب کاربری",
                onClick = { showLogoutConfirmDialog = true },
                isPrimary = false,
                isDestructive = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom Spacing ensuring complete scroll clearance above circular navigation bar
            Spacer(modifier = Modifier.height(148.dp).navigationBarsPadding())
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            TacticalConfirmDialog(
                title = "تأیید خروج از حساب کاربری",
                message = "با خروج از سامانه، اشتراک‌گذاری موقعیت مکانی متوقف و سشن عملیاتی پایان می‌یابد. آیا مطمئن هستید؟",
                confirmText = "خروج از حساب",
                dismissText = "انصراف",
                isDestructive = true,
                onConfirm = {
                    showLogoutConfirmDialog = false
                    onLogoutClicked()
                },
                onDismiss = { showLogoutConfirmDialog = false }
            )
        }
    }
}

