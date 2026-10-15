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