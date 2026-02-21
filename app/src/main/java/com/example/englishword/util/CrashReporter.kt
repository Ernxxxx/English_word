package com.example.englishword.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

interface CrashReporter {
    fun recordException(throwable: Throwable)
    fun log(message: String)
    fun setUserId(userId: String)
}

@Singleton
class DebugCrashReporter @Inject constructor() : CrashReporter {
    override fun recordException(throwable: Throwable) {
        Log.e(TAG, "Non-fatal exception", throwable)
    }

    override fun log(message: String) {
        Log.d(TAG, message)
    }

    override fun setUserId(userId: String) {
        Log.d(TAG, "User ID: $userId")
    }

    companion object {
        private const val TAG = "CrashReporter"
    }
}

@Singleton
class ProductionCrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) : CrashReporter {

    private val logFile: File by lazy {
        File(context.filesDir, CRASH_LOG_FILE).also { file ->
            file.parentFile?.mkdirs()
        }
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    override fun recordException(throwable: Throwable) {
        Log.e(TAG, "Non-fatal exception", throwable)
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        appendToLog("EXCEPTION", "${throwable.message}\n$sw")
    }

    override fun log(message: String) {
        Log.e(TAG, message)
        appendToLog("LOG", message)
    }

    override fun setUserId(userId: String) {
        Log.e(TAG, "User ID set: $userId")
        appendToLog("USER", "User ID: $userId")
    }

    /**
     * Returns recent crash log entries as a string.
     * Reads at most the last [maxLines] lines from the crash log file.
     */
    fun getRecentCrashLogs(maxLines: Int = MAX_LOG_LINES): String {
        return try {
            if (!logFile.exists()) return ""
            val lines = logFile.readLines()
            lines.takeLast(maxLines).joinToString("\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash logs", e)
            ""
        }
    }

    private fun appendToLog(level: String, message: String) {
        try {
            trimLogIfNeeded()
            val timestamp = LocalDateTime.now().format(dateFormatter)
            logFile.appendText("[$timestamp] [$level] $message\n")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
        }
    }

    private fun trimLogIfNeeded() {
        try {
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                val lines = logFile.readLines()
                val trimmed = lines.takeLast(MAX_LOG_LINES / 2)
                logFile.writeText(trimmed.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trim crash log", e)
        }
    }

    companion object {
        private const val TAG = "CrashReporter"
        private const val CRASH_LOG_FILE = "crash_logs.txt"
        private const val MAX_LOG_SIZE_BYTES = 512 * 1024L // 512 KB
        private const val MAX_LOG_LINES = 1000
    }
}
