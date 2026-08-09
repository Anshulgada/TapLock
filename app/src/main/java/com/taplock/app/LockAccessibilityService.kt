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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Minimal accessibility service used only to perform [GLOBAL_ACTION_LOCK_SCREEN].
 *
 * In ghost mode the service is flash-enabled via [SecureSettingsGate], locks on
 * connect, then removes itself. In classic mode it stays enabled in system settings.
 */
@Keep
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Service connected")
        executeArmedLockIfNeeded()
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    private fun executeArmedLockIfNeeded() {
        if (!armed.get()) return
        // Ghost mode waits for the animation to finish before locking.
        if (ghostMode.get()) return

        val locked = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        if (!locked) {
            Log.w(TAG, "executeArmedLockIfNeeded: performGlobalAction returned false")
        }
        armed.set(false)
        notifyLockResult(locked)
    }

    companion object {

        private const val TAG = "LockAccessibilityService"

        @Keep
        @Volatile
        private var instance: LockAccessibilityService? = null

        private val armed = AtomicBoolean(false)
        private val ghostMode = AtomicBoolean(false)

        @Volatile
        private var lockResultCallback: ((Boolean) -> Unit)? = null

        /** Classic mode: service appears in system accessibility settings. */
        fun isEnabled(context: Context): Boolean {
            val component = ComponentName(context, LockAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(':').any { it.equals(component.flattenToString(), true) }
        }

        fun canLock(context: Context): Boolean =
            SecureSettingsGate.isGranted(context) || isEnabled(context)

        /**
         * Prepare for an imminent lock. In ghost mode [SecureSettingsGate.arm] must be called
         * before the service binds.
         */
        fun setArmed(
            isArmed: Boolean,
            useGhostMode: Boolean = false,
            onLockResult: ((Boolean) -> Unit)? = null
        ) {
            armed.set(isArmed)
            ghostMode.set(isArmed && useGhostMode)
            lockResultCallback = if (isArmed) onLockResult else null
            if (isArmed) {
                instance?.executeArmedLockIfNeeded()
            }
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

        /** Lock, then remove ghost-mode accessibility entry from system settings. */
        fun completeGhostLock(context: Context): Boolean {
            val service = instance
            if (service == null) {
                Log.w(TAG, "completeGhostLock: service not connected yet")
                return false
            }
            val locked = service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            if (!locked) {
                Log.w(TAG, "completeGhostLock: performGlobalAction returned false")
            }
            service.disableSelf()
            SecureSettingsGate.disarm(context)
            armed.set(false)
            ghostMode.set(false)
            return locked
        }

        fun lockScreenWithRetry(
            handler: Handler = Handler(Looper.getMainLooper()),
            maxAttempts: Int = 80,
            intervalMs: Long = 25L,
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

        fun completeGhostLockWithRetry(
            context: Context,
            handler: Handler = Handler(Looper.getMainLooper()),
            maxAttempts: Int = 80,
            intervalMs: Long = 25L,
            onResult: (Boolean) -> Unit
        ) {
            var attempts = 0
            val attemptLock = object : Runnable {
                override fun run() {
                    if (completeGhostLock(context)) {
                        onResult(true)
                        return
                    }
                    attempts++
                    if (attempts < maxAttempts) {
                        handler.postDelayed(this, intervalMs)
                    } else {
                        Log.e(TAG, "completeGhostLockWithRetry: gave up after $maxAttempts attempts")
                        SecureSettingsGate.disarm(context)
                        armed.set(false)
                        ghostMode.set(false)
                        onResult(false)
                    }
                }
            }
            handler.post(attemptLock)
        }

        private fun notifyLockResult(success: Boolean) {
            val callback = lockResultCallback
            lockResultCallback = null
            callback?.invoke(success)
        }
    }
}
