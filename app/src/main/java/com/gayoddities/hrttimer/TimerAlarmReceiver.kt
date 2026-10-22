package com.gayoddities.hrttimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
//import androidx.glance.appwidget.state.updateAppWidgetState
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
                val appWidgetId = intent.getIntExtra("appWidgetId", android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
                TimerPreferences.resetTimer(context, appWidgetId)

                // Force all widgets to update their UI
                HRTWidget().updateAll(context)
                Log.d("TimerAlarmReceiver", "Widget state cleared and updateAll finished successfully.")
            } catch (e: Exception) {
                Log.e("TimerAlarmReceiver", "Failed to update widget on alarm", e)
            }
        }
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.d(
                    "BootReceiver",
                    "Device rebooted. Re-evaluating and rescheduling active timers..."
                )
            } else if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                Log.d(
                    "BootReceiver",
                    "HRT Widget Updated. Re-evaluating and rescheduling active timers..."
                )
            }

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Get ALL active widget instances using Glance
                    val manager = androidx.glance.appwidget.GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(HRTWidget::class.java)

                    for (glanceId in glanceIds) {
                        val appWidgetId = manager.getAppWidgetId(glanceId)

                        // 2. Evaluate each widget instance independently
                        if (TimerUtils.isTimerExpired(context, appWidgetId)) {
                            Log.d(
                                "BootReceiver",
                                "Timer expired for widget ID $appWidgetId while off/updating. Clearing state."
                            )
                            TimerPreferences.resetTimer(context, appWidgetId)
                        } else {
                            val finishTime = TimerUtils.getFinishTime(context, appWidgetId)
                            Log.d(
                                "BootReceiver",
                                "Timer still active for widget ID $appWidgetId. Rescheduling alarm for target time: $finishTime"
                            )
                            TimerUtils.scheduleTimerAlarm(context, finishTime)
                        }
                    }

                    // 3. Update all widgets so they reflect the correct state immediately
                    HRTWidget().updateAll(context)

                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot/update", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}