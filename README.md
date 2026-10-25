# hrt-timer
a simple but effective HRT dose timer android widget.  It is a  widget that either says "ready" or "Next dose at: $DATE$". When the app says ready, you take your dose and hit the button.  Customizable medicine name, timer duration, and ready state indicator color.  You can have multiple widgets on the screen, all have independent settings.  

I made this because I was tired of moving calendar events around when I inevitably missed my dose or having to remember manually when my next dose would be.  

Current planned future features:
-streak tracker - for a duration measure in hours, taking the dose within 24 hours of the triggers a streak. It will count how many weeks you keep a streak going for.  If it is set to duration < 24 hours it will give a streak for taking it within an hour of the time ready state.


There is no data collection, or remote reporting.  For people new to android project folder structures, the code predominantly lives in 5 files.  

Folder Location: app > src > main > java/com/gayoddities/hrttimer: 
- HRTWidget.kt
- SettingsActivity.kt
- TimerAlarmReceiver.kt
- TimerPreferences.kt
- TimerUtils.kt

