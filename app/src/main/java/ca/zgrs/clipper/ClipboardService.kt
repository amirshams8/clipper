package ca.zgrs.clipper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Base64
import android.util.Log

/**
 * Foreground service that owns all clipboard operations.
 * On Android 12+ background receivers cannot set clipboard.
 * This service runs in foreground so it CAN set clipboard.
 *
 * ClipperReceiver forwards intents here via startService().
 * This service then sets the clipboard from foreground context.
 *
 * Actions handled:
 *   clipper.set      — plain text via "text" extra
 *   clipper.set.b64  — base64 text via "text" extra
 *   clipper.get      — logs clipboard (get not supported via service)
 */
class ClipboardService : Service() {

    companion object {
        private const val TAG = "ClipboardService"
        private const val CHANNEL_ID = "clipper_channel"
        private const val NOTIFICATION_ID = 1
    }

    // Manifest receivers do not get implicit broadcasts (targetSdk 26+), so
    // `am broadcast -a clipper.get` with no -n was silently dropped (result=0).
    // Registering the same receiver here restores plain `-a` broadcasts.
    private val receiver = ClipperReceiver()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction("clipper.get"); addAction("clipper.set")
            addAction("clipper.set.b64"); addAction("get"); addAction("set")
        }
        registerReceiver(receiver, filter, RECEIVER_EXPORTED)
        Log.d(TAG, "ClipboardService running in foreground")
    }

    override fun onDestroy() {
        try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        val cb = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        when (action) {
            "clipper.set", "set" -> {
                val text = intent.getStringExtra("text") ?: return
                cb.setPrimaryClip(ClipData.newPlainText("", text))
                Log.d(TAG, "Set clipboard: ${text.length} chars")
            }

            "clipper.set.b64" -> {
                val b64 = intent.getStringExtra("text") ?: return
                try {
                    val decoded = Base64.decode(b64, Base64.DEFAULT)
                    val text = String(decoded, Charsets.UTF_8)
                    cb.setPrimaryClip(ClipData.newPlainText("", text))
                    Log.d(TAG, "Set clipboard via b64: ${text.length} chars")
                } catch (e: Exception) {
                    Log.e(TAG, "Base64 decode failed: ${e.message}")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Clipper",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Clipper active")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Clipper active")
                .setSmallIcon(android.R.drawable.ic_menu_edit)
                .setOngoing(true)
                .build()
        }
}
