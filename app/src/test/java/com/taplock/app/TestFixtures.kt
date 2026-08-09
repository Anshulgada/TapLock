package com.taplock.app

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Context
import org.robolectric.Shadows.shadowOf

object TestFixtures {

    fun serviceComponent(context: Context): ComponentName =
        ComponentName(context, LockAccessibilityService::class.java)

    fun flattenedService(context: Context): String =
        serviceComponent(context).flattenToString()

    fun grantWriteSecureSettings(context: Context) {
        val app = context.applicationContext as Application
        shadowOf(app).grantPermissions(Manifest.permission.WRITE_SECURE_SETTINGS)
    }
}
