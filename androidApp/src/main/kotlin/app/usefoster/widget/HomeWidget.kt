package app.usefoster.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.action.actionStartActivity
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.usefoster.MainActivity
import app.usefoster.R
import app.usefoster.theme.BackgroundB0Dark
import app.usefoster.theme.BackgroundB0Light
import app.usefoster.theme.GraySecondaryDark
import app.usefoster.theme.GraySecondaryLight
import app.usefoster.theme.GreenHover
import app.usefoster.theme.InkDark
import app.usefoster.theme.InkLight
import app.usefoster.theme.TextPrimaryDark
import app.usefoster.theme.TextPrimaryLight
import app.usefoster.theme.TextTertiaryDark
import app.usefoster.theme.TextTertiaryLight
import app.usefoster.theme.Yellow
import app.usefoster.home.widget.WidgetCheckInData
import app.usefoster.home.widget.WidgetContactRow
import app.usefoster.home.widget.WidgetDayCell
import app.usefoster.home.widget.WidgetDayKind

/**
 * Foster home widget — adaptive 4x2 (resizable to half width).
 *
 * Wide (≥ [WIDE_MIN_WIDTH]): calendar (~48%) + checklist (~52%), edge-to-edge.
 * Narrow (< breakpoint): checklist only (actionable half-size layout).
 *
 * Uses [SizeMode.Exact] so [LocalSize] matches the launcher-allocated bounds
 * and content can fill the full widget (avoids the empty right band from
 * SizeMode.Single laying out at min size only).
 *
 * Renders LIVE check-in data fetched from Supabase in [HomeWidgetDataLoader]
 * (see widget/WidgetData.kt); offline fallback shows the last cached state.
 */
class HomeWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Real data, fetched on a background coroutine. Debounced + cached:
        // one network hit per widget update burst; offline shows last-good.
        val data = HomeWidgetDataLoader.load(context)
        provideContent { HomeWidgetContent(data) }
    }
}

// ---------------------------------------------------------------------------
// Sizing / grid geometry (same timeline shape as the home screen)
// ---------------------------------------------------------------------------

private const val TIMELINE_COLUMNS = 7
private const val TIMELINE_ROWS = 5

/** Bottom→top capacities — identical to the home screen's zigzag grid. */
private val TIMELINE_ROW_CAPACITIES = listOf(1, 7, 7, 7, 4)

/**
 * Calendar / list split measured from the design screenshot (~400/850 ≈ 0.47).
 * List pane uses defaultWeight() for the remainder so it always fills the row.
 */
private const val CALENDAR_WIDTH_RATIO = 0.48f

/** Below this width (after padding), show checklist only. */
private val WIDE_MIN_WIDTH = 220.dp

private const val WIDGET_CORNER_RADIUS = 28
/** Tight inset so the Check in CTA is not clipped at the trailing edge. */
private const val CONTENT_PADDING = 20

// ---------------------------------------------------------------------------
// Palette — mirrors shared/theme/Color.kt + CheckinGrid tokens, resolved for
// the current system night mode (the widget follows the device theme).
// ---------------------------------------------------------------------------

private class WidgetPalette(
    val cardBg: Color,
    val textPrimary: Color,
    val textTertiary: Color,
    /** FosterTheme.colors.fill.secondary — inactive day dots + avatar discs. */
    val cellFill: Color,
    /** FosterTheme.colors.stroke.secondary — dashed / vertical dividers. */
    val divider: Color,
    /** FosterTheme.colors.gray.secondary — checked-in glyph tint. */
    val checkTint: Color,
)

private val LightPalette = WidgetPalette(
    cardBg = BackgroundB0Light,
    textPrimary = TextPrimaryLight,
    textTertiary = TextTertiaryLight,
    // Glance ColorFilter.tint drops alpha — bake fill/stroke overlays onto B0.
    cellFill = InkLight.copy(alpha = 0.08f).compositeOver(BackgroundB0Light),
    divider = InkLight.copy(alpha = 0.06f).compositeOver(BackgroundB0Light),
    checkTint = GraySecondaryLight,
)

