package com.taplock.app

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifies [LockReceiver] routes to setup when the lock service is unavailable. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LockReceiverTest {

    private lateinit var context: Application
    private lateinit var receiver: LockReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = LockReceiver()
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
        assertEquals(
            ComponentName(context, MainActivity::class.java),
            started.component
        )
    }
}
