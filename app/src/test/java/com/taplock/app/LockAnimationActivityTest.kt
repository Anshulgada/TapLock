package com.taplock.app

import android.app.Application
import android.content.Intent
import android.provider.Settings
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit

/** Verifies lock animation overlay behavior without forcing a home-screen launch. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LockAnimationActivityTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
    }

    @Test
    fun onCreate_doesNotLaunchHomeIntent() {
        TestFixtures.grantWriteSecureSettings(context)

        Robolectric.buildActivity(LockAnimationActivity::class.java)
            .create()
            .start()
            .visible()
            .get()

        advanceMainLooper(1, TimeUnit.SECONDS)

        assertFalse(homeIntentStarted())
    }

    @Test
    fun triggerLock_snapsDimLayerFullyOpaque() {
        TestFixtures.grantWriteSecureSettings(context)

        val activity = Robolectric.buildActivity(LockAnimationActivity::class.java)
            .create()
            .start()
            .visible()
            .get()

        advanceMainLooper(600, TimeUnit.MILLISECONDS)

        val dimLayer = activity.findViewById<View>(R.id.dim_layer)
        assertEquals(1f, dimLayer.alpha, 0.001f)
    }

    @Test
    fun classicMode_startsOverlayWithoutSetupRedirect() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )

        val controller = Robolectric.buildActivity(LockAnimationActivity::class.java)
            .create()
            .start()
            .visible()

        val activity = controller.get()
        assertTrue(activity.findViewById<View>(R.id.dim_layer).isShown)
        assertFalse(homeIntentStarted())
    }

    private fun homeIntentStarted(): Boolean {
        val shadow = shadowOf(context)
        return generateSequence { shadow.nextStartedActivity }
            .any { it.action == Intent.ACTION_MAIN && it.hasCategory(Intent.CATEGORY_HOME) }
    }

    private fun advanceMainLooper(duration: Long, unit: TimeUnit) {
        ShadowSystemClock.advanceBy(duration, unit)
        ShadowLooper.idleMainLooper()
    }
}
