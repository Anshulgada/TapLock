package com.taplock.app

import android.app.Application
import android.os.Build
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Android 14 (API 34) ghost-mode compatibility checks.
 *
 * On Android 14+, sideloaded apps need both WRITE_SECURE_SETTINGS and the
 * ACCESS_RESTRICTED_SETTINGS app-op before programmatic accessibility enablement
 * persists. TapLock surfaces the second command in setup when ECM is blocked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34 = Android 14
class SecureSettingsGateAndroid14Test {

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
    fun android14_ghostModeArmWorksWithWriteSecureSettings() {
        TestFixtures.grantWriteSecureSettings(context)
        assertTrue(SecureSettingsGate.isGranted(context))
        assertTrue(SecureSettingsGate.arm(context))
    }

    @Test
    fun android14_ecmCheckDoesNotFalsePositiveOnRobolectric() {
        // Robolectric defaults ACCESS_RESTRICTED_SETTINGS to MODE_DEFAULT, which we treat as allowed.
        assertFalse(SecureSettingsGate.isEcmBlocked(context))
    }

    @Test
    fun android14_setupCommandsIncludeEcmAllow() {
        val ecm = SecureSettingsGate.ecmAllowCommand(context)
        assertTrue(ecm.contains("cmd appops set"))
        assertTrue(ecm.contains("ACCESS_RESTRICTED_SETTINGS allow"))
    }

    @Test
    fun android14_disarmAfterArmLeavesEmptyList() {
        TestFixtures.grantWriteSecureSettings(context)
        SecureSettingsGate.arm(context)
        SecureSettingsGate.disarm(context)
        val readBack = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        assertTrue(readBack.isNullOrEmpty())
    }
}
