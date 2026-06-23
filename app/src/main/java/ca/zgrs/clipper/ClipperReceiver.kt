package ca.zgrs.clipper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Base64
import android.util.Log

/**
 * Receives ADB broadcasts and handles clipboard operations.
 *
 * FIX (Session 22 / Android 10):
 * The old code forwarded clipper.set to ClipboardService via
 * startForegroundService(). On Android 10 (API 29), background apps
 * cannot call startForegroundService() — it fails silently, result=0.
 *
 * Fix: set clipboard DIRECTLY in the receiver.
 * Android 10 (API 29) background receivers CAN still set clipboard.
 * The background clipboard restriction only applies to Android 12+ (API 31).
 * So direct setPrimaryClip() works fine here on the A10s (Android 10).
 *
 * For Android 12+ devices, we still attempt direct set first,
 * and fall back to service delegation if needed.
 */
class ClipperReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ClipperReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received: $action")

        when (action) {
            "clipper.set", "set" -> {
                val text = intent.getStringExtra("text") ?: run {
                    Log.e(TAG, "clipper.set: missing 'text' extra")
                    resultCode = Activity.RESULT_CANCELED
                    return
                }
                setClipboard(context, text)
            }

            "clipper.set.b64" -> {
                val b64 = intent.getStringExtra("text") ?: run {
                    Log.e(TAG, "clipper.set.b64: missing 'text' extra")
                    resultCode = Activity.RESULT_CANCELED
                    return
                }
                try {
                    val decoded = Base64.decode(b64, Base64.DEFAULT)
                    val text = String(decoded, Charsets.UTF_8)
                    setClipboard(context, text)
                } catch (e: Exception) {
                    Log.e(TAG, "Base64 decode failed: ${e.message}")
                    resultCode = Activity.RESULT_CANCELED
                }
            }

            "clipper.get", "get" -> {
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

    private fun setClipboard(context: Context, text: String) {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            // Android 10/11: background receivers can set clipboard directly
            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("", text))
            Log.d(TAG, "Set clipboard directly (API ${Build.VERSION.SDK_INT}): ${text.length} chars")
            resultCode = Activity.RESULT_OK
            resultData = "OK"
        } else {
            // Android 12+: background clipboard set is blocked — delegate to foreground service
            Log.d(TAG, "API ${Build.VERSION.SDK_INT} >= 31, delegating to ClipboardService")
            val serviceIntent = Intent(context, ClipboardService::class.java).apply {
                this.action = "clipper.set"
                putExtra("text", text)
            }
            context.startForegroundService(serviceIntent)
            resultCode = Activity.RESULT_OK
            resultData = "Forwarded to foreground service."
        }
    }
}
