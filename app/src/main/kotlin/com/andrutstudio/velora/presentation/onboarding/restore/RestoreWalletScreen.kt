package com.andrutstudio.velora.presentation.onboarding.restore

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.presentation.onboarding.OnboardingViewModel
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import com.andrutstudio.velora.presentation.theme.SurfaceContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreWalletScreen(
    state: OnboardingViewModel.State,
    onNameChange: (String) -> Unit,
    onMnemonicTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    val view = LocalView.current
    if (!LocalInspectionMode.current) {
        DisposableEffect(Unit) {
            val window = (view.context as Activity).window
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restore Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isLoading) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Enter your seed phrase",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Type or paste all words separated by spaces.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // Wallet name
            OutlinedTextField(
                value = state.walletName,
                onValueChange = onNameChange,
                label = { Text("Wallet name") },
                placeholder = { Text("e.g. Restored Wallet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))

            // Single text area for the entire seed phrase
            OutlinedTextField(
                value = state.mnemonicText,
                onValueChange = onMnemonicTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = {
                    Text(
                        "When entering a seed phrase, separate each word with a space",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                isError = state.mnemonicError != null,
                supportingText = state.mnemonicError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                shape = RoundedCornerShape(15.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = SurfaceContainer,
                    unfocusedContainerColor = SurfaceContainer,
                ),
                maxLines = 5,
            )

            val wordCount = state.restoreWords.count { it.isNotEmpty() }
            if (wordCount > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "$wordCount word${if (wordCount != 1) "s" else ""} entered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End),
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSubmit,
                enabled = wordCount >= 12 && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Restore Wallet", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun RestoreWalletScreenPreview() {
    VeloraTheme {
        RestoreWalletScreen(
            state = OnboardingViewModel.State(
                walletName = "Restored Wallet",
                wordCount = 12,
                restoreWords = List(12) { "" },
            ),
            onNameChange = {},
            onMnemonicTextChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
