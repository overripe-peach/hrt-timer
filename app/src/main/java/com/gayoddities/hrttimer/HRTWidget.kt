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
//import android.app.PendingIntent
//import android.content.Intent
import androidx.glance.action.actionStartActivity
import androidx.glance.layout.Row
import androidx.glance.layout.width
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxHeight
import androidx.datastore.preferences.core.stringPreferencesKey

object TimerKeys {
    val START_TIME = longPreferencesKey("start_time")
    val MEDICINE_NAME = stringPreferencesKey("medicine_name")
    val READY_COLOR = longPreferencesKey("ready_color")
}

class HRTWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val startTime = prefs[TimerKeys.START_TIME] ?: 0L
            val timerDuration = TimerPreferences.getDurationMS(context)
            // Check if timer duration has elapsed
            val isReady = TimerUtils.isTimerExpired(startTime, timerDuration)

            // Read from Glance state, fallback to preferences if empty
            val medicineName = prefs[TimerKeys.MEDICINE_NAME] ?: TimerPreferences.getMedicineName(context)
            val customReadyColor = prefs[TimerKeys.READY_COLOR]?.toInt() ?: TimerPreferences.getReadyColor(context)
            val backgroundColor = if (isReady) Color(customReadyColor) else Color(0xFF333333)

            //var modifier = GlanceModifier.fillMaxSize().background(backgroundColor).padding(16.dp)
            var statusText = "$medicineName Ready\n\n(Tap to Start)"

            if (!isReady) { //information text for when the timer is running and the button is disabled
                val finishTime = startTime + timerDuration
                val formatter = SimpleDateFormat("EEE, MMM d 'at' hh:mm a", Locale.getDefault())
                val formattedDate = formatter.format(Date(finishTime))
                statusText = "Next dose of $medicineName at:\n\n$formattedDate"
            } else {
                // Only clickable when finished/ready
               // modifier = modifier.clickable(actionRunCallback<ToggleTimerAction>())
            }

            Row(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Main Timer Column
                Column(
                    modifier = GlanceModifier
                        .defaultWeight() // Add this so it shares space with the gear button
                        .fillMaxHeight()
                        .background(backgroundColor)
                        .padding(16.dp)
                        .let { if (isReady) it.clickable(actionRunCallback<ToggleTimerAction>()) else it },
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {  //text formatting for timer area of widget
                    Text(
                        text = statusText,
                        style = TextStyle(
                            color = ColorProvider(day = Color.White, night = Color.White),
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                //Visual layout for Settings Gear Button Area
                Box(
                    modifier = GlanceModifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF222222))
                        .clickable(actionStartActivity<SettingsActivity>()),
                    contentAlignment = Alignment.Center
                ) { //text color
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
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val currentTime = System.currentTimeMillis()
        val timerDuration = TimerPreferences.getDurationMS(context)
        val triggerTime = currentTime + timerDuration

        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[TimerKeys.START_TIME] = currentTime
            Log.d("HRTWidget", "Timer started at $currentTime")
        }

        TimerUtils.scheduleTimerAlarm(context, triggerTime)
        HRTWidget().update(context, glanceId)
    }
}

class HRTWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HRTWidget()
}