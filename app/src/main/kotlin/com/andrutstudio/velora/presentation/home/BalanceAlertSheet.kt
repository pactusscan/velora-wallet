package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.domain.model.Amount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceAlertSheet(
    address: String,
    currentThresholdNanoPac: Long?,
    isEnabled: Boolean,
    currentType: com.andrutstudio.velora.data.local.db.AlertType,
    onDismiss: () -> Unit,
    onSave: (thresholdNanoPac: Long, enabled: Boolean, type: com.andrutstudio.velora.data.local.db.AlertType) -> Unit,
    onDelete: () -> Unit
) {
    var thresholdInput by remember { 
        mutableStateOf(if (currentThresholdNanoPac != null) (currentThresholdNanoPac / 1_000_000_000.0).toString() else "") 
    }
    var enabledState by remember { mutableStateOf(isEnabled) }
    var selectedType by remember { mutableStateOf(currentType) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Balance Alert",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Get notified based on the account balance threshold.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Alert Type Selection
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedType == com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN,
                    onClick = { selectedType = com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("Lower Than")
                }
                SegmentedButton(
                    selected = selectedType == com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN,
                    onClick = { selectedType = com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Higher Than")
                }
            }

            OutlinedTextField(
                value = thresholdInput,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) thresholdInput = it },
                label = { Text("Threshold (PAC)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = { Text("e.g. 10.0") }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Enable Alert", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = enabledState,
                    onCheckedChange = { enabledState = it }
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val threshold = thresholdInput.toDoubleOrNull() ?: 0.0
                    onSave((threshold * 1_000_000_000).toLong(), enabledState, selectedType)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = thresholdInput.toDoubleOrNull() != null
            ) {
                Text("Save Alert")
            }

            if (currentThresholdNanoPac != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove Alert")
                }
            }
        }
    }
}
