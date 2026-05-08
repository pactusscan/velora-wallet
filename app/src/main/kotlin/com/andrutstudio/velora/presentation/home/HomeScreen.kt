package com.andrutstudio.velora.presentation.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import com.andrutstudio.velora.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.fragment.app.FragmentActivity
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.presentation.components.AccountCard
import com.andrutstudio.velora.presentation.components.BalanceCard
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.components.ShimmerBox
import com.andrutstudio.velora.presentation.components.ShimmerLine
import com.andrutstudio.velora.presentation.components.SendReceiveActionBar
import com.andrutstudio.velora.presentation.components.SwipeRevealCard
import com.andrutstudio.velora.presentation.components.UnlockWalletDialog
import com.andrutstudio.velora.presentation.components.WalletSelectorBar
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BiometricEntryPoint {
    fun biometricHelper(): BiometricHelper
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    
    val biometricHelper = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java).biometricHelper()
    }

    val renamingAddress = state.renamingAddress
    if (renamingAddress != null) {
        RenameAccountDialog(
            currentLabel = state.renameLabel,
            onLabelChange = viewModel::onRenameLabelChange,
            onDismiss = viewModel::onDismissRename,
            onConfirm = viewModel::onConfirmRename,
        )
    }

    if (state.showUnlockDialog) {
        UnlockWalletDialog(
            isLoading = state.isUnlocking,
            error = state.unlockError,
            isBiometricEnabled = state.isBiometricEnabled,
            onBiometricClick = { viewModel.onBiometricUnlockTriggered() },
            onDismiss = viewModel::onDismissUnlock,
            onUnlock = viewModel::onUnlock,
        )
    }

    if (state.showAddAccountSheet) {
        AddAccountSheet(
            isAdding = state.isAddingAccount,
            onDismiss = viewModel::onDismissAddAccountSheet,
            onAdd = viewModel::onAddAccount,
        )
    }

    if (state.showWalletPickerSheet) {
        WalletPickerSheet(
            wallets = state.wallets,
            activeWalletId = state.wallet?.id,
            onSelect = viewModel::onSelectWallet,
            onRequestDelete = viewModel::onRequestDeleteWallet,
            onAddWallet = viewModel::onAddWalletClick,
            onDismiss = viewModel::onDismissWalletPicker,
        )
    }

    if (state.showAddWalletSheet) {
        AddWalletSheet(
            isCreating = state.isCreatingWallet,
            error = state.createWalletError,
            onDismiss = viewModel::onDismissAddWallet,
            onCreate = viewModel::onCreateWallet,
        )
    }

    state.showAlertSheetForAddress?.let { address ->
        val alert = state.alerts[address]
        BalanceAlertSheet(
            address = address,
            currentThresholdNanoPac = alert?.thresholdNanoPac,
            isEnabled = alert?.isEnabled ?: true,
            currentType = alert?.type ?: com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN,
            onDismiss = viewModel::onDismissAlertSheet,
            onSave = viewModel::onSaveAlert,
            onDelete = viewModel::onDeleteAlert
        )
    }

    if (state.pendingDeleteAccountAddress != null) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteAccount,
            title = { Text(stringResource(R.string.home_delete_account_title)) },
            text = { Text(stringResource(R.string.home_delete_account_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmDeleteAccount,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteAccount) { Text(stringResource(R.string.action_reject)) }
            },
        )
    }

    if (state.pendingDeleteWalletId != null) {
        val name = state.wallets.find { it.id == state.pendingDeleteWalletId }?.name ?: ""
        AlertDialog(
            onDismissRequest = viewModel::onDismissDeleteWallet,
            title = { Text(stringResource(R.string.home_delete_wallet_title, name)) },
            text = { Text(stringResource(R.string.home_delete_wallet_message)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::onConfirmDeleteWallet,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissDeleteWallet) { Text(stringResource(R.string.action_reject)) }
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeViewModel.Effect.AddressCopied -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Pactus Address", effect.address))
                    snackbarHostState.showSnackbar(context.getString(R.string.home_address_copied))
                }
                is HomeViewModel.Effect.NavigateTo -> navController.navigate(effect.route)
                is HomeViewModel.Effect.ShowError -> snackbarHostState.showSnackbar(effect.message)
                is HomeViewModel.Effect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is HomeViewModel.Effect.RequestBiometricUnlock -> {
                    if (activity != null) {
                        biometricHelper.showBiometricPromptForDecryption(
                            activity = activity,
                            onSuccess = viewModel::onBiometricUnlockSuccess,
                            onError = { viewModel.navigateTo(Screen.Home.route) } // Or show error
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            MainBottomNavigation(
                navController = navController,
                currentRoute = Screen.Home.route
            )
        },
    ) { padding ->
        WalletTab(
            state = state,
            onRefresh = viewModel::refresh,
            onCopyAddress = viewModel::onAddressCopied,
            onRenameAccount = { address, label -> viewModel.onShowRename(address, label) },
            onDeleteAccount = viewModel::onDeleteAccount,
            onShowAddAccount = viewModel::onShowAddAccountSheet,
            onSelectWallet = viewModel::onWalletSelectorClick,
            onAddWallet = viewModel::onAddWalletClick,
            onReceive = {
                val address = state.wallet?.accounts?.firstOrNull()?.address ?: ""
                navController.navigate(Screen.Receive.withAddress(address))
            },
            onSend = { navController.navigate(Screen.Send.withArgs()) },
            onStake = { navController.navigate(Screen.Stake.route) },
            onShowAlert = viewModel::onShowAlertSheet,
            modifier = Modifier.padding(padding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletTab(
    state: HomeViewModel.State,
    onRefresh: () -> Unit,
    onCopyAddress: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRenameAccount: (address: String, currentLabel: String) -> Unit = { _, _ -> },
    onDeleteAccount: (String) -> Unit = {},
    onShowAddAccount: () -> Unit = {},
    onSelectWallet: () -> Unit = {},
    onAddWallet: () -> Unit = {},
    onReceive: () -> Unit = {},
    onSend: () -> Unit = {},
    onStake: () -> Unit = {},
    onShowAlert: (String) -> Unit = {},
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                WalletSelectorBar(
                    walletName = state.wallet?.name ?: "Main Wallet",
                    onSelectWallet = onSelectWallet,
                    onAddWallet = onAddWallet,
                )
            }

            item {
                if (state.isLoading) {
                    BalanceCardSkeleton()
                } else {
                    BalanceCard(
                        totalBalance = state.totalBalance,
                        network = state.wallet?.network ?: Network.MAINNET,
                        pacPriceUsd = state.pacPriceUsd,
                        priceHistory = state.priceHistory,
                        isLoading = false,
                    )
                }
            }

            if (state.wallet?.isWatchOnly != true) {
                item {
                    SendReceiveActionBar(
                        onReceive = onReceive,
                        onSend = onSend,
                        onStake = onStake,
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = state.error != null && !state.isOffline,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    ErrorBanner(message = state.error ?: "")
                }
            }

            if (state.wallet?.isWatchOnly != true) {
                item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_accounts_header),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    TextButton(onClick = onShowAddAccount) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.home_add_account), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            }

            if (state.isLoading) {
                items(2) { AccountCardSkeleton() }
            } else {
                items(
                    items = state.wallet?.accounts ?: emptyList(),
                    key = { it.address },
                ) { account ->
                    SwipeRevealCard(
                        onEdit = { onRenameAccount(account.address, account.label) },
                        onDelete = { onDeleteAccount(account.address) },
                        onCustom = { onShowAlert(account.address) },
                        customIcon = Icons.Rounded.NotificationsActive,
                        customLabel = "Alert",
                        customColor = MaterialTheme.colorScheme.tertiary
                    ) {
                        AccountCard(
                            account = account,
                            balance = state.balances[account.address] ?: Amount.ZERO,
                            onClick = { onCopyAddress(account.address) },
                            hasAlert = state.alerts.containsKey(account.address)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val wallet = Wallet(
        name = "Main Wallet",
        network = Network.MAINNET,
        accounts = listOf(
            Account(
                address = "pc1rmv39cmjl7hknrxn27l5jg5wv67am5hy2velora",
                label = "Account 1",
                type = AccountType.ED25519,
                derivationIndex = 0,
            ),
        ),
    )
    VeloraTheme {
        Scaffold(
            bottomBar = {
                MainBottomNavigation(
                    navController = rememberNavController(),
                    currentRoute = Screen.Home.route
                )
            }
        ) { padding ->
            WalletTab(
                state = HomeViewModel.State(
                    wallet = wallet,
                    balances = mapOf(wallet.accounts[0].address to Amount.fromPac(42.5)),
                    isLoading = false,
                ),
                onRefresh = {},
                onCopyAddress = {},
                onStake = {},
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Preview(showSystemUi = true, name = "Home – Loading")
@Composable
fun HomeScreenLoadingPreview() {
    VeloraTheme {
        WalletTab(
            state = HomeViewModel.State(isLoading = true),
            onRefresh = {},
            onCopyAddress = {},
            onStake = {},
        )
    }
}

@Composable
private fun RenameAccountDialog(
    currentLabel: String,
    onLabelChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_rename_account_title)) },
        text = {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = onLabelChange,
                label = { Text(stringResource(R.string.home_rename_account_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = currentLabel.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_reject)) }
        },
    )
}

@Composable
private fun BalanceCardSkeleton() {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    )
}

@Composable
private fun AccountCardSkeleton() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ShimmerBox(modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ShimmerLine(width = 100.dp, height = 14.dp)
                ShimmerLine(width = 160.dp, height = 12.dp)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}
