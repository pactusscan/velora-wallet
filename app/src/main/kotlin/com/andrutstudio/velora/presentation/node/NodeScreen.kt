package com.andrutstudio.velora.presentation.node

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.rpc.AgentParsed
import com.andrutstudio.velora.data.rpc.PeerDetailResponse
import com.andrutstudio.velora.data.rpc.PeerInfo
import com.andrutstudio.velora.data.rpc.ValidatorPeerInfo
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs

private val OnlineGreen = Color(0xFF00E676)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeScreen(navController: NavController) {
    val viewModel: NodeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    var validatorsSheet by remember { mutableStateOf<List<ValidatorPeerInfo>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.node_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(NodeViewModel.Event.OpenAdd) }) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.node_add_content_description),
                            tint = BrandTeal,
                        )
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
                .padding(top = padding.calculateTopPadding()),
        ) {
            if (state.nodes.isEmpty()) {
                EmptyNodeState(
                    modifier = Modifier.fillMaxSize(),
                    onAdd = { viewModel.onEvent(NodeViewModel.Event.OpenAdd) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.nodes) { address ->
                        NodeMonitorCard(
                            address = address,
                            loadState = state.peerData[address],
                            onRemove = { viewModel.onEvent(NodeViewModel.Event.Remove(address)) },
                            onRefresh = { viewModel.onEvent(NodeViewModel.Event.Refresh(address)) },
                            onShowValidators = { validatorsSheet = it },
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) {
                MainBottomNavigation(navController = navController, currentRoute = Screen.Node.route)
            }
        }
    }

    validatorsSheet?.let { list ->
        ValidatorsBottomSheet(
            validators = list,
            sheetState = sheetState,
            onDismiss = { validatorsSheet = null },
        )
    }

    if (state.showAddDialog) {
        AddNodeDialog(
            input = state.addInput,
            error = state.addError,
            loading = state.addLoading,
            onInputChange = { viewModel.onEvent(NodeViewModel.Event.InputChanged(it)) },
            onConfirm = { viewModel.onEvent(NodeViewModel.Event.ConfirmAdd) },
            onDismiss = { viewModel.onEvent(NodeViewModel.Event.CloseAdd) },
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyNodeState(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Hub,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = BrandTeal.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.node_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.node_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.node_add_button))
        }
    }
}

// ── Node monitoring card ──────────────────────────────────────────────────────

@Composable
private fun NodeMonitorCard(
    address: String,
    loadState: NodeViewModel.PeerLoadState?,
    onRemove: () -> Unit,
    onRefresh: () -> Unit,
    onShowValidators: (List<ValidatorPeerInfo>) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = BrandTeal.copy(alpha = 0.2f),
                spotColor = BrandTeal.copy(alpha = 0.3f),
            ),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrandTeal.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.node_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.node_refresh),
                        tint = BrandTeal.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.node_remove),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (loadState) {
                null, NodeViewModel.PeerLoadState.Loading -> NodeLoadingContent()
                is NodeViewModel.PeerLoadState.Error -> NodeErrorContent(loadState.message, onRefresh)
                is NodeViewModel.PeerLoadState.Loaded -> NodeLoadedContent(loadState.response, onShowValidators)
            }
        }
    }
}

