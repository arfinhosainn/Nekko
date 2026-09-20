package app.usefoster.widget

import app.usefoster.IosAppSupabase
import app.usefoster.home.data.supabase.SupabaseContactDataSource
import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.widget.WidgetCheckInData
import app.usefoster.home.widget.buildWidgetCheckInData
import app.usefoster.shared.domain.Result
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import platform.Foundation.NSNotificationCenter

import platform.Foundation.NSUserDefaults
import platform.darwin.NSObjectProtocol
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * iOS counterpart of the Android `WidgetRefresher` + `HomeWidgetDataLoader`:
 * on foreground/background transitions the app fetches the latest check-in
 * data (same pipeline as the home screen), encodes it with the SAME kotlinx
 * serializer Android uses, publishes the JSON into the shared App Group
 * `NSUserDefaults`, and asks the host app to reload the widget timelines.
 *
 * WidgetKit is not exposed by Kotlin/Native platform bindings, so the reload
 * call itself lives on the Swift side: this bridge posts
 * [RELOAD_NOTIFICATION] on the app's NSNotificationCenter and the AppDelegate
 * forwards it to `WidgetCenter.shared.reloadTimelines(ofKind:)`.
 *
 * The `FosterWidget` extension (pure Swift) is the only reader of the JSON:
 * it decodes from the App Group and renders — no network, no Kotlin
 * framework inside the extension process.
 */
object IosWidgetBridge {

    /** Must match FosterWidget.entitlements / both targets' Info.plist. */
    const val APP_GROUP_ID = "group.app.usefoster.Foster"
    const val CACHE_KEY = "widget_checkin_data_v1"

    /** Observed by the Swift AppDelegate → WidgetCenter.reloadTimelines. */
    const val RELOAD_NOTIFICATION = "app.usefoster.widget.reload"

    private val REFETCH_DELAY = 10.toDuration(DurationUnit.SECONDS)

    private val json = Json { ignoreUnknownKeys = true }
    private var lastRefreshAt: kotlin.time.Instant? = null
    private var observers: List<NSObjectProtocol>? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var inFlight = false

    /**
     * Registers UIApplication lifecycle observers. Idempotent — called from
     * [app.usefoster.MainViewController] on every composition, but must only
     * subscribe once per process.
     */
    fun install() {
        if (observers != null) return
        val center = NSNotificationCenter.defaultCenter
        observers = listOf(
            center.addObserverForName(
                name = "UIApplicationDidEnterBackgroundNotification",
                `object` = null,
                queue = null,
            ) { _ ->
                // Leaving the app → the widget becomes visible: force fresh.
                refresh(force = true)
            },
            center.addObserverForName(
                name = "UIApplicationWillEnterForegroundNotification",
                `object` = null,
                queue = null,
            ) { _ ->
                refresh(force = false)
            },
        )
        // Mirror Android's MainActivity.onStart: publish once as soon as the
        // app is up, so a widget placed without ever backgrounding the app
        // still picks up the current state.
        refresh(force = true)
    }

    /** Fire-and-forget refresh; [force] drops the debounce window. */
    fun refresh(force: Boolean = false) {
        val now = Clock.System.now()
        if (!force) {
            val last = lastRefreshAt
            if (last != null && now - last < REFETCH_DELAY) return
        }
        if (inFlight) return
        inFlight = true
        scope.launch {
            try {
                runCatching { refreshNow() }
            } finally {
                lastRefreshAt = Clock.System.now()
                inFlight = false
            }
        }
    }

    private suspend fun refreshNow() {
        val client = IosAppSupabase.client
        if (client.auth.currentSessionOrNull() == null) return

        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val to = today.plus(DatePeriod(days = 13)).toString()
        val dataSource = SupabaseContactDataSource(client)
        val contacts: Result<List<Contact>, ContactError>
        val checkIns: Result<List<CheckIn>, ContactError>
        val missed: Result<List<MissedCheckIn>, ContactError>
        coroutineScope {
            val c = async { dataSource.getContacts() }
            val i = async {
                dataSource.getCheckIns(contactId = null, from = "1970-01-01", to = to)
            }
            val m = async {
                dataSource.getMissedCheckIns(from = "1970-01-01", to = today.toString())
            }
            contacts = c.await()
            checkIns = i.await()
            missed = m.await()
        }
        if (contacts is Result.Error || checkIns is Result.Error || missed is Result.Error) {
            return // keep whatever the widget already shows
        }

        val data = buildWidgetCheckInData(
            contacts = (contacts as Result.Success).data,
            checkIns = (checkIns as Result.Success).data,
            missedCheckIns = (missed as Result.Success).data,
            today = today,
            nowEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
        publish(data)
    }

    /**
     * Encodes with the exact serializer Android caches, writes to the App
     * Group, and pings the Swift host to reload widget timelines.
     */
    fun publish(data: WidgetCheckInData) {
        val encoded = json.encodeToString(WidgetCheckInData.serializer(), data)
        val defaults = NSUserDefaults(suiteName = APP_GROUP_ID) ?: return
        defaults.setObject(encoded, forKey = CACHE_KEY)
        // The widget renders in its own process: flush the shared defaults so
        // cfprefsd hands the fresh blob to the extension immediately.
        defaults.synchronize()
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = RELOAD_NOTIFICATION,
            `object` = null,
        )
    }
}