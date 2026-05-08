package com.andrutstudio.velora.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@Composable
fun UnlockWalletDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onUnlock: (password: String) -> Unit,
    isBiometricEnabled: Boolean = false,
    onBiometricClick: () -> Unit = {},
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(Icons.Rounded.Lock, contentDescription = null)
        },
        title = { Text(stringResource(R.string.unlock_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.unlock_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                PasswordTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(R.string.backup_password_label),
                    isError = error != null,
                    supportingText = error,
                    onImeAction = { if (password.isNotBlank() && !isLoading) onUnlock(password) },
                )

                if (isBiometricEnabled) {
                    TextButton(
                        onClick = onBiometricClick,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.unlock_biometric))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onUnlock(password) },
                enabled = password.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.unlock_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.action_reject))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun UnlockWalletDialogPreview() {
    VeloraTheme {
        UnlockWalletDialog(
            isLoading = false,
            error = null,
            onDismiss = {},
            onUnlock = {},
            isBiometricEnabled = true
        )
    }
}
