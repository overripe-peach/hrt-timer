package com.gayoddities.hrttimer

import android.content.Context
import androidx.core.content.edit
import android.content.SharedPreferences


object TimerPreferences {
    private const val PREFS_NAME = "hrt_timer_prefs"
    private const val KEY_START_TIME = "start_time"

    // Change this back to 5 * 24 * 60 * 60 * 1000L when you want the full 5 days!
    const val TIMER_DURATION_MS = 5 * 24 * 60 * 60 * 1000L//10 * 1000L for 5 second test

   fun startTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(KEY_START_TIME, System.currentTimeMillis()) }
    }

    fun resetTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_START_TIME) }
    }
}