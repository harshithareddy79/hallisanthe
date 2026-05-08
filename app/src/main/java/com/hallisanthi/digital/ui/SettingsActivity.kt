package com.hallisanthi.digital.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.hallisanthi.digital.R
import com.hallisanthi.digital.data.AppSettings

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val toolbar = findViewById<MaterialToolbar>(R.id.settingsToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"

        // Dark mode toggle
        val darkSwitch = findViewById<Switch>(R.id.darkModeSwitch)
        darkSwitch.isChecked = AppSettings.isDarkMode(this)
        darkSwitch.setOnCheckedChangeListener { _, on ->
            AppSettings.setDarkMode(this, on)
        }

        // Notifications toggle
        val notifSwitch = findViewById<Switch>(R.id.notifSwitch)
        notifSwitch.isChecked = AppSettings.isNotifsEnabled(this)
        notifSwitch.setOnCheckedChangeListener { _, on ->
            AppSettings.setNotifsEnabled(this, on)
        }

        // App version row
        val versionView = findViewById<TextView?>(R.id.settingsVersion)
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            versionView?.text = "v${info.versionName}"
        } catch (e: Exception) { versionView?.text = "v7.0" }

        // About row
        findViewById<android.view.View?>(R.id.settingsAboutRow)?.setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("About Halli-Santhe Digital")
                .setMessage("Version 7.0\n\nA hyper-local marketplace connecting village artisans with urban buyers.\n\n🏺 Vocal for Local\n🧵 Made in Karnataka")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
