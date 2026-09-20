import WidgetKit
import SwiftUI

// ---------------------------------------------------------------------------
// Data model — mirrors the kotlinx-serialization JSON published by the app
// (see onboarding/.../widget/IosWidgetBridge.kt and
// home/.../widget/WidgetCheckInData.kt). Field names must stay in lockstep.
// ---------------------------------------------------------------------------

enum WidgetDayKind: String, Codable {
    case Plain
    case AvatarDay
    case FireToday
}

struct WidgetDayCell: Codable {
    var kind: WidgetDayKind
    var avatarColorHex: String?
    var overflow: Int?
}

struct WidgetContactRow: Codable {
    var name: String
    var avatarColorHex: String?
    var subtitle: String
    var checkedInToday: Bool
}

struct WidgetCheckInData: Codable {
    var contacts: [WidgetContactRow]?
    var waitingToday: Int?
    var countdownLabel: String?
    var cells: [WidgetDayCell]?
    var today: String?
    var fetchedAt: Int64?
}

// ---------------------------------------------------------------------------
// Storage — the App Group defaults the app bridge writes into.
// ---------------------------------------------------------------------------

enum WidgetStore {
    static let appGroup = "group.app.usefoster.Foster"
    static let cacheKey = "widget_checkin_data_v1"

    static func load() -> WidgetCheckInData? {
        guard let defaults = UserDefaults(suiteName: appGroup) else { return nil }
        guard let raw = defaults.string(forKey: cacheKey) else { return nil }
        return try? JSONDecoder().decode(WidgetCheckInData.self, from: Data(raw.utf8))
    }
}

// ---------------------------------------------------------------------------
// Palette — tuned for better contrast/softness in widgets.
// ---------------------------------------------------------------------------

struct WidgetPalette {
    let cardBg: Color
    let textPrimary: Color
    let textSecondary: Color
    let textTertiary: Color
    let cellFill: Color
    let divider: Color
    let hairline: Color
    let checkTint: Color
    let accentYellow: Color
    let ringGreen: Color

    static func resolve(_ scheme: ColorScheme) -> WidgetPalette {
        if scheme == .dark {
            return WidgetPalette(
                cardBg: Color(red: 0x10 / 255, green: 0x10 / 255, blue: 0x12 / 255),
                textPrimary: Color.white.opacity(0.96),
                textSecondary: Color.white.opacity(0.86),
                textTertiary: Color.white.opacity(0.65),
                cellFill: Color.white.opacity(0.08),
                divider: Color.white.opacity(0.08),
                hairline: Color.white.opacity(0.12),
                checkTint: Color.white.opacity(0.75),
                accentYellow: Color(red: 0xEA / 255, green: 0xB3 / 255, blue: 0x08 / 255),
                ringGreen: Color(red: 0x28 / 255, green: 0xD8 / 255, blue: 0x6F / 255)
            )
        } else {
            return WidgetPalette(
                cardBg: Color(red: 0xFA / 255, green: 0xFA / 255, blue: 0xFA / 255),
                textPrimary: Color(red: 0x18 / 255, green: 0x18 / 255, blue: 0x1B / 255),
                textSecondary: Color(red: 0x2A / 255, green: 0x2A / 255, blue: 0x2E / 255),
                textTertiary: Color(red: 0x71 / 255, green: 0x71 / 255, blue: 0x7A / 255),
                cellFill: Color.black.opacity(0.05),
                divider: Color.black.opacity(0.06),
                hairline: Color.black.opacity(0.10),
                checkTint: Color(red: 0x55 / 255, green: 0xB5 / 255, blue: 0x79 / 255),
                accentYellow: Color(red: 0xEA / 255, green: 0xB3 / 255, blue: 0x08 / 255),
                ringGreen: Color(red: 0x28 / 255, green: 0xD8 / 255, blue: 0x6F / 255)
            )
        }
    }
}

