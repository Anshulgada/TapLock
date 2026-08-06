package com.taplock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.content.getSystemService

/**
 * Handles widget tap events. Shows a center-screen lock animation, then locks
 * via [LockAccessibilityService]. Opens [MainActivity] only when the service
 * is disabled in system settings.
 */
class LockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_LOCK) return

        if (!LockAccessibilityService.isEnabled(context)) {
            openSetup(context)
            return
        }

        vibrateTap(context)

        val animationIntent = Intent(context, LockAnimationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(animationIntent)
    }

    private fun openSetup(context: Context) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Lock unavailable — opening setup")
        }
        val setupIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(setupIntent)
    }

    private fun vibrateTap(context: Context) {
        try {
            val vibrator = context.getSystemService<Vibrator>() ?: return
            if (!vibrator.hasVibrator()) return
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "Vibrate permission missing", e)
        }
    }

    companion object {
        const val ACTION_LOCK = "com.taplock.app.ACTION_LOCK"

        private const val TAG = "LockReceiver"
    }
}
