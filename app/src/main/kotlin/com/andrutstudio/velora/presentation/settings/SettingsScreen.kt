
package com.andrutstudio.velora.presentation.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.fragment.app.FragmentActivity
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.components.UnlockWalletDialog
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
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onSecuritySettings: () -> Unit,
    onNetworkSettings: () -> Unit,
    onBackupSettings: () -> Unit,
    onAboutSettings: () -> Unit,
    onWalletReset: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember { context.isBiometricAvailable() }
    var showResetConfirm by remember { mutableStateOf(false) }

    val biometricHelper = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java).biometricHelper()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsViewModel.Effect.WalletReset -> onWalletReset()
                is SettingsViewModel.Effect.RequestBiometricEnrollment -> {
                    if (activity != null) {
                        biometricHelper.showBiometricPromptForEncryption(
                            activity = activity,
                            passwordToStore = effect.password,
                            onSuccess = { viewModel.onBiometricEnrollmentSuccess() },
                            onError = { /* Handle error */ }
                        )
                    }
                }
                is SettingsViewModel.Effect.RequestBiometricUnlock -> {
                    if (activity != null) {
                        biometricHelper.showBiometricPromptForDecryption(
                            activity = activity,
                            onSuccess = { viewModel.onBiometricUnlockSuccess(it) },
                            onError = { /* Handle error */ }
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Wallet?") },
            text = {
                Text(
                    "This will permanently delete your wallet from this device. " +
                    "Make sure you have backed up your seed phrase before proceeding.",
                )
            },
            confirmButton = {
                Button(
                    onClick = { showResetConfirm = false; viewModel.onResetWallet() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (state.isRenamingWallet) {
        RenameWalletDialog(
            currentName = state.renameName,
            onNameChange = viewModel::onRenameNameChange,
            onDismiss = viewModel::onDismissRenameWallet,
            onConfirm = viewModel::onConfirmRenameWallet,
        )
    }

    if (state.showUnlockForBiometric) {
        UnlockWalletDialog(
            isLoading = state.isUnlocking,
            error = state.unlockError,
            onDismiss = viewModel::onDismissBiometricUnlock,
            onUnlock = viewModel::onConfirmBiometricUnlock,
            isBiometricEnabled = false, // We're in the process of enabling it
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            MainBottomNavigation(
                navController = navController,
                currentRoute = Screen.Settings.route
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Wallet info card
            state.wallet?.let { wallet ->
                Card(
                    onClick = viewModel::onShowRenameWallet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "Tap to rename",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        NetworkBadge(network = wallet.network)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Security section
            SettingsSectionHeader("Security")
            SettingsItem(
                icon = Icons.Rounded.Lock,
                title = "Change Password",
                subtitle = "Update your wallet password",
                onClick = onSecuritySettings,
            )
            if (biometricAvailable) {
                SettingsToggleItem(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face to unlock",
                    checked = state.isBiometricEnabled,
                    onCheckedChange = viewModel::onToggleBiometric,
                )
            }

            Spacer(Modifier.height(4.dp))

            // Network section
            SettingsSectionHeader("Network")
            SettingsItem(
                icon = Icons.Rounded.Language,
                title = "Network",
                subtitle = state.wallet?.network?.displayName ?: "Mainnet",
                trailingContent = {
                    NetworkBadge(network = state.wallet?.network ?: Network.MAINNET)
                },
                onClick = onNetworkSettings,
            )

            Spacer(Modifier.height(4.dp))

            // Backup section
            SettingsSectionHeader("Backup")
            SettingsItem(
                icon = Icons.Rounded.Shield,
                title = "Backup Phrase",
                subtitle = "View your 12-word recovery phrase",
                onClick = onBackupSettings,
            )

            Spacer(Modifier.height(4.dp))

            // About section
            SettingsSectionHeader("About")
            SettingsItem(
                icon = Icons.Rounded.Info,
                title = "About Velora Wallet",
                subtitle = "Version, links, and legal",
                onClick = onAboutSettings,
            )

            Spacer(Modifier.height(4.dp))

            // Danger zone
            SettingsSectionHeader("Wallet")
            Card(
                onClick = { showResetConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Reset / Switch Wallet", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error)
                        Text(
                            "Delete wallet from device and start over",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailingContent?.invoke() ?: Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
internal fun NetworkBadge(network: Network) {
    val (containerColor, contentColor) = when (network) {
        Network.MAINNET -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        Network.TESTNET -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Text(
            text = network.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun Context.isBiometricAvailable(): Boolean =
    BiometricManager.from(this)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

@Composable
private fun RenameWalletDialog(
    currentName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Wallet") },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Wallet Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = currentName.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    VeloraTheme {
        val navController = androidx.navigation.compose.rememberNavController()
        // Simple UI-only preview setup
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            },
            bottomBar = {
                MainBottomNavigation(
                    navController = navController,
                    currentRoute = Screen.Settings.route
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.height(4.dp))

                // Wallet info card mock
                Card(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Preview Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Text(
                                text = "Tap to rename",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        NetworkBadge(network = Network.MAINNET)
                    }
                }

                Spacer(Modifier.height(8.dp))

                SettingsSectionHeader("Security")
                SettingsItem(
                    icon = Icons.Rounded.Lock,
                    title = "Change Password",
                    subtitle = "Update your wallet password",
                    onClick = {},
                )
                SettingsToggleItem(
                    icon = Icons.Rounded.Fingerprint,
                    title = "Biometric Unlock",
                    subtitle = "Use fingerprint or face to unlock",
                    checked = true,
                    onCheckedChange = {},
                )

                Spacer(Modifier.height(4.dp))

                SettingsSectionHeader("Network")
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = "Network",
                    subtitle = "Mainnet",
                    trailingContent = {
                        NetworkBadge(network = Network.MAINNET)
                    },
                    onClick = {},
                )

                Spacer(Modifier.height(4.dp))

                SettingsSectionHeader("Backup")
                SettingsItem(
                    icon = Icons.Rounded.Shield,
                    title = "Backup Phrase",
                    subtitle = "View your 12-word recovery phrase",
                    onClick = {},
                )

                Spacer(Modifier.height(4.dp))

                SettingsSectionHeader("About")
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = "About Velora Wallet",
                    subtitle = "Version, links, and legal",
                    onClick = {},
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
