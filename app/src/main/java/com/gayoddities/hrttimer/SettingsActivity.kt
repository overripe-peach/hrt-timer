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
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentDays = TimerPreferences.getDurationMs(this) / (24 * 60 * 60 * 1000L)
        val currentMedicine = TimerPreferences.getMedicineName(this)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialDays = currentDays.toInt(),
                    initialMedicine = currentMedicine,
                    onSaveDays = { newDays ->
                        TimerPreferences.setDurationDays(this@SettingsActivity, newDays)
                        Toast.makeText(this, "Timer duration set to $newDays days", Toast.LENGTH_SHORT).show()
                    },
                    onSaveMedicine = { newName ->
                        TimerPreferences.setMedicineName(this@SettingsActivity, newName)
                    },
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

                            finish()
                            @Suppress("DEPRECATION")
                            overridePendingTransition(0, 0)
                        }
                    },
                    onClose = {
                        lifecycleScope.launch {
                            val glanceIds = GlanceAppWidgetManager(this@SettingsActivity)
                                .getGlanceIds(HRTWidget::class.java)

                            glanceIds.forEach { glanceId ->
                                // Touch the glance state so it's forced to re-render
                                updateAppWidgetState(this@SettingsActivity, glanceId) { prefs ->
                                    // We can toggle or re-set the start time (or a dummy key) to force a state change
                                    val currentStart = prefs[TimerKeys.START_TIME] ?: 0L
                                    prefs[TimerKeys.START_TIME] = currentStart
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
    initialDays: Int,
    initialMedicine: String,
    onSaveDays: (Int) -> Unit,
    onSaveMedicine: (String) -> Unit,
    onResetTimer: () -> Unit,
    onClose: () -> Unit
) {
    var daysText by remember { mutableStateOf(initialDays.toString()) }
    var medicineText by remember { mutableStateOf(initialMedicine) }

    fun updateDays(delta: Int) {
        val currentDays = daysText.toIntOrNull() ?: TimerPreferences.DEFAULT_DAYS
        val newDays = (currentDays + delta).coerceAtLeast(1)
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

            // Stepper Row (- [ Input ] +)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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