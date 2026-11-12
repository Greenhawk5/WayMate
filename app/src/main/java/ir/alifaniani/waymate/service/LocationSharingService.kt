package ir.alifaniani.waymate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import ir.alifaniani.waymate.MainActivity
import ir.alifaniani.waymate.R
import ir.alifaniani.waymate.WayMateApp
import ir.alifaniani.waymate.domain.model.LocationSharingStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground Service responsible for continuous, consent-based location sharing during hiking trips.
 *
 * Requirements:
 * - Declares foregroundServiceType="location"
 * - Displays a mandatory persistent ongoing notification with an immediate "Stop" action
 * - Continues safely while screen is locked or application is backgrounded
 * - Immediately stops when user taps "Stop Sharing" in UI or notification
 * - Never auto-starts on reboot or application launch (START_NOT_STICKY)
 */
class LocationSharingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateObservationJob: Job? = null

    companion object {
        const val ACTION_START_SHARING = "ir.alifaniani.waymate.action.START_LOCATION_SHARING"
        const val ACTION_STOP_SHARING = "ir.alifaniani.waymate.action.STOP_LOCATION_SHARING"
        const val EXTRA_TEAM_ID = "extra_team_id"

        private const val NOTIFICATION_ID = 4001
        private const val CHANNEL_ID = "waymate_location_sharing_channel"

        fun start(context: Context, teamId: String? = null) {
            val intent = Intent(context, LocationSharingService::class.java).apply {
                action = ACTION_START_SHARING
                if (teamId != null) {
                    putExtra(EXTRA_TEAM_ID, teamId)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationSharingService::class.java).apply {
                action = ACTION_STOP_SHARING
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_SHARING) {
            handleStopSharing()
            return START_NOT_STICKY
        }

        if (action == ACTION_START_SHARING) {
            val teamId = intent.getStringExtra(EXTRA_TEAM_ID)
            handleStartSharing(teamId)
            return START_NOT_STICKY
        }

        return START_NOT_STICKY
    }

    private fun handleStartSharing(teamId: String?) {
        val notification = buildOngoingNotification()

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
        } catch (e: Exception) {
            stopSelf()
            return
        }

        val app = application as? WayMateApp ?: run {
            stopSelf()
            return
        }

        serviceScope.launch {
            app.container.startLocationSharingUseCase(teamId)
        }

        stateObservationJob?.cancel()
        stateObservationJob = serviceScope.launch {
            app.container.observeLocationSharingStateUseCase().collect { state ->
                when (state.status) {
                    LocationSharingStatus.STOPPED,
                    LocationSharingStatus.NOT_SHARING -> {
                        handleStopSharing()
                    }
                    LocationSharingStatus.GPS_DISABLED,
                    LocationSharingStatus.PERMISSION_DENIED -> {
                        // Keep notification updated with warning
                        updateNotification(state.status.displayNamePersian)
                    }
                    LocationSharingStatus.SHARING -> {
                        val memberCount = state.activeSharers.size
                        val text = if (memberCount > 0) {
                            "موقعیت شما برای $memberCount نفر از اعضای گروه نمایش داده می‌شود."
                        } else {
                            "موقعیت مکانی شما به صورت زنده برای اعضای گروه ارسال می‌شود."
                        }
                        updateNotification(text)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun handleStopSharing() {
        val app = application as? WayMateApp
        app?.container?.stopLocationSharingUseCase?.invoke()

        stateObservationJob?.cancel()
        stateObservationJob = null

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "اشتراک‌گذاری زنده موقعیت مکانی",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "نمایش وضعیت فعال اشتراک‌گذاری موقعیت مکانی برای همنوردان"
                setShowBadge(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingNotification(contentText: String = "موقعیت مکانی شما به صورت زنده برای اعضای گروه ارسال می‌شود."): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, LocationSharingService::class.java).apply {
            action = ACTION_STOP_SHARING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIconBitmap = runCatching {
            BitmapFactory.decodeResource(resources, R.drawable.ic_large_notify_location)
        }.getOrNull()

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("اشتراک‌گذاری زنده موقعیت در گروه کوهنوردی")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_stat_notify_location)
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "توقف اشتراک‌گذاری",
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (largeIconBitmap != null) {
            builder.setLargeIcon(largeIconBitmap)
        }

        return builder.build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildOngoingNotification(contentText))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
