package com.andrutstudio.velora.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.model.TransactionStatus
import com.andrutstudio.velora.domain.model.TransactionType
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.components.ShimmerBox
import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.DangerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    viewModel: TransactionHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val myAddresses = remember(state.wallet) {
        state.wallet?.accounts?.map { it.address }?.toSet() ?: emptySet()
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TransactionHistoryViewModel.Effect.NavigateToDetail ->
                    navController.navigate(Screen.TransactionDetail.withId(effect.txId))
            }
        }
    }

    TransactionHistoryContent(
        navController = navController,
        state = state,
        myAddresses = myAddresses,
        onBack = { navController.popBackStack() },
        onFilterSelect = viewModel::setFilter,
        onTransactionClick = viewModel::onTransactionClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterRow(
    selected: TransactionHistoryViewModel.Filter,
    onSelect: (TransactionHistoryViewModel.Filter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        TransactionHistoryViewModel.Filter.entries.forEach { filter ->
            FilterChip(
                selected = selected == filter,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        when (filter) {
                            TransactionHistoryViewModel.Filter.ALL -> "All"
                            TransactionHistoryViewModel.Filter.TRANSFER -> "Transfers"
                            TransactionHistoryViewModel.Filter.BOND -> "Bonds"
                        },
                    )
                },
            )
        }
    }
}

@Composable
private fun TransactionList(
    transactions: List<Transaction>,
    myAddresses: Set<String>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(transactions, key = { it.id }) { tx ->
            TransactionItem(
                tx = tx,
                isIncoming = myAddresses.contains(tx.to) && !myAddresses.contains(tx.from),
                onClick = { onItemClick(tx.id) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionItem(
    tx: Transaction,
    isIncoming: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Direction icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isIncoming) BrandTeal.copy(alpha = 0.15f)
                        else DangerRed.copy(alpha = 0.15f),
                    ),
                contentAlignment = Alignment.Center,
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
                    tint = if (isIncoming) BrandTeal else DangerRed,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Address + time
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isIncoming) tx.from.truncate() else tx.to?.truncate() ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tx.blockTime.toDateString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tx.status != TransactionStatus.CONFIRMED) {
                    Spacer(Modifier.height(2.dp))
                    StatusChip(tx.status)
                }
            }

            // Amount
            Text(
                text = "${if (isIncoming) "+" else "-"}${formatPac(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isIncoming) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusChip(status: TransactionStatus) {
    val (label, color) = when (status) {
        TransactionStatus.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
        TransactionStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        TransactionStatus.CONFIRMED -> "Confirmed" to BrandTeal
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.SwapHoriz,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Your transaction history will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun LoadingList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(5) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
            )
        }
    }
}

private fun String.truncate(): String =
    if (length > 18) "${take(8)}…${takeLast(6)}" else this

private fun Long.toDateString(): String =
    SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(this * 1000))



@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TransactionHistoryScreenPreview() {
    // ... (mock setup kept same)
    val mockTransactions = listOf(
        // ... (mock data kept same)
        Transaction(
            id = "tx1",
            type = TransactionType.TRANSFER,
            from = "pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29",
            to = "pc1rn3h74rlujrs3p4u8ad2yggkxj367td3crhugle",
            amount = Amount.fromNanoPac(10000000000), // 10 PAC
            fee = Amount.fromNanoPac(10000000),
            memo = "Test Transfer",
            blockHeight = 12345,
            blockTime = System.currentTimeMillis() / 1000 - 3600,
            status = TransactionStatus.CONFIRMED
        ),
        Transaction(
            id = "tx2",
            type = TransactionType.BOND,
            from = "pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29",
            to = "pc1rn3h74rlujrs3p4u8ad2yggkxj367td3crhugle",
            amount = Amount.fromNanoPac(50000000000), // 50 PAC
            fee = Amount.fromNanoPac(10000000),
            memo = null,
            blockHeight = 12340,
            blockTime = System.currentTimeMillis() / 1000 - 86400,
            status = TransactionStatus.CONFIRMED
        ),
        Transaction(
            id = "tx3",
            type = TransactionType.TRANSFER,
            from = "pc1rn3h74rlujrs3p4u8ad2yggkxj367td3crhugle",
            to = "pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29",
            amount = Amount.fromNanoPac(2500000000), // 2.5 PAC
            fee = Amount.fromNanoPac(10000000),
            memo = "Pending Gift",
            blockHeight = 0,
            blockTime = System.currentTimeMillis() / 1000,
            status = TransactionStatus.PENDING
        )
    )

    val mockWallet = com.andrutstudio.velora.domain.model.Wallet(
        name = "Preview Wallet",
        accounts = listOf(
            com.andrutstudio.velora.domain.model.Account(
                address = "pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29",
                label = "Account 1",
                type = com.andrutstudio.velora.domain.model.AccountType.ED25519,
                derivationIndex = 0
            )
        )
    )

    val mockState = TransactionHistoryViewModel.State(
        transactions = mockTransactions,
        filter = TransactionHistoryViewModel.Filter.ALL,
        isLoading = false,
        wallet = mockWallet
    )

    com.andrutstudio.velora.presentation.theme.VeloraTheme {
        TransactionHistoryContent(
            navController = rememberNavController(),
            state = mockState,
            myAddresses = setOf("pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29"),
            onBack = {},
            onFilterSelect = {},
            onTransactionClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionHistoryContent(
    navController: NavController,
    state: TransactionHistoryViewModel.State,
    myAddresses: Set<String>,
    onBack: () -> Unit,
    onFilterSelect: (TransactionHistoryViewModel.Filter) -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History") },
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
        bottomBar = {
            MainBottomNavigation(
                navController = navController,
                currentRoute = Screen.TransactionHistory.route
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            FilterRow(
                selected = state.filter,
                onSelect = onFilterSelect,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            when {
                state.isLoading -> LoadingList()
                state.filtered.isEmpty() -> EmptyState()
                else -> TransactionList(
                    transactions = state.filtered,
                    myAddresses = myAddresses,
                    onItemClick = onTransactionClick,
                )
            }
        }
    }
}
