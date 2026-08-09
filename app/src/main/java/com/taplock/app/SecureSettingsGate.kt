package com.taplock.app

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Ghost-mode gate: flash-enable [LockAccessibilityService] via
 * [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES], then scrub it after lock.
 *
 * Requires a one-time `adb shell pm grant … WRITE_SECURE_SETTINGS`.
 */
object SecureSettingsGate {

    private const val TAG = "SecureSettingsGate"
    private const val OP_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"

    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * True when Android 13+ ECM blocks programmatic accessibility enablement.
     * [AppOpsManager.MODE_DEFAULT] is treated as allowed (common on Android 16).
     */
    fun isEcmBlocked(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        return try {
            val mode = appOps.unsafeCheckOpNoThrow(
                OP_ACCESS_RESTRICTED_SETTINGS,
                context.applicationInfo.uid,
                context.packageName
            )
            mode != AppOpsManager.MODE_ALLOWED && mode != AppOpsManager.MODE_DEFAULT
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "ECM check failed", e)
            false
        }
    }

    fun serviceComponent(context: Context): ComponentName =
        ComponentName(context, LockAccessibilityService::class.java)

    /**
     * Append our service to the enabled list and verify the write persisted.
     * Returns false if the ROM silently reverted the change or ECM stripped it.
     */
    fun arm(context: Context): Boolean {
        if (!isGranted(context)) return false

        val resolver = context.contentResolver
        val flattened = serviceComponent(context).flattenToString()
        val current = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val updated = AccessibilityServicesList.append(current, flattened)

        Settings.Secure.putString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            updated
        )
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)

        val readBack = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val persisted = AccessibilityServicesList.contains(readBack, flattened)
        if (!persisted && BuildConfig.DEBUG) {
            Log.w(TAG, "arm: write did not persist (ECM or ROM rollback?)")
        }
        return persisted
    }

    /** Remove our service from the enabled list; leave other services untouched. */
    fun disarm(context: Context) {
        if (!isGranted(context)) return

        val resolver = context.contentResolver
        val flattened = serviceComponent(context).flattenToString()
        val current = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val updated = AccessibilityServicesList.remove(current, flattened)

        Settings.Secure.putString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            updated
        )
        if (updated.isEmpty()) {
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        }
    }

    /** Turn off developer options and ADB flags so UPI apps stop blocking. */
    fun cleanupDeveloperOptions(context: Context): Boolean =
        setDeveloperOptionsEnabled(context, enabled = false)

    /** Re-enable developer options and USB debugging (e.g. before reinstall setup). */
    fun enableDeveloperOptions(context: Context): Boolean {
        if (!setDeveloperOptionsEnabled(context, enabled = true)) return false
        return setUsbDebuggingEnabled(context, enabled = true)
    }

    fun isDeveloperOptionsEnabled(context: Context): Boolean {
        if (!isGranted(context)) return false
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    }

    fun setDeveloperOptionsEnabled(context: Context, enabled: Boolean): Boolean {
        if (!isGranted(context)) return false
        val resolver = context.contentResolver
        return try {
            Settings.Global.putInt(
                resolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                if (enabled) 1 else 0
            )
            if (!enabled) {
                Settings.Global.putInt(resolver, Settings.Global.ADB_ENABLED, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Settings.Global.putInt(resolver, "adb_wifi_enabled", 0)
                }
            }
            true
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setDeveloperOptionsEnabled failed", e)
            false
        }
    }

    fun isUsbDebuggingEnabled(context: Context): Boolean {
        if (!isGranted(context)) return false
        return Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1
    }

    fun setUsbDebuggingEnabled(context: Context, enabled: Boolean): Boolean {
        if (!isGranted(context)) return false
        return try {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                if (enabled) 1 else 0
            )
            true
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) Log.w(TAG, "setUsbDebuggingEnabled failed", e)
            false
        }
    }

    fun adbGrantCommand(context: Context): String =
        "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"

    fun ecmAllowCommand(context: Context): String =
        "adb shell cmd appops set ${context.packageName} ACCESS_RESTRICTED_SETTINGS allow"
}
