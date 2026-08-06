package com.taplock.app

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal accessibility service used only to perform [GLOBAL_ACTION_LOCK_SCREEN].
 * This locks the device the same way as the power button, preserving fingerprint
 * and face unlock on the next wake.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {

        @Volatile
        private var instance: LockAccessibilityService? = null

        /** Returns true when the service is enabled in system settings. */
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, LockAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(component.flattenToString(), true) }
        }

        /**
         * Locks the screen using the system lock action. Returns false if the
         * service is not connected yet.
         */
        fun lockScreen(): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }

        /**
         * Retries [lockScreen] until the service connects (common after cold start
         * from a widget tap) or [maxAttempts] is exhausted.
         */
        fun lockScreenWithRetry(
            handler: Handler = Handler(Looper.getMainLooper()),
            maxAttempts: Int = 12,
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
                        onResult(false)
                    }
                }
            }
            handler.post(attemptLock)
        }
    }
}
