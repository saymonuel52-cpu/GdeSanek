package ru.gdesanek

import android.app.Application
import java.io.PrintWriter
import java.io.StringWriter

class CrashLogger : Application() {
    override fun onCreate() {
        super.onCreate()
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                getSharedPreferences("crash", MODE_PRIVATE).edit().putString("log", sw.toString()).apply()
            } catch (ex: Exception) {}
            default?.uncaughtException(t, e)
        }
    }
}
