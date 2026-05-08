package com.andrutstudio.velora.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.EntryPointAccessors
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.components.UnlockWalletDialog
import com.andrutstudio.velora.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkSettingsScreen(
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

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsViewModel.Effect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is SettingsViewModel.Effect.RequestBiometricUnlock -> {
                    if (activity != null) {
                        biometricHelper.showBiometricPromptForDecryption(
                            activity = activity,
                            onSuccess = { viewModel.onBiometricUnlockSuccess(it) },
                            onError = { /* Handle error or show fallback */ }
                        )
                    }
                }
                else -> Unit
            }
        }
    }

    if (state.showUnlockForNetwork) {
        UnlockWalletDialog(
            isLoading = state.isUnlocking,
            error = state.unlockError,
            onDismiss = viewModel::onDismissUnlock,
            onUnlock = viewModel::onUnlock,
            isBiometricEnabled = state.isBiometricEnabled,
            onBiometricClick = { viewModel.onBiometricUnlockTriggered() }
        )
    }

    NetworkSettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onNetworkSelected = viewModel::onSwitchNetwork,
        onCustomRpcChanged = { network, url -> viewModel.onCustomRpcChange(network, url) },
        onSaveCustomRpc = { network -> viewModel.onSaveCustomRpc(network) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkSettingsContent(
    state: SettingsViewModel.State,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onNetworkSelected: (Network) -> Unit,
    onCustomRpcChanged: (Network, String) -> Unit,
    onSaveCustomRpc: (Network) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.network_select),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val currentNetwork = state.wallet?.network ?: Network.MAINNET

            NetworkCard(
                network = Network.MAINNET,
                isSelected = currentNetwork == Network.MAINNET,
                isSyncing = currentNetwork == Network.MAINNET && state.isNetworkLoading,
                onClick = { onNetworkSelected(Network.MAINNET) }
            )

            CustomRpcField(
                value = state.customRpcMainnet,
                placeholder = stringResource(R.string.network_mainnet_rpc),
                isLoading = state.isCustomRpcSaving,
                onValueChange = { onCustomRpcChanged(Network.MAINNET, it) },
                onSave = { onSaveCustomRpc(Network.MAINNET) }
            )

            Spacer(Modifier.height(8.dp))

            NetworkCard(
                network = Network.TESTNET,
                isSelected = currentNetwork == Network.TESTNET,
                isSyncing = currentNetwork == Network.TESTNET && state.isNetworkLoading,
                onClick = { onNetworkSelected(Network.TESTNET) }
            )

            CustomRpcField(
                value = state.customRpcTestnet,
                placeholder = stringResource(R.string.network_testnet_rpc),
                isLoading = state.isCustomRpcSaving,
                onValueChange = { onCustomRpcChanged(Network.TESTNET, it) },
                onSave = { onSaveCustomRpc(Network.TESTNET) }
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        stringResource(R.string.network_switch_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun NetworkSettingsScreenPreview() {
    VeloraTheme {
        NetworkSettingsContent(
            state = SettingsViewModel.State(
                customRpcMainnet = "https://rpc.pactus.org"
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onNavigateBack = {},
            onNetworkSelected = {},
            onCustomRpcChanged = { _, _ -> },
            onSaveCustomRpc = {}
        )
    }
}

@Composable
private fun CustomRpcField(
    value: String,
    placeholder: String,
    isLoading: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else if (value.isNotBlank()) {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save", tint = BrandTeal)
                    }
                }
            },
            singleLine = true
        )
    }
}

@Composable
private fun NetworkCard(
    network: Network,
    isSelected: Boolean,
    isSyncing: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsNetworkBadge(network)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (network == Network.MAINNET) stringResource(R.string.network_mainnet) else stringResource(R.string.network_testnet),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (network == Network.MAINNET) stringResource(R.string.network_mainnet_desc) else stringResource(R.string.network_testnet_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SettingsNetworkBadge(network: Network) {
    val (color, label) = when (network) {
        Network.MAINNET -> BrandTeal to "M"
        Network.TESTNET -> Color(0xFFFF9800) to "T"
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun NetworkCardPreview() {
    VeloraTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NetworkCard(Network.MAINNET, isSelected = true, isSyncing = false, onClick = {})
            NetworkCard(Network.TESTNET, isSelected = false, isSyncing = false, onClick = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CustomRpcFieldPreview() {
    VeloraTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CustomRpcField(
                value = "https://rpc.pactus.org",
                placeholder = "https://your-node.com",
                isLoading = false,
                onValueChange = {},
                onSave = {}
            )
        }
    }
}
