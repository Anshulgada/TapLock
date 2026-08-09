package com.taplock.app

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Center-screen lock animation: shackle closes while the screen dims, then locks.
 *
 * Ghost mode arms [SecureSettingsGate] at t=0. The lock fires once the dim overlay
 * is fully opaque so the system lock screen never flashes through.
 */
class LockAnimationActivity : AppCompatActivity() {

    private val animHandler = Handler(Looper.getMainLooper())
    private val lockHandler = Handler(Looper.getMainLooper())

    private val useGhostMode: Boolean by lazy { SecureSettingsGate.isGranted(this) }
    private var lockTriggered = false
    private var lockAttemptFinished = false
    private var finishScheduled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_animation)
        configureLockWindow()

        if (useGhostMode) {
            val armed = SecureSettingsGate.arm(this)
            if (!armed) {
                Log.e(TAG, "Ghost mode arm failed — opening setup")
                finish()
                startActivity(
                    android.content.Intent(this, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                )
                return
            }
            LockAccessibilityService.setArmed(isArmed = true, useGhostMode = true)
        }

        val dimLayer = findViewById<View>(R.id.dim_layer)
        val lockIcon = findViewById<ImageView>(R.id.lock_icon)

        lockIcon.setImageResource(R.drawable.ic_lock_open)
        dimLayer.alpha = 0f
        lockIcon.alpha = 0f
        lockIcon.scaleX = 0.92f
        lockIcon.scaleY = 0.92f

        lockIcon.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        dimLayer.animate()
            .alpha(1f)
            .setDuration(DIM_RAMP_MS)
            .setInterpolator(AccelerateInterpolator(2f))
            .start()

        playShackleFlipbook(lockIcon)

        lockIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        animHandler.postDelayed({ triggerLock() }, LOCK_AT_MS)
        animHandler.postDelayed({ scheduleFinish() }, FINISH_MS)
        animHandler.postDelayed({
            if (!lockAttemptFinished) {
                Log.w(TAG, "Lock timed out — finishing overlay")
                lockAttemptFinished = true
                maybeFinish()
            }
        }, FINISH_MS + 800L)
    }

    private fun configureLockWindow() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        }
    }

    override fun onDestroy() {
        animHandler.removeCallbacksAndMessages(null)
        lockHandler.removeCallbacksAndMessages(null)
        LockAccessibilityService.setArmed(isArmed = false)
        if (useGhostMode && !lockAttemptFinished) {
            SecureSettingsGate.disarm(this)
        }
        super.onDestroy()
    }

    private fun playShackleFlipbook(lockIcon: ImageView) {
        animHandler.postDelayed({
            lockIcon.setImageResource(R.drawable.ic_lock_mid)
        }, FRAME_OPEN_MS)

        animHandler.postDelayed({
            lockIcon.setImageResource(R.drawable.ic_lock)
        }, FRAME_OPEN_MS + FRAME_MID_MS)

        animHandler.postDelayed({
            lockIcon.animate()
                .alpha(0.35f)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(SHUTDOWN_MS)
                .setInterpolator(AccelerateInterpolator())
                .start()
        }, FRAME_OPEN_MS + FRAME_MID_MS + FRAME_CLOSED_MS)
    }

    private fun triggerLock() {
        if (lockTriggered) return
        lockTriggered = true

        findViewById<View>(R.id.dim_layer).alpha = 1f

        if (useGhostMode) {
            LockAccessibilityService.completeGhostLockWithRetry(
                context = this,
                handler = lockHandler,
                maxAttempts = RETRY_MAX_ATTEMPTS,
                intervalMs = RETRY_INTERVAL_MS
            ) { success ->
                if (!success) {
                    Log.e(TAG, "Ghost lock failed — check TapLock setup")
                }
                lockAttemptFinished = true
                maybeFinish()
            }
            return
        }

        LockAccessibilityService.lockScreenWithRetry(
            handler = lockHandler,
            maxAttempts = RETRY_MAX_ATTEMPTS,
            intervalMs = RETRY_INTERVAL_MS
        ) { success ->
            if (!success) {
                Log.e(TAG, "Lock failed — check TapLock setup")
            }
            lockAttemptFinished = true
            maybeFinish()
        }
    }

    private fun scheduleFinish() {
        finishScheduled = true
        maybeFinish()
    }

    private fun maybeFinish() {
        if (finishScheduled && lockAttemptFinished) {
            finishWithNoTransition()
        }
    }

    private fun finishWithNoTransition() {
        if (isFinishing) return
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val TAG = "LockAnimationActivity"
        private const val ENTER_MS = 150L
        private const val FRAME_OPEN_MS = 80L
        private const val FRAME_MID_MS = 80L
        private const val FRAME_CLOSED_MS = 65L
        private const val SHUTDOWN_MS = 335L
        private const val DIM_RAMP_MS = 480L
        private const val LOCK_AT_MS = DIM_RAMP_MS + 40L
        private const val FINISH_MS =
            LOCK_AT_MS + SHUTDOWN_MS + 80L

        private const val RETRY_MAX_ATTEMPTS = 80
        private const val RETRY_INTERVAL_MS = 25L
    }
}
