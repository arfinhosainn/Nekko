package app.usefoster.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Pushes fresh data into the home widget after in-app changes.
 *
 * The widget itself only refreshes on the 30-minute system tick
 * ([HomeWidgetReceiver] / updatePeriodMillis), so anything the user does
 * inside the app (check in, add/remove a contact, complete a check-in flow)
 * would otherwise be invisible to the widget until the next half-hour roll.
 * The app therefore re-composes the widget explicitly:
 *
 *  - [MainActivity.onStart] — light refresh (loader debounce still applies);
 *  - [MainActivity.onStop]  — forced refresh: the session may have mutated
 *    data, so the loader debounce is dropped and the next compose re-fetches.
 *
 * Fire-and-forget: failures (offline, no session) fall back to the cached
 * widget state inside [HomeWidgetDataLoader.load] and never crash the app.
 */
object WidgetRefresher {

    fun refresh(context: Context, force: Boolean = false) {
        val appContext = context.applicationContext
        if (force) HomeWidgetDataLoader.invalidate()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { HomeWidget().updateAll(appContext) }
        }
    }
}