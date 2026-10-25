package com.gayoddities.hrttimer

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra("appWidgetId", android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)

        val currentMedicine = TimerPreferences.getMedicineName(this@SettingsActivity, appWidgetId)
        val currentColor = TimerPreferences.getReadyColor(this@SettingsActivity, appWidgetId)
        val currentDurationMs = TimerPreferences.getDurationMS(this@SettingsActivity, appWidgetId)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialDurationMs = currentDurationMs,
                    initialMedicine = currentMedicine,
                    initialColor = currentColor,
                    onSaveDuration = { durationMs ->
                        TimerPreferences.setDurationMS(this@SettingsActivity, appWidgetId, durationMs)
                    },
                    onSaveMedicine = { newName ->
                        TimerPreferences.setMedicineName(this@SettingsActivity, appWidgetId, newName)
                    },
                    onSaveColor = { newColor ->
                        TimerPreferences.setReadyColor(this@SettingsActivity, appWidgetId, newColor)
                        Toast.makeText(this, "Timer indication color set to $newColor", Toast.LENGTH_SHORT).show()
                    },
                    onResetTimer = {
                        lifecycleScope.launch {
                            if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceId = GlanceAppWidgetManager(this@SettingsActivity).getGlanceIdBy(appWidgetId)

                                androidx.glance.appwidget.state.updateAppWidgetState(this@SettingsActivity, glanceId) { prefs ->
                                    prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                                }

                                TimerPreferences.resetTimer(this@SettingsActivity, appWidgetId)
                                HRTWidget().update(this@SettingsActivity, glanceId)

                                Toast.makeText(this@SettingsActivity, "Timer Reset!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Please open settings from the widget gear icon.", Toast.LENGTH_LONG).show()
                            }

                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    },
                    onClose = {
                        lifecycleScope.launch {
                            if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceId = GlanceAppWidgetManager(this@SettingsActivity).getGlanceIdBy(appWidgetId)

                                androidx.glance.appwidget.state.updateAppWidgetState(this@SettingsActivity, glanceId) { prefs ->
                                    prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                                }

                                HRTWidget().update(this@SettingsActivity, glanceId)
                            }

                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    initialDurationMs: Long,
    initialMedicine: String,
    @Suppress("UNUSED_PARAMETER") //why TF is this required?
    initialColor: Int,
    onSaveDuration: (Long) -> Unit,
    onSaveMedicine: (String) -> Unit,
    onSaveColor: (Int) -> Unit,
    onResetTimer: () -> Unit,
    onClose: () -> Unit
) {
    // Determine initial state based on whether stored ms is less than 24 hours
    val oneDayMs = 24L * 60L * 60L * 1000L
    val startingInHours = initialDurationMs < oneDayMs

    var isHoursUnit by remember { mutableStateOf(startingInHours) }

    val initialDisplayValue = if (startingInHours) {
        maxOf(1L, initialDurationMs / (60L * 60L * 1000L)).toString()
    } else {
        maxOf(1L, initialDurationMs / oneDayMs).toString()
    }

    var durationText by remember { mutableStateOf(initialDisplayValue) }
    var medicineText by remember { mutableStateOf(initialMedicine) }

    fun persistDuration(value: Int, inHours: Boolean) {
        val durationMs = if (inHours) {
            value * 60L * 60L * 1000L
        } else {
            value * 24L * 60L * 60L * 1000L
        }
        onSaveDuration(durationMs)
    }

    fun updateDuration(delta: Int) {
        val currentValue = durationText.toIntOrNull() ?: 1

        if (!isHoursUnit) {
            // Currently in Days
            val newDays = currentValue + delta
            if (newDays < 1) {
                // Switch to Hours starting at 23
                isHoursUnit = true
                durationText = "23"
                persistDuration(23, true)
            } else {
                durationText = newDays.toString()
                persistDuration(newDays, false)
            }
        } else {
            // Currently in Hours
            val newHours = currentValue + delta
            if (newHours > 23) {
                // Switch back to Days starting at 1
                isHoursUnit = false
                durationText = "1"
                persistDuration(1, false)
            } else if (newHours < 1) {
                durationText = "1"
                persistDuration(1, true)
            } else {
                durationText = newHours.toString()
                persistDuration(newHours, true)
            }
        }
    }

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
                text = "Timer Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Medicine Name Input Field
            OutlinedTextField(
                value = medicineText,
                onValueChange = { newValue ->
                    medicineText = newValue
                    onSaveMedicine(newValue)
                },
                label = { Text("Medicine / Timer Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Unit Label
            Text(
                text = if (isHoursUnit) "Timer Duration (Hours)" else "Timer Duration (Days)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            ///// UPDATE TIMER DURATION + / - BUTTONS ////
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // - BUTTON ON LEFT
                Button(
                    onClick = { updateDuration(-1) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 8.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text("-", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }

                // DURATION TEXT BOX
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { newValue ->
                        durationText = newValue
                        newValue.toIntOrNull()?.let { persistDuration(it, isHoursUnit) }
                    },
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
                )

                // + BUTTON ON RIGHT
                Button(
                    onClick = { updateDuration(1) },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp
                    ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Ready State Color Picker
            Text(
                text = "Ready State Color",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            val presetColors = listOf(
                0xFF4CAF50, // Green
                0xFF2196F3, // Blue
                0xFF9C27B0, // Purple
                0xFF009688, // Teal
                0xFFE91E63  // Pink
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                presetColors.forEach { colorVal ->
                    val color = Color(colorVal)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onSaveColor(colorVal.toInt()) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = color
                    ) {}
                }
            }
            Spacer(modifier = Modifier.height(28.dp))

            // Reset Timer Button
            Button(
                onClick = onResetTimer,
                modifier = Modifier.fillMaxWidth().height(50.dp),
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
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Close Settings", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}