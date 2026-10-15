package com.gayoddities.hrttimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onResetTimer = {
                        lifecycleScope.launch {
                            val glanceIds = GlanceAppWidgetManager(this@SettingsActivity)
                                .getGlanceIds(HRTWidget::class.java)

                            glanceIds.forEach { glanceId ->
                                updateAppWidgetState(this@SettingsActivity, glanceId) { prefs ->
                                    prefs.remove(TimerKeys.START_TIME)
                                }
                                HRTWidget().update(this@SettingsActivity, glanceId)
                            }

                            Toast.makeText(this@SettingsActivity, "Timer reset!", Toast.LENGTH_SHORT).show()

                            // Finish with zero animation to prevent widget flicker
                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    },
                    onClose = {
                        finish()
                        @Suppress("DEPRECATION")
                        overridePendingTransition(0, 0) // Instant close, no flicker
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onResetTimer: () -> Unit,
    onClose: () -> Unit
) {
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
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reset Timer Button
            Button(
                onClick = onResetTimer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Reset Timer", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close / Done Button
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Close Settings", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Version Display
            Text(
                text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}