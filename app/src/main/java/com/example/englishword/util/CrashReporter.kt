package com.example.englishword.util

import android.util.Log
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
