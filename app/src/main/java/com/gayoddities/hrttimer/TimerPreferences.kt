package com.gayoddities.hrttimer

import android.content.Context
import androidx.core.content.edit
//import android.content.SharedPreferences


object TimerPreferences {
    private const val PREFS_NAME = "hrt_timer_prefs"
    private const val KEY_START_TIME = "start_time"
    private const val KEY_DURATION_DAYS = "duration_days"
    private const val KEY_MEDICINE_NAME = "medicine_name"
    //ready state background color store
    private const val KEY_READY_COLOR = "ready_color"
    //background ready state defaults to green
    const val DEFAULT_READY_COLOR = 0xFF4CAF50.toInt()
    // Default to 5 days if none is set
    const val DEFAULT_DAYS = 5
    const val DEFAULT_MEDICINE_NAME = "HRT"
    //const val TIMER_DURATION_MS = 5 * 24 * 60 * 60 * 1000L//10 * 1000L for 5 second test

    @Suppress("UNUSED_PARAMETER")
    fun startTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putLong(KEY_START_TIME, System.currentTimeMillis()) }
    }

    @Suppress("UNUSED_PARAMETER")
    fun resetTimer(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_START_TIME) }
    }

    fun getDurationMS(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val days = prefs.getInt(KEY_DURATION_DAYS, DEFAULT_DAYS)
        return days * 24 * 60 * 60 * 1000L
    }
    fun setDurationDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_DURATION_DAYS, days) }
    }

    fun getMedicineName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MEDICINE_NAME, DEFAULT_MEDICINE_NAME) ?: DEFAULT_MEDICINE_NAME
    }

    fun setMedicineName(context: Context, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_MEDICINE_NAME, name.ifBlank { DEFAULT_MEDICINE_NAME }) }
    }

    fun getReadyColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_READY_COLOR, DEFAULT_READY_COLOR)
    }

    fun setReadyColor(context: Context, colorInt: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putInt(KEY_READY_COLOR, colorInt) }
    }
}