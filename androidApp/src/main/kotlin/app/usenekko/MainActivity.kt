package app.usenekko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import app.usenekko.navigation.Navigator
import app.usenekko.navigation.Screen
import app.usenekko.onboarding.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val navigator = remember { Navigator(startDestination = Screen.Welcome) }
            App(navigator) { screen ->
                when (screen) {
                    is Screen.Welcome -> WelcomeScreen()
                }
            }
        }
    }
}