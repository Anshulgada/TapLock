package com.taplock.app

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

/**
 * Center-screen lock animation: icon appears, shackle closes, screen dims to
 * black, then the device locks.
 */
class LockAnimationActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_animation)

        val dimLayer = findViewById<View>(R.id.dim_layer)
        val lockIcon = findViewById<ImageView>(R.id.lock_icon)

        lockIcon.setImageResource(R.drawable.ic_lock_open)
        dimLayer.alpha = 0f
        lockIcon.alpha = 0f
        lockIcon.scaleX = 0.92f
        lockIcon.scaleY = 0.92f

        lockIcon.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        dimLayer.animate()
            .alpha(DIM_LIGHT)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()

        lockIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction { playShackleFlipbook(lockIcon, dimLayer) }
            .start()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun playShackleFlipbook(lockIcon: ImageView, dimLayer: View) {
        handler.postDelayed({
            lockIcon.setImageResource(R.drawable.ic_lock_mid)

            handler.postDelayed({
                lockIcon.setImageResource(R.drawable.ic_lock)

                handler.postDelayed({
                    playShutdown(lockIcon, dimLayer)
                }, FRAME_CLOSED_MS)
            }, FRAME_MID_MS)
        }, FRAME_OPEN_MS)
    }

    private fun playShutdown(lockIcon: ImageView, dimLayer: View) {
        lockIcon.animate()
            .alpha(0.35f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(SHUTDOWN_MS)
            .setInterpolator(AccelerateInterpolator())
            .start()

        dimLayer.animate()
            .alpha(1f)
            .setDuration(SHUTDOWN_MS)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction { lockAndFinish() }
            .start()
    }

    private fun lockAndFinish() {
        LockAccessibilityService.lockScreenWithRetry(handler) { success ->
            if (!success && BuildConfig.DEBUG) {
                Log.w(TAG, "Lock failed — accessibility service not connected")
            }
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val TAG = "LockAnimationActivity"
        private const val DIM_LIGHT = 0.18f
        private const val ENTER_MS = 160L
        private const val FRAME_OPEN_MS = 90L
        private const val FRAME_MID_MS = 90L
        private const val FRAME_CLOSED_MS = 70L
        private const val SHUTDOWN_MS = 370L
    }
}
