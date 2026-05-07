package com.andrutstudio.velora.presentation.send

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    navController: NavController,
    onBack: () -> Unit,
    viewModel: SendViewModel = hiltViewModel(),
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

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.onToAddressChange(it) }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan Pactus Address")
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            options.setOrientationLocked(false)
            scanLauncher.launch(options)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SendViewModel.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is SendViewModel.Effect.TxSuccess -> {
                    successTxId = effect.txId
                }
                SendViewModel.Effect.NavigateBack -> onBack()
                is SendViewModel.Effect.RequestBiometricUnlock -> {
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

    SendScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToAddressChange = viewModel::onToAddressChange,
        onAmountChange = viewModel::onAmountChange,
        onMaxAmount = viewModel::onMaxAmount,
        onFeeChange = viewModel::onFeeChange,
        onResetFee = viewModel::onResetFee,
        onMemoChange = viewModel::onMemoChange,
        onPreview = viewModel::onPreview,
        onScanClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onDismissConfirm = viewModel::onDismissConfirm,
        onConfirmSend = viewModel::onConfirmSend,
        onPasswordChange = viewModel::onPasswordChange,
        onSelectAccount = viewModel::onSelectAccount
    )

    if (successTxId != null) {
        TransactionSuccessDialog(
            txId = successTxId!!,
            onCopyId = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("TX ID", successTxId))
                scope.launch { snackbarHostState.showSnackbar("Transaction ID copied") }
            },
            onViewExplorer = {
                val url = "https://pactusscan.com/transaction/$successTxId"
                navController.navigate(com.andrutstudio.velora.presentation.navigation.Screen.Browser.withUrl(url))
            },
            onDismiss = {
                successTxId = null
                onBack()
            }
        )
    }
}

@Composable
private fun TransactionSuccessDialog(
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
                text = "Transfer Successful",
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
                    text = "Your PAC has been sent to the network and is being processed.",
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
                                "Transaction ID",
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
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onViewExplorer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("View on Explorer")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendScreenContent(
    state: SendViewModel.State,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToAddressChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onMaxAmount: () -> Unit,
    onFeeChange: (String) -> Unit,
    onResetFee: () -> Unit,
    onMemoChange: (String) -> Unit,
    onPreview: () -> Unit,
    onScanClick: () -> Unit,
    onDismissConfirm: () -> Unit,
    onConfirmSend: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onSelectAccount: (Account) -> Unit,
) {
    if (state.isConfirmVisible) {
        ConfirmSendSheet(
            state = state,
            onDismiss = onDismissConfirm,
            onConfirm = onConfirmSend,
            onPasswordChange = onPasswordChange,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Send PAC") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
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
                        "Send From",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    FromAccountSection(
                        accounts = state.wallet?.accounts ?: emptyList(),
                        selected = state.selectedAccount,
                        balance = state.availableBalance,
                        onSelect = onSelectAccount,
                    )
                }
            }

            // Transaction Details
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.toAddress,
                    onValueChange = onToAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Recipient Address") },
                    placeholder = { Text("pc1r...") },
                    trailingIcon = {
                        IconButton(onClick = onScanClick) {
                            Icon(Icons.Rounded.QrCodeScanner, contentDescription = "Scan QR")
                        }
                    },
                    isError = state.toAddressError != null,
                    supportingText = state.toAddressError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    suffix = { Text("PAC", fontWeight = FontWeight.Bold) },
                    trailingIcon = {
                        TextButton(onClick = onMaxAmount) {
                            Text("MAX")
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    isError = state.amountError != null,
                    supportingText = state.amountError?.let { { Text(it) } },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = state.feeText,
                    onValueChange = onFeeChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Network Fee") },
                    placeholder = { Text("0.01") },
                    suffix = { Text("PAC") },
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
                    label = { Text("Memo (Optional)") },
                    placeholder = { Text("What is this for?") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
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
                Text("Review Transaction", style = MaterialTheme.typography.titleMedium)
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun SendScreenPreview() {
    VeloraTheme {
        SendScreenContent(
            state = SendViewModel.State(
                wallet = null,
                selectedAccount = Account(
                    address = "pc1rmv39cmjlexample27l5jg5wv67am5hy2velora",
                    label = "Main Account",
                    type = com.andrutstudio.velora.domain.model.AccountType.ED25519,
                    derivationIndex = 0
                ),
                availableBalance = Amount.fromPac(100.0),
                toAddress = "",
                amountText = "",
                memo = "",
                isLoading = false
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onToAddressChange = {},
            onAmountChange = {},
            onMaxAmount = {},
            onFeeChange = {},
            onResetFee = {},
            onMemoChange = {},
            onPreview = {},
            onScanClick = {},
            onDismissConfirm = {},
            onConfirmSend = {},
            onPasswordChange = {},
            onSelectAccount = {}
        )
    }
}
