package com.andrutstudio.velora.presentation.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.presentation.components.PasswordTextField
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    val biometricHelper = remember {
        EntryPointAccessors.fromApplication(context.applicationContext, BiometricEntryPoint::class.java).biometricHelper()
    }

    // Mnemonic is never stored in VM state — kept only in local compose state
    var revealedMnemonic by remember { mutableStateOf<String?>(null) }

    // Disable screenshots for the entire duration of this screen to prevent flickering
    DisposableEffect(Unit) {
        if (activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            viewModel.onClearBackupForm()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsViewModel.Effect.MnemonicRevealed -> {
                    revealedMnemonic = effect.mnemonic
                }
                is SettingsViewModel.Effect.ShowSnackbar ->
                    snackbarHostState.showSnackbar(effect.message)
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

    BackupSettingsContent(
        state = state,
        revealedMnemonic = revealedMnemonic,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onBackupPasswordChange = viewModel::onBackupPasswordChange,
        onRevealMnemonic = viewModel::onRevealMnemonic,
        onHideMnemonic = { revealedMnemonic = null }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackupSettingsContent(
    state: SettingsViewModel.State,
    revealedMnemonic: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onBackupPasswordChange: (String) -> Unit,
    onRevealMnemonic: () -> Unit,
    onHideMnemonic: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup Phrase") },
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Warning card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Keep your phrase secret",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        Text(
                            "Anyone with this phrase can access your wallet and funds. " +
                                    "Never share it and never store it digitally.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            if (revealedMnemonic == null) {
                // Password auth form
                Text(
                    text = if (state.isBiometricEnabled) "Authenticate with biometric or enter your password to reveal the phrase" 
                           else "Enter your password to reveal the phrase",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PasswordTextField(
                    value = state.backupPassword,
                    onValueChange = onBackupPasswordChange,
                    label = "Password",
                    isError = state.backupError != null,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.backupError != null) {
                    Text(
                        text = state.backupError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Button(
                    onClick = onRevealMnemonic,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBackupLoading,
                ) {
                    if (state.isBackupLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(
                            imageVector = if (state.isBiometricEnabled && state.backupPassword.isEmpty()) 
                                Icons.Rounded.Fingerprint else Icons.Rounded.Visibility, 
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.isBiometricEnabled && state.backupPassword.isEmpty()) 
                                "Reveal with Biometric" else "Reveal Phrase"
                        )
                    }
                }
            } else {
                // Mnemonic or Private Key display
                Text(
                    text = if (state.wallet?.isPrivateKeyImport == true) "Your private key" else "Your recovery phrase",
                    style = MaterialTheme.typography.titleSmall,
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = revealedMnemonic,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val label = if (state.wallet?.isPrivateKeyImport == true) "Private Key" else "Mnemonic"
                            clipboard.setPrimaryClip(ClipData.newPlainText(label, revealedMnemonic))
                            scope.launch { snackbarHostState.showSnackbar("Copied to clipboard") }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = onHideMnemonic,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hide")
                    }
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun BackupSettingsScreenPreview() {
    VeloraTheme {
        BackupSettingsContent(
            state = SettingsViewModel.State(backupPassword = "password"),
            revealedMnemonic = null,
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onBackupPasswordChange = {},
            onRevealMnemonic = {},
            onHideMnemonic = {}
        )
    }
}

@Preview(showSystemUi = true, name = "Backup – Revealed Mnemonic")
@Composable
private fun BackupSettingsRevealedPreview() {
    VeloraTheme {
        BackupSettingsContent(
            state = SettingsViewModel.State(),
            revealedMnemonic = "apple banana cherry date elderberry fig grape honeydew ice jelly kiwi lemon",
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onBackupPasswordChange = {},
            onRevealMnemonic = {},
            onHideMnemonic = {}
        )
    }
}

@Preview(showSystemUi = true, name = "Backup – Revealed Private Key")
@Composable
private fun BackupSettingsRevealedPrivateKeyPreview() {
    VeloraTheme {
        BackupSettingsContent(
            state = SettingsViewModel.State(
                wallet = com.andrutstudio.velora.domain.model.Wallet(
                    name = "Imported",
                    isPrivateKeyImport = true
                )
            ),
            revealedMnemonic = "SECRET1RFLCY8AAQG2TEP2CPC06P9KPAR4EJRFUKY98QZ4TK0WYHL65S4HFQDM8URD",
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onBackupPasswordChange = {},
            onRevealMnemonic = {},
            onHideMnemonic = {}
        )
    }
}
