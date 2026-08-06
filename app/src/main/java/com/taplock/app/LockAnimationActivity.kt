package com.taplock.app

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
 */
class LockAnimationActivity : AppCompatActivity() {

    private val animHandler = Handler(Looper.getMainLooper())
    private val lockHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_animation)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val dimLayer = findViewById<View>(R.id.dim_layer)
        val lockIcon = findViewById<ImageView>(R.id.lock_icon)

        lockIcon.setImageResource(R.drawable.ic_lock_open)
        dimLayer.alpha = 0f
        lockIcon.alpha = 0f
        lockIcon.scaleX = 0.92f
        lockIcon.scaleY = 0.92f

        lockIcon.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)

        // Dim + shackle start together from t=0.
        dimLayer.animate()
            .alpha(1f)
            .setDuration(TOTAL_MS)
            .setInterpolator(AccelerateInterpolator(1.3f))
            .withEndAction { lockAndFinish() }
            .start()

        playShackleFlipbook(lockIcon)

        lockIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(ENTER_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    override fun onDestroy() {
        animHandler.removeCallbacksAndMessages(null)
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

    private fun lockAndFinish() {
        LockAccessibilityService.lockScreenWithRetry(lockHandler) { success ->
            if (!success) {
                Log.e(TAG, "Lock failed — re-enable TapLock in Accessibility settings")
            }
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        private const val TAG = "LockAnimationActivity"
        private const val ENTER_MS = 150L
        private const val FRAME_OPEN_MS = 80L
        private const val FRAME_MID_MS = 80L
        private const val FRAME_CLOSED_MS = 65L
        private const val SHUTDOWN_MS = 335L
        private const val TOTAL_MS = ENTER_MS + FRAME_OPEN_MS + FRAME_MID_MS + FRAME_CLOSED_MS + SHUTDOWN_MS
    }
}
