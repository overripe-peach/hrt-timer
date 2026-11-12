package com.gayoddities.hrttimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
//import androidx.glance.appwidget.state.updateAppWidgetState
//import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TimerAlarmReceiver", "Alarm fired! Clearing timer state and forcing widget update.")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(HRTWidget::class.java)

                for (glanceId in glanceIds) {
                    val appWidgetId = manager.getAppWidgetId(glanceId)

                    // Check if this specific widget's timer has expired/fired
                    if (TimerUtils.isTimerExpired(context, appWidgetId)) {
                        TimerPreferences.expireTimer(context, appWidgetId)

                        // Poke Glance state for this specific widget so it redraws
                        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                        }
                        HRTWidget().update(context,glanceId)
                    }
                }

                Log.d("TimerAlarmReceiver", "Widget state cleared and updateAll finished successfully.")
            } catch (e: Exception) {
                Log.e("TimerAlarmReceiver", "Failed to update widget on alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                Log.d("BootReceiver", "Device rebooted. Re-evaluating and rescheduling active timers...")
            } else if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
                Log.d("BootReceiver", "HRT Widget Updated. Re-evaluating and rescheduling active timers...")
            }

            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val manager = GlanceAppWidgetManager(context)
                    val glanceIds = manager.getGlanceIds(HRTWidget::class.java)

                    for (glanceId in glanceIds) {
                        val appWidgetId = manager.getAppWidgetId(glanceId)

                        //Evaluate and fix SharedPreferences state
                        if (TimerUtils.isTimerExpired(context, appWidgetId)) {
                            Log.d("BootReceiver", "Timer expired for widget ID $appWidgetId while off. Clearing state.")
                            TimerPreferences.resetTimer(context, appWidgetId)
                        } else {
                            val finishTime = TimerUtils.getFinishTime(context, appWidgetId)
                            Log.d("BootReceiver", "Timer still active for widget ID $appWidgetId. Rescheduling alarm for: $finishTime")
                            TimerUtils.scheduleTimerAlarm(context, finishTime)
                        }

                        //Poke Glance's state so it knows it needs to recompose!
                        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
                            prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                        }

                        //Force update current widgets
                        HRTWidget().update(context,glanceId)
                    }



                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms on boot/update", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}