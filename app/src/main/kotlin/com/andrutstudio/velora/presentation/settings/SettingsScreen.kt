
package com.andrutstudio.velora.presentation.settings

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.fragment.app.FragmentActivity
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.data.local.ThemePreference
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
            title = { Text(stringResource(R.string.home_reset_wallet_title)) },
            text = {
                Text(stringResource(R.string.home_reset_wallet_message))
            },
            confirmButton = {
                Button(
                    onClick = { showResetConfirm = false; viewModel.onResetWallet() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.home_reset_wallet_title).split("?")[0]) }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text(stringResource(R.string.action_reject)) }
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

    if (state.showLanguageSelector) {
        LanguageSelectorDialog(
            currentLanguage = state.currentLanguage,
            onLanguageChange = viewModel::onLanguageChange,
            onDismiss = viewModel::onDismissLanguageSelector,
        )
    }

    if (state.showThemeSelector) {
        ThemeSelectorDialog(
            currentTheme = state.themePreference,
            onThemeChange = viewModel::onThemeChange,
            onDismiss = viewModel::onDismissThemeSelector,
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
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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
                                    text = stringResource(R.string.create_wallet_name_subtitle),
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
                SettingsSectionHeader(stringResource(R.string.settings_section_security))
                SettingsItem(
                    icon = Icons.Rounded.Lock,
                    title = stringResource(R.string.settings_change_password),
                    subtitle = stringResource(R.string.settings_change_password_subtitle),
                    onClick = onSecuritySettings,
                )
                if (biometricAvailable) {
                    SettingsToggleItem(
                        icon = Icons.Rounded.Fingerprint,
                        title = stringResource(R.string.settings_biometric),
                        subtitle = stringResource(R.string.settings_biometric_subtitle),
                        checked = state.isBiometricEnabled,
                        onCheckedChange = viewModel::onToggleBiometric,
                    )
                }

                SettingsItem(
                    icon = Icons.Rounded.Translate,
                    title = stringResource(R.string.settings_language),
                    subtitle = when (state.currentLanguage) {
                        "in" -> stringResource(R.string.language_indonesian)
                        "ms" -> stringResource(R.string.language_malay)
                        "vi" -> stringResource(R.string.language_vietnamese)
                        "fr" -> stringResource(R.string.language_french)
                        "es" -> stringResource(R.string.language_spanish)
                        else -> stringResource(R.string.language_english)
                    },
                    onClick = viewModel::onShowLanguageSelector,
                )

                Spacer(Modifier.height(4.dp))

                // Appearance section
                SettingsSectionHeader(stringResource(R.string.settings_section_appearance))
                SettingsItem(
                    icon = Icons.Rounded.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (state.themePreference) {
                        ThemePreference.DARK -> stringResource(R.string.theme_dark)
                        ThemePreference.LIGHT -> stringResource(R.string.theme_light)
                        ThemePreference.SYSTEM -> stringResource(R.string.theme_system)
                    },
                    onClick = viewModel::onShowThemeSelector,
                )

                Spacer(Modifier.height(4.dp))

                // Network section
                SettingsSectionHeader(stringResource(R.string.settings_section_network))
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.network_title),
                    subtitle = state.wallet?.network?.displayName ?: "Mainnet",
                    trailingContent = {
                        NetworkBadge(network = state.wallet?.network ?: Network.MAINNET)
                    },
                    onClick = onNetworkSettings,
                )

                Spacer(Modifier.height(4.dp))

                // Backup section
                SettingsSectionHeader(stringResource(R.string.settings_section_backup))
                SettingsItem(
                    icon = Icons.Rounded.Shield,
                    title = stringResource(R.string.settings_backup_phrase),
                    subtitle = stringResource(R.string.settings_backup_phrase_subtitle),
                    onClick = onBackupSettings,
                )

                Spacer(Modifier.height(4.dp))

                // About section
                SettingsSectionHeader(stringResource(R.string.settings_section_about))
                SettingsItem(
                    icon = Icons.Rounded.Info,
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_subtitle),
                    onClick = onAboutSettings,
                )

                Spacer(Modifier.height(4.dp))

                // Danger zone
                SettingsSectionHeader(stringResource(R.string.nav_wallet))
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
                            Text(stringResource(R.string.home_reset_wallet_title).replace("?", ""), style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error)
                            Text(
                                stringResource(R.string.home_reset_wallet_message).take(40) + "...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(100.dp)) // Extra space for floating dock
            }

            // Floating Navigation Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                MainBottomNavigation(
                    navController = navController,
                    currentRoute = Screen.Settings.route
                )
            }
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
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                thumbContent = if (checked) {
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
    }
}

@Composable
private fun LanguageSelectorDialog(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val languages = listOf(
        "en" to stringResource(R.string.language_english),
        "es" to stringResource(R.string.language_spanish),
        "fr" to stringResource(R.string.language_french),
        "in" to stringResource(R.string.language_indonesian),
        "ms" to stringResource(R.string.language_malay),
        "vi" to stringResource(R.string.language_vietnamese),
    ).sortedBy { it.second }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_select)) },
        text = {
            Column {
                languages.forEach { (code, name) ->
                    LanguageOption(
                        title = name,
                        selected = currentLanguage == code,
                        onClick = { onLanguageChange(code) }
                    )
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ThemeSelectorDialog(
    currentTheme: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        ThemePreference.SYSTEM to stringResource(R.string.theme_system),
        ThemePreference.LIGHT  to stringResource(R.string.theme_light),
        ThemePreference.DARK   to stringResource(R.string.theme_dark),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_select)) },
        text = {
            Column {
                options.forEach { (preference, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeChange(preference) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(selected = currentTheme == preference, onClick = { onThemeChange(preference) })
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
    )
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
        title = { Text(stringResource(R.string.home_rename_wallet_title)) },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.create_wallet_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = currentName.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_reject)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    VeloraTheme {
        val navController = androidx.navigation.compose.rememberNavController()
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
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
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

                    SettingsSectionHeader("Appearance")
                    SettingsItem(
                        icon = Icons.Rounded.DarkMode,
                        title = "Theme",
                        subtitle = "System Default",
                        onClick = {},
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

                    Spacer(Modifier.height(100.dp))
                }

                // Floating Navigation Dock
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    MainBottomNavigation(
                        navController = navController,
                        currentRoute = Screen.Settings.route
                    )
                }
            }
        }
    }
}
