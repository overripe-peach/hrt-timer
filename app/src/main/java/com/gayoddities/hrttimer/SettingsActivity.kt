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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra("appWidgetId", android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)

        val currentMedicine = TimerPreferences.getMedicineName(this@SettingsActivity, appWidgetId)
        val currentColor = TimerPreferences.getReadyColor(this@SettingsActivity, appWidgetId)
        val currentDurationMs = TimerPreferences.getDurationMS(this@SettingsActivity, appWidgetId)
        val currentStreakWindowMs = TimerPreferences.getStreakWindowMS(this@SettingsActivity, appWidgetId)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialDurationMs = currentDurationMs,
                    initialStreakWindowMs = currentStreakWindowMs,
                    initialMedicine = currentMedicine,
                    initialColor = currentColor,
                    onSaveDuration = { durationMs ->
                        TimerPreferences.setDurationMS(this@SettingsActivity, appWidgetId, durationMs)
                    },
                    onSaveStreakWindow = { windowMs ->
                        TimerPreferences.setStreakWindowMS(this@SettingsActivity, appWidgetId, windowMs)
                    },
                    onSaveMedicine = { newName ->
                        TimerPreferences.setMedicineName(this@SettingsActivity, appWidgetId, newName)
                    },
                    onSaveColor = { newColor ->
                        TimerPreferences.setReadyColor(this@SettingsActivity, appWidgetId, newColor)
                        //Toast.makeText(this, "Timer indication color set to $newColor", Toast.LENGTH_SHORT).show()
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
    initialStreakWindowMs: Long,
    initialMedicine: String,
    @Suppress("UNUSED_PARAMETER")
    initialColor: Int,
    onSaveDuration: (Long) -> Unit,
    onSaveStreakWindow: (Long) -> Unit,
    onSaveMedicine: (String) -> Unit,
    onSaveColor: (Int) -> Unit,
    onResetTimer: () -> Unit,
    onClose: () -> Unit
) {
    val oneHourMs = TimerPreferences.HOUR_MS
    val oneDayMs = TimerPreferences.DAYS_MS
    val oneMinMS = TimerPreferences.MINUTE_MS

    // Determine initial unit tier based on stored milliseconds for duration
    val unitTier = when {
        initialDurationMs < oneHourMs -> "MINUTES"
        initialDurationMs < oneDayMs -> "HOURS"
        else -> "DAYS"
    }

    var currentTier by remember { mutableStateOf(unitTier) }

    val initialDisplayValue = when (currentTier) {
        "MINUTES" -> maxOf(1L, initialDurationMs / oneMinMS).toString()
        "HOURS" -> maxOf(1L, initialDurationMs / oneHourMs).toString()
        else -> maxOf(1L, initialDurationMs / oneDayMs).toString()
    }

    var durationText by remember { mutableStateOf(initialDisplayValue) }
    var medicineText by remember { mutableStateOf(initialMedicine) }

    fun persistDuration(value: Int, tier: String) {
        val durationMs = when (tier) {
            "MINUTES" -> value * oneMinMS
            "HOURS" -> value * oneHourMs
            else -> value * oneDayMs
        }
        onSaveDuration(durationMs)
    }

    fun updateDuration(delta: Int) {
        val currentValue = durationText.toIntOrNull() ?: 1
        val newValue = currentValue + delta

        when (currentTier) {
            "DAYS" -> {
                if (newValue < 1) {
                    currentTier = "HOURS"
                    durationText = "23"
                    persistDuration(23, "HOURS")
                } else {
                    durationText = newValue.toString()
                    persistDuration(newValue, "DAYS")
                }
            }

            "HOURS" -> {
                if (newValue > 23) {
                    currentTier = "DAYS"
                    durationText = "1"
                    persistDuration(1, "DAYS")
                } else if (newValue < 1) {
                    currentTier = "MINUTES"
                    durationText = "59"
                    persistDuration(59, "MINUTES")
                } else {
                    durationText = newValue.toString()
                    persistDuration(newValue, "HOURS")
                }
            }

            "MINUTES" -> {
                if (newValue > 59) {
                    currentTier = "HOURS"
                    durationText = "1"
                    persistDuration(1, "HOURS")
                } else if (newValue < 1) {
                    durationText = "1"
                    persistDuration(1, "MINUTES")
                } else {
                    durationText = newValue.toString()
                    persistDuration(newValue, "MINUTES")
                }
            }
        }
    }

    val tierLabelText = when (currentTier) {
        "MINUTES" -> "Timer Duration (Minutes)"
        "HOURS" -> "Timer Duration (Hours)"
        else -> "Timer Duration (Days)"
    }

    // Determine initial unit tier for streak window
    val streakUnitTier = when {
        initialStreakWindowMs < oneDayMs -> "HOURS"
        else -> "DAYS"
    }

    var currentStreakTier by remember { mutableStateOf(streakUnitTier) }

    val initialStreakDisplayValue = when (currentStreakTier) {
        "HOURS" -> maxOf(0L, initialStreakWindowMs / oneHourMs).toString()
        else -> maxOf(1L, initialStreakWindowMs / oneDayMs).toString()
    }

    var streakWindowText by remember { mutableStateOf(initialStreakDisplayValue) }

    fun persistStreakWindow(value: Int, tier: String) {
        val windowMs = when (tier) {
            "HOURS" -> value * oneHourMs
            else -> value * oneDayMs
        }
        onSaveStreakWindow(windowMs)
    }

    fun updateStreakWindow(delta: Int) {
        val currentValue = streakWindowText.toIntOrNull() ?: 0
        val newValue = currentValue + delta

        when (currentStreakTier) {
            "DAYS" -> {
                if (newValue < 1) {
                    currentStreakTier = "HOURS"
                    streakWindowText = "23"
                    persistStreakWindow(23, "HOURS")
                } else {
                    streakWindowText = newValue.toString()
                    persistStreakWindow(newValue, "DAYS")
                }
            }

            "HOURS" -> {
                if (newValue > 23) {
                    currentStreakTier = "DAYS"
                    streakWindowText = "1"
                    persistStreakWindow(1, "DAYS")
                } else if (newValue < 0 ) {
                    // Clamp at 0 hours minimum
                    streakWindowText = "0"
                    persistStreakWindow(0, "HOURS")
                } else {
                    streakWindowText = newValue.toString()
                    persistStreakWindow(newValue, "HOURS")
                }
            }
        }
    }

    val streakTierLabelText = when (currentStreakTier) {
        "HOURS" -> "Streak Window (Hours)"
        else -> "Streak Window (Days)"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
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

            // Dynamic Unit Label (Duration)
            Text(
                text = tierLabelText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // DURATION CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                OutlinedTextField(
                    value = durationText,
                    onValueChange = { newValue ->
                        durationText = newValue
                        newValue.toIntOrNull()?.let { persistDuration(it, currentTier) }
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

            // Streak Window Unit Label
            Text(
                text = streakTierLabelText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // STREAK WINDOW CONTROLS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { updateStreakWindow(-1) },
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
                    value = streakWindowText,
                    onValueChange = { newValue ->
                        streakWindowText = newValue
                        newValue.toIntOrNull()?.let { persistStreakWindow(it, currentStreakTier) }
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
                    onClick = { updateStreakWindow(1) },
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