package com.gayoddities.hrttimer

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import android.content.SharedPreferences
//import androidx.glance.GlanceId


object TimerPreferences {
    //TIMER DATA STORE
    private const val PREFS_NAME = "hrt_timer_prefs"
    private const val KEY_START_TIME = "start_time"
    //this determines the timer duration for the next time you start the timer
    private const val KEY_DURATION_MS = "duration_ms"
    private const val KEY_MEDICINE_NAME = "medicine_name"
    //Ready state background color store
    private const val KEY_READY_COLOR = "ready_color"
    //this holds the timer duration for the current running timers
    private const val KEY_CURRENT_TIMER_DURATION_MS = "current_timer_duration_ms"

    //STREAK TRACKING DATA STORE
    private const val KEY_STREAK_COUNT = "streak_count"
    private const val KEY_STREAK_WINDOW_MS = "streak_window_ms"
    private const val KEY_LAST_FINISH_TIME = "last_finish_time"

    //DEFAULTS SETTINGS CONSTANTS
    //Background ready state defaults to green
    const val DEFAULT_READY_COLOR = 0xFF4CAF50.toInt()
    // Default to 5 days if none is set
    const val DEFAULT_DURATION = 5 * 24 * 60 * 60 * 1000L
    const val DEFAULT_MEDICINE_NAME = "HRT"
    const val RUNNING_COLOR = 0xFF333333.toInt()
    const val DEFAULT_STREAK_WINDOW_MS = 12 * 60 * 60 * 1000L // Default 12 hours grace window

    //TIME CONSTANTS
    const val DAYS_MS = 24 * 60 * 60 * 1000L //or 86,400,000
    const val HOUR_MS = 60 * 60 * 1000L //or 3,600,000
    const val MINUTE_MS = 60 * 1000L //or 60,000

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
        val prefs = getPrefs(context, appWidgetId)
        val lastFinishTime = getLastFinishTime(context, appWidgetId)
        val streakWindow = getStreakWindowMS(context, appWidgetId)

        //take duration_ms and place it into current timer duration_MS.
        //This prevents settings page from updating the finish date / time
        //on a current running timer
        val newDuration =  getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION)
        prefs.edit(commit = true) { putLong(KEY_CURRENT_TIMER_DURATION_MS, newDuration) }

        //place system current time into start time key
        prefs.edit(commit = true) { putLong(KEY_START_TIME, currentTime) }
        Log.d("TimerPreferences", "Timer started at $currentTime for widget ID: $appWidgetId")

        //set streak window to 0 to disable streak
        if (streakWindow > 0) {
            //now we assess streak status
            if (lastFinishTime > 0L) {
                val deadline = lastFinishTime + streakWindow

                if (currentTime <= deadline) {
                    // Success within grace window: Increment streak
                    incrementStreakCount(context, appWidgetId)
                } else { // Past grace window: Reset streak to 1
                    resetStreakCount(context, appWidgetId)
                }
            } else { //time has never been started
                resetStreakCount(context, appWidgetId)
            }
        }
    }

    fun resetTimer(context: Context, appWidgetId: Int) {
        getPrefs(context, appWidgetId).edit (commit = true) {
            putLong(KEY_START_TIME, -1L)
            putInt(KEY_STREAK_COUNT, 0)
            putLong( KEY_LAST_FINISH_TIME, -1L)
        }
        Log.d("TimerPreferences", "Reset timer for widget ID: $appWidgetId")
    }

    fun expireTimer(context: Context, appWidgetId: Int) {
        val finishTime = System.currentTimeMillis()

        getPrefs(context, appWidgetId).edit(commit = true) {
            putLong(KEY_LAST_FINISH_TIME, finishTime)
            putLong(KEY_START_TIME, -1L)
        }
        Log.d("TimerPreferences", "Timer expired for widget ID: $appWidgetId. Recorded finish time: $finishTime")
    }

    fun getDurationMS(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_DURATION_MS, DEFAULT_DURATION)
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

    //STREAK FUNCTIONS
    // --- New Streak Methods ---

    fun getStreakCount(context: Context, appWidgetId: Int): Int {
        return getPrefs(context, appWidgetId).getInt(KEY_STREAK_COUNT, 0)
    }

    fun incrementStreakCount(context: Context, appWidgetId: Int): Int {
        val currentStreak = getStreakCount(context, appWidgetId) + 1
        getPrefs(context, appWidgetId).edit(commit = true) { putInt(KEY_STREAK_COUNT, currentStreak) }
        return currentStreak
    }

    fun resetStreakCount(context: Context, appWidgetId: Int) {
        getPrefs(context, appWidgetId).edit(commit = true) { putInt(KEY_STREAK_COUNT, 0) }
    }

    fun getStreakWindowMS(context: Context, appWidgetId: Int): Long {
        return getPrefs(context, appWidgetId).getLong(KEY_STREAK_WINDOW_MS, DEFAULT_STREAK_WINDOW_MS)
    }

    fun setStreakWindowMS(context: Context, appWidgetId: Int, windowMs: Long) {
        getPrefs(context, appWidgetId).edit(commit = true) { putLong(KEY_STREAK_WINDOW_MS, windowMs) }
    }

    fun getLastFinishTime(context: Context, appWidgetId: Int): Long {
        return getPrefs(context,appWidgetId).getLong(KEY_LAST_FINISH_TIME, -1L)
    }
}