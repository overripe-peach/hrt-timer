package com.gayoddities.hrttimer

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.Row
import androidx.glance.layout.width
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxHeight
import androidx.glance.appwidget.updateAll
//import androidx.activity.ComponentActivity

object TimerKeys {
    val START_TIME = longPreferencesKey("start_time")
}

class HRTWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    companion object {
        suspend fun updateAllWidgets(context: Context) {
            HRTWidget().updateAll(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val startTime = prefs[TimerKeys.START_TIME] ?: 0L
            val timerDurationMs = TimerPreferences.getDurationMs(context)
            //val currentTime = System.currentTimeMillis()
            val isFinished = startTime == 0L || (System.currentTimeMillis() - startTime >= timerDurationMs)
            // Check if duration has elapsed
            //val hasExpired = startTime != 0L && (currentTime - startTime >= timerDurationMs)
            //val isFinished = startTime == 0L || hasExpired
            val medicineName = TimerPreferences.getMedicineName(context)
            val backgroundColor = if (isFinished) Color(0xFF4CAF50) else Color(0xFF333333)

            val statusText = if (isFinished) {
                "$medicineName Ready\n\n(Tap to Start)"
            } else {
                val finishTime = startTime + timerDurationMs
                // Include day of week (EEE) and month/day (MMM d) alongside the time
                val formatter = SimpleDateFormat("EEE, MMM d 'at' hh:mm a", Locale.getDefault())
                val formattedDate = formatter.format(Date(finishTime))
                "Next dose of $medicineName\n\nFinishing at:\n\n$formattedDate"
            }

            // Base modifier
            var modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(16.dp)

            // Only clickable when finished/ready
            if (isFinished) {
                modifier = modifier.clickable(actionRunCallback<ToggleTimerAction>())
            }

            // Change your outer container from a Column to a Row:
            Row(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // 1. Your Existing Main Timer Column (Add .defaultWeight() so it takes remaining space)
                Column(
                    modifier = GlanceModifier
                        .defaultWeight() // Add this so it shares space with the gear button
                        .fillMaxHeight()
                        .background(backgroundColor)
                        .padding(16.dp)
                        .let { if (isFinished) it.clickable(actionRunCallback<ToggleTimerAction>()) else it },
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = statusText,
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                // 2. Settings Gear Button Area
                Box(
                    modifier = GlanceModifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF222222))
                        .clickable(actionStartActivity<SettingsActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚙️",
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class ToggleTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val currentTime = System.currentTimeMillis()
        val durationMs = TimerPreferences.getDurationMs(context)

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[TimerKeys.START_TIME] = currentTime
            Log.d("HRTWidget", "Timer started at $currentTime")
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimerAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = currentTime + durationMs

        // Check exact alarm permission on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d("HRTWidget", "Exact alarm scheduled for $triggerTime")
            } else {
                // Fallback to standard inexact alarm if exact permission isn't available
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d("HRTWidget", "Inexact alarm scheduled (exact permission not granted)")
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d("HRTWidget", "Exact alarm scheduled for $triggerTime")
        }

        HRTWidget().update(context, glanceId)
    }
}

class HRTWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HRTWidget()
}