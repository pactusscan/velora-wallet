package com.andrutstudio.velora.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andrutstudio.velora.R
import com.andrutstudio.velora.data.rpc.NodeHealth
import com.andrutstudio.velora.data.rpc.ProbeError
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.NodeRules
import com.andrutstudio.velora.domain.model.p2pName
import com.andrutstudio.velora.presentation.components.asString
import com.andrutstudio.velora.presentation.theme.BrandPurple
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import java.net.URI
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NodeListSection(viewModel: NodeListViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    NodeListContent(
        state = state,
        onTestAll = viewModel::refresh,
        onAuto = viewModel::onAutoMode,
        onPinnedMode = viewModel::onPinnedMode,
        onPin = viewModel::onPin,
        onDelete = viewModel::onDelete,
        onAdd = viewModel::onOpenAdd,
    )
    state.addForm?.let { form ->
        AddNodeSheet(
            network = state.network,
            form = form,
            onUrlChange = viewModel::onAddUrlChange,
            onNameChange = viewModel::onAddNameChange,
            onTest = viewModel::onTestConnection,
            onSave = viewModel::onSaveNode,
            onDismiss = viewModel::onCloseAdd,
        )
    }
}

@Composable
private fun NodeListContent(
    state: NodeListViewModel.State,
    onTestAll: () -> Unit,
    onAuto: () -> Unit,
    onPinnedMode: () -> Unit,
    onPin: (String) -> Unit,
    onDelete: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val pinned = state.pinnedUrl
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.node_list_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.node_list_count, networkLabel(state.network), state.nodes.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onTestAll,
                enabled = !state.isChecking,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(if (state.isChecking) R.string.node_checking else R.string.node_test_all), fontSize = 13.sp)
            }
        }

        ModeSwitch(pinned = pinned != null, enabled = state.nodes.isNotEmpty(), onAuto = onAuto, onPinned = onPinnedMode)
        Text(
            stringResource(if (pinned != null) R.string.node_pinned_hint else R.string.node_auto_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.nodes.forEach { node ->
                NodeRowItem(
                    node = node,
                    topHeight = state.topHeight,
                    inUse = node.url == state.inUse,
                    pinnedMode = pinned != null,
                    isPinned = node.url == pinned,
                    onPin = { onPin(node.url) },
                    onDelete = { onDelete(node.url) },
                )
            }
        }

        OutlinedButton(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.node_add))
        }
        Text(
            stringResource(R.string.node_trust),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ModeSwitch(pinned: Boolean, enabled: Boolean, onAuto: () -> Unit, onPinned: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            Triple(R.string.node_mode_auto, !pinned, onAuto),
            Triple(R.string.node_mode_pinned, pinned, onPinned),
        ).forEach { (label, active, onClick) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, if (active) BrandPurple else Color.Transparent, RoundedCornerShape(8.dp))
                    .background(if (active) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(8.dp))
                    .selectable(selected = active, enabled = enabled, role = Role.RadioButton, onClick = { if (!active) onClick() })
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NodeRowItem(
    node: NodeListViewModel.NodeRow,
    topHeight: Long,
    inUse: Boolean,
    pinnedMode: Boolean,
    isPinned: Boolean,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    val health = node.health
    val behind = if (health?.ok == true) (topHeight - (health.height ?: 0L)).takeIf { it > NodeRules.BEHIND_WARN } ?: 0L else 0L
    val color = when {
        health == null -> MaterialTheme.colorScheme.onSurfaceVariant
        !health.ok -> MaterialTheme.colorScheme.error
        behind > 0 -> warningColor()
        else -> BrandTeal
    }
    val status = when {
        health == null -> stringResource(R.string.node_checking)
        !health.ok -> stringResource(probeErrorText(health.error))
        behind > 0 -> stringResource(R.string.node_behind, formatNumber(behind))
        else -> stringResource(R.string.node_block, formatNumber(health.height ?: 0L))
    }
    val rowModifier = if (pinnedMode) {
        Modifier.selectable(selected = isPinned, role = Role.RadioButton, onClick = onPin)
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (inUse) BrandTeal else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
            .then(rowModifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (pinnedMode) {
            RadioButton(
                selected = isPinned,
                onClick = null,
                colors = RadioButtonDefaults.colors(selectedColor = BrandTeal),
                modifier = Modifier.size(20.dp),
            )
        } else {
            Box(Modifier.size(8.dp).background(color, CircleShape))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    node.name.ifEmpty { host(node.url) },
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (node.custom) {
                    Text(
                        stringResource(R.string.node_custom),
                        color = BrandPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .border(1.dp, BrandPurple, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp),
                    )
                }
            }
            Text(
                if (node.custom) status else stringResource(R.string.node_default_status, status),
                style = MaterialTheme.typography.bodySmall,
                color = if (health?.ok == true && behind == 0L) MaterialTheme.colorScheme.onSurfaceVariant else color,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            if (inUse) {
                Text(stringResource(R.string.node_in_use), color = BrandTeal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
            if (health?.ok == true) {
                Text(
                    stringResource(R.string.node_latency_ms, health.latencyMs ?: 0L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (node.custom) {
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.node_delete, host(node.url)),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNodeSheet(
    network: Network,
    form: NodeListViewModel.AddForm,
    onUrlChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Fully open, so the test and save buttons are never hidden below the fold
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AddNodeForm(network, form, onUrlChange, onNameChange, onTest, onSave, onDismiss)
    }
}

@Composable
private fun AddNodeForm(
    network: Network,
    form: NodeListViewModel.AddForm,
    onUrlChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val wrongNetwork = form.wrongNetwork(network.p2pName)
    val canSave = !form.isTesting && form.url.isNotBlank() && form.result?.ok == true && !wrongNetwork
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.node_add), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.node_add_hint, networkLabel(network)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = form.url,
            onValueChange = onUrlChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.node_url)) },
            placeholder = { Text("https://…/jsonrpc", fontFamily = FontFamily.Monospace) },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            supportingText = { Text(form.error?.asString() ?: stringResource(R.string.node_url_hint)) },
            isError = form.error != null,
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.node_name)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        OutlinedButton(
            onClick = onTest,
            enabled = !form.isTesting && form.url.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (form.isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(stringResource(if (form.isTesting) R.string.node_checking else R.string.node_test_connection))
        }

        form.result?.let { TestResultCard(network, it, wrongNetwork) }

        Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.node_save))
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
            Text(stringResource(R.string.action_cancel))
        }
    }
}

@Composable
private fun TestResultCard(network: Network, result: NodeHealth, wrongNetwork: Boolean) {
    val good = result.ok && !wrongNetwork
    val accent = if (good) BrandTeal else MaterialTheme.colorScheme.error
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, accent.copy(alpha = if (good) 0.6f else 1f)), RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(if (good) Icons.Rounded.CheckCircle else Icons.Rounded.Warning, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
            Text(
                when {
                    !result.ok -> stringResource(probeErrorText(result.error))
                    wrongNetwork -> stringResource(R.string.node_wrong_network)
                    else -> stringResource(R.string.node_answered)
                },
                color = accent,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (wrongNetwork) {
            Text(
                stringResource(R.string.node_wrong_network_hint, result.networkName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (good) {
            ResultRow(
                stringResource(R.string.node_network),
                if (result.networkName.isNotEmpty()) networkLabel(network) else stringResource(R.string.node_network_unknown),
            )
            ResultRow(stringResource(R.string.node_last_block), formatNumber(result.height ?: 0L))
            ResultRow(stringResource(R.string.node_latency), stringResource(R.string.node_latency_ms, result.latencyMs ?: 0L))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

private fun probeErrorText(error: ProbeError?): Int = when (error) {
    ProbeError.LOGIN -> R.string.node_login
    ProbeError.REFUSED -> R.string.node_refused
    else -> R.string.node_unreachable
}

@Composable
private fun networkLabel(network: Network): String =
    stringResource(if (network == Network.MAINNET) R.string.network_mainnet else R.string.network_testnet)

// Same amber as the extension's --warning token, darker on light backgrounds
@Composable
private fun warningColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0xFFE0A23A) else Color(0xFFA16207)

private fun host(url: String): String = runCatching { URI(url).host }.getOrNull() ?: url

private fun formatNumber(n: Long): String = NumberFormat.getIntegerInstance(Locale.US).format(n)

// ── Previews ─────────────────────────────────────────────────────────────────

private val previewState = NodeListViewModel.State(
    network = Network.MAINNET,
    nodes = listOf(
        NodeListViewModel.NodeRow("https://bootstrap1.pactus.org/jsonrpc", "", false, NodeHealth(true, 92, 5_123_456, "pactus")),
        NodeListViewModel.NodeRow("https://bootstrap2.pactus.org/jsonrpc", "", false, NodeHealth(true, 140, 5_123_430, "pactus")),
        NodeListViewModel.NodeRow("https://bootstrap3.pactus.org/jsonrpc", "", false, NodeHealth(false, error = ProbeError.LOGIN)),
        NodeListViewModel.NodeRow("https://node.example.com/jsonrpc", "Home node", true, NodeHealth(true, 210, 5_123_455, "pactus")),
    ),
    inUse = "https://bootstrap1.pactus.org/jsonrpc",
)

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1E)
@Composable
private fun NodeListAutoPreview() {
    VeloraTheme {
        Box(Modifier.padding(16.dp)) {
            NodeListContent(previewState, {}, {}, {}, {}, {}, {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1E)
@Composable
private fun NodeListPinnedPreview() {
    VeloraTheme {
        Box(Modifier.padding(16.dp)) {
            NodeListContent(
                previewState.copy(pinnedUrl = "https://node.example.com/jsonrpc", inUse = "https://node.example.com/jsonrpc"),
                {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1E)
@Composable
private fun AddNodeFormPreview() {
    VeloraTheme {
        AddNodeForm(
            network = Network.MAINNET,
            form = NodeListViewModel.AddForm(
                url = "https://node.example.com/jsonrpc",
                name = "Home node",
                result = NodeHealth(true, 210, 5_123_455, "pactus"),
            ),
            onUrlChange = {}, onNameChange = {}, onTest = {}, onSave = {}, onCancel = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0E1E)
@Composable
private fun AddNodeWrongNetworkPreview() {
    VeloraTheme {
        AddNodeForm(
            network = Network.MAINNET,
            form = NodeListViewModel.AddForm(
                url = "https://testnet.example.com/jsonrpc",
                result = NodeHealth(true, 180, 4_000_000, "pactus-testnet"),
            ),
            onUrlChange = {}, onNameChange = {}, onTest = {}, onSave = {}, onCancel = {},
        )
    }
}
