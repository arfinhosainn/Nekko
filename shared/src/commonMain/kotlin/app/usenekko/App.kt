package app.usenekko

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.usenekko.theme.NekkoTheme

@Composable
@Preview
fun App() {
    NekkoTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NekkoTheme.colors.background.b1),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nekko App",
                style = NekkoTheme.typography.heading2Bold,
                color = NekkoTheme.colors.text.primary,
            )
        }
    }
}
