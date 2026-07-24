package app.usenekko.navigation

import androidx.compose.runtime.mutableStateListOf

class Navigator(startDestination: Screen) {
    val backStack = mutableStateListOf(startDestination)

    fun navigate(screen: Screen) {
        backStack.add(screen)
    }

    fun goBack() {
        backStack.removeLastOrNull()
    }
}