private let leafGreen = Color(red: 0x22 / 255, green: 0xC5 / 255, blue: 0x5E / 255)
private let currentOutline = Color(red: 0x28 / 255, green: 0xD8 / 255, blue: 0x6F / 255)

/// Avatar glyph per contact color (same index map as Android `avatarDrawableFor`).
private func avatarImage(for hex: String?) -> Image {
    switch hex?.uppercased().replacingOccurrences(of: "#", with: "") {
    case "FFCC33": return Image("AvatarYellow")
    case "34C759": return Image("AvatarGreen")
    case "FF9500": return Image("AvatarOrange")
    case "FF3B30": return Image("AvatarRed")
    case "AF52DE": return Image("AvatarMaroon")
    case "007AFF": return Image("AvatarBlue")
    default: return Image("AvatarYellow")
    }
}

/// Tapping the widget deep-links into the app (scheme registered in
/// the app target's Info.plist CFBundleURLSchemes).
private let appOpenURL = URL(string: "app.usefoster://home")!

extension View {
    /// `containerBackground(for: .widget)` is iOS 17+; fall back on iOS 16.
    @ViewBuilder
    func widgetBackground(_ color: Color) -> some View {
        if #available(iOSApplicationExtension 17.0, *) {
            containerBackground(for: .widget) { color }
        } else {
            background(color)
        }
    }
}

// ---------------------------------------------------------------------------
// Timeline provider — purely reads the App Group snapshot; no network.
// ---------------------------------------------------------------------------

struct FosterEntry: TimelineEntry {
    let date: Date
    let data: WidgetCheckInData?
}

struct FosterProvider: TimelineProvider {
    func placeholder(in context: Context) -> FosterEntry {
        FosterEntry(date: Date(), data: WidgetStore.load())
    }