@Composable
private fun NodeLoadingContent() {
    Box(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandTeal, modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun NodeErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.node_load_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.node_retry), color = BrandTeal)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NodeLoadedContent(
    data: PeerDetailResponse,
    onShowValidators: (List<ValidatorPeerInfo>) -> Unit,
) {
    val peer = data.peer
    val nodeName = peer.moniker.ifBlank { peer.peerId.take(12) + "…" }
    val isOnline = data.peerOnline

    Row(verticalAlignment = Alignment.CenterVertically) {
        PulsingDot(online = isOnline)
        Spacer(Modifier.width(8.dp))
        Text(
            nodeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Rounded.Wifi,
            contentDescription = null,
            tint = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
    Text(
        if (isOnline) stringResource(R.string.node_online) else stringResource(R.string.node_offline),
        style = MaterialTheme.typography.labelSmall,
        color = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 22.dp),
    )

    Spacer(Modifier.height(12.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NodeChip(peer.agentParsed.os.uppercase(Locale.ROOT))
        NodeChip(peer.agentParsed.arch.uppercase(Locale.ROOT))
        NodeChip(peer.agentParsed.nodeType.uppercase(Locale.ROOT))
    }

    Spacer(Modifier.height(6.dp))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        NodeChip(stringResource(R.string.node_protocol, peer.agentParsed.protocolVersion))
        NodeChip(
            stringResource(
                R.string.node_peer_id,
                peer.peerId.take(6),
                peer.peerId.takeLast(6),
            )
        )
    }

    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = BrandTeal.copy(alpha = 0.12f))
    Spacer(Modifier.height(14.dp))

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.node_current_height),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "#${formatNumber(peer.height)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandTeal,
                fontFamily = FontFamily.Monospace,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                stringResource(R.string.node_last_active, relativeTime(peer.lastReceived)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DirectionBadge(direction = peer.direction)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "v${peer.agentParsed.nodeVersion}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = BrandTeal.copy(alpha = 0.12f))
    Spacer(Modifier.height(14.dp))

    NetworkOverviewSection(
        validators = data.validators,
        onShowValidators = { onShowValidators(data.validators) },
    )
}

// ── Sub-components ────────────────────────────────────────────────────────────

@Composable
private fun PulsingDot(online: Boolean) {
    if (online) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "dot_alpha",
        )
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen.copy(alpha = alpha)))
    } else {
        Box(
            modifier = Modifier.size(10.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        )
    }
}

