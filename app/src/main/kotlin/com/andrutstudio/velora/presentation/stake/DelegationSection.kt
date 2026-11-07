package com.andrutstudio.velora.presentation.stake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.DelegationRules
import com.andrutstudio.velora.presentation.components.asString
import com.andrutstudio.velora.presentation.components.formatPac
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Approximate calendar date of a future block, at 10 s per block. */
internal fun approxDate(block: Long, height: Long): String =
    Instant.now().plusSeconds(DelegationRules.secondsUntil(block, height))
        .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

internal fun shortAddr(address: String): String =
    if (address.length > 18) "${address.take(10)}…${address.takeLast(6)}" else address

/** What the node knows about the validator, and the Stake Owner's unbond. */
@Composable
internal fun ValidatorStatusCard(state: StakeViewModel.State, onUnbondOwned: () -> Unit) {
    if (!state.validatorLookedUp) return
    val info = state.validatorInfo
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (state.validatorMoniker.isNotBlank()) {
                Text(state.validatorMoniker, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.stake_current_label, formatPac(info?.stake ?: com.andrutstudio.velora.domain.model.Amount.ZERO)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (info?.delegated == true) {
                Text(
                    stringResource(R.string.stake_delegated_info, shortAddr(info.delegateOwner), "%,d".format(info.delegateExpiry)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (info?.isUnbonded == true) {
                Text(
                    stringResource(R.string.stake_error_unbonded_validator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            val owned = state.ownedDelegation
            if (owned != null) {
                val canUnbond = state.height >= owned.delegateExpiry
                Text(stringResource(R.string.stake_owner_title), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text(
                    if (canUnbond) stringResource(R.string.stake_owner_can_unbond)
                    else stringResource(R.string.stake_owner_locked, "%,d".format(owned.delegateExpiry)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = onUnbondOwned,
                    enabled = canUnbond && !state.isStaking,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.stake_unbond)) }
            }
        }
    }
}

/** PIP-49: stake 1,000 PAC on an operator's validator and stay the Stake Owner. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DelegationSection(
    state: StakeViewModel.State,
    onDelegateChange: (Boolean) -> Unit,
    onSharePercentChange: (String) -> Unit,
    onTermChange: (StakeViewModel.Term) -> Unit,
    onCustomBlockChange: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.delegate, onCheckedChange = onDelegateChange)
            Text(stringResource(R.string.stake_delegate_toggle), style = MaterialTheme.typography.bodyLarge)
        }
        if (!state.delegate) return@Column

        Text(
            stringResource(R.string.stake_delegate_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.stake_amount_label))
            Text("1,000 PAC", fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = state.sharePercentText,
            onValueChange = onSharePercentChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.stake_share_label)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
        )
        Text(stringResource(R.string.stake_term_label), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DelegationRules.TERM_DAYS.keys.forEach { months ->
                FilterChip(
                    selected = state.term == StakeViewModel.Term.Months(months),
                    onClick = { onTermChange(StakeViewModel.Term.Months(months)) },
                    label = { Text(stringResource(R.string.stake_term_months, months)) },
                )
            }
            FilterChip(
                selected = state.term is StakeViewModel.Term.Date,
                onClick = { showDatePicker = true },
                label = { Text(stringResource(R.string.stake_term_date)) },
            )
            FilterChip(
                selected = state.term == StakeViewModel.Term.Block,
                onClick = { onTermChange(StakeViewModel.Term.Block) },
                label = { Text(stringResource(R.string.stake_term_block)) },
            )
        }
        if (state.term == StakeViewModel.Term.Block) {
            OutlinedTextField(
                value = state.customBlockText,
                onValueChange = onCustomBlockChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.stake_term_block_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
            )
        }
        val expiry = state.expiry
        Text(
            when {
                state.height <= 0 -> stringResource(R.string.stake_term_needs_validator)
                expiry != null -> stringResource(R.string.stake_term_ends, "%,d".format(expiry), approxDate(expiry, state.height))
                else -> stringResource(R.string.stake_error_pick_term)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.delegationError?.let {
            Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDatePicker) {
        val picker = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    onTermChange(StakeViewModel.Term.Date(picker.selectedDateMillis))
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(android.R.string.cancel)) }
            },
        ) { DatePicker(state = picker) }
    }
}

/** Delegation rows of the confirm sheet, as in the extension's approval window. */
@Composable
internal fun DelegationConfirmRows(state: StakeViewModel.State) {
    val percent = state.sharePercent ?: return
    val expiry = state.expiry ?: return
    val fmt = { p: Double -> "${"%.2f".format(p).trimEnd('0').trimEnd('.', ',')}%" }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text(
            stringResource(R.string.bond_owner_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LabelValue(stringResource(R.string.bond_share), fmt(percent))
        Text(
            stringResource(R.string.bond_split, fmt(DelegationRules.MAX_SHARE_PERCENT - percent)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        LabelValue(
            stringResource(R.string.bond_expiry),
            stringResource(R.string.bond_expiry_value, "%,d".format(expiry), approxDate(expiry, state.height)),
        )
        Text(
            stringResource(R.string.bond_lock_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Composable
internal fun LabelValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