private val NightPalette = WidgetPalette(
    cardBg = BackgroundB0Dark,
    textPrimary = TextPrimaryDark,
    textTertiary = TextTertiaryDark,
    cellFill = InkDark.copy(alpha = 0.08f).compositeOver(BackgroundB0Dark),
    divider = InkDark.copy(alpha = 0.06f).compositeOver(BackgroundB0Dark),
    checkTint = GraySecondaryDark,
)

/** Accents mirrored from CheckinGrid / ContactCheckInRow / theme. */
private val LeafGreen = GreenHover
private val LeafBadgeBg = LeafGreen.copy(alpha = 0.18f)
/** timelineGridColors.currentOutline */
private val CurrentOutline = Color(0xFF28D86F)
/** timelineGridColors.badge / badgeText */
private val OverflowBadgeBg = Color(0xFFF4F4F6)
private val OverflowBadgeText = Color(0xFF1C1C1F)
private val Gold = Yellow
/** Gold pill fill sampled from the design (~#38341B). */
private val GoldPillFillNight = Color(0xFF38341B)
private val GoldPillFillLight = Color(0xFFF7EFC2)
private val PillWhite = Color(0xFFFFFFFF)
private val PillTextDark = Color(0xFF18181B)
private class WidgetTextStyles(
    val title: TextStyle,
    val waiting: TextStyle,
    val contactName: TextStyle,
    val contactMeta: TextStyle,
    val pill: TextStyle,
    val badge: TextStyle,
    val gold: TextStyle,
    val fire: (cell: Dp) -> TextStyle,
)

private fun widgetTextStyles(palette: WidgetPalette) = WidgetTextStyles(
    title = TextStyle(
        color = ColorProvider(palette.textTertiary),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    ),
    waiting = TextStyle(
        color = ColorProvider(palette.textPrimary),
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    ),
    contactName = TextStyle(
        color = ColorProvider(palette.textPrimary),
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
    ),
    contactMeta = TextStyle(
        color = ColorProvider(palette.textTertiary),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
    ),
    pill = TextStyle(
        color = ColorProvider(PillTextDark),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    ),
    badge = TextStyle(
        color = ColorProvider(OverflowBadgeText),
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
    ),
    gold = TextStyle(
        color = ColorProvider(Gold),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
    ),
    fire = { cell -> TextStyle(color = ColorProvider(Color.White), fontSize = (cell.value * 0.55f).sp) },
)

// ---------------------------------------------------------------------------
// Sample data — DESIGN-PHASE ONLY. Replaced by real check-in data later.
// ---------------------------------------------------------------------------

private enum class MiniDayKind {
    /** Plain gray day circle. */
    Plain,

    /** Grown plant day — green sprout on the day circle (home timeline). */
    Plant,

    /** Day with check-in activity: avatar image + ring, optional "+N" badge. */
    AvatarDay,

    /** Today: amber ring + 🔥 streak. */
    FireToday,
}

/**
 * Adapts a real timeline cell (from [WidgetCheckInData]) to the visual kinds
 * this design renders. Real data never produces a plant yet — that branch
 * stays available for the future scheduled-day wiring.
 */
private fun visualKindOf(cell: WidgetDayCell?): MiniDayKind = when (cell?.kind) {
    WidgetDayKind.AvatarDay -> MiniDayKind.AvatarDay
    WidgetDayKind.FireToday -> MiniDayKind.FireToday
    else -> MiniDayKind.Plain
}

// ---------------------------------------------------------------------------
// Geometry
// ---------------------------------------------------------------------------

/**
 * Cell sizing derived from BOTH axes so a dot/badge can never overflow its
 * slot (no neighbor overlap) and the grid always spans the full pane width.
 */
private class CalendarGeometry(val cell: Dp, val badge: Dp)

private fun calendarGeometry(paneWidth: Dp, innerHeight: Dp): CalendarGeometry {
    val reservedTop = 16.dp + 6.dp // header row + gap
    val rowPitch = ((innerHeight - reservedTop) / TIMELINE_ROWS).coerceAtLeast(10.dp)
    val colPitch = (paneWidth / TIMELINE_COLUMNS).coerceAtLeast(10.dp)
    val pitch = minOf(rowPitch, colPitch)
    return CalendarGeometry(
        // Smaller than pitch so dots read as inactive dots (home ~0.36× cell)
        // with room between neighbors; badges stay slightly larger.
        cell = (pitch * 0.52f).coerceAtLeast(7.dp),
        badge = (pitch * 0.72f).coerceAtLeast(9.dp),
    )
}

