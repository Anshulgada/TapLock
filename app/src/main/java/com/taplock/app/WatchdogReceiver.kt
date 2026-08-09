package com.taplock.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Scrubs a leftover ghost-mode accessibility entry after reboot or app update.
 */
class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> SecureSettingsGate.disarm(context)
        }
    }
}
