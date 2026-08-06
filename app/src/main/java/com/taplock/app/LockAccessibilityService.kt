package com.taplock.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.Keep

/**
 * Minimal accessibility service used only to perform [GLOBAL_ACTION_LOCK_SCREEN].
 */
@Keep
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {

        private const val TAG = "LockAccessibilityService"

        @Keep
        @Volatile
        private var instance: LockAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, LockAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(component.flattenToString(), true) }
        }

        fun lockScreen(): Boolean {
            val service = instance
            if (service == null) {
                Log.w(TAG, "lockScreen: service not connected yet")
                return false
            }
            val locked = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            if (!locked) {
                Log.w(TAG, "lockScreen: performGlobalAction returned false")
            }
            return locked
        }

        fun lockScreenWithRetry(
            handler: Handler = Handler(Looper.getMainLooper()),
            maxAttempts: Int = 40,
            intervalMs: Long = 50L,
            onResult: (Boolean) -> Unit
        ) {
            var attempts = 0
            val attemptLock = object : Runnable {
                override fun run() {
                    if (lockScreen()) {
                        onResult(true)
                        return
                    }
                    attempts++
                    if (attempts < maxAttempts) {
                        handler.postDelayed(this, intervalMs)
                    } else {
                        Log.e(TAG, "lockScreenWithRetry: gave up after $maxAttempts attempts")
                        onResult(false)
                    }
                }
            }
            handler.post(attemptLock)
        }
    }
}