// ---------------------------------------------------------------------------
// Content
// ---------------------------------------------------------------------------

@Composable
internal fun HomeWidgetContent(data: WidgetCheckInData = WidgetCheckInData()) {
    val size = LocalSize.current
    val night = isNightMode(LocalContext.current)
    val palette = if (night) NightPalette else LightPalette
    val styles = widgetTextStyles(palette)

    val contentPadding = CONTENT_PADDING.dp
    val innerWidth = size.width - contentPadding * 2
    val innerHeight = size.height - contentPadding * 2
    val wide = size.width >= WIDE_MIN_WIDTH

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(WIDGET_CORNER_RADIUS.dp)
            // Whole-widget tap target: opens the app (MainActivity is singleTop,
            // so a running instance is brought forward instead of duplicated).
            .clickable(actionStartActivity<MainActivity>())
            .background(palette.cardBg)
            .padding(contentPadding),
    ) {
        if (wide) {
            WideWidgetBody(
                data = data,
                innerWidth = innerWidth,
                innerHeight = innerHeight,
                night = night,
                palette = palette,
                styles = styles,
            )
        } else {
            CheckInPane(
                data = data,
                modifier = GlanceModifier.fillMaxSize(),
                night = night,
                palette = palette,
                styles = styles,
                compact = false,
            )
        }
    }
}

/** Calendar + checklist side by side; list weight fills all remaining width. */
@Composable
private fun WideWidgetBody(
    data: WidgetCheckInData,
    innerWidth: Dp,
    innerHeight: Dp,
    night: Boolean,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
) {
    val calendarWidth = innerWidth * CALENDAR_WIDTH_RATIO
    val dividerWidth = 1.dp
    // Space on both sides of the divider so panes don't touch it.
    val dividerGap = 12.dp

    Row(
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
        verticalAlignment = Alignment.Top,
    ) {
        MiniCalendarPane(
            data = data,
            width = calendarWidth,
            innerHeight = innerHeight,
            palette = palette,
            styles = styles,
        )
        Spacer(GlanceModifier.width(dividerGap))
        Spacer(
            modifier = GlanceModifier
                .width(dividerWidth)
                .fillMaxHeight()
                .background(palette.divider),
        )
        Spacer(GlanceModifier.width(dividerGap))
        CheckInPane(
            data = data,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            night = night,
            palette = palette,
            styles = styles,
            compact = true,
        )
    }
}

private fun isNightMode(context: Context): Boolean {
    @Suppress("DEPRECATION")
    val uiMode = context.resources.configuration.uiMode
    return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

// ---------------------------------------------------------------------------
// Calendar pane — leaf "CHECK IN" header + 26-cell zigzag grid
// ---------------------------------------------------------------------------

@Composable
private fun MiniCalendarPane(
    data: WidgetCheckInData,
    width: Dp,
    innerHeight: Dp,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
) {
    val geo = calendarGeometry(width, innerHeight)

    Column(modifier = GlanceModifier.width(width).fillMaxHeight()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(14.dp)
                    .background(LeafBadgeBg)
                    .cornerRadius(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_leaf),
                    contentDescription = null,
                    modifier = GlanceModifier.size(10.dp),
                    colorFilter = ColorFilter.tint(ColorProvider(LeafGreen)),
                )
            }
            Spacer(GlanceModifier.width(5.dp))
            Text(text = "CHECK IN", style = styles.title)
        }
        Spacer(GlanceModifier.height(6.dp))
        // Weighted rows make the grid fill the pane's full height exactly —
        // no dead band under the last row.
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            for (visualRow in TIMELINE_ROWS - 1 downTo 0) {
                TimelineRow(
                    visualRow = visualRow,
                    data = data,
                    geo = geo,
                    palette = palette,
                    styles = styles,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(
    visualRow: Int,
    data: WidgetCheckInData,
    geo: CalendarGeometry,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
    modifier: GlanceModifier,
) {
    val capacity = TIMELINE_ROW_CAPACITIES[visualRow]
    val emptyColumns = TIMELINE_COLUMNS - capacity
    // The top row is right-aligned in the home screen, everything else left.
    val leadingEmpty = if (visualRow == TIMELINE_ROWS - 1) emptyColumns else 0
    val slotIndices = timelineRowSlotIndices(visualRow)

    // Seven equal weighted slots => the grid spans the full pane width.
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (column in 0 until TIMELINE_COLUMNS) {
            val isTimelineSlot = column >= leadingEmpty && column < leadingEmpty + capacity
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (isTimelineSlot) {
                    MiniDayCell(
                        slotIndex = slotIndices[column - leadingEmpty],
                        data = data,
                        geo = geo,
                        palette = palette,
                        styles = styles,
                    )
                }
            }
        }
    }
}

