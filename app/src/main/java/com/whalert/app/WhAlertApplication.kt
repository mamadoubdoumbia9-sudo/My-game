package com.whalert.app

import android.app.Application

class WhAlertApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // No analytics, no tracking. Tech logs stay on-device (SecureStore).
    }
}
