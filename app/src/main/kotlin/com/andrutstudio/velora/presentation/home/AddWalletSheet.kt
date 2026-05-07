package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

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
        Text("Add new wallet", style = MaterialTheme.typography.titleLarge)
        Text(
            "A 12-word recovery phrase will be generated. Reveal it later from Settings before transferring funds.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Wallet name") },
            placeholder = { Text("e.g. Savings") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            supportingText = {
                if (password.isNotEmpty() && password.length < 8) {
                    Text("At least 8 characters")
                }
            },
            isError = password.isNotEmpty() && password.length < 8,
        )

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Confirm password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            isError = confirm.isNotEmpty() && !passwordsMatch,
            supportingText = {
                if (confirm.isNotEmpty() && !passwordsMatch) Text("Passwords do not match")
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
            Text("Create wallet")
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