/** Same chronological index mapping as the app's `timelineRowSlotIndices`. */
private fun timelineRowSlotIndices(visualRow: Int): List<Int> {
    val rowStart = TIMELINE_ROW_CAPACITIES.take(visualRow).sum()
    val capacity = TIMELINE_ROW_CAPACITIES[visualRow]
    return (0 until capacity).map { rowStart + capacity - 1 - it }
}

@Composable
private fun MiniDayCell(
    slotIndex: Int,
    data: WidgetCheckInData,
    geo: CalendarGeometry,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
) {
    val cell = data.cells.getOrNull(slotIndex)
        ?: WidgetDayCell(WidgetDayKind.Plain)
    when (visualKindOf(cell)) {
        MiniDayKind.Plain -> tintedDot(R.drawable.widget_dot, palette.cellFill, geo.cell)

        MiniDayKind.Plant -> {
            tintedDot(R.drawable.widget_dot, palette.cellFill, geo.cell)
            Image(
                provider = ImageProvider(R.drawable.widget_leaf),
                contentDescription = null,
                modifier = GlanceModifier.size(geo.cell * 0.72f),
                colorFilter = ColorFilter.tint(ColorProvider(LeafGreen)),
            )
        }

        MiniDayKind.AvatarDay -> {
            // Base day circle with the avatar image + home gradient ring on top.
            tintedDot(R.drawable.widget_dot, palette.cellFill, geo.cell)
            AvatarBadge(
                colorHex = cell.avatarColorHex,
                cellFill = palette.cellFill,
                size = geo.badge,
                overflow = cell.overflow,
                badgeStyle = styles.badge,
            )
        }

        MiniDayKind.FireToday -> {
            // Home uses currentOutline green for the today ring.
            tintedDot(R.drawable.widget_ring, CurrentOutline, geo.badge)
            Text(text = "🔥", style = styles.fire(geo.badge))
        }
    }
}

/**
 * Avatar cluster for an activity day: fill.secondary disc + home yellow→green
 * ring + glyph, plus a "+N" pill (timelineGridColors.badge).
 */
@Composable
private fun AvatarBadge(
    colorHex: String?,
    cellFill: Color,
    size: Dp,
    overflow: Int,
    badgeStyle: TextStyle,
) {
    Box(
        modifier = GlanceModifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        tintedDot(R.drawable.widget_dot, cellFill, size)
        Image(
            provider = ImageProvider(R.drawable.widget_avatar_ring),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
        )
        Image(
            provider = ImageProvider(avatarDrawableFor(colorHex)),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.72f),
        )
        if (overflow > 0) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.TopStart,
            ) {
                Box(
                    modifier = GlanceModifier
                        .height(10.dp)
                        .wrapContentWidth()
                        .background(OverflowBadgeBg)
                        .cornerRadius(5.dp)
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "+$overflow", style = badgeStyle)
                }
            }
        }
    }
}
// ---------------------------------------------------------------------------
// List pane — "N waiting today" header + contact rows
// ---------------------------------------------------------------------------

@Composable
private fun CheckInPane(
    data: WidgetCheckInData,
    modifier: GlanceModifier,
    night: Boolean,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
    compact: Boolean,
) {
    val avatarSize = if (compact) 28.dp else 32.dp
    val checkSize = if (compact) 22.dp else 24.dp

    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${data.waitingToday} waiting today",
                style = styles.waiting,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            val countdown = data.countdownLabel
            if (countdown != null) {
                Spacer(GlanceModifier.width(4.dp))
                GoldPill(label = countdown, night = night, styles = styles)
            }
        }
        Spacer(GlanceModifier.height(6.dp))
        // Weighted rows share ALL remaining height evenly — the list reaches
        // the bottom of the card with no dead space under the last row.
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight(),
        ) {
            data.contacts.forEachIndexed { index, contact ->
                if (index > 0) {
                    DashedRowDivider(palette = palette)
                }
                CheckInRow(
                    contact = contact,
                    palette = palette,
                    styles = styles,
                    avatarSize = avatarSize,
                    checkSize = checkSize,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            }
        }
    }
}

