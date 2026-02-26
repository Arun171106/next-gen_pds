package com.example.nextgen_pds_kiosk.kiosk

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.nextgen_pds_kiosk.MainActivity
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CrashRestartHandler — Phase K1
 *
 * A global UncaughtExceptionHandler that:
 *  1. Writes a crash log to internal storage for later diagnosis
 *  2. Schedules a 3-second delayed restart of MainActivity via AlarmManager
 *  3. Terminates the current process cleanly
 *
 * Install this in MainActivity.onCreate() to activate watchdog recovery.
 */
class CrashRestartHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    companion object {
        private const val RESTART_DELAY_MS = 3_000L
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_LOG_FILES = 10

        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashRestartHandler(context.applicationContext, defaultHandler)
            )
            Log.i("KioskCrash", "Crash restart watchdog installed")
        }
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            writeCrashLog(throwable)
            scheduleRestart()
        } catch (e: Exception) {
            Log.e("KioskCrash", "Error in crash handler itself: ${e.message}")
        } finally {
            // Kill the process — AlarmManager will restart it
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun writeCrashLog(throwable: Throwable) {
        try {
            val logDir = File(context.filesDir, CRASH_LOG_DIR)
            logDir.mkdirs()

            // Prune old logs if more than MAX_LOG_FILES exist
            val files = logDir.listFiles()?.sortedBy { it.lastModified() } ?: emptyList()
            if (files.size >= MAX_LOG_FILES) {
                files.take(files.size - MAX_LOG_FILES + 1).forEach { it.delete() }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val logFile = File(logDir, "crash_$timestamp.txt")

            PrintWriter(logFile).use { writer ->
                writer.println("=== KIOSK CRASH REPORT ===")
                writer.println("Timestamp: $timestamp")
                writer.println("Thread: ${Thread.currentThread().name}")
                writer.println("")
                throwable.printStackTrace(writer)
            }

            Log.e("KioskCrash", "Crash log written: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("KioskCrash", "Could not write crash log: ${e.message}")
        }
    }

    private fun scheduleRestart() {
        val restartIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.RTC,
            System.currentTimeMillis() + RESTART_DELAY_MS,
            pendingIntent
        )
        Log.i("KioskCrash", "Restart scheduled in ${RESTART_DELAY_MS}ms")
    }
}
