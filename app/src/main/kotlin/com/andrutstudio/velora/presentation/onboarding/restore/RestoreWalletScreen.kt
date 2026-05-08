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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.onboarding.OnboardingViewModel
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import com.andrutstudio.velora.presentation.theme.SurfaceContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreWalletScreen(
    state: OnboardingViewModel.State,
    onNameChange: (String) -> Unit,
    onWordChange: (Int, String) -> Unit,
    onWordCountChange: (Int) -> Unit,
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
                title = { Text(stringResource(R.string.restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !state.isLoading) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back))
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.mnemonic_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.mnemonic_restore_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // Wallet name
            OutlinedTextField(
                value = state.walletName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.restore_wallet_name_label)) },
                placeholder = { Text(stringResource(R.string.restore_wallet_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Word count selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.wordCount == 12,
                    onClick = { onWordCountChange(12) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(stringResource(R.string.restore_12_words))
                }
                SegmentedButton(
                    selected = state.wordCount == 24,
                    onClick = { onWordCountChange(24) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(stringResource(R.string.restore_24_words))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Grid of word inputs
            val words = state.restoreWords
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (i in 0 until state.wordCount step 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WordInput(
                            index = i,
                            value = words.getOrElse(i) { "" },
                            onValueChange = { onWordChange(i, it) },
                            modifier = Modifier.weight(1f)
                        )
                        if (i + 1 < state.wordCount) {
                            WordInput(
                                index = i + 1,
                                value = words.getOrElse(i + 1) { "" },
                                onValueChange = { onWordChange(i + 1, it) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (state.mnemonicError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = state.mnemonicError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onSubmit,
                enabled = words.all { it.isNotBlank() } && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(R.string.restore_button), style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WordInput(
    index: Int,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(48.dp),
        leadingIcon = {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                )
            }
        },
        placeholder = { Text("", style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrect = false,
            imeAction = ImeAction.Next
        ),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = SurfaceContainer,
            unfocusedContainerColor = SurfaceContainer,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
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
            onWordChange = { _, _ -> },
            onWordCountChange = {},
            onSubmit = {},
            onBack = {},
        )
    }
}
