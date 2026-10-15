package com.gayoddities.hrttimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
//import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
//import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.datastore.preferences.core.edit
//import androidx.glance.appwidget.updateAll
//import androidx.glance.state.GlanceStateDefinition
//import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                onResetTimer = {
                    // 1. Clear the timer preference (using whatever storage method your app uses, e.g., TimerPreferences.resetTimer)
                    TimerPreferences.resetTimer(this@SettingsActivity)

                    //Force the Glance widget to redraw immediately
                    lifecycleScope.launch {
                        val context = this@SettingsActivity
                        val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(HRTWidget::class.java)

                        glanceIds.forEach { glanceId ->
                            updateAppWidgetState(context, glanceId) { prefs ->
                                prefs.remove(TimerKeys.START_TIME)
                            }
                            HRTWidget().update(context, glanceId)
                        }

                        Toast.makeText(context, "Timer has been reset!", Toast.LENGTH_SHORT).show()
                        finish()
                    }

                    Toast.makeText(this@SettingsActivity, "Timer has been reset!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            )
        }
    }
}

@Composable
fun SettingsScreen(onResetTimer: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HRT Timer Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onResetTimer,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = "Reset Timer", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}