package ca.zgrs.clipper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives ADB broadcasts and forwards clipboard-SET operations
 * to ClipboardService (which runs in foreground and CAN set clipboard).
 *
 * clipper.get is handled directly here since reading clipboard
 * is allowed from background context.
 */
class ClipperReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ClipboardReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received: $action")

        when (action) {
            "clipper.set", "set",
            "clipper.set.b64" -> {
                // Forward to service — service has foreground context
                // and can set clipboard on Android 12+/16
                val serviceIntent = Intent(context, ClipboardService::class.java).apply {
                    this.action = action
                    putExtra("text", intent.getStringExtra("text"))
                }
                context.startForegroundService(serviceIntent)
                resultCode = Activity.RESULT_OK
                resultData = "Forwarded to foreground service."
            }

            "clipper.get", "get" -> {
                // Reading is allowed from background
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as ClipboardManager
                val clip = cb.primaryClip?.getItemAt(0)?.text?.toString()
                if (clip != null) {
                    resultCode = Activity.RESULT_OK
                    resultData = clip
                    Log.d(TAG, "Got clipboard: ${clip.length} chars")
                } else {
                    resultCode = Activity.RESULT_CANCELED
                    resultData = ""
                }
            }
        }
    }
}
