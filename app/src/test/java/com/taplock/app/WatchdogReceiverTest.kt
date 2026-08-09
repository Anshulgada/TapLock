package com.taplock.app

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for [WatchdogReceiver] boot/update scrub behaviour. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WatchdogReceiverTest {

    private lateinit var context: Application
    private lateinit var receiver: WatchdogReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        receiver = WatchdogReceiver()
        TestFixtures.grantWriteSecureSettings(context)
    }

    @Test
    fun bootCompleted_disarmsLeftoverService() {
        SecureSettingsGate.arm(context)
        assertTrue(LockAccessibilityService.isEnabled(context))

        receiver.onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertFalse(LockAccessibilityService.isEnabled(context))
    }

    @Test
    fun myPackageReplaced_disarmsLeftoverService() {
        SecureSettingsGate.arm(context)
        receiver.onReceive(context, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))
        assertFalse(LockAccessibilityService.isEnabled(context))
    }

    @Test
    fun unknownAction_isIgnored() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )
        receiver.onReceive(context, Intent("com.example.unknown"))
        assertTrue(LockAccessibilityService.isEnabled(context))
    }
}
