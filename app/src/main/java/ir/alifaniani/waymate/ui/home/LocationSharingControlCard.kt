package ir.alifaniani.waymate.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.domain.model.LocationSharingState
import ir.alifaniani.waymate.domain.model.LocationSharingStatus
import ir.alifaniani.waymate.ui.common.BadgeTone
import ir.alifaniani.waymate.ui.common.BannerType
import ir.alifaniani.waymate.ui.common.TacticalFeedbackBanner
import ir.alifaniani.waymate.ui.common.TacticalOutlinedButton
import ir.alifaniani.waymate.ui.common.TacticalPrimaryButton
import ir.alifaniani.waymate.ui.common.TacticalStatusBadge
import ir.alifaniani.waymate.ui.designsystem.TacticalIcons

/**
 * Dedicated control panel for consent-based real-time location sharing.
 *
 * Implements:
 * - Transparent status indicators (Not sharing, Starting, Sharing, Reconnecting, GPS disabled, Error)
 * - Immediate Stop action
 * - Privacy-conscious explanations in Persian RTL
 */
@Composable
fun LocationSharingControlCard(
    sharingState: LocationSharingState,
    onStartSharing: () -> Unit,
    onStopSharing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSharing = sharingState.isSharingActive

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSharing) Color(0xFF00E676).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (sharingState.status) {
                                    LocationSharingStatus.SHARING -> Color(0xFF00E676)
                                    LocationSharingStatus.STARTING,
                                    LocationSharingStatus.RECONNECTING -> Color(0xFFFFB300)
                                    LocationSharingStatus.GPS_DISABLED,
                                    LocationSharingStatus.PERMISSION_DENIED,
                                    LocationSharingStatus.ERROR -> Color(0xFFEF5350)
                                    else -> Color(0xFF8B949E)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "اشتراک‌گذاری زنده موقعیت مکانی",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSharing) Color(0xFF00E676) else MaterialTheme.colorScheme.primary
                    )
                }

                TacticalStatusBadge(
                    text = sharingState.status.displayNamePersian,
                    tone = when (sharingState.status) {
                        LocationSharingStatus.SHARING -> BadgeTone.SUCCESS
                        LocationSharingStatus.STARTING,
                        LocationSharingStatus.RECONNECTING -> BadgeTone.WARNING
                        LocationSharingStatus.GPS_DISABLED,
                        LocationSharingStatus.PERMISSION_DENIED,
                        LocationSharingStatus.ERROR -> BadgeTone.ERROR
                        else -> BadgeTone.NEUTRAL
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation Text
            Text(
                text = if (isSharing) {
                    "سرویس پس‌زمینه فعال است. موقعیت مکانی شما به صورت زنده برای اعضای گروه ارسال می‌شود و با قفل شدن صفحه قطع نخواهد شد."
                } else {
                    "موقعیت مکانی صرفاً با رضایت صریح شما و فقط برای اعضای همین گروه به اشتراک گذاشته می‌شود."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Live Telemetry Row (when active)
            if (isSharing && sharingState.myLocation != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10161D))
                        .border(1.dp, Color(0xFF1E2836), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "آخرین موقعیت ثبت‌شده شما:",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${sharingState.myLocation.formattedCoordinates} (دقت: ±${sharingState.myLocation.accuracyMeters.toInt()}م)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (sharingState.activeSharers.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TacticalStatusBadge(
                            text = "${sharingState.activeSharers.size} همنورد فعال",
                            tone = BadgeTone.SUCCESS
                        )
                    }
                }
            }

            // Error / Warning Banner
            if (sharingState.errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TacticalFeedbackBanner(
                    title = "وضعیت سرویس موقعیت",
                    message = sharingState.errorMessage,
                    type = BannerType.ERROR
                )
            } else if (sharingState.status == LocationSharingStatus.GPS_DISABLED) {
                Spacer(modifier = Modifier.height(10.dp))
                TacticalFeedbackBanner(
                    title = "موقعیت‌یاب خاموش است",
                    message = "برای ارسال موقعیت به همنوردان، حسگر GPS دستگاه را روشن کنید.",
                    type = BannerType.WARNING
                )
            } else if (sharingState.status == LocationSharingStatus.PERMISSION_DENIED) {
                Spacer(modifier = Modifier.height(10.dp))
                TacticalFeedbackBanner(
                    title = "مجوز دسترسی داده نشده است",
                    message = "برای استفاده از این قابلیت، دسترسی موقعیت مکانی (Location) الزامی است.",
                    type = BannerType.WARNING
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (isSharing) {
                TacticalOutlinedButton(
                    text = "توقف اشتراک‌گذاری موقعیت",
                    leadingIcon = TacticalIcons.Stop,
                    onClick = onStopSharing,
                    isDestructive = true
                )
            } else {
                TacticalPrimaryButton(
                    text = "شروع اشتراک‌گذاری موقعیت",
                    leadingIcon = TacticalIcons.Play,
                    onClick = onStartSharing,
                    isLoading = sharingState.status == LocationSharingStatus.STARTING
                )
            }
        }
    }
}

/**
 * Transparent pre-permission rationale dialog shown prior to system permission requests.
 */
@Composable
fun LocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "دسترسی به موقعیت مکانی گروه کوهنوردی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_location_access),
                    contentDescription = "دسترسی موقعیت مکانی",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 12.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = "وای‌میت برای نمایش موقعیت زندهٔ شما به همنوردان گروه در طول پیمایش و حفظ ایمنی شما در زمان قفل بودن صفحه، نیازمند مجوز موقعیت مکانی و نمایش یک اعلان ماندگار است.\n\nاطلاعات مکانی شما بدون درخواست مستقیم خودتان ارسال نمی‌شود و هر زمان بخواهید می‌توانید اشتراک‌گذاری را فوراً متوقف کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "متوجه شدم و تایید می‌کنم",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "انصراف",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    )
}

/**
 * Dialog shown when system location permission is denied by the user.
 * Provides clear explanation and settings redirect if permanently denied.
 */
@Composable
fun LocationPermissionDeniedDialog(
    isPermanentlyDenied: Boolean,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "مجوز موقعیت مکانی داده نشد",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_empty_location_access),
                    contentDescription = "عدم دسترسی به موقعیت",
                    modifier = Modifier
                        .size(120.dp)
                        .padding(bottom = 12.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = if (isPermanentlyDenied) {
                        "مجوز دسترسی به موقعیت مکانی برای اشتراک‌گذاری با همنوردان گروه الزامی است. با توجه به رد شدن مجوز، لطفاً از بخش تنظیمات برنامه دسترسی موقعیت را فعال نمایید."
                    } else {
                        "برای نمایش و ارسال موقعیت زنده به همنوردان گروه، اعطای دسترسی به موقعیت مکانی الزامی است."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPermanentlyDenied) onOpenSettings() else onRetry()
                }
            ) {
                Text(
                    text = if (isPermanentlyDenied) "باز کردن تنظیمات" else "درخواست مجدد مجوز",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "انصراف",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    )
}
