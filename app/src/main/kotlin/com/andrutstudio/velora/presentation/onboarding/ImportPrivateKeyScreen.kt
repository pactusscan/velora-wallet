package com.andrutstudio.velora.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPrivateKeyScreen(
    state: OnboardingViewModel.State,
    onNameChange: (String) -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Account") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isLoading) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Enter your private key to import an existing account. " +
                       "A private key is a secret code that gives you access to your funds.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = state.walletName,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Wallet name") },
                placeholder = { Text("My Imported Wallet") },
                singleLine = true,
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            val pkError = state.privateKeyError
            OutlinedTextField(
                value = state.privateKeyInput,
                onValueChange = onPrivateKeyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Private key") },
                placeholder = { Text("Enter your secret key…", fontFamily = FontFamily.Monospace) },
                isError = pkError != null,
                supportingText = pkError?.let { { Text(it) } },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                ),
                minLines = 2,
                maxLines = 4,
            )

            Text(
                text = "Make sure no one is watching your screen when entering the key.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state.privateKeyInput.isNotBlank() && !state.isLoading,
                shape = MaterialTheme.shapes.large,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ImportPrivateKeyScreenPreview() {
    VeloraTheme {
        ImportPrivateKeyScreen(
            state = OnboardingViewModel.State(),
            onNameChange = {},
            onPrivateKeyChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
