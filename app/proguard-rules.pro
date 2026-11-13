# Keep your app's widgets, receivers, and preferences
-keep class com.gayoddities.hrttimer.** {
    *;
}

# Keep Glance App Widget receivers and entry points specifically
-keep class androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# Keep serialization or custom state classes if used by your widget state
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod