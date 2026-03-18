package ca.zgrs.clipper

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log

/**
 * Handles clipboard broadcast commands.
 *
 * Supported actions:
 *   clipper.set      — set clipboard from plain text (shell-safe strings only)
 *   clipper.set.b64  — set clipboard from base64-encoded text (safe for ALL chars)
 *   clipper.get      — get clipboard text (returned via resultData)
 *
 * JarvisMinI usage (clipper.set.b64):
 *   B64=$(echo -n "Top 5: 1. Caramel (4.8) [best]" | base64)
 *   adb shell am broadcast -a clipper.set.b64 --es text "$B64"
 */
class ClipperReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ClipboardReceiver"
        const val ACTION_GET      = "clipper.get"
        const val ACTION_GET_S    = "get"
        const val ACTION_SET      = "clipper.set"
        const val ACTION_SET_S    = "set"
        const val ACTION_SET_B64  = "clipper.set.b64"
        const val EXTRA_TEXT      = "text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        when (intent.action) {

            ACTION_SET, ACTION_SET_S -> {
                val text = intent.getStringExtra(EXTRA_TEXT)
                if (text != null) {
                    cb.setPrimaryClip(ClipData.newPlainText("", text))
                    resultCode = Activity.RESULT_OK
                    resultData = "Text is copied into clipboard."
                    Log.d(TAG, "Set clipboard: ${text.length} chars")
                } else {
                    resultCode = Activity.RESULT_CANCELED
                    resultData = "No text provided. Use --es text \"value\""
                }
            }

            ACTION_SET_B64 -> {
                val b64 = intent.getStringExtra(EXTRA_TEXT)
                if (b64 != null) {
                    try {
                        val decoded = Base64.decode(b64, Base64.DEFAULT)
                        val text = String(decoded, Charsets.UTF_8)
                        cb.setPrimaryClip(ClipData.newPlainText("", text))
                        resultCode = Activity.RESULT_OK
                        resultData = "Text is copied into clipboard."
                        Log.d(TAG, "Set clipboard via b64: ${decoded.size} bytes → ${text.length} chars")
                    } catch (e: Exception) {
                        resultCode = Activity.RESULT_CANCELED
                        resultData = "Failed to decode base64: ${e.message}"
                        Log.e(TAG, "Base64 decode failed", e)
                    }
                } else {
                    resultCode = Activity.RESULT_CANCELED
                    resultData = "No text provided."
                }
            }

            ACTION_GET, ACTION_GET_S -> {
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
