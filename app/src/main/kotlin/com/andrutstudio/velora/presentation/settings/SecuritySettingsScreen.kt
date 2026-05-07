package com.andrutstudio.velora.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.andrutstudio.velora.presentation.components.PasswordTextField
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsViewModel.Effect.ShowSnackbar ->
                    scope.launch { snackbarHostState.showSnackbar(effect.message) }
                else -> Unit
            }
        }
    }

    SecuritySettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onOldPasswordChange = viewModel::onOldPasswordChange,
        onNewPasswordChange = viewModel::onNewPasswordChange,
        onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
        onChangePassword = viewModel::onChangePassword
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecuritySettingsContent(
    state: SettingsViewModel.State,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onChangePassword: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Change Password", style = MaterialTheme.typography.titleMedium)

            PasswordTextField(
                value = state.oldPassword,
                onValueChange = onOldPasswordChange,
                label = "Current Password",
                isError = state.passwordError != null && state.oldPassword.isBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordTextField(
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                label = "New Password",
                isError = state.passwordError != null && state.newPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )

            PasswordTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = "Confirm New Password",
                isError = state.passwordError != null,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.passwordError != null) {
                Text(
                    text = state.passwordError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // Password strength hint
            if (state.newPassword.isNotBlank()) {
                PasswordStrengthRow(password = state.newPassword)
            }

            Button(
                onClick = onChangePassword,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isPasswordLoading,
            ) {
                if (state.isPasswordLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SecuritySettingsScreenPreview() {
    VeloraTheme {
        SecuritySettingsContent(
            state = SettingsViewModel.State(
                oldPassword = "current",
                newPassword = "newPassword123!",
                confirmPassword = "newPassword123!"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onOldPasswordChange = {},
            onNewPasswordChange = {},
            onConfirmPasswordChange = {},
            onChangePassword = {}
        )
    }
}

@Composable
private fun PasswordStrengthRow(password: String) {
    val strength = when {
        password.length >= 12 && password.any { it.isUpperCase() } &&
                password.any { it.isDigit() } && password.any { !it.isLetterOrDigit() } -> 3
        password.length >= 10 && (password.any { it.isUpperCase() } || password.any { it.isDigit() }) -> 2
        password.length >= 8 -> 1
        else -> 0
    }
    val (label, color) = when (strength) {
        3 -> "Strong" to MaterialTheme.colorScheme.tertiary
        2 -> "Medium" to MaterialTheme.colorScheme.primary
        1 -> "Weak" to MaterialTheme.colorScheme.error
        else -> "Too short" to MaterialTheme.colorScheme.error
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                LinearProgressIndicator(
                    progress = { if (index < strength) 1f else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
