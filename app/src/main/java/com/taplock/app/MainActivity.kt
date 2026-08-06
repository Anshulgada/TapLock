package com.taplock.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * One-time setup activity. Guides the user to enable the lock accessibility
 * service, then exits and never appears in Recent Apps.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var explanationText: TextView
    private lateinit var enableServiceButton: Button
    private lateinit var successText: TextView
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        explanationText = findViewById(R.id.text_explanation)
        enableServiceButton = findViewById(R.id.btn_enable_admin)
        successText = findViewById(R.id.text_success)
        closeButton = findViewById(R.id.btn_close)

        enableServiceButton.setOnClickListener { openAccessibilitySettings() }
        closeButton.setOnClickListener { finishAndRemoveTask() }

        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun updateUi() {
        val serviceEnabled = LockAccessibilityService.isEnabled(this)

        explanationText.visibility = if (serviceEnabled) View.GONE else View.VISIBLE
        enableServiceButton.visibility = if (serviceEnabled) View.GONE else View.VISIBLE
        successText.visibility = if (serviceEnabled) View.VISIBLE else View.GONE
        closeButton.visibility = if (serviceEnabled) View.VISIBLE else View.GONE
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