@Composable
private fun DashedRowDivider(palette: WidgetPalette) {
    Image(
        provider = ImageProvider(R.drawable.widget_dashed_divider),
        contentDescription = null,
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp),
        colorFilter = ColorFilter.tint(ColorProvider(palette.divider)),
    )
}

/** One contact line: avatar | bold name / meta | trailing state — on one line. */
@Composable
private fun CheckInRow(
    contact: WidgetContactRow,
    palette: WidgetPalette,
    styles: WidgetTextStyles,
    avatarSize: Dp,
    checkSize: Dp,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowAvatar(
            colorHex = contact.avatarColorHex,
            cellFill = palette.cellFill,
            size = avatarSize,
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                text = contact.name,
                style = styles.contactName,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(2.dp))
            Text(
                text = contact.subtitle,
                style = styles.contactMeta,
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        Spacer(GlanceModifier.width(4.dp))
        if (contact.checkedInToday) {
            Image(
                provider = ImageProvider(R.drawable.ic_circlecheckmark),
                contentDescription = null,
                modifier = GlanceModifier.size(checkSize),
                colorFilter = ColorFilter.tint(ColorProvider(palette.checkTint)),
            )
        } else {
            CheckInPill(styles = styles)
        }
    }
}

/** Ringed avatar: fill.secondary disc + home gradient ring + glyph (ContactAvatar). */
@Composable
private fun RowAvatar(colorHex: String?, cellFill: Color, size: Dp) {
    Box(
        modifier = GlanceModifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        tintedDot(R.drawable.widget_dot, cellFill, size)
        Image(
            provider = ImageProvider(R.drawable.widget_avatar_ring),
            contentDescription = null,
            modifier = GlanceModifier.size(size),
        )
        Image(
            provider = ImageProvider(avatarDrawableFor(colorHex)),
            contentDescription = null,
            modifier = GlanceModifier.size(size * 0.72f),
        )
    }
}

@Composable
private fun CheckInPill(styles: WidgetTextStyles) {
    Box(
        modifier = GlanceModifier
            .height(24.dp)
            .wrapContentWidth()
            .background(PillWhite)
            .cornerRadius(12.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Check in", style = styles.pill)
    }
}

@Composable
private fun GoldPill(label: String, night: Boolean, styles: WidgetTextStyles) {
    // Outer gold stroke + inner dark-olive fill (matches the design's outlined pill).
    Box(
        modifier = GlanceModifier
            .wrapContentWidth()
            .background(Gold)
            .cornerRadius(10.dp)
            .padding(1.dp),
    ) {
        Box(
            modifier = GlanceModifier
                .height(16.dp)
                .wrapContentWidth()
                .background(if (night) GoldPillFillNight else GoldPillFillLight)
                .cornerRadius(9.dp)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = label, style = styles.gold)
        }
    }
}
// ---------------------------------------------------------------------------
// Shared drawables helpers
// ---------------------------------------------------------------------------

@Composable
private fun tintedDot(drawableId: Int, tint: Color, size: Dp) {
    Image(
        provider = ImageProvider(drawableId),
        contentDescription = null,
        modifier = GlanceModifier.size(size),
        colorFilter = ColorFilter.tint(ColorProvider(tint)),
    )
}

/** Avatar glyph per contact color (same index map as `avatarIndexForColor`). */
private fun avatarDrawableFor(hex: String?): Int = when (hex?.uppercase()?.removePrefix("#")) {
    "FFCC33" -> R.drawable.widget_avatar_yellow
    "34C759" -> R.drawable.widget_avatar_green
    "FF9500" -> R.drawable.widget_avatar_orange
    "FF3B30" -> R.drawable.widget_avatar_red
    "AF52DE" -> R.drawable.widget_avatar_maroon
    "007AFF" -> R.drawable.widget_avatar_blue
    else -> R.drawable.widget_avatar_yellow
}