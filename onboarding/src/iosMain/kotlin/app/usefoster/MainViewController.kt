package app.usefoster

import androidx.compose.ui.window.ComposeUIViewController
import app.usefoster.navigation.Screen
import app.usefoster.navigation.rememberNavigator
import app.usefoster.onboarding.OnboardingApp
import app.usefoster.onboarding.data.supabase.createAppSupabaseClient
import app.usefoster.shared.secrets.configureSecretsFromInfoPlist
import app.usefoster.shared.subscription.initRevenueCat
import app.usefoster.widget.IosWidgetBridge
import io.github.jan.supabase.SupabaseClient

/**
 * The single app-wide Supabase client on iOS. Previously a private lazy in
 * [MainViewController]; now shared so the widget bridge (see
 * widget/IosWidgetBridge.kt) reuses the exact instance the UI uses — same
 * persisted auth session, one connection pool.
 */
object IosAppSupabase {
    val client: SupabaseClient by lazy { createAppSupabaseClient() }
}

fun MainViewController() = ComposeUIViewController {
    // Secrets bootstrap must precede both initRevenueCat() and the lazy
    // Supabase client below — values come from Info.plist entries that Xcode
    // injects from the gitignored Secrets.xcconfig.
    configureSecretsFromInfoPlist()
    initRevenueCat()

    // Home-widget bridge: pushes fresh check-in data into the shared App
    // Group on foreground/background transitions (mirrors the Android
    // WidgetRefresher lifecycle hooks).
    IosWidgetBridge.install()

    val navigator = rememberNavigator(startDestination = Screen.Splash)
    OnboardingApp(navigator, IosAppSupabase.client)
}
