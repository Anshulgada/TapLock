package com.taplock.app

import android.app.Application
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.button.MaterialButton
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric integration tests for [MainActivity] setup UI states. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            null
        )
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )
    }

    @Test
    fun setupNeeded_showsAdbCommandsWhenNotConfigured() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.text_explanation).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.text_adb_commands).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_copy_commands).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.panel_ghost_active).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.panel_help).visibility)
    }

    @Test
    fun ghostActive_showsStatusPanelAndActions() {
        TestFixtures.grantWriteSecureSettings(context)

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.panel_ghost_active).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.panel_setup_needed).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_add_widget).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_close).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_cleanup_dev_options).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_open_accessibility).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.btn_copy_commands).visibility)
    }

    @Test
    fun ghostActive_showsUsbDebuggingButtonWhenDevOnButUsbOff() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_enable_usb_debugging).visibility)
    }

    @Test
    fun ghostActive_enablingUsbDebuggingUpdatesButton() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        activity.findViewById<MaterialButton>(R.id.btn_enable_usb_debugging).performClick()

        assertEquals(1, Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ))
        assertEquals(View.GONE, activity.findViewById<View>(R.id.btn_enable_usb_debugging).visibility)
    }

    @Test
    fun ghostActive_showsOpenDevOptionsWhenEnabled() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_open_dev_options).visibility)
    }

    @Test
    fun ghostActive_hidesOpenDevOptionsWhenDisabled() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.GONE, activity.findViewById<View>(R.id.btn_open_dev_options).visibility)
    }

    @Test
    fun ghostActive_devOptionsToggleTurnsBackOn() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        activity.findViewById<MaterialButton>(R.id.btn_cleanup_dev_options).performClick()

        assertEquals(1, Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ))
        assertEquals(1, Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ))
        assertEquals(
            activity.getString(R.string.btn_cleanup_dev_options),
            activity.findViewById<MaterialButton>(R.id.btn_cleanup_dev_options).text.toString()
        )
    }

    @Test
    fun ghostActive_devOptionsButtonOffersTurnOffWhenEnabled() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        val button = activity.findViewById<MaterialButton>(R.id.btn_cleanup_dev_options)
        assertEquals(activity.getString(R.string.btn_cleanup_dev_options), button.text.toString())
    }

    @Test
    fun ghostActive_devOptionsButtonOffersTurnOnWhenDisabled() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        val button = activity.findViewById<MaterialButton>(R.id.btn_cleanup_dev_options)
        assertEquals(activity.getString(R.string.btn_turn_on_dev_options), button.text.toString())
    }

    @Test
    fun ghostActive_devOptionsButtonUpdatesAfterToggleOff() {
        TestFixtures.grantWriteSecureSettings(context)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        val button = activity.findViewById<MaterialButton>(R.id.btn_cleanup_dev_options)
        button.performClick()

        assertEquals(activity.getString(R.string.btn_turn_on_dev_options), button.text.toString())
        assertEquals(0, Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        ))
    }

    @Test
    fun toggleHelp_expandsAndCollapsesContent() {
        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        val helpPanel = activity.findViewById<LinearLayout>(R.id.panel_help_content)
        val toggle = activity.findViewById<MaterialButton>(R.id.btn_toggle_help)

        assertEquals(View.GONE, helpPanel.visibility)

        toggle.performClick()
        assertEquals(View.VISIBLE, helpPanel.visibility)
        assertEquals(activity.getString(R.string.btn_hide_help), toggle.text.toString())

        toggle.performClick()
        assertEquals(View.GONE, helpPanel.visibility)
        assertEquals(activity.getString(R.string.btn_show_help), toggle.text.toString())
    }

    @Test
    fun classicActive_showsClassicSuccessWithoutDevOptions() {
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            TestFixtures.flattenedService(context)
        )

        val activity = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .resume()
            .get()

        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.panel_classic_active).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_open_accessibility).visibility)
        assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.btn_add_widget).visibility)
        assertEquals(View.GONE, activity.findViewById<View>(R.id.btn_cleanup_dev_options).visibility)
    }
}
