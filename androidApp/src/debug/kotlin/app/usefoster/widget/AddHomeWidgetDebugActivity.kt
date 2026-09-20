package app.usefoster.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * DEBUG-ONLY helper for widget design iterations.
 *
 * Triggers the launcher's "pin widget" flow so you can drop the Foster 4x2
 * widget on the home screen without hunting it in the widget picker:
 *
 *     adb shell am start -n app.usefoster/.widget.AddHomeWidgetDebugActivity
 *
 * Registered in the debug manifest only (src/debug/AndroidManifest.xml), so it
 * never ships in release builds.
 */
class AddHomeWidgetDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val manager = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, HomeWidgetReceiver::class.java)
        val canPin = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.isRequestPinAppWidgetSupported()
        if (canPin) {
            manager.requestPinAppWidget(provider, Bundle(), null)
        }
        finish()
    }
}