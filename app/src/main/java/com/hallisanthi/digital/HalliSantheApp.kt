package com.hallisanthi.digital

import android.app.Application
import com.hallisanthi.digital.data.AppSettings

class HalliSantheApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.applyFromPrefs(this)
    }
}
