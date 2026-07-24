package app.usenekko.onboarding.name

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.usenekko.designsystem.buttons.NekkoButton
import app.usenekko.theme.NekkoTheme

@Composable
fun NameScreen(
    onNavigateToNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fullName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NekkoTheme.colors.background.b1)
            .padding(horizontal = 30.dp)
            .statusBarsPadding()
            .imePadding()
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "What should we call you?",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = NekkoTheme.colors.text.primary,
                lineHeight = 36.sp,
            )
        }

        Spacer(Modifier.height(25.dp))

        NameField(
            value = fullName,
            onValueChange = { fullName = it },
            onDone = {
                if (fullName.isNotBlank()) onNavigateToNext()
            },
        )

        Spacer(Modifier.weight(1f))

        NekkoButton(
            text = "Continue",
            onClick = onNavigateToNext,
            enabled = fullName.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit = {},
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = "Full name",
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = NekkoTheme.colors.text.tertiary,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(25.dp),
        singleLine = true,
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = NekkoTheme.colors.text.primary,
        ),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDone() }
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = NekkoTheme.colors.fill.secondary,
            unfocusedContainerColor = NekkoTheme.colors.fill.secondary,
            disabledContainerColor = NekkoTheme.colors.fill.secondary,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = NekkoTheme.colors.text.primary,
        ),
    )
}

@PreviewLightDark
@Composable
private fun NameScreenPreview() {
    NekkoTheme {
        NameScreen(onNavigateToNext = {})
    }
}
