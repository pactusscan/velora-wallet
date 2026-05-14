package com.andrutstudio.velora.presentation.stake

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.presentation.components.FromAccountSection
import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import dagger.hilt.android.EntryPointAccessors
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.presentation.settings.BiometricEntryPoint
import com.andrutstudio.velora.presentation.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakeScreen(
    navController: NavController,
    onNavigateBack: () -> Unit,
    viewModel: StakeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var successTxId by remember { mutableStateOf<String?>(null) }

    val biometricHelper = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java).biometricHelper()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is StakeViewModel.Effect.NavigateBack -> onNavigateBack()
                is StakeViewModel.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is StakeViewModel.Effect.ShowSuccess -> {
                    successTxId = effect.txId
                }
                is StakeViewModel.Effect.RequestBiometricUnlock -> {
                    if (activity != null) {
                        biometricHelper.showBiometricPromptForDecryption(
                            activity = activity,
                            onSuccess = { viewModel.onBiometricUnlockSuccess(it) },
                            onError = { /* Handle error */ }
                        )
                    }
                }
            }
        }
    }

    StakeScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onValidatorAddressChange = viewModel::onValidatorAddressChange,
        onValidatorPublicKeyChange = viewModel::onValidatorPublicKeyChange,
        onAmountChange = viewModel::onAmountChange,
        onFeeChange = viewModel::onFeeChange,
        onResetFee = viewModel::onResetFee,
        onMemoChange = viewModel::onMemoChange,
        onPreview = viewModel::onPreview,
        onDismissConfirm = viewModel::onDismissConfirm,
        onConfirmStake = viewModel::onConfirmStake,
        onPasswordChange = viewModel::onPasswordChange,
        onSelectAccount = viewModel::onSelectAccount
    )

    if (successTxId != null) {
        StakeSuccessDialog(
            txId = successTxId!!,
            onCopyId = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("TX ID", successTxId))
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.stake_tx_id_copied)) }
            },
            onViewExplorer = {
                val url = "https://pactusscan.com/transaction/$successTxId"
                navController.navigate(Screen.Browser.withUrl(url))
            },
            onDismiss = {
                successTxId = null
                onNavigateBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StakeScreenContent(
    state: StakeViewModel.State,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onValidatorAddressChange: (String) -> Unit,
    onValidatorPublicKeyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onFeeChange: (String) -> Unit,
    onResetFee: () -> Unit,
    onMemoChange: (String) -> Unit,
    onPreview: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmStake: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectAccount: (Account) -> Unit,
) {
    if (state.isConfirmVisible) {
        ConfirmStakeSheet(
            state = state,
            onDismiss = onDismissConfirm,
            onConfirm = onConfirmStake,
            onPasswordChange = onPasswordChange,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stake_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Account Selector Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.stake_sender_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FromAccountSection(
                        accounts = state.accounts,
                        selected = state.selectedAccount,
                        balance = state.availableBalance,
                        onSelect = onSelectAccount,
                    )
                }
            }

            // Staking Details
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.validatorAddress,
                    onValueChange = onValidatorAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stake_validator_label)) },
                    placeholder = { Text(stringResource(R.string.stake_validator_placeholder)) },
                    isError = state.validatorAddressError != null,
                    supportingText = {
                        Column {
                            state.validatorAddressError?.let { Text(it) }
                            state.validatorStake?.let {
                                Text(
                                    text = stringResource(R.string.stake_current_label, formatPac(it)),
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.validatorPublicKey,
                    onValueChange = onValidatorPublicKeyChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stake_validator_pubkey_label)) },
                    placeholder = { Text(stringResource(R.string.stake_validator_pubkey_placeholder)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stake_amount_label)) },
                    placeholder = { Text(stringResource(R.string.common_amount_placeholder)) },
                    suffix = { Text(stringResource(R.string.send_unit_pac), fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.amountError != null,
                    supportingText = {
                        state.amountError?.let {
                            if (state.maxStakableAmount != null) {
                                Text(stringResource(R.string.stake_max_error, formatPac(state.maxStakableAmount)))
                            } else {
                                Text(it)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.feeText,
                    onValueChange = onFeeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stake_fee_label)) },
                    placeholder = { Text(stringResource(R.string.common_fee_placeholder)) },
                    suffix = { Text(stringResource(R.string.send_unit_pac)) },
                    trailingIcon = {
                        if (state.isFeeLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (state.isFeeManual) {
                            IconButton(onClick = onResetFee) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Auto")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.feeError != null,
                    supportingText = state.feeError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.memo,
                    onValueChange = onMemoChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.stake_memo_label)) },
                    placeholder = { Text(stringResource(R.string.stake_memo_placeholder)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state.canPreview,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.stake_preview_button), style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StakeSuccessDialog(
    txId: String,
    onCopyId: () -> Unit,
    onViewExplorer: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = com.andrutstudio.velora.presentation.theme.BrandTeal,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.stake_success_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stake_success_message),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.browser_sign_fee).split(":")[0].replace("Fee", "Transaction ID"), // Better to add tx_id string
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = txId.take(12) + "..." + txId.takeLast(12),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(onClick = onCopyId, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onViewExplorer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.about_explorer))
            }
        }
    )
}

@Preview(showSystemUi = true)
@Composable
fun StakeScreenPreview() {
    VeloraTheme {
        StakeScreenContent(
            state = StakeViewModel.State(
                accounts = listOf(
                    Account(
                        address = "pc1rmv39cmjlexample27l5jg5wv67am5hy2velora",
                        label = "Account 1",
                        type = com.andrutstudio.velora.domain.model.AccountType.ED25519,
                        derivationIndex = 0
                    )
                ),
                selectedAccount = Account(
                    address = "pc1rmv39cmjlexample27l5jg5wv67am5hy2velora",
                    label = "Account 1",
                    type = com.andrutstudio.velora.domain.model.AccountType.ED25519,
                    derivationIndex = 0
                ),
                availableBalance = Amount.fromPac(100.0),
                isLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onValidatorAddressChange = {},
            onValidatorPublicKeyChange = {},
            onAmountChange = {},
            onFeeChange = {},
            onResetFee = {},
            onMemoChange = {},
            onPreview = {},
            onDismissConfirm = {},
            onConfirmStake = {},
            onPasswordChange = {},
            onSelectAccount = {}
        )
    }
}
