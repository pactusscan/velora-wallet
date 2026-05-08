package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    isAdding: Boolean,
    onDismiss: () -> Unit,
    onAdd: (type: AccountType, label: String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        AddAccountSheetContent(
            isAdding = isAdding,
            onAdd = onAdd
        )
    }
}

@Composable
fun AddAccountSheetContent(
    isAdding: Boolean,
    onAdd: (type: AccountType, label: String) -> Unit,
) {
    var label by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_add_account), style = MaterialTheme.typography.titleLarge)

        Text(
            text = stringResource(R.string.home_add_account_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = label,
            onValueChange = { label = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.home_rename_account_label)) },
            placeholder = { Text(stringResource(R.string.add_wallet_name_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )

        Button(
            onClick = { onAdd(AccountType.ED25519, label.trim()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isAdding,
        ) {
            if (isAdding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(R.string.action_add))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddAccountSheetPreview() {
    VeloraTheme {
        Surface {
            AddAccountSheetContent(
                isAdding = false,
                onAdd = { _, _ -> },
            )
        }
    }
}
