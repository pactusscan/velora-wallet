package com.andrutstudio.velora.presentation.onboarding.security

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.presentation.components.PasswordTextField
import com.andrutstudio.velora.presentation.components.evaluateStrength
import com.andrutstudio.velora.presentation.onboarding.OnboardingViewModel
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSecurityScreen(
    state: OnboardingViewModel.State,
    onFinish: (password: String, confirm: String, biometric: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val canUseBiometric = remember {
        try {
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                    BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            false
        }
    }

    SetupSecurityContent(
        state = state,
        canUseBiometric = canUseBiometric,
        onFinish = onFinish,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupSecurityContent(
    state: OnboardingViewModel.State,
    canUseBiometric: Boolean,
    onFinish: (password: String, confirm: String, biometric: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(false) }

    val passwordsMatch = password == confirm && confirm.isNotBlank()
    val canProceed = password.length >= 8 && passwordsMatch && !state.isLoading

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Your Wallet") },
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
            Spacer(Modifier.height(8.dp))

            com.andrutstudio.velora.presentation.components.StepIndicator(totalSteps = 3, currentStep = 2)

            Spacer(Modifier.height(16.dp))

            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Set a password",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "This password encrypts your wallet on this device.\nYou'll need it to approve transactions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))

            PasswordTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                showStrengthMeter = true,
                imeAction = ImeAction.Next,
            )

            Spacer(Modifier.height(16.dp))

            PasswordTextField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirm password",
                isError = confirm.isNotBlank() && !passwordsMatch,
                supportingText = when {
                    state.passwordError != null -> state.passwordError
                    confirm.isNotBlank() && !passwordsMatch -> "Passwords do not match"
                    else -> null
                },
                imeAction = ImeAction.Done,
                onImeAction = { if (canProceed) onFinish(password, confirm, biometric) },
            )

            // Biometric toggle
            if (canUseBiometric) {
                Spacer(Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable biometric unlock",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "Use fingerprint or face to unlock without password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = biometric,
                            onCheckedChange = { biometric = it },
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onFinish(password, confirm, biometric) },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("Create Wallet", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SetupSecurityScreenPreview() {
    VeloraTheme {
        SetupSecurityContent(
            state = OnboardingViewModel.State(),
            canUseBiometric = true,
            onFinish = { _, _, _ -> },
            onBack = {},
        )
    }
}
