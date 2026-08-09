package com.taplock.app

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit tests for [SecureSettingsGate] ghost-mode settings I/O. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SecureSettingsGateTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
        Settings.Secure.putInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
    }

    @Test
    fun isGranted_falseWithoutPermission() {
        assertFalse(SecureSettingsGate.isGranted(context))
    }

    @Test
    fun isGranted_trueAfterGrant() {
        TestFixtures.grantWriteSecureSettings(context)
        assertTrue(SecureSettingsGate.isGranted(context))
    }

    @Test
    fun arm_returnsFalseWithoutPermission() {
        assertFalse(SecureSettingsGate.arm(context))
    }

    @Test
    fun arm_persistsServiceInSettings() {
        TestFixtures.grantWriteSecureSettings(context)
        val flattened = TestFixtures.flattenedService(context)

        assertTrue(SecureSettingsGate.arm(context))

        val readBack = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        assertTrue(AccessibilityServicesList.contains(readBack, flattened))
        assertEquals(1, Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0))
    }

    @Test
    fun arm_preservesExistingServices() {
        TestFixtures.grantWriteSecureSettings(context)
        val foreign = "com.other/com.other.Service"
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            foreign
        )

        assertTrue(SecureSettingsGate.arm(context))

        val readBack = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        assertTrue(AccessibilityServicesList.contains(readBack, foreign))
        assertTrue(AccessibilityServicesList.contains(readBack, TestFixtures.flattenedService(context)))
    }

    @Test
    fun disarm_removesOnlyOurService() {
        TestFixtures.grantWriteSecureSettings(context)
        val foreign = "com.other/com.other.Service"
        val flattened = TestFixtures.flattenedService(context)
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "$foreign:$flattened"
        )
        Settings.Secure.putInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)

        SecureSettingsGate.disarm(context)

        val readBack = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        assertFalse(AccessibilityServicesList.contains(readBack, flattened))
        assertTrue(AccessibilityServicesList.contains(readBack, foreign))
        assertEquals(1, Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0))
    }

    @Test
    fun disarm_clearsAccessibilityEnabledWhenLastService() {
        TestFixtures.grantWriteSecureSettings(context)
        SecureSettingsGate.arm(context)
        SecureSettingsGate.disarm(context)

        assertEquals("", Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: "")
        assertEquals(0, Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1))
    }

    @Test
    fun adbCommands_includePackageName() {
        assertTrue(SecureSettingsGate.adbGrantCommand(context).contains(context.packageName))
        assertTrue(SecureSettingsGate.ecmAllowCommand(context).contains(context.packageName))
        assertTrue(SecureSettingsGate.adbGrantCommand(context).contains(Manifest.permission.WRITE_SECURE_SETTINGS))
        assertTrue(SecureSettingsGate.ecmAllowCommand(context).contains("ACCESS_RESTRICTED_SETTINGS"))
    }

    @Test
    fun cleanupDeveloperOptions_requiresGrant() {
        assertFalse(SecureSettingsGate.cleanupDeveloperOptions(context))
    }

    @Test
    fun cleanupDeveloperOptions_succeedsWithGrant() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )

        assertTrue(SecureSettingsGate.cleanupDeveloperOptions(context))
        assertFalse(SecureSettingsGate.isDeveloperOptionsEnabled(context))
    }

    @Test
    fun enableDeveloperOptions_succeedsWithGrant() {
        TestFixtures.grantWriteSecureSettings(context)

        assertTrue(SecureSettingsGate.enableDeveloperOptions(context))
        assertTrue(SecureSettingsGate.isDeveloperOptionsEnabled(context))
    }

    @Test
    fun isDeveloperOptionsEnabled_falseWithoutGrant() {
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        assertFalse(SecureSettingsGate.isDeveloperOptionsEnabled(context))
    }

    @Test
    fun isDeveloperOptionsEnabled_reflectsGlobalSetting() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        assertTrue(SecureSettingsGate.isDeveloperOptionsEnabled(context))
    }

    @Test
    fun setDeveloperOptionsEnabled_noOpWithoutGrant() {
        assertFalse(SecureSettingsGate.setDeveloperOptionsEnabled(context, enabled = true))
    }

    @Test
    fun isUsbDebuggingEnabled_reflectsGlobalSetting() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(context.contentResolver, Settings.Global.ADB_ENABLED, 1)
        assertTrue(SecureSettingsGate.isUsbDebuggingEnabled(context))
    }

    @Test
    fun setUsbDebuggingEnabled_turnsOnWithGrant() {
        TestFixtures.grantWriteSecureSettings(context)
        assertTrue(SecureSettingsGate.setUsbDebuggingEnabled(context, enabled = true))
        assertTrue(SecureSettingsGate.isUsbDebuggingEnabled(context))
    }

    @Test
    fun enableDeveloperOptions_alsoEnablesUsbDebugging() {
        TestFixtures.grantWriteSecureSettings(context)
        assertTrue(SecureSettingsGate.enableDeveloperOptions(context))
        assertTrue(SecureSettingsGate.isDeveloperOptionsEnabled(context))
        assertTrue(SecureSettingsGate.isUsbDebuggingEnabled(context))
    }
}