@Composable
private fun NodeChip(label: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(0.5.dp, BrandTeal.copy(alpha = 0.2f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun DirectionBadge(direction: Int) {
    val unknownColor = MaterialTheme.colorScheme.onSurfaceVariant
    val (arrow, label, color) = when (direction) {
        1 -> Triple("↑", stringResource(R.string.node_direction_inbound), BrandTeal)
        2 -> Triple("↓", stringResource(R.string.node_direction_outbound), Color(0xFF82B1FF))
        else -> Triple("⟷", stringResource(R.string.node_direction_unknown), unknownColor)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(arrow, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun NetworkOverviewSection(
    validators: List<ValidatorPeerInfo>,
    onShowValidators: () -> Unit,
) {
    val activeValidators = validators.filter { it.stake > 0 }
    val totalStakePac = activeValidators.sumOf { it.stake } / 1_000_000_000L
    val avgScore = if (activeValidators.isEmpty()) 0.0
    else activeValidators.sumOf { it.availabilityScore } / activeValidators.size

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.node_overview_title),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onShowValidators,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(
                    Icons.Rounded.List,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = BrandTeal,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.node_view_validators),
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandTeal,
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.node_active_validators),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.node_validators_count, activeValidators.size, validators.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal,
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Hub, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.node_total_stake),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${formatNumber(totalStakePac)} PAC",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandTeal,
                )
                Text(
                    stringResource(
                        R.string.node_avg_score,
                        String.format(Locale.US, "%.2f", avgScore * 100),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(BrandTeal.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Hub, contentDescription = null, tint = BrandTeal, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ── Validators bottom sheet ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ValidatorsBottomSheet(
    validators: List<ValidatorPeerInfo>,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var copiedAddress by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.node_validators_sheet_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.node_validators_count_label, validators.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.node_validators_copy_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = BrandTeal.copy(alpha = 0.12f))

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                itemsIndexed(validators) { index, validator ->
                    val isCopied = copiedAddress == validator.address
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${index + 1}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                validator.address,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (validator.stake > 0) {
                                Text(
                                    stringResource(
                                        R.string.node_stake_label,
                                        formatNumber(validator.stake / 1_000_000_000L),
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrandTeal.copy(alpha = 0.8f),
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(validator.address))
                                copiedAddress = validator.address
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                if (isCopied) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                                contentDescription = if (isCopied)
                                    stringResource(R.string.node_address_copied)
                                else
                                    stringResource(R.string.node_copy_address),
                                tint = if (isCopied) OnlineGreen else BrandTeal.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    if (index < validators.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 28.dp),
                            color = BrandTeal.copy(alpha = 0.07f),
                        )
                    }
                }
            }
        }
    }
}

// ── Add node dialog ───────────────────────────────────────────────────────────

@Composable
private fun AddNodeDialog(
    input: String,
    error: String?,
    loading: Boolean,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        icon = { Icon(Icons.Rounded.Hub, contentDescription = null, tint = BrandTeal) },
        title = { Text(stringResource(R.string.node_add_dialog_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.node_add_dialog_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onInputChange,
                    label = { Text(stringResource(R.string.node_address_label)) },
                    placeholder = {
                        Text(
                            stringResource(R.string.node_address_placeholder),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandTeal,
                        focusedLabelColor = BrandTeal,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !loading && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                shape = RoundedCornerShape(10.dp),
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.node_add_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatNumber(value: Long): String =
    DecimalFormat("#,###").format(value).replace(',', ',')

private fun relativeTime(unixSeconds: Long): String {
    val nowSec = System.currentTimeMillis() / 1000
    val diffSec = abs(nowSec - unixSeconds)
    return when {
        diffSec < 60 -> "${diffSec}s ago"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86400 -> "${diffSec / 3600}h ago"
        else -> "${diffSec / 86400}d ago"
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewMockPeer = PeerDetailResponse(
    peer = PeerInfo(
        peerId = "12D3KooWJEu2J1oRGHVZyM3K7xKUYVFiBd5YrEgSdzyn3zGE5oQp",
        moniker = "Velora-Validator-A",
        agent = "",
        agentParsed = AgentParsed(
            nodeType = "daemon", nodeVersion = "1.13.0",
            os = "linux", arch = "amd64", protocolVersion = "3",
        ),
        status = 3,
        height = 7_139_352L,
        lastSent = System.currentTimeMillis() / 1000 - 120,
        lastReceived = System.currentTimeMillis() / 1000 - 60,
        address = "/ip4/217.154.145.137/tcp/21888",
        direction = 1,
        services = 2,
    ),
    peerOnline = true,
    validators = List(31) { ValidatorPeerInfo("pc1p…", 1_000_000_000_000L, 1.0, 7_139_000L) },
)

@Preview(showBackground = true, name = "Node — loaded (Dark)")
@Composable
private fun PreviewNodeCardDark() {
    VeloraTheme(darkTheme = true) {
        Column(modifier = Modifier.padding(16.dp)) {
            NodeMonitorCard(
                address = "pc1pmmt05md5u79kkg3hajdna5r86jfz2h2exv4746",
                loadState = NodeViewModel.PeerLoadState.Loaded(previewMockPeer),
                onRemove = {},
                onRefresh = {},
                onShowValidators = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Node — loaded (Light)")
@Composable
private fun PreviewNodeCardLight() {
    VeloraTheme(darkTheme = false) {
        Column(modifier = Modifier.padding(16.dp)) {
            NodeMonitorCard(
                address = "pc1pmmt05md5u79kkg3hajdna5r86jfz2h2exv4746",
                loadState = NodeViewModel.PeerLoadState.Loaded(previewMockPeer),
                onRemove = {},
                onRefresh = {},
                onShowValidators = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Node — empty state (Dark)")
@Composable
private fun PreviewNodeEmptyDark() {
    VeloraTheme(darkTheme = true) {
        EmptyNodeState(modifier = Modifier.fillMaxSize(), onAdd = {})
    }
}

@Preview(showBackground = true, name = "Node — empty state (Light)")
@Composable
private fun PreviewNodeEmptyLight() {
    VeloraTheme(darkTheme = false) {
        EmptyNodeState(modifier = Modifier.fillMaxSize(), onAdd = {})
    }
}
