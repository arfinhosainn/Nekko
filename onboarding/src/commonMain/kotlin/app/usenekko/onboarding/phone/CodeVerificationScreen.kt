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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.onboarding.components.PhoneNumberField
import app.usenekko.onboarding.components.VerificationCodeField
import app.usenekko.theme.NekkoTheme

@Composable
fun CodeVerificationScreen(
    phoneNumber: String,
    onNavigateToNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b1)
            .padding(horizontal = 32.dp)
            .statusBarsPadding()
            .imePadding()
    ) {
        PhoneNumberField(
            phoneNumber = phoneNumber.removePrefix("+60"),
            onPhoneNumberChange = {},
            isConfirmed = true,
        )

        Spacer(Modifier.height(16.dp))

        VerificationCodeField(
            code = code,
            onCodeChange = { code = it },
            isLoading = isVerifying,
            onDone = {
                isVerifying = true
                onNavigateToNext()
            },
        )

        Spacer(Modifier.weight(1f))

        NekkoButton(
            text = "Next",
            onClick = {
                if (!isVerifying) {
                    isVerifying = true
                    onNavigateToNext()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@PreviewLightDark
@Composable
fun PreviewCodeVerificationScreen() {
    NekkoTheme {
        CodeVerificationScreen(phoneNumber = "+60123456789", onNavigateToNext = {})
    }
}
