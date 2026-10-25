package com.gayoddities.hrttimer

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import android.content.SharedPreferences
//import androidx.glance.GlanceId


object TimerPreferences {
    private const val PREFS_NAME = "hrt_timer_prefs"
    private const val KEY_START_TIME = "start_time"
    //this determines the timer duration for the next time you start the timer
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_MEDICINE_NAME = "medicine_name"
    //Ready state background color store
    private const val KEY_READY_COLOR = "ready_color"
    //this holds the timer duration for the current running timers
    private const val KEY_CURRENT_TIMER_DURATION_MS = "current_timer_duration_ms"
    //Background ready state defaults to green
    const val DEFAULT_READY_COLOR = 0xFF4CAF50.toInt()
    // Default to 5 days if none is set
    const val DEFAULT_DURATION = 5 * 24 * 60 * 60 * 1000L
    const val DEFAULT_MEDICINE_NAME = "HRT"
    const val RUNNING_COLOR = 0xFF333333.toInt()
    const val DAYS_MS = 24 * 60 * 60 * 1000L //or 86,400,000

    /**
     * Returns a SharedPreferences instance scoped to a specific widget instance ID if provided,
     * ensuring independent storage per widget.
     */
    private fun getPrefs(context: Context, appWidgetId: Int): SharedPreferences {
        val name = if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            "${PREFS_NAME}_$appWidgetId"
        } else {
            PREFS_NAME
        }
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    fun startTimer(context: Context, appWidgetId: Int) {
        val currentTime = System.currentTimeMillis()

        //take duration_ms and place it into current_timer_duration_ms.
        //This prevents settings page from updating the finish date / time
        //on a current running timer
        val newDuration =  getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION)
        getPrefs(context, appWidgetId).edit(commit = true) { putLong(KEY_CURRENT_TIMER_DURATION_MS, newDuration) }

        //place system current time into start time key
        getPrefs(context, appWidgetId).edit(commit = true) { putLong(KEY_START_TIME, currentTime) }
        Log.d("TimerPreferences", "Timer started at $currentTime for widget ID: $appWidgetId")
    }


    fun resetTimer(context: Context, appWidgetId: Int) {
        getPrefs(context, appWidgetId).edit (commit = true) { putLong(KEY_START_TIME, -1L) }
        Log.d("TimerPreferences", "Reset timer for widget ID: $appWidgetId")
    }

    fun getDurationMS(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION)
    }

    fun getDurationDays(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION) / (24L * 60L * 60L * 1000L)
    }

    fun getDurationHours(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION) / (60L * 60L * 1000L)
    }

    fun setDurationMS(context: Context, appWidgetId: Int, duration: Long) {
        getPrefs(context, appWidgetId).edit(commit = true) { putLong(KEY_DURATION_MS, duration) }
    }

    fun getCurrentTimerDurationMS(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_CURRENT_TIMER_DURATION_MS, DEFAULT_DURATION)
    }

    fun getStartTime(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_START_TIME, -1L)
    }

    fun getMedicineName(context: Context, appWidgetId: Int): String {
        return getPrefs(context, appWidgetId).getString(KEY_MEDICINE_NAME, DEFAULT_MEDICINE_NAME) ?: DEFAULT_MEDICINE_NAME
    }

    fun setMedicineName(context: Context, appWidgetId: Int, name: String) {
        getPrefs(context, appWidgetId).edit(commit = true) { putString(KEY_MEDICINE_NAME, name.ifBlank { DEFAULT_MEDICINE_NAME }) }
    }

    fun getReadyColor(context: Context, appWidgetId: Int): Int {
        return getPrefs(context, appWidgetId).getInt(KEY_READY_COLOR, DEFAULT_READY_COLOR)
    }

    fun setReadyColor(context: Context, appWidgetId: Int, colorInt: Int) {
        getPrefs(context, appWidgetId).edit(commit = true) { putInt(KEY_READY_COLOR, colorInt) }
    }
}