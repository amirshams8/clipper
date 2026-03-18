package ca.zgrs.clipper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log

/**
 * Foreground service — keeps Clipper alive and grants clipboard access on Android 12+.
 * Android 16 blocks clipboard writes from background broadcast receivers.
 * Running as a foreground service bypasses this restriction.
 *
 * Start with:
 *   adb shell am startforegroundservice ca.zgrs.clipper/.ClipboardService
 * Or still works with:
 *   adb shell am startservice ca.zgrs.clipper/.ClipboardService
 */
class ClipboardService : Service() {

    companion object {
        private const val TAG = "ClipboardService"
        private const val CHANNEL_ID = "clipper_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ClipboardService created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d(TAG, "Running as foreground service — clipboard access granted")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ClipboardService started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipper Service",
                NotificationManager.IMPORTANCE_MIN  // silent, no sound, minimal UI
            ).apply {
                description = "Keeps Clipper active for ADB clipboard access"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Clipper")
                .setContentText("ADB clipboard service running")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Clipper")
                .setContentText("ADB clipboard service running")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true)
                .build()
        }
    }
}
