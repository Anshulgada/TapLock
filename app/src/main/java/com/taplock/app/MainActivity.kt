package com.taplock.app

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton

/**
 * Home screen for TapLock: setup guidance, status, quick actions, and help.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var titleText: TextView
    private lateinit var versionText: TextView
    private lateinit var ghostPanel: View
    private lateinit var classicPanel: View
    private lateinit var setupPanel: View
    private lateinit var sectionActions: View
    private lateinit var ecmWarningText: TextView
    private lateinit var adbCommandsText: TextView
    private lateinit var helpContentPanel: LinearLayout
    private lateinit var copyCommandsButton: MaterialButton
    private lateinit var enableAccessibilityButton: MaterialButton
    private lateinit var cleanupDevOptionsButton: MaterialButton
    private lateinit var addWidgetButton: MaterialButton
    private lateinit var openDevOptionsButton: MaterialButton
    private lateinit var enableUsbDebuggingButton: MaterialButton
    private lateinit var openAccessibilityButton: MaterialButton
    private lateinit var closeButton: MaterialButton
    private lateinit var toggleHelpButton: MaterialButton

    private var helpExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        titleText = findViewById(R.id.text_title)
        versionText = findViewById(R.id.text_version)
        ghostPanel = findViewById(R.id.panel_ghost_active)
        classicPanel = findViewById(R.id.panel_classic_active)
        setupPanel = findViewById(R.id.panel_setup_needed)
        sectionActions = findViewById(R.id.text_section_actions)
        ecmWarningText = findViewById(R.id.text_ecm_warning)
        adbCommandsText = findViewById(R.id.text_adb_commands)
        helpContentPanel = findViewById(R.id.panel_help_content)
        copyCommandsButton = findViewById(R.id.btn_copy_commands)
        enableAccessibilityButton = findViewById(R.id.btn_enable_accessibility)
        cleanupDevOptionsButton = findViewById(R.id.btn_cleanup_dev_options)
        addWidgetButton = findViewById(R.id.btn_add_widget)
        openDevOptionsButton = findViewById(R.id.btn_open_dev_options)
        enableUsbDebuggingButton = findViewById(R.id.btn_enable_usb_debugging)
        openAccessibilityButton = findViewById(R.id.btn_open_accessibility)
        closeButton = findViewById(R.id.btn_close)
        toggleHelpButton = findViewById(R.id.btn_toggle_help)

        versionText.text = getString(R.string.home_version, BuildConfig.VERSION_NAME)
        applyWindowInsets()

        copyCommandsButton.setOnClickListener { copySetupCommands() }
        enableAccessibilityButton.setOnClickListener { openAccessibilitySettings() }
        openAccessibilityButton.setOnClickListener { openAccessibilitySettings() }
        openDevOptionsButton.setOnClickListener { openDeveloperOptions() }
        enableUsbDebuggingButton.setOnClickListener { enableUsbDebugging() }
        cleanupDevOptionsButton.setOnClickListener { toggleDeveloperOptions() }
        addWidgetButton.setOnClickListener { showWidgetHelpDialog() }
        closeButton.setOnClickListener { finishAndRemoveTask() }
        toggleHelpButton.setOnClickListener { toggleHelp() }

        if (savedInstanceState != null) {
            helpExpanded = savedInstanceState.getBoolean(KEY_HELP_EXPANDED, false)
            applyHelpExpandedState()
        }

        updateUi()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_HELP_EXPANDED, helpExpanded)
    }

    override fun onResume() {
        super.onResume()
        SecureSettingsGate.disarm(this)
        updateUi()
    }

    private fun applyWindowInsets() {
        val content = findViewById<View>(R.id.main_content)
        ViewCompat.setOnApplyWindowInsetsListener(content) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top + dpToPx(12))
            insets
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    internal fun updateUi() {
        when {
            SecureSettingsGate.isGranted(this) -> showGhostActive()
            LockAccessibilityService.isEnabled(this) -> showClassicActive()
            else -> showSetupNeeded()
        }
        updateDevControls()
    }

    private fun showGhostActive() {
        titleText.setText(R.string.setup_title_active)
        ghostPanel.visibility = View.VISIBLE
        classicPanel.visibility = View.GONE
        setupPanel.visibility = View.GONE

        sectionActions.visibility = View.VISIBLE
        cleanupDevOptionsButton.visibility = View.VISIBLE
        addWidgetButton.visibility = View.VISIBLE
        openAccessibilityButton.visibility = View.VISIBLE
        openAccessibilityButton.setText(R.string.btn_verify_accessibility)
        copyCommandsButton.visibility = View.GONE
        enableAccessibilityButton.visibility = View.GONE
        closeButton.visibility = View.VISIBLE
    }

    private fun showClassicActive() {
        titleText.setText(R.string.setup_title_active)
        ghostPanel.visibility = View.GONE
        classicPanel.visibility = View.VISIBLE
        setupPanel.visibility = View.GONE

        sectionActions.visibility = View.VISIBLE
        cleanupDevOptionsButton.visibility = View.GONE
        openDevOptionsButton.visibility = View.GONE
        enableUsbDebuggingButton.visibility = View.GONE
        addWidgetButton.visibility = View.VISIBLE
        openAccessibilityButton.visibility = View.VISIBLE
        openAccessibilityButton.setText(R.string.btn_open_accessibility)
        copyCommandsButton.visibility = View.GONE
        enableAccessibilityButton.visibility = View.GONE
        closeButton.visibility = View.VISIBLE
    }

    private fun showSetupNeeded() {
        titleText.setText(R.string.setup_title)
        ghostPanel.visibility = View.GONE
        classicPanel.visibility = View.GONE
        setupPanel.visibility = View.VISIBLE

        ecmWarningText.visibility =
            if (SecureSettingsGate.isEcmBlocked(this)) View.VISIBLE else View.GONE

        adbCommandsText.text = buildSetupCommands()

        sectionActions.visibility = View.VISIBLE
        copyCommandsButton.visibility = View.VISIBLE
        cleanupDevOptionsButton.visibility = View.GONE
        openDevOptionsButton.visibility = View.GONE
        enableUsbDebuggingButton.visibility = View.GONE
        addWidgetButton.visibility = View.GONE
        openAccessibilityButton.visibility = View.GONE
        enableAccessibilityButton.visibility = View.VISIBLE
        closeButton.visibility = View.GONE
    }

    internal fun updateDevControls() {
        if (!SecureSettingsGate.isGranted(this)) {
            openDevOptionsButton.visibility = View.GONE
            enableUsbDebuggingButton.visibility = View.GONE
            return
        }
        if (cleanupDevOptionsButton.visibility != View.VISIBLE) {
            openDevOptionsButton.visibility = View.GONE
            enableUsbDebuggingButton.visibility = View.GONE
            return
        }

        val devOptionsOn = SecureSettingsGate.isDeveloperOptionsEnabled(this)
        val usbOn = SecureSettingsGate.isUsbDebuggingEnabled(this)

        cleanupDevOptionsButton.setText(
            if (devOptionsOn) R.string.btn_cleanup_dev_options else R.string.btn_turn_on_dev_options
        )
        openDevOptionsButton.visibility = if (devOptionsOn) View.VISIBLE else View.GONE
        enableUsbDebuggingButton.visibility =
            if (devOptionsOn && !usbOn) View.VISIBLE else View.GONE
    }

    private fun buildSetupCommands(): String = buildString {
        append(SecureSettingsGate.adbGrantCommand(this@MainActivity))
        append('\n')
        append(SecureSettingsGate.ecmAllowCommand(this@MainActivity))
    }

    private fun copySetupCommands() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("TapLock setup", buildSetupCommands()))
        Toast.makeText(this, R.string.toast_commands_copied, Toast.LENGTH_SHORT).show()
    }

    private fun toggleDeveloperOptions() {
        val turnOn = !SecureSettingsGate.isDeveloperOptionsEnabled(this)
        val ok = if (turnOn) {
            SecureSettingsGate.enableDeveloperOptions(this)
        } else {
            SecureSettingsGate.cleanupDeveloperOptions(this)
        }

        Toast.makeText(
            this,
            when {
                !ok -> R.string.toast_dev_options_failed
                turnOn -> R.string.toast_dev_options_on
                else -> R.string.toast_dev_options_off
            },
            Toast.LENGTH_SHORT
        ).show()

        updateDevControls()
    }

    private fun enableUsbDebugging() {
        val ok = SecureSettingsGate.setUsbDebuggingEnabled(this, enabled = true)
        Toast.makeText(
            this,
            if (ok) R.string.toast_usb_debug_on else R.string.toast_usb_debug_failed,
            Toast.LENGTH_SHORT
        ).show()
        if (!ok) {
            openDeveloperOptions()
        } else {
            updateDevControls()
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun openDeveloperOptions() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
    }

    private fun showWidgetHelpDialog() {
        val dialog = Dialog(this, R.style.Theme_TapLock_Dialog)
        dialog.setContentView(R.layout.dialog_widget_help)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<MaterialButton>(R.id.btn_dialog_dismiss).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun toggleHelp() {
        helpExpanded = !helpExpanded
        applyHelpExpandedState()
    }

    private fun applyHelpExpandedState() {
        helpContentPanel.visibility = if (helpExpanded) View.VISIBLE else View.GONE
        toggleHelpButton.setText(
            if (helpExpanded) R.string.btn_hide_help else R.string.btn_show_help
        )
    }

    companion object {
        private const val KEY_HELP_EXPANDED = "help_expanded"
    }
}
