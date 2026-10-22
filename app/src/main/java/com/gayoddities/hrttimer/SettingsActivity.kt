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
import kotlin.time.Duration.Companion.milliseconds

//import androidx.core.graphics.toColor

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Retrieve the specific widget instance ID passed from the gear icon
        val appWidgetId = intent.getIntExtra("appWidgetId", android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)

        val currentDays = TimerPreferences.getDurationDays(this, appWidgetId)
        val currentMedicine = TimerPreferences.getMedicineName(this, appWidgetId)
        val currentColor = TimerPreferences.getReadyColor(this, appWidgetId)


        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialDays = currentDays.toInt(),
                    initialMedicine = currentMedicine,
                    initialColor = currentColor,
                    onSaveDays = { newDays ->
                        val durationMs = newDays * 24 * 60 * 60 * 1000L
                        TimerPreferences.setDurationMS(this@SettingsActivity, appWidgetId, durationMs)
                        Toast.makeText(this, "Timer duration set to $newDays days", Toast.LENGTH_SHORT).show()
                    },

                    onSaveMedicine = { newName ->
                        TimerPreferences.setMedicineName(this@SettingsActivity, appWidgetId, newName)
                    },

                    onSaveColor = { newColor ->
                        TimerPreferences.setReadyColor(this@SettingsActivity, appWidgetId,newColor)
                        Toast.makeText(this, "Timer indication color set to $newColor", Toast.LENGTH_SHORT).show()
                    },
                    onResetTimer = {
                        lifecycleScope.launch {
                            // Target ONLY the specific widget instance opened via gear icon
                            if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceId = GlanceAppWidgetManager(this@SettingsActivity).getGlanceIdBy(appWidgetId)

                                // 1. Reset your underlying timer preferences for this instance
                                TimerPreferences.resetTimer(this@SettingsActivity, appWidgetId)

                                //Ping Glance state to force a redraw
                                androidx.glance.appwidget.state.updateAppWidgetState(this@SettingsActivity, glanceId) { prefs ->
                                    prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                                }

                                //Update the widget UI
                                HRTWidget().update(this@SettingsActivity, glanceId)
                                Toast.makeText(this@SettingsActivity, "Timer Reset!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@SettingsActivity, "Please open settings from the widget gear icon. ", Toast.LENGTH_LONG).show()
                            }

                            // Give Glance's session worker a fraction of a second to render
                            // before the activity window closes and the home screen refocuses
                            kotlinx.coroutines.delay(150.milliseconds)

                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    },
                    onClose = {
                        lifecycleScope.launch {
                            if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceId = GlanceAppWidgetManager(this@SettingsActivity)
                                    .getGlanceIdBy(appWidgetId)

                                //Ping Glance state to force a redraw
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
@Suppress("UNUSED_PARAMETER")
fun SettingsScreen(
    initialDays: Int,
    initialMedicine: String,
    initialColor: Int,
    onSaveDays: (Int) -> Unit,
    onSaveMedicine: (String) -> Unit,
    onSaveColor: (Int) -> Unit,
    onResetTimer: () -> Unit,
    onClose: () -> Unit
) {
    var daysText by remember { mutableStateOf(initialDays.toString()) }
    var medicineText by remember { mutableStateOf(initialMedicine) }

    fun updateDays(delta: Int) {
        val currentDays =  (daysText.toIntOrNull() ?: ((TimerPreferences.DEFAULT_DURATION / (24 * 60 * 60 * 1000L)).toInt()))
        val newDays = (currentDays + delta).coerceAtLeast(1) // Keeps minimum at 1 day

        daysText = newDays.toString()
        onSaveDays(newDays)
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

            Text(
                text = "Timer Duration (Days)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            /////UPDATE TIMER DURATION + / - BUTTONS ////
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { //- BUTTON ON LEFT
                Button(
                    onClick = { updateDays(-1) },
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
                //DURATION DAYS TEXT BOX
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { newValue ->
                        daysText = newValue
                        newValue.toIntOrNull()?.let { onSaveDays(it) }
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
                //+ DURATION DAYS BUTTON ON RIGHT
                Button(
                    onClick = { updateDays(1) },
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
            ////END OF DURATION DAYS INPUT CODE ///////
            ////READY STATE COLOR PICKER CODE////
            Text(
                text = "Ready State Color",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Preset color options row
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
            ////END OF READY STATE COLOR PICKER CODE////

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