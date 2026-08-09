package com.taplock.app

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for [LockAccessibilityService] companion helpers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LockAccessibilityServiceTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
        LockAccessibilityService.setArmed(isArmed = false)
    }

    @Test
    fun isEnabled_falseWhenNotInSettings() {
        assertFalse(LockAccessibilityService.isEnabled(context))
    }

    @Test
    fun isEnabled_trueWhenListedInSettings() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )
        assertTrue(LockAccessibilityService.isEnabled(context))
    }

    @Test
    fun canLock_trueWithGhostGrant() {
        TestFixtures.grantWriteSecureSettings(context)
        assertTrue(LockAccessibilityService.canLock(context))
    }

    @Test
    fun canLock_trueWithClassicEnabled() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )
        assertTrue(LockAccessibilityService.canLock(context))
    }

    @Test
    fun canLock_falseWhenNeitherModeAvailable() {
        assertFalse(LockAccessibilityService.canLock(context))
    }

    @Test
    fun lockScreen_falseWhenServiceNotConnected() {
        assertFalse(LockAccessibilityService.lockScreen())
    }
}
