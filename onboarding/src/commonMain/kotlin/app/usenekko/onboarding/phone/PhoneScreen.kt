package app.usenekko.onboarding.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.PhoneNumberField
import app.usenekko.theme.NekkoTheme

@Composable
fun PhoneScreen(
    onNavigateToCodeVerification: (phoneNumber: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phone by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b0)
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .imePadding()
    ) {
        PhoneNumberField(
            phoneNumber = phone,
            onPhoneNumberChange = { phone = it },
            onDone = { fullNumber -> onNavigateToCodeVerification(fullNumber) },
        )

        Spacer(Modifier.weight(1f))

        NekkoButton(
            text = "Next",
            onClick = { onNavigateToCodeVerification("+60$phone") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NekkoTheme.typography.heading3Bold.fontFamily
            )
        )
        Spacer(Modifier.height(24.dp))
    }
}

@PreviewLightDark
@Composable
fun PreviewPhoneScreen() {
    NekkoTheme {
        PhoneScreen(onNavigateToCodeVerification = {})
    }
}
