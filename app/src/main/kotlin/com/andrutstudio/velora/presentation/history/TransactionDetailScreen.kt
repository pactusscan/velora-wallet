package com.andrutstudio.velora.presentation.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SouthWest
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.model.TransactionStatus
import com.andrutstudio.velora.domain.model.TransactionType
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.DangerRed
import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.theme.VeloraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    navController: NavController,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }

            state.transaction != null -> TxDetailContent(
                tx = state.transaction!!,
                isIncoming = state.isIncoming,
                dateDisplayMode = state.dateDisplayMode,
                onToggleDateMode = viewModel::onToggleDateMode,
                onCopyId = { id ->
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("TX ID", id))
                    scope.launch { snackbarHostState.showSnackbar("Transaction ID copied") }
                },
                onViewExplorer = { id ->
                    val url = "https://pactusscan.com/transaction/$id"
                    navController.navigate(Screen.Browser.withUrl(url))
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun TxDetailContent(
    tx: Transaction,
    isIncoming: Boolean,
    dateDisplayMode: TransactionDetailViewModel.DateDisplayMode,
    onToggleDateMode: () -> Unit,
    onCopyId: (String) -> Unit,
    onViewExplorer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (tx.direction) {
        1 -> BrandTeal
        2 -> DangerRed
        else -> MaterialTheme.colorScheme.primary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Amount hero card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = accentColor.copy(alpha = 0.1f),
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = when (tx.type) {
                        TransactionType.TRANSFER ->
                            if (isIncoming) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward
                        TransactionType.BOND -> Icons.Rounded.VerifiedUser
                        TransactionType.UNBOND -> Icons.Rounded.Unarchive
                        TransactionType.SORTITION -> Icons.Rounded.Shuffle
                        TransactionType.WITHDRAW -> Icons.Rounded.Payments
                        TransactionType.BATCH_TRANSFER -> Icons.Rounded.Layers
                        TransactionType.SELF -> Icons.Rounded.QuestionMark
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.height(8.dp))
                val sign = when (tx.direction) {
                    1 -> "+"
                    2 -> "-"
                    else -> ""
                }
                Text(
                    text = "$sign${formatPac(tx.amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
                Text(
                    text = when (tx.direction) {
                        1 -> "Received"
                        2 -> "Sent"
                        else -> "Self"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Info card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailRow(label = "Status") {
                    StatusBadge(tx.status)
                }
                DetailRow(label = "Type") {
                    Text(
                        text = tx.type.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow(label = "From") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tx.from,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { onCopyId(tx.from) },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy From Address",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                if (tx.to != null) {
                    DetailRow(label = "To") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = tx.to,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onCopyId(tx.to) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy To Address",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                DetailRow(label = "Fee") {
                    Text(
                        text = formatPac(tx.fee),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DetailRow(label = "Block") {
                    Text(
                        text = "#${tx.blockHeight}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DetailRow(label = "Date") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = formatDateTime(tx.blockTime, dateDisplayMode),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(
                            onClick = onToggleDateMode,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Toggle Date Mode",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                if (tx.memo != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    DetailRow(label = "Memo") {
                        Text(
                            text = tx.memo,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // TX ID with copy button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = "TX ID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = tx.id,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onCopyId(tx.id) },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            Icons.Rounded.ContentCopy,
                            contentDescription = "Copy TX ID",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Explorer button
        OutlinedButton(
            onClick = { onViewExplorer(tx.id) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("View on Explorer")
        }
    }
}

@Composable
private fun DetailRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun StatusBadge(status: TransactionStatus) {
    val (label, color) = when (status) {
        TransactionStatus.CONFIRMED -> "Confirmed" to BrandTeal
        TransactionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
        TransactionStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private fun formatDateTime(
    timestamp: Long,
    mode: TransactionDetailViewModel.DateDisplayMode
): String {
    val date = Date(timestamp * 1000)
    return when (mode) {
        TransactionDetailViewModel.DateDisplayMode.LOCAL -> {
            SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(date)
        }
        TransactionDetailViewModel.DateDisplayMode.UTC -> {
            SimpleDateFormat("MMM d, yyyy  HH:mm 'UTC'", Locale.getDefault()).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(date)
        }
        TransactionDetailViewModel.DateDisplayMode.UNIX -> timestamp.toString()
    }
}



@Preview(showSystemUi = true)
@Composable
private fun TransactionDetailPreview() {
    val sampleTx = Transaction(
        id = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
        amount = Amount.fromPac(125.5),
        fee = Amount.fromPac(0.001),
        from = "pc1r...from_address",
        to = "pc1r...to_address",
        type = TransactionType.TRANSFER,
        status = TransactionStatus.CONFIRMED,
        blockHeight = 1234567,
        blockTime = System.currentTimeMillis() / 1000,
        memo = "Payment for services",
        direction = 1
    )

    VeloraTheme {
        Surface {
            TxDetailContent(
                tx = sampleTx,
                isIncoming = true,
                dateDisplayMode = TransactionDetailViewModel.DateDisplayMode.LOCAL,
                onToggleDateMode = {},
                onCopyId = {},
                onViewExplorer = {}
            )
        }
    }
}