    func getSnapshot(in context: Context, completion: @escaping (FosterEntry) -> Void) {
        completion(FosterEntry(date: Date(), data: WidgetStore.load()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<FosterEntry>) -> Void) {
        let entry = FosterEntry(date: Date(), data: WidgetStore.load())
        // The app bridge requests reloads on data changes; a modest periodic
        // refresh keeps the countdown/count honest across day rollovers.
        let next = Calendar.current.date(byAdding: .minute, value: 30, to: Date())!
        completion(Timeline(entries: [entry], policy: .after(next)))
    }
}

struct FosterWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "FosterWidget", provider: FosterProvider()) { entry in
            FosterWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Check-ins")
        .description("Today's check-ins at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct FosterWidgetEntryView: View {
    @Environment(\.colorScheme) private var scheme
    @Environment(\.widgetFamily) private var family
    let entry: FosterEntry

    var body: some View {
        content
            .padding(12) // keep content away from edges
            .widgetURL(appOpenURL)
            .widgetBackground(palette.cardBg)
    }

    private var palette: WidgetPalette { WidgetPalette.resolve(scheme) }

    @ViewBuilder
    private var content: some View {
        if family == .systemMedium {
            mediumLayout
        } else {
            smallLayout
        }
    }

    // MARK: Small — header + waiting count + countdown + last 7 cells

    private var smallLayout: some View {
        VStack(alignment: .leading, spacing: 8) {
            HeaderChip(palette: palette)
            Text(waitingHeadline)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(palette.textPrimary)
                .lineLimit(2)
            if let countdown = entry.data?.countdownLabel, !countdown.isEmpty {
                Text(countdown)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(palette.textTertiary)
            }
            Spacer(minLength: 0)
            HStack(spacing: 4) {
                let tail = Array((entry.data?.cells ?? []).suffix(7))
                ForEach(tail.indices, id: \.self) { index in
                    MiniDayCellView(cell: tail[index], palette: palette, badgeScale: 0.85)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
    }

    // MARK: Medium — 26-dot zigzag grid + checklist pane

    private var mediumLayout: some View {
        GeometryReader { geo in
            VStack(spacing: 10) {
                // Top bar
                HStack(spacing: 10) {
                    HeaderChip(palette: palette)
                    Spacer(minLength: 0)
                    // Slight nudge to the right per feedback
                    Text(waitingHeadline)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(palette.textPrimary)
                        .lineLimit(1)
                        .padding(.leading, 2)
                    // Countdown pill removed as requested
                }

                HStack(spacing: 12) {
                    // Left decorative field — exactly 26 dots
                    DotFieldPane(
                        data: entry.data ?? WidgetCheckInData(),
                        palette: palette
                    )
                    .frame(width: geo.size.width * 0.40,
                           height: geo.size.height * 0.64,
                           alignment: .center)

                    // Center divider sized and centered to match the dot field height
                    Rectangle()
                        .fill(palette.hairline)
                        .frame(width: 1, height: geo.size.height * 0.64)
                        .frame(maxHeight: .infinity, alignment: .center)

                    // Right list
                    CheckInListPane(
                        data: entry.data ?? WidgetCheckInData(),
                        palette: palette
                    )
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                }
            }
        }
    }

    private var waitingHeadline: String {
        let count = entry.data?.waitingToday ?? 0
        if count > 0 {
            return count == 1 ? "1 waiting today" : "\(count) waiting today"
        }
        return entry.data?.contacts?.isEmpty == false ? "All checked in 🎉" : "No contacts yet"
    }
}

// MARK: Header chip

private struct HeaderChip: View {
    let palette: WidgetPalette
    var body: some View {
        HStack(spacing: 6) {
            ZStack {
                Circle()
                    .fill(leafGreen.opacity(0.18))
                    .frame(width: 18, height: 18)
                Image(systemName: "leaf.fill")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundColor(leafGreen)
            }
            Text("CHECK IN")
                .font(.system(size: 10, weight: .semibold))
                .foregroundColor(palette.textSecondary)
        }
    }
}

// MARK: Decorative dot field — 26-dot zigzag (capacities [1,7,7,7,4])

private struct DotFieldPane: View {
    let data: WidgetCheckInData
    let palette: WidgetPalette

    // 7 columns; rows bottom→top have capacities [1,7,7,7,4] (total 26)
    private static let columns = 7
    private static let rowCapacities = [1, 7, 7, 7, 4]

    var body: some View {
        GeometryReader { geo in
            let rows = DotFieldPane.rowCapacities.count
            let colWidth = geo.size.width / CGFloat(DotFieldPane.columns)
            let rowHeight = geo.size.height / CGFloat(rows)
            let dotSize = min(colWidth, rowHeight) * 0.42

            ZStack {
                // Draw rows from top to bottom, but compute capacities as bottom→top pattern
                VStack(spacing: 0) {
                    ForEach((0..<rows).reversed(), id: \.self) { row in
                        let capacity = DotFieldPane.rowCapacities[row]
                        let emptyColumns = DotFieldPane.columns - capacity
                        // Top row right-aligned, others left-aligned
                        let leadingEmpty = (row == rows - 1) ? emptyColumns : 0

                        HStack(spacing: 0) {
                            ForEach(0..<DotFieldPane.columns, id: \.self) { column in
                                if column >= leadingEmpty, column < leadingEmpty + capacity {
                                    Circle()
                                        .fill(palette.divider)
                                        .frame(width: dotSize, height: dotSize)
                                        .frame(width: colWidth, height: rowHeight, alignment: .center)
                                } else {
                                    Color.clear
                                        .frame(width: colWidth, height: rowHeight)
                                }
                            }
                        }
                    }
                }

                // Flame marker — position on FireToday slot if available
                if let pos = flamePosition(in: CGSize(width: geo.size.width, height: geo.size.height)) {
                    Circle()
                        .stroke(currentOutline.opacity(0.95), lineWidth: 1.4)
                        .frame(width: dotSize * 1.25, height: dotSize * 1.25)
                        .position(pos)
                    Text("🔥")
                        .font(.system(size: dotSize * 0.70))
                        .position(pos)
                }

                // Up to 3 avatars overlaid decoratively near existing dots
                let avatarCells = (data.cells ?? []).filter { $0.kind == .AvatarDay }
                ForEach(avatarCells.prefix(3).indices, id: \.self) { i in
                    let pos = avatarPosition(index: i, in: CGSize(width: geo.size.width, height: geo.size.height))
                    let size = dotSize * 2.0
                    ZStack {
                        Circle()
                            .fill(palette.cellFill)
                            .frame(width: size, height: size)
                        Circle()
                            .stroke(
                                LinearGradient(
                                    colors: [palette.accentYellow.opacity(0.95), leafGreen],
                                    startPoint: .topLeading, endPoint: .bottomTrailing
                                ),
                                lineWidth: 1.6
                            )
                            .frame(width: size, height: size)
                        avatarImage(for: avatarCells[i].avatarColorHex)
                            .resizable()
                            .scaledToFit()
                            .frame(width: size * 0.66, height: size * 0.66)
                        if let overflow = avatarCells[i].overflow, overflow > 0 {
                            Text("+\(overflow)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 2)
                                .background(Capsule().fill(leafGreen))
                                .offset(x: -size * 0.34, y: -size * 0.34)
                        }
                    }
                    .position(pos)
                }
            }
        }
    }

    // Map FireToday cell index into a row/column center point
    private func flamePosition(in size: CGSize) -> CGPoint? {
        guard let idx = (data.cells ?? []).firstIndex(where: { $0.kind == .FireToday }) else {
            // Fallback to a pleasant center-ish spot in the grid
            let col = 4
            let rowFromBottom = 2
            return pointFor(col: col, rowFromBottom: rowFromBottom, in: size)
        }
        let position = positionForCellIndex(idx)
        return pointFor(col: position.col, rowFromBottom: position.rowFromBottom, in: size)
    }

    private func positionForCellIndex(_ index: Int) -> (rowFromBottom: Int, col: Int) {
        var remaining = index
        for row in 0..<DotFieldPane.rowCapacities.count {
            let capacity = DotFieldPane.rowCapacities[row]
            if remaining < capacity {
                let empty = DotFieldPane.columns - capacity
                let leadingEmpty = (row == DotFieldPane.rowCapacities.count - 1) ? empty : 0
                let col = leadingEmpty + (capacity - 1 - remaining)
                return (rowFromBottom: row, col: col)
            } else {
                remaining -= capacity
            }
        }
        return (rowFromBottom: 2, col: 3)
    }

    private func pointFor(col: Int, rowFromBottom: Int, in size: CGSize) -> CGPoint {
        let rows = DotFieldPane.rowCapacities.count
        let colWidth = size.width / CGFloat(DotFieldPane.columns)
        let rowHeight = size.height / CGFloat(rows)
        let rowFromTop = (rows - 1) - rowFromBottom
        return CGPoint(
            x: (CGFloat(col) + 0.5) * colWidth,
            y: (CGFloat(rowFromTop) + 0.5) * rowHeight
        )
    }

    // Decorative avatar anchor points aligned to meaningful cells
    private func avatarPosition(index: Int, in size: CGSize) -> CGPoint {
        let anchorIndices = [3, 10, 22] // within 0..<26
        let pos = positionForCellIndex(anchorIndices[min(index, anchorIndices.count - 1)])
        return pointFor(col: pos.col, rowFromBottom: pos.rowFromBottom, in: size)
    }
}

// MARK: Mini cell (used in small widget)

private struct MiniDayCellView: View {
    let cell: WidgetDayCell?
    let palette: WidgetPalette
    let badgeScale: CGFloat

    var body: some View {
        switch cell?.kind ?? .Plain {
        case .Plain:
            Circle()
                .fill(palette.cellFill)
                .frame(width: cellSize, height: cellSize)
        case .AvatarDay:
            ZStack {
                Circle()
                    .fill(palette.cellFill)
                    .frame(width: cellSize, height: cellSize)
                Circle()
                    .stroke(
                        LinearGradient(
                            colors: [palette.accentYellow, leafGreen],
                            startPoint: .topLeading, endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.4
                    )
                    .frame(width: cellSize, height: cellSize)
                avatarImage(for: cell?.avatarColorHex)
                    .resizable()
                    .scaledToFit()
                    .frame(width: cellSize * 0.66, height: cellSize * 0.66)
                if let overflow = cell?.overflow, overflow > 0 {
                    Text("+\(overflow)")
                        .font(.system(size: 7, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 2.5)
                        .background(Capsule().fill(leafGreen))
                        .offset(x: -cellSize / 2 + 4, y: -cellSize / 2 + 4)
                }
            }
            .frame(width: cellSize, height: cellSize)
        case .FireToday:
            ZStack {
                Circle()
                    .stroke(currentOutline, lineWidth: 1.4)
                    .frame(width: cellSize, height: cellSize)
                Text("🔥")
                    .font(.system(size: cellSize * 0.52))
            }
            .frame(width: cellSize, height: cellSize)
        }
    }

    private var cellSize: CGFloat { 16 * badgeScale }
}

// MARK: Right-hand list — tighter button that preserves name

private struct CheckInListPane: View {
    let data: WidgetCheckInData
    let palette: WidgetPalette

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ForEach(Array((data.contacts ?? []).prefix(2).enumerated()), id: \.offset) { index, row in
                ContactRowView(row: row, isFirst: index == 0, palette: palette)
            }
            Spacer(minLength: 0)
        }
    }
}

private struct ContactRowView: View {
    let row: WidgetContactRow
    let isFirst: Bool
    let palette: WidgetPalette

    var body: some View {
        HStack(spacing: 8) {
            AvatarRing(image: avatarImage(for: row.avatarColorHex),
                       ringColor: row.checkedInToday ? palette.ringGreen : palette.accentYellow.opacity(0.95),
                       size: 24,
                       ringWidth: row.checkedInToday ? 1.1 : 1.4,
                       fill: palette.cellFill)

            VStack(alignment: .leading, spacing: 2) {
                Text(row.name)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(palette.textPrimary)
                    .lineLimit(1)
                    .truncationMode(.tail)
                    .layoutPriority(1) // prefer name over button
                Text(row.subtitle)
                    .font(.system(size: 10, weight: .medium))
                    .foregroundColor(palette.textTertiary)
                    .lineLimit(1)
            }

            Spacer(minLength: 6)

            if row.checkedInToday {
                // trailing green check in a subtle circle
                ZStack {
                    Circle()
                        .fill(Color.clear)
                        .frame(width: 18, height: 18)
                        .overlay(Circle().stroke(palette.hairline, lineWidth: 1))
                    Image(systemName: "checkmark")
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(palette.ringGreen)
                }
                .layoutPriority(0)
            } else if isFirst {
                // Smaller capsule so the name has room; softer outline
                Text("Check in")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundColor(palette.textPrimary)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(Capsule().fill(palette.cellFill))
                    .overlay(Capsule().stroke(currentOutline.opacity(0.85), lineWidth: 1.0))
                    .lineLimit(1)
                    .fixedSize(horizontal: true, vertical: false)
                    .layoutPriority(0)
            }
        }
    }
}

private struct AvatarRing: View {
    let image: Image
    let ringColor: Color
    let size: CGFloat
    let ringWidth: CGFloat
    let fill: Color

    var body: some View {
        ZStack {
            Circle()
                .fill(fill)
                .frame(width: size, height: size)
            Circle()
                .stroke(ringColor, lineWidth: ringWidth)
                .frame(width: size, height: size)
            image
                .resizable()
                .scaledToFit()
                .frame(width: size * 0.68, height: size * 0.68)
        }
    }
}

@main
struct FosterWidgetBundle: WidgetBundle {
    var body: some Widget {
        FosterWidget()
    }
}
