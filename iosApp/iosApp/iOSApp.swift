import SwiftUI
import UserNotifications
import WidgetKit

@main
struct iOSApp: App {
    init() {
        let delegate = NotificationDelegate.shared
        delegate.registerCategories()
        UNUserNotificationCenter.current().delegate = delegate

        // Home-widget bridge: the Kotlin side publishes fresh check-in data
        // into the shared App Group and posts this notification; forward it
        // to WidgetKit so placed widgets re-render immediately.
        NotificationCenter.default.addObserver(
            forName: Notification.Name("app.usefoster.widget.reload"),
            object: nil,
            queue: .main
        ) { _ in
            WidgetCenter.shared.reloadTimelines(ofKind: "FosterWidget")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
