package app.usefoster.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * AppWidgetProvider for the Foster home widget. Glance drives all updates;
 * this receiver only hands them to [HomeWidget]. Registered in the manifest
 * with `android.appwidget.provider` → @xml/home_widget_info (4x2).
 */
class HomeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeWidget()
}