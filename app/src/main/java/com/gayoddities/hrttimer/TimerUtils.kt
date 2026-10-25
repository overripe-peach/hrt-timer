// TimerUtils.kt
package com.gayoddities.hrttimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

object TimerUtils {


    /**
     * Check if an active timer has expired based on start time and duration.
     * Returns true only if a timer was started (startTime > 0) and the elapsed time has reached the duration.
     */
    fun isTimerExpired(startTime: Long, duration: Long, currentTime: Long = System.currentTimeMillis()): Boolean {
        //if timer starting time and duration are not 0 AND the current time is past the target time, return true
        //return (startTime <= 0L || duration <= 0L) || currentTime >= (startTime + duration)
        return getRemainingTime(startTime,duration,currentTime) <= 0
    }

    /**
     * Convenience overload that automatically fetches the user's TimerPreferences.
     */
    fun isTimerExpired(context: Context, appWidgetId: Int) : Boolean {
        val currentTime = System.currentTimeMillis()
        val startTime = TimerPreferences.getStartTime(context,appWidgetId)
        val duration = TimerPreferences.getCurrentTimerDurationMS(context, appWidgetId)
        return isTimerExpired(startTime, duration, currentTime)
    }

    /**
     * Calculate remaining time in milliseconds.
     */
    fun getRemainingTime(startTime: Long, duration: Long, currentTime: Long = System.currentTimeMillis()): Long {
        if (startTime <= 0L || duration <= 0L) return 0L
        val targetTime = startTime + duration
        return maxOf(0L, targetTime - currentTime)
    }

    /**Overload function that automatically fetches TimerPreferences and returns remaining Time
     *
     */
    @Suppress("unused")
    fun getRemainingTime(context: Context, appWidgetId: Int): Long {
        val startTime = TimerPreferences.getStartTime(context, appWidgetId)
        val currentTime = System.currentTimeMillis()
        val duration = TimerPreferences.getCurrentTimerDurationMS(context, appWidgetId)
        return getRemainingTime(startTime, duration, currentTime)
    }

    fun getFinishTime(context:Context, appWidgetId: Int): Long {
        return TimerPreferences.getStartTime(context,appWidgetId) + TimerPreferences.getDurationMS(context,appWidgetId)
    }
   /////
    /**
     * Centralized function to schedule the alarm for when the timer completes,
     * complete with Android 12+ (API 31+) exact alarm permission checks and fallback.
     */
    fun scheduleTimerAlarm(context: Context, targetTime: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val alarmIntent = Intent(context, TimerAlarmReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        // Check exact alarm permission on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    alarmIntent
                )
                Log.d("TimerUtils", "Exact alarm scheduled for $targetTime")
            } else {
                // Fallback to standard inexact alarm if exact permission isn't available
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    targetTime,
                    alarmIntent
                )
                Log.d("TimerUtils", "Inexact alarm scheduled (exact permission not granted)")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                targetTime,
                alarmIntent
            )
            Log.d("TimerUtils", "Exact alarm scheduled for $targetTime")
        }
    }
}