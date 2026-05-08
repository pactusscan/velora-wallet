package com.andrutstudio.velora.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FromAccountSection(
    accounts: List<Account>,
    selected: Account?,
    balance: Amount,
    onSelect: (Account) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (accounts.size > 1) expanded = it },
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.label}  •  ${it.address.take(10)}…" } ?: "",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            trailingIcon = {
                if (accounts.size > 1) ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            supportingText = {
                Text(
                    text = stringResource(R.string.common_balance, formatPac(balance)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
        if (accounts.size > 1) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(account.label, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    account.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        onClick = {
                            onSelect(account)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FromAccountSectionPreview() {
    val accounts = listOf(
        Account("pc1address1...", "Main Account", AccountType.ED25519, 0),
        Account("pc1address2...", "Savings", AccountType.ED25519, 1)
    )
    VeloraTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            FromAccountSection(
                accounts = accounts,
                selected = accounts[0],
                balance = Amount.fromPac(100.0),
                onSelect = {}
            )
        }
    }
}


