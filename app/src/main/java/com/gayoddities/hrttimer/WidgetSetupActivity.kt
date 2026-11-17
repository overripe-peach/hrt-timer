package com.gayoddities.hrttimer

import android.content.Intent
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

class WidgetSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        setContent {
            MaterialTheme {
                WidgetSetupScreen(
                    onNextClicked = { targetMillis ->
                        lifecycleScope.launch {
                            val currentTime = System.currentTimeMillis()
                            val durationMillis = targetMillis - currentTime

                            if (durationMillis <= 0) {
                                Toast.makeText(
                                    this@WidgetSetupActivity,
                                    "Please select a future date and time.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            // 1. Save this custom duration as the duration for this widget
                            TimerPreferences.setDurationMS(this@WidgetSetupActivity, appWidgetId, durationMillis)

                            // 2. Start the timer immediately so it enters the running state targeting that end time
                            TimerPreferences.startTimer(this@WidgetSetupActivity, appWidgetId)

                            // 3. Schedule the background alarm
                            val triggerTime = currentTime + durationMillis
                            TimerUtils.scheduleTimerAlarm(this@WidgetSetupActivity, triggerTime)

                            // 4. Update Glance state & force widget refresh
                            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                                val glanceId = GlanceAppWidgetManager(this@WidgetSetupActivity).getGlanceIdBy(appWidgetId)
                                androidx.glance.appwidget.state.updateAppWidgetState(this@WidgetSetupActivity, glanceId) { prefs ->
                                    prefs[TimerKeys.LAST_UPDATED] = System.currentTimeMillis()
                                }
                                HRTWidget().update(this@WidgetSetupActivity, glanceId)
                            }

                            // 5. Return success result to Android so the widget is successfully pinned/added
                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, resultValue)

                            // 6. Launch SettingsActivity for this widget instance so they can customize name/color if desired
                            val settingsIntent = Intent(this@WidgetSetupActivity, SettingsActivity::class.java).apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                // If your SettingsActivity uses "appWidgetId" as the extra key:
                                putExtra("appWidgetId", appWidgetId)
                            }

                            //set streak and duration to defaults
                            TimerPreferences.setDurationMS(this@WidgetSetupActivity,appWidgetId,TimerPreferences.DEFAULT_DURATION)
                            TimerPreferences.setStreakWindowMS(this@WidgetSetupActivity,appWidgetId,TimerPreferences.DEFAULT_STREAK_WINDOW)

                            startActivity(settingsIntent)

                            Toast.makeText(this@WidgetSetupActivity, "Timer started!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                )
            }
        }
    }
}

//@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun WidgetSetupScreen(onNextClicked: (Long) -> Unit) {
    // Initialize calendar to current date/time + default 5 days ahead as a helpful starting suggestion
    val calendar = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 5)
        }
    }

    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Date of your next dose:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // A clean date picker state / dialog trigger or simple numeric selectors / Material DatePicker
            // For simplicity and robustness across versions, we can use Material3 Date & Time pickers or compact controls:

            // Display chosen summary text
            val formattedSelectedDate = String.format(
                "%02d/%02d/%d at %02d:%02d",
                selectedMonth + 1,
                selectedDay,
                selectedYear,
                selectedHour,
                selectedMinute
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Selected Target:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedSelectedDate,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Button to open standard Android DatePickerDialog
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            selectedYear = year
                            selectedMonth = month
                            selectedDay = dayOfMonth

                            // Immediately prompt for Time after date is picked
                            android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    selectedHour = hourOfDay
                                    selectedMinute = minute
                                },
                                selectedHour,
                                selectedMinute,
                                false
                            ).show()
                        },
                        selectedYear,
                        selectedMonth,
                        selectedDay
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Pick Date & Time")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Next Button
            Button(
                onClick = {
                    val targetCalendar = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onNextClicked(targetCalendar.timeInMillis)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Next", fontSize = 16.sp)
            }
        }
    }
}