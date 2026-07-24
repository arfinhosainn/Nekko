package app.usenekko

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.name.NameScreen
import app.usenekko.onboarding.phone.CodeVerificationScreen
import app.usenekko.onboarding.phone.PhoneScreen
import app.usenekko.onboarding.welcome.WelcomeScreen

fun MainViewController() = ComposeUIViewController {
    val navigator = remember { Navigator(startDestination = Screen.Welcome) }
    App(navigator) { screen ->
        when (screen) {
            is Screen.Welcome -> WelcomeScreen(
                onNavigateToPhone = { navigator.navigate(Screen.Phone) },
            )
            is Screen.Phone -> PhoneScreen(
                onNavigateToCodeVerification = { phoneNumber ->
                    navigator.navigate(Screen.CodeVerification(phoneNumber))
                },
            )
            is Screen.CodeVerification -> CodeVerificationScreen(
                phoneNumber = screen.phoneNumber,
                onNavigateToNext = { navigator.navigate(Screen.Name) },
            )
            is Screen.Name -> NameScreen(
                onNavigateToNext = { /* TODO: next screen */ },
            )
        }
    }
}
