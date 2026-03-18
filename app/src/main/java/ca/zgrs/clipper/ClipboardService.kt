package ca.zgrs.clipper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Sticky background service that keeps ClipperReceiver active.
 * Start with: adb shell am startservice ca.zgrs.clipper/.ClipboardService
 */
class ClipboardService : Service() {

    companion object {
        private const val TAG = "ClipboardService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ClipboardService started")
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ClipboardService created — receiver is active")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
