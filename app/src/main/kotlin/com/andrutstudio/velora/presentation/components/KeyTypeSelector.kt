package com.andrutstudio.velora.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.presentation.theme.VeloraTheme

/** Picks one of the three account key types; the chosen one is filled. */
@Composable
fun KeyTypeSelector(
    selected: AccountType,
    onSelect: (AccountType) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AccountType.entries.forEach { type ->
            val label = @Composable {
                Text(
                    text = stringResource(type.labelRes()),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
            val buttonModifier = Modifier.weight(1f)
            val shape = RoundedCornerShape(8.dp)
            if (type == selected) {
                Button(onClick = {}, modifier = buttonModifier, enabled = enabled, shape = shape) { label() }
            } else {
                OutlinedButton(onClick = { onSelect(type) }, modifier = buttonModifier, enabled = enabled, shape = shape) {
                    label()
                }
            }
        }
    }
}

fun AccountType.labelRes(): Int = when (this) {
    AccountType.ED25519 -> R.string.key_type_ed25519
    AccountType.BLS -> R.string.key_type_bls
    AccountType.SECP256K1 -> R.string.key_type_secp256k1
}

@Preview(showBackground = true)
@Composable
private fun KeyTypeSelectorPreview() {
    VeloraTheme {
        Surface {
            KeyTypeSelector(selected = AccountType.BLS, onSelect = {}, modifier = Modifier.padding(16.dp))
        }
    }
}
