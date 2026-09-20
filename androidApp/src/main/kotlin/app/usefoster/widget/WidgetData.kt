package app.usefoster.widget

import android.content.Context
import app.usefoster.AppSupabase
import app.usefoster.home.data.supabase.SupabaseContactDataSource
import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.ContactError
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.widget.WidgetCheckInData
import app.usefoster.home.widget.buildWidgetCheckInData
import app.usefoster.home.widget.emptyWidgetCheckInData
import app.usefoster.shared.domain.Result
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.DurationUnit
import kotlin.time.Instant
import kotlin.time.toDuration

// ---------------------------------------------------------------------------
// Loading: lean Supabase fetch + SharedPreferences cache so the widget keeps
// showing the last-known state when offline or unauthenticated.
// ---------------------------------------------------------------------------

object HomeWidgetDataLoader {

    private const val PREFS_FILE = "foster_home_widget"
    private const val CACHE_KEY = "widget_checkin_data_v1"
    private val REFETCH_DELAY = 15.toDuration(DurationUnit.SECONDS)

    private val json = Json { ignoreUnknownKeys = true }

    private var cache: WidgetCheckInData? = null
    private var lastFetchAt: Instant? = null

    /**
     * Drops the debounce window so the next [load] does a full network fetch.
     * Call after in-app data mutations (check-ins, contact add/remove) so the
     * widget never shows stale rows; the in-memory/persisted cache stays in
     * place as the offline fallback.
     */
    fun invalidate() {
        lastFetchAt = null
    }

    /**
     * Returns the freshest check-in data we can get. Called from
     * [HomeWidget.provideGlance] (a background worker); debounced so the
     * placement/size flurry of widget updates only hits the network once.
     */
    suspend fun load(context: Context): WidgetCheckInData {
        val now = Clock.System.now()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val lastGood = cache
        val lastFetched = lastFetchAt
        if (lastGood != null && lastGood.today == today.toString() && lastFetched != null) {
            if (now - lastFetched < REFETCH_DELAY) return lastGood
        }

        val fresh = fetch(today)
        if (fresh != null) {
            cache = fresh
            lastFetchAt = now
            persist(context, fresh)
            return fresh
        }

        // Offline / auth failure: last known-good, then persisted, then empty.
        return cache
            ?: restore(context)
            ?: emptyWidgetCheckInData(today)
    }

    private suspend fun fetch(today: LocalDate): WidgetCheckInData? {
        if (AppSupabase.client.auth.currentSessionOrNull() == null) return null

        val dataSource = SupabaseContactDataSource(AppSupabase.client)
        val to = today.plus(DatePeriod(days = 13)).toString()
        val results = coroutineScope {
            val contacts = async { dataSource.getContacts() }
            val checkIns = async {
                dataSource.getCheckIns(contactId = null, from = "1970-01-01", to = to)
            }
            val missed = async {
                dataSource.getMissedCheckIns(from = "1970-01-01", to = today.toString())
            }
            FetchResults(
                contacts = contacts.await(),
                checkIns = checkIns.await(),
                missed = missed.await(),
            )
        }

        if (results.contacts is Result.Error ||
            results.checkIns is Result.Error ||
            results.missed is Result.Error
        ) {
            return null
        }

        return buildWidgetCheckInData(
            contacts = (results.contacts as Result.Success).data,
            checkIns = (results.checkIns as Result.Success).data,
            missedCheckIns = (results.missed as Result.Success).data,
            today = today,
            nowEpochMillis = Clock.System.now().toEpochMilliseconds(),
        )
    }

    private data class FetchResults(
        val contacts: Result<List<Contact>, ContactError>,
        val checkIns: Result<List<CheckIn>, ContactError>,
        val missed: Result<List<MissedCheckIn>, ContactError>,
    )

    private suspend fun persist(context: Context, data: WidgetCheckInData) {
        withContext(Dispatchers.IO) {
            runCatching {
                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                    .edit()
                    .putString(CACHE_KEY, json.encodeToString(WidgetCheckInData.serializer(), data))
                    .commit()
            }
        }
    }

    private suspend fun restore(context: Context): WidgetCheckInData? {
        val raw = withContext(Dispatchers.IO) {
            runCatching {
                context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                    .getString(CACHE_KEY, null)
            }.getOrNull()
        } ?: return null
        return runCatching { json.decodeFromString<WidgetCheckInData>(raw) }.getOrNull()
    }
}