package com.taplock.app

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device smoke tests. Run with `./gradlew connectedDebugAndroidTest` when a device is attached.
 */
@RunWith(AndroidJUnit4::class)
class TapLockInstrumentedTest {

    @Test
    fun versionMatchesGradleConfig() {
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
        assertTrue(BuildConfig.VERSION_CODE > 0)
    }

    @Test
    fun manifestDeclaresAccessibilityService() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager
        val component = ComponentName(context, LockAccessibilityService::class.java)
        val info = pm.getServiceInfo(component, PackageManager.GET_META_DATA)
        assertNotNull(info)
        assertEquals("com.taplock.app", info.packageName)
    }

    @Test
    fun manifestDeclaresWidgetProvider() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager
        val component = ComponentName(context, LockWidgetProvider::class.java)
        val info = pm.getReceiverInfo(component, PackageManager.GET_META_DATA)
        assertNotNull(info)
        assertTrue(info.exported)
    }

    @Test
    fun manifestDeclaresWriteSecureSettingsPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permission = android.Manifest.permission.WRITE_SECURE_SETTINGS
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        assertTrue(packageInfo.requestedPermissions?.contains(permission) == true)
    }

    @Test
    fun lockAnimationActivityUsesSingleTopLaunchMode() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val pm = context.packageManager
        val component = ComponentName(context, LockAnimationActivity::class.java)
        val info = pm.getActivityInfo(component, PackageManager.GET_META_DATA)
        assertEquals("singleTop", info.launchMode.toLaunchModeName())
    }

    private fun Int.toLaunchModeName(): String = when (this) {
        android.content.pm.ActivityInfo.LAUNCH_SINGLE_TOP -> "singleTop"
        android.content.pm.ActivityInfo.LAUNCH_SINGLE_INSTANCE -> "singleInstance"
        android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK -> "singleTask"
        android.content.pm.ActivityInfo.LAUNCH_MULTIPLE -> "standard"
        else -> "unknown"
    }
}
