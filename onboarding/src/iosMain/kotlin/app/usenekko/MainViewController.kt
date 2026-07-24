package app.usenekko

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.welcome.WelcomeScreen

fun MainViewController() = ComposeUIViewController {
    val navigator = remember { Navigator(startDestination = Screen.Welcome) }
    App(navigator) { screen ->
        when (screen) {
            is Screen.Welcome -> WelcomeScreen()
        }
    }
}
