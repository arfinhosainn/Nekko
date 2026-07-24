package app.usenekko.navigation

import androidx.compose.runtime.Composable

@Composable
fun NekkoNavHost(
    navigator: Navigator,
    screenContent: @Composable (Screen) -> Unit
) {
    val currentScreen = navigator.backStack.lastOrNull() ?: return
    screenContent(currentScreen)
}
