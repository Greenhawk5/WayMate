package ir.alifaniani.waymate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import ir.alifaniani.waymate.MainActivity
import ir.alifaniani.waymate.R

/**
 * Supported tactical notification classifications mapped to the 15 custom icons.
 */
enum class TacticalNotificationType(
    val notificationId: Int,
    val channelId: String,
    @get:DrawableRes val smallIconRes: Int,
    @get:DrawableRes val largeIconRes: Int,
    val priority: Int = NotificationCompat.PRIORITY_DEFAULT
) {
    TEAM_UPDATE(
        notificationId = 4001,
        channelId = WayMateNotificationManager.CHANNEL_SQUAD,
        smallIconRes = R.drawable.ic_stat_notify_team,
        largeIconRes = R.drawable.ic_large_notify_team,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    COMMAND_ORDER(
        notificationId = 4002,
        channelId = WayMateNotificationManager.CHANNEL_TACTICAL_ORDERS,
        smallIconRes = R.drawable.ic_stat_notify_command,
        largeIconRes = R.drawable.ic_large_notify_command,
        priority = NotificationCompat.PRIORITY_HIGH
    ),
    LOCATION_SHARING(
        notificationId = 4003,
        channelId = WayMateNotificationManager.CHANNEL_LOCATION_SHARING,
        smallIconRes = R.drawable.ic_stat_notify_location,
        largeIconRes = R.drawable.ic_large_notify_location,
        priority = NotificationCompat.PRIORITY_LOW
    ),
    TACTICAL_ALERT(
        notificationId = 4004,
        channelId = WayMateNotificationManager.CHANNEL_TACTICAL_ORDERS,
        smallIconRes = R.drawable.ic_stat_notify_alert,
        largeIconRes = R.drawable.ic_large_notify_alert,
        priority = NotificationCompat.PRIORITY_HIGH
    ),
    ELEVATION_SUMMIT(
        notificationId = 4005,
        channelId = WayMateNotificationManager.CHANNEL_NAVIGATION,
        smallIconRes = R.drawable.ic_stat_notify_elevation,
        largeIconRes = R.drawable.ic_large_notify_elevation,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    COMMS_STATUS(
        notificationId = 4006,
        channelId = WayMateNotificationManager.CHANNEL_TACTICAL_ORDERS,
        smallIconRes = R.drawable.ic_stat_notify_comms,
        largeIconRes = R.drawable.ic_large_notify_comms,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    OBJECTIVE_POI(
        notificationId = 4007,
        channelId = WayMateNotificationManager.CHANNEL_NAVIGATION,
        smallIconRes = R.drawable.ic_stat_notify_objective,
        largeIconRes = R.drawable.ic_large_notify_objective,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    SYNC_QUEUE(
        notificationId = 4008,
        channelId = WayMateNotificationManager.CHANNEL_SYNC,
        smallIconRes = R.drawable.ic_stat_notify_sync,
        largeIconRes = R.drawable.ic_large_notify_sync,
        priority = NotificationCompat.PRIORITY_LOW
    ),
    TRAIL_ROUTE(
        notificationId = 4009,
        channelId = WayMateNotificationManager.CHANNEL_NAVIGATION,
        smallIconRes = R.drawable.ic_stat_notify_trail,
        largeIconRes = R.drawable.ic_large_notify_trail,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    EMERGENCY_SOS(
        notificationId = 4010,
        channelId = WayMateNotificationManager.CHANNEL_EMERGENCY,
        smallIconRes = R.drawable.ic_stat_notify_sos,
        largeIconRes = R.drawable.ic_large_notify_sos,
        priority = NotificationCompat.PRIORITY_MAX
    ),
    HEADING_UPDATE(
        notificationId = 4011,
        channelId = WayMateNotificationManager.CHANNEL_NAVIGATION,
        smallIconRes = R.drawable.ic_stat_notify_heading,
        largeIconRes = R.drawable.ic_large_notify_heading,
        priority = NotificationCompat.PRIORITY_LOW
    ),
    COMPASS_GAUGE(
        notificationId = 4012,
        channelId = WayMateNotificationManager.CHANNEL_NAVIGATION,
        smallIconRes = R.drawable.ic_stat_notify_compass,
        largeIconRes = R.drawable.ic_large_notify_compass,
        priority = NotificationCompat.PRIORITY_LOW
    ),
    TELEMETRY_BATTERY(
        notificationId = 4013,
        channelId = WayMateNotificationManager.CHANNEL_SYNC,
        smallIconRes = R.drawable.ic_stat_notify_telemetry,
        largeIconRes = R.drawable.ic_large_notify_telemetry,
        priority = NotificationCompat.PRIORITY_DEFAULT
    ),
    PROXIMITY_RADAR(
        notificationId = 4014,
        channelId = WayMateNotificationManager.CHANNEL_SQUAD,
        smallIconRes = R.drawable.ic_stat_notify_radar,
        largeIconRes = R.drawable.ic_large_notify_radar,
        priority = NotificationCompat.PRIORITY_HIGH
    ),
    SEARCH_SCOPE(
        notificationId = 4015,
        channelId = WayMateNotificationManager.CHANNEL_SYNC,
        smallIconRes = R.drawable.ic_stat_notify_search,
        largeIconRes = R.drawable.ic_large_notify_search,
        priority = NotificationCompat.PRIORITY_LOW
    )
}

/**
 * Centralized notification manager for the WayMate application,
 * responsible for notification channel creation, icon associations, and delivery.
 */
class WayMateNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_LOCATION_SHARING = "waymate_location_sharing_channel"
        const val CHANNEL_TACTICAL_ORDERS = "waymate_tactical_orders_channel"
        const val CHANNEL_EMERGENCY = "waymate_emergency_channel"
        const val CHANNEL_SYNC = "waymate_sync_channel"
        const val CHANNEL_SQUAD = "waymate_squad_channel"
        const val CHANNEL_NAVIGATION = "waymate_navigation_channel"
    }

    init {
        createNotificationChannels()
    }

    /**
     * Initializes all semantic notification channels on Android O+.
     */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_LOCATION_SHARING,
                    "اشتراک‌گذاری زنده موقعیت مکانی",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "نمایش اعلان پیش‌زمینه فعال بودن اشتراک‌گذاری موقعیت برای همنوردان"
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_TACTICAL_ORDERS,
                    "دستورات و هشدارهای تاکتیکی",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "دریافت دستورات عملیاتی فوری از سوی فرمانده و هشدارهای مهم"
                    setShowBadge(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_EMERGENCY,
                    "هشدارهای اضطراری و امداد (SOS)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "پیام‌های کمک اضطراری، حوادث و سیگنال‌های نجات"
                    setShowBadge(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_SYNC,
                    "همگام‌سازی و پایگاه داده",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "وضعیت صف ذخیره و ارسال آفلاین و هشدار باتری"
                    setShowBadge(false)
                },
                NotificationChannel(
                    CHANNEL_SQUAD,
                    "رویدادهای گروه و اعضا",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "عضویت در گروه، پیوستن همنوردان و رادار نزدیکی"
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_NAVIGATION,
                    "ناوبری و مسیرهای کوهنوردی",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "نقاط نشانه (POI)، جهت قطب‌نما و نشانه‌های فتح قله"
                    setShowBadge(true)
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    /**
     * Constructs and displays a notification based on the configured tactical notification type.
     */
    fun showNotification(
        type: TacticalNotificationType,
        title: String,
        body: String,
        ongoing: Boolean = false,
        notificationId: Int = type.notificationId
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.notificationId,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, type.largeIconRes)
        }.getOrNull()

        val builder = NotificationCompat.Builder(context, type.channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(type.smallIconRes)
            .setPriority(type.priority)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setContentIntent(pendingIntent)

        if (largeBitmap != null) {
            builder.setLargeIcon(largeBitmap)
        }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    /**
     * Dispatches incoming tactical command order / message notification.
     */
    fun showTacticalMessageNotification(senderCallsign: String, body: String, isUrgent: Boolean) {
        val type = if (isUrgent) TacticalNotificationType.TACTICAL_ALERT else TacticalNotificationType.COMMAND_ORDER
        val title = if (isUrgent) "هشدار فوری از $senderCallsign" else "دستور عملیاتی از $senderCallsign"
        showNotification(type = type, title = title, body = body)
    }

    /**
     * Dispatches Emergency SOS beacon notification.
     */
    fun showEmergencySosNotification(callsign: String, details: String) {
        showNotification(
            type = TacticalNotificationType.EMERGENCY_SOS,
            title = "سیگنال اضطراری امداد ($callsign)",
            body = details
        )
    }

    /**
     * Dispatches offline sync completed notification.
     */
    fun showOfflineSyncNotification(syncedCount: Int, isError: Boolean = false) {
        val title = if (isError) "خطا در همگام‌سازی ابری" else "همگام‌سازی آفلاین موفق"
        val body = if (isError) "ارتباط با سرور ابری برقرار نشد، داده‌ها در حافظه محلی محفوظ ماندند."
        else "$syncedCount عملیات با موفقیت در پایگاه ابری D1 ثبت شد."
        showNotification(
            type = TacticalNotificationType.SYNC_QUEUE,
            title = title,
            body = body
        )
    }

    /**
     * Dispatches team membership / squad update notification.
     */
    fun showTeamUpdateNotification(teamName: String, updateText: String) {
        showNotification(
            type = TacticalNotificationType.TEAM_UPDATE,
            title = "به‌روزرسانی گروه $teamName",
            body = updateText
        )
    }

    /**
     * Dispatches waypoint / summit milestone reached notification.
     */
    fun showSummitWaypointNotification(landmarkName: String, altitudeMeters: Int) {
        showNotification(
            type = TacticalNotificationType.ELEVATION_SUMMIT,
            title = "فتح نشانه: $landmarkName",
            body = "ارتفاع ثبت‌شده: $altitudeMeters متر از سطح دریا"
        )
    }

    /**
     * Dispatches proximity radar warning.
     */
    fun showProximityRadarNotification(callsign: String, distanceMeters: Int) {
        showNotification(
            type = TacticalNotificationType.PROXIMITY_RADAR,
            title = "رادار نزدیکی همنورد",
            body = "کاربر $callsign در فاصله $distanceMeters متری شما قرار دارد."
        )
    }

    /**
     * Dispatches telemetry battery power-saving alert.
     */
    fun showTelemetryBatteryWarning(batteryPercent: Int) {
        showNotification(
            type = TacticalNotificationType.TELEMETRY_BATTERY,
            title = "پایش باتری تاکتیکی",
            body = "شارژ باتری به $batteryPercent٪ رسید. حالت بهینه‌سازی موقعیت فعال شد."
        )
    }

    /**
     * Cancels an active notification by ID.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }
}

