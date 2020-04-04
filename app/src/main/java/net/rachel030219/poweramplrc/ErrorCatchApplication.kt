package net.rachel030219.poweramplrc

import android.app.Application

class ErrorCatchApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        val crashHandler = SAFCrashHandler().apply {
            init(this@ErrorCatchApplication)
        }
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }
}