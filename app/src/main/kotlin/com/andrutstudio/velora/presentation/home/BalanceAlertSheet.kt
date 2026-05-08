package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
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
                stringResource(R.string.alert_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                stringResource(R.string.alert_description),
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
                    Text(stringResource(R.string.alert_lower_than))
                }
                SegmentedButton(
                    selected = selectedType == com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN,
                    onClick = { selectedType = com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(stringResource(R.string.alert_higher_than))
                }
            }

            OutlinedTextField(
                value = thresholdInput,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) thresholdInput = it },
                label = { Text(stringResource(R.string.alert_threshold_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.alert_threshold_placeholder)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.alert_enable), style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = enabledState,
                    onCheckedChange = { enabledState = it },
                    thumbContent = if (enabledState) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = Color.Transparent,
                        uncheckedBorderColor = MaterialTheme.colorScheme.primary,
                    )
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
                Text(stringResource(R.string.alert_save))
            }

            if (currentThresholdNanoPac != null) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.alert_remove))
                }
            }
        }
    }
}
