package com.pro.logcatreader

import android.app.Application

class LogLynxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogger.install(this)
    }
}
