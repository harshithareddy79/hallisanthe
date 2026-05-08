package com.hallisanthi.digital.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object AppSettings {
    private const val PREFS       = "app_settings"
    private const val KEY_DARK    = "dark_mode"
    private const val KEY_NOTIFS  = "notifs_enabled"

    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false)

    fun setDarkMode(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, on).apply()
        applyDarkMode(on)
    }

    fun applyDarkMode(on: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (on) AppCompatDelegate.MODE_NIGHT_YES
            else    AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applyFromPrefs(context: Context) = applyDarkMode(isDarkMode(context))

    fun isNotifsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFS, true)

    fun setNotifsEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_NOTIFS, on).apply()
    }
}
