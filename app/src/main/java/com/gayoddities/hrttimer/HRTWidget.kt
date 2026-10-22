package com.gayoddities.hrttimer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Row
import androidx.glance.layout.width
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxHeight
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

object TimerKeys {
    var LAST_UPDATED = longPreferencesKey("last_updated")
}

class HRTWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    @SuppressLint("NonObservableLocale")
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            // Inside provideGlance in HRTWidget.kt:
            val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(id)
            val settingsIntent = Intent(context, SettingsActivity::class.java).apply {
                putExtra("appWidgetId", appWidgetId)
            }

            // Reading currentState ensures Glance actively subscribes to state changes
            val prefs = androidx.glance.currentState<androidx.datastore.preferences.core.Preferences>()
            val lastUpdated = prefs[TimerKeys.LAST_UPDATED] ?: 0L


            val startTime = TimerPreferences.getStartTime(context, appWidgetId)
            val timerDuration = TimerPreferences.getDurationMS(context, appWidgetId)
            val medicineName = TimerPreferences.getMedicineName(context, appWidgetId)
            val readyColor =TimerPreferences.getReadyColor(context,appWidgetId)

            val isReady = TimerUtils.isTimerExpired(context,appWidgetId)
            val backgroundColor = if (isReady) Color(readyColor) else Color(TimerPreferences.RUNNING_COLOR)

            //var modifier = GlanceModifier.fillMaxSize().background(backgroundColor).padding(16.dp)
            var statusText: String

            Log.d("hrt-widget", "Is timer expired: $isReady (Trigger: $lastUpdated)")

            if (!isReady) { //information text for when the timer is running and the button is disabled
                val finishTime = startTime + timerDuration
                val formatter = SimpleDateFormat("EEE, MMM d 'at' hh:mm a", Locale.getDefault())
                val formattedDate = formatter.format(Date(finishTime))
                statusText = "Next dose of $medicineName at:\n\n$formattedDate"
            } else {
                statusText = "$medicineName Ready\n\n(Tap to Start)"
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
                        .clickable(actionStartActivity(intent = settingsIntent)),
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
        val appWidgetId = androidx.glance.appwidget.GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val currentTime = System.currentTimeMillis()
        val timerDuration = TimerPreferences.getDurationMS(context, appWidgetId)
        val triggerTime = currentTime + timerDuration

        TimerPreferences.startTimer(context, appWidgetId)
        TimerUtils.scheduleTimerAlarm(context, triggerTime)

        //Ping Glance state so it re-composes immediately
        androidx.glance.appwidget.state.updateAppWidgetState(context, glanceId) { prefs ->
            prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
        }

        HRTWidget().update(context, glanceId)
    }
}

class HRTWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HRTWidget()
}