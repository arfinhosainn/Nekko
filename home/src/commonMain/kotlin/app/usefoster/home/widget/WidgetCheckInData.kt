package app.usefoster.home.widget

import app.usefoster.home.domain.CheckIn
import app.usefoster.home.domain.Contact
import app.usefoster.home.domain.MissedCheckIn
import app.usefoster.home.domain.checkInCountdownLabel
import app.usefoster.home.domain.forTodayCheckInList
import app.usefoster.home.domain.isCheckedInToday
import app.usefoster.home.domain.isOutstanding
import app.usefoster.home.domain.localDate
import app.usefoster.home.domain.nextUpcomingCheckInTargetEpochMillis
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.Serializable

/** 26 chronological cells, like the home screen's grid. */
const val WIDGET_TIMELINE_COUNT = 26

/** Today occupies cell 12 (bottom-up zigzag rows [1,7,7,7,4]). */
const val WIDGET_TODAY_INDEX = 12

/** The design shows at most two contact rows (waiting first, then done). */
const val WIDGET_MAX_CONTACT_ROWS = 2

// ---------------------------------------------------------------------------
// Widget-facing model (serializable so it can be cached offline / handed to
// the iOS widget extension through the shared App Group container).
// ---------------------------------------------------------------------------

@Serializable
enum class WidgetDayKind {
    /** Inactive / empty day — small gray dot. */
    Plain,

    /** Day with check-in activity — avatar ring + artwork, "+N" when stacked. */
    AvatarDay,

    /** Today with contacts still waiting — amber ring + 🔥. */
    FireToday,
}

@Serializable
data class WidgetDayCell(
    val kind: WidgetDayKind,
    val avatarColorHex: String? = null,
    val overflow: Int = 0,
)

@Serializable
data class WidgetContactRow(
    val name: String,
    val avatarColorHex: String?,
    val subtitle: String,
    val checkedInToday: Boolean,
)

@Serializable
data class WidgetCheckInData(
    val contacts: List<WidgetContactRow> = emptyList(),
    val waitingToday: Int = 0,
    val countdownLabel: String? = null,
    val cells: List<WidgetDayCell> = emptyList(),
    /** ISO date this data was built for — lets the loader detect day rollover. */
    val today: String = "",
    val fetchedAt: Long = 0L,
)

fun emptyWidgetCheckInData(today: LocalDate): WidgetCheckInData = WidgetCheckInData(
    contacts = emptyList(),
    waitingToday = 0,
    countdownLabel = null,
    cells = List(WIDGET_TIMELINE_COUNT) { WidgetDayCell(WidgetDayKind.Plain) },
    today = today.toString(),
    fetchedAt = 0L,
)

// ---------------------------------------------------------------------------
// Pure mapping: domain data -> widget model (mirrors the home screen's
// timeline derivation; shared so Android and iOS widgets stay in lockstep).
// ---------------------------------------------------------------------------

/**
 * Builds the widget model from the same data the home screen uses.
 *
 * [checkIns] should include history — it drives both the 26-day timeline and
 * the per-contact "N check-ins" row subtitles. The home repository already
 * loads the same table unbounded on its 30s refresh cycle.
 *
 * [missedCheckIns] is accepted for call-site symmetry with the fetch pipeline
 * (both platform loaders query it in parallel); the timeline derivation reads
 * it only through the denormalized contact.lastCheckInDate above.
 */
fun buildWidgetCheckInData(
    contacts: List<Contact>,
    checkIns: List<CheckIn>,
    missedCheckIns: List<MissedCheckIn>,
    today: LocalDate,
    nowEpochMillis: Long,
): WidgetCheckInData {
    val contactById = contacts.associateBy { it.id }
    val start = today.minus(DatePeriod(days = WIDGET_TODAY_INDEX))
    val end = today

    // Per-day checked-in contact ids (intersected with the 26-day window).
    val checkedInIdsByDate = mutableMapOf<LocalDate, MutableList<String>>()
    checkIns.forEach { checkIn ->
        checkIn.localDate()?.let { date ->
            if (date >= start && date <= end) {
                val dayIds = checkedInIdsByDate.getOrPut(date) { mutableListOf() }
                if (checkIn.contactId !in dayIds) dayIds.add(checkIn.contactId)
            }
        }
    }
    // Denormalized last_check_in_date is authoritative even if a check-in row
    // was pruned server-side.
    contacts.forEach { contact ->
        contact.lastCheckInDate
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?.let { date ->
                val dayIds = checkedInIdsByDate.getOrPut(date) { mutableListOf() }
                if (contact.id !in dayIds) dayIds.add(contact.id)
            }
    }
    // Only keep contacts we know about (deleted contacts may leave rows).
    checkedInIdsByDate.forEach { (_, ids) -> ids.removeAll { it !in contactById } }

    val cells = List(WIDGET_TIMELINE_COUNT) { index ->
        val date = start.plus(DatePeriod(days = index))
        val ids = checkedInIdsByDate[date].orEmpty()
        val isCurrent = date == today
        val hasPendingToday = isCurrent && contacts.any { it.isOutstanding(today) && it.id !in ids }
        when {
            isCurrent && hasPendingToday ->
                WidgetDayCell(WidgetDayKind.FireToday)
            ids.isNotEmpty() ->
                WidgetDayCell(
                    kind = WidgetDayKind.AvatarDay,
                    avatarColorHex = ids.firstNotNullOfOrNull { contactById[it]?.avatarColor },
                    overflow = (ids.size - 1).coerceAtLeast(0),
                )
            else ->
                WidgetDayCell(WidgetDayKind.Plain)
        }
    }

    val checkInCounts = mutableMapOf<String, Int>()
    checkIns.forEach { checkIn ->
        if (checkIn.contactId in contactById) {
            checkInCounts[checkIn.contactId] = (checkInCounts[checkIn.contactId] ?: 0) + 1
        }
    }
    val rows = contacts
        .forTodayCheckInList(today)
        .take(WIDGET_MAX_CONTACT_ROWS)
        .map { contact ->
            WidgetContactRow(
                name = contact.name,
                avatarColorHex = contact.avatarColor,
                subtitle = checkInCountLabel(checkInCounts[contact.id] ?: 0),
                checkedInToday = contact.isCheckedInToday(today),
            )
        }
    val waitingToday = contacts.count { it.isOutstanding(today) }
    val countdownLabel = contacts
        .nextUpcomingCheckInTargetEpochMillis(nowEpochMillis)
        ?.let { checkInCountdownLabel(it - nowEpochMillis) }

    return WidgetCheckInData(
        contacts = rows,
        waitingToday = waitingToday,
        countdownLabel = countdownLabel,
        cells = cells,
        today = today.toString(),
        fetchedAt = nowEpochMillis,
    )
}

private fun checkInCountLabel(count: Int): String = when (count) {
    1 -> "1 check-in"
    else -> "$count check-ins"
}