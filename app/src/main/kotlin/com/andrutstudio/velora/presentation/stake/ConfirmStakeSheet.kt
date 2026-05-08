package com.andrutstudio.velora.presentation.stake

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import com.andrutstudio.velora.presentation.components.formatPac

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmStakeSheet(
    state: StakeViewModel.State,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onPasswordChange: (String) -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = stringResource(R.string.stake_confirm_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConfirmRow(label = stringResource(R.string.stake_sender_label), value = state.selectedAccount?.address ?: "")
                    ConfirmRow(label = stringResource(R.string.stake_validator_label), value = state.validatorAddress)
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )

                    val amount = state.parsedAmount ?: Amount.ZERO
                    val fee = state.parsedFee ?: state.estimatedFee ?: Amount.ZERO
                    val total = amount + fee

                    ConfirmRow(label = stringResource(R.string.stake_amount_label), value = formatPac(amount), isHighlight = true)
                    ConfirmRow(label = stringResource(R.string.send_fee_label), value = formatPac(fee))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.stake_confirm_total),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = formatPac(total),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (state.memo.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
                        .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            stringResource(R.string.browser_sign_memo),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            state.memo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.backup_password_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let { { Text(it) } },
                enabled = !state.isStaking,
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isStaking && (state.password.isNotEmpty() || state.isBiometricEnabled),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                if (state.isStaking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = if (state.isBiometricEnabled && state.password.isEmpty()) 
                            Icons.Rounded.Fingerprint else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isBiometricEnabled && state.password.isEmpty()) 
                            stringResource(R.string.stake_confirm_biometric) else stringResource(R.string.stake_confirm_button)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isStaking,
            ) {
                Text(stringResource(R.string.action_reject))
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String, isHighlight: Boolean = false) {
    val displayValue = if (label == "Sender" || label == "Validator") value.truncateAddress() else value
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = displayValue,
            style = if (isHighlight) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            fontFamily = if (label == "Sender" || label == "Validator") FontFamily.Monospace else FontFamily.Default,
            color = if (isHighlight) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = 18.sp
        )
    }
}

private fun String.truncateAddress(): String =
    if (length > 20) "${take(10)}…${takeLast(8)}" else this

@Preview(showBackground = true)
@Composable
private fun ConfirmStakeSheetPreview() {
    VeloraTheme {
        ConfirmStakeSheet(
            state = StakeViewModel.State(
                selectedAccount = Account(
                    address = "pc1rmv39cmjlexample27l5jg5wv67am5hy2velora",
                    label = "Account 1",
                    type = AccountType.ED25519,
                    derivationIndex = 0
                ),
                validatorAddress = "pc1p...",
                amountText = "100.0",
                isConfirmVisible = true
            ),
            onDismiss = {},
            onConfirm = {}
        )
    }
}
