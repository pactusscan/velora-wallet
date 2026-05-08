package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletSheet(
    isCreating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, password: String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AddWalletSheetContent(
            isCreating = isCreating,
            error = error,
            onCreate = onCreate
        )
    }
}

@Composable
fun AddWalletSheetContent(
    isCreating: Boolean,
    error: String?,
    onCreate: (name: String, password: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val passwordsMatch = password == confirm
    val canSubmit = !isCreating && password.length >= 8 && passwordsMatch

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(stringResource(R.string.add_wallet_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.add_wallet_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.create_wallet_name_label)) },
            placeholder = { Text(stringResource(R.string.add_wallet_name_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.security_setup_password_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            supportingText = {
                if (password.isNotEmpty() && password.length < 8) {
                    Text(stringResource(R.string.add_wallet_password_hint))
                }
            },
            isError = password.isNotEmpty() && password.length < 8,
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.security_setup_confirm_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            isError = confirm.isNotEmpty() && !passwordsMatch,
            supportingText = {
                if (confirm.isNotEmpty() && !passwordsMatch) Text(stringResource(R.string.security_setup_passwords_mismatch))
            },
        )

        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = { onCreate(name, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit,
        ) {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.add_wallet_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddWalletSheetPreview() {
    VeloraTheme {
        Surface {
            AddWalletSheetContent(
                isCreating = false,
                error = null,
                onCreate = { _, _ -> }
            )
        }
    }
}
