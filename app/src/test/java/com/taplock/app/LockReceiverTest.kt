package com.taplock.app

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifies [LockReceiver] routing for ghost, classic, and unavailable states. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LockReceiverTest {

    private lateinit var context: Application
    private lateinit var receiver: LockReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = LockReceiver()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
    }

    @Test
    fun actionLock_isStable() {
        assertEquals("com.taplock.app.ACTION_LOCK", LockReceiver.ACTION_LOCK)
    }

    @Test
    fun onReceive_ignoresUnknownAction() {
        receiver.onReceive(context, Intent("com.example.unknown"))
        assertNull(shadowOf(context).peekNextStartedActivity())
    }

    @Test
    fun onReceive_opensSetupWhenLockUnavailable() {
        receiver.onReceive(context, Intent(LockReceiver.ACTION_LOCK))
        val started = shadowOf(context).nextStartedActivity
        assertEquals(ComponentName(context, MainActivity::class.java), started.component)
    }

    @Test
    fun onReceive_opensAnimationWhenGhostModeGranted() {
        TestFixtures.grantWriteSecureSettings(context)
        receiver.onReceive(context, Intent(LockReceiver.ACTION_LOCK))
        assertAnimationStarted()
    }

    @Test
    fun onReceive_opensAnimationWhenClassicModeEnabled() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )
        receiver.onReceive(context, Intent(LockReceiver.ACTION_LOCK))
        assertAnimationStarted()
    }

    @Test
    fun onReceive_doesNotLaunchHomeWhenLockAvailable() {
        TestFixtures.grantWriteSecureSettings(context)
        receiver.onReceive(context, Intent(LockReceiver.ACTION_LOCK))
        val homeLaunch = startedActivities().any {
            it.action == Intent.ACTION_MAIN && it.hasCategory(Intent.CATEGORY_HOME)
        }
        assertEquals(false, homeLaunch)
    }

    private fun assertAnimationStarted() {
        val activities = startedActivities()
        val animation = activities.firstOrNull {
            it.component == ComponentName(context, LockAnimationActivity::class.java)
        }
        assertNotNull("Expected animation among started activities: $activities", animation)
        assertEquals(ComponentName(context, LockAnimationActivity::class.java), animation!!.component)
    }

    private fun startedActivities(): List<Intent> {
        val shadow = shadowOf(context)
        return buildList {
            while (true) {
                val next = shadow.nextStartedActivity ?: break
                add(next)
            }
        }
    }
}
