package com.gayoddities.hrttimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerAlarmReceiver", "Alarm fired! Clearing timer state and forcing widget update.")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear the start time in DataStore preferences so it resets to Ready / Green
                androidx.glance.appwidget.GlanceAppWidgetManager(context)
                    .getGlanceIds(HRTWidget::class.java)
                    .forEach { glanceId ->
                        updateAppWidgetState(context, glanceId) { prefs ->
                            prefs.remove(TimerKeys.START_TIME)
                        }
                    }

                // Force all widgets to update their UI
                HRTWidget().updateAll(context)
                Log.d("TimerAlarmReceiver", "Widget state cleared and updateAll finished successfully.")
            } catch (e: Exception) {
                Log.e("TimerAlarmReceiver", "Failed to update widget on alarm", e)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device rebooted. Re-evaluating and rescheduling active timers...")

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    androidx.glance.appwidget.GlanceAppWidgetManager(context)
                        .getGlanceIds(HRTWidget::class.java)
                        .forEach { glanceId ->
                            updateAppWidgetState(context, glanceId) { prefs ->
                                val startTime = prefs[TimerKeys.START_TIME] ?: 0L
                                val timerDuration = TimerPreferences.getDurationMS(context)

                                if (TimerUtils.isTimerExpired(startTime, timerDuration)) {
                                    // Time already passed while off: clear state to show Ready/Green
                                    Log.d(
                                        "BootReceiver",
                                        "Timer expired while device was off. Clearing state."
                                    )
                                    // Clear state or handle expiration
                                    prefs.remove(TimerKeys.START_TIME)
                                } else {
                                    // 2. Timer is still running: RE-REGISTER the AlarmManager!

                                    val finishTime = startTime + timerDuration
                                    Log.d(
                                        "BootReceiver",
                                        "Timer still active. Rescheduling alarm for target time: $finishTime"
                                    )

                                    // Reschedule using the exact same centralized function!
                                    TimerUtils.scheduleTimerAlarm(context, finishTime)
                                } //end of TimerUtils.isExpired()
                            }
                        }
                        // Update all widgets so they reflect the correct state immediately post-boot
                        HRTWidget().updateAll(context)
                    } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarm on boot", e)
                }
            }
        }
    }
}