package com.andrutstudio.velora.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.model.TransactionStatus
import com.andrutstudio.velora.domain.model.TransactionType
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.components.MainBottomNavigation
import com.andrutstudio.velora.presentation.components.ShimmerBox
import com.andrutstudio.velora.presentation.components.formatPac
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.DangerRed
import kotlinx.coroutines.launch
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
        onAddressFilterSelect = viewModel::setAddressFilter,
        onTransactionClick = viewModel::onTransactionClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectorHeader(
    selectedAddress: String?,
    accounts: List<com.andrutstudio.velora.domain.model.Account>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedAccount = accounts.find { it.address == selectedAddress }
    val label = selectedAccount?.label?.ifBlank { selectedAccount.address.truncate() }
        ?: stringResource(R.string.history_filter_all_accounts)

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Icon(
                Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountSelectionSheet(
    accounts: List<com.andrutstudio.velora.domain.model.Account>,
    selectedAddress: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.history_select_account),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                fontWeight = FontWeight.Bold
            )
            
            LazyColumn {
                item {
                    AccountItem(
                        label = stringResource(R.string.history_filter_all_accounts),
                        isSelected = selectedAddress == null,
                        onClick = { onSelect(null); onDismiss() }
                    )
                }
                items(accounts) { account ->
                    AccountItem(
                        label = account.label.ifBlank { account.address.truncate() },
                        address = account.address,
                        isSelected = selectedAddress == account.address,
                        onClick = { onSelect(account.address); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountItem(
    label: String,
    address: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) BrandTeal.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSelected) Icons.Rounded.Check else Icons.Rounded.History,
                contentDescription = null,
                tint = if (isSelected) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) BrandTeal else MaterialTheme.colorScheme.onSurface
            )
            if (address != null) {
                Text(
                    text = address.truncate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BrandTeal.copy(alpha = 0.1f),
                    selectedLabelColor = BrandTeal,
                    selectedLeadingIconColor = BrandTeal
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == filter,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    selectedBorderColor = BrandTeal.copy(alpha = 0.5f),
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.dp
                )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
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
                text = stringResource(R.string.history_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.history_empty_subtitle),
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
            onAddressFilterSelect = {},
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
    onAddressFilterSelect: (String?) -> Unit,
    onTransactionClick: (String) -> Unit,
) {
    var showAccountSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                modifier = Modifier.fillMaxSize(),
            ) {
                state.wallet?.let { wallet ->
                    AccountSelectorHeader(
                        selectedAddress = state.selectedAddressFilter,
                        accounts = wallet.accounts,
                        onClick = { showAccountSheet = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                FilterRow(
                    selected = state.filter,
                    onSelect = onFilterSelect,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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

            // Floating Navigation Dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                MainBottomNavigation(
                    navController = navController,
                    currentRoute = Screen.TransactionHistory.route
                )
            }
        }
    }

    if (showAccountSheet) {
        state.wallet?.let { wallet ->
            AccountSelectionSheet(
                accounts = wallet.accounts,
                selectedAddress = state.selectedAddressFilter,
                onSelect = onAddressFilterSelect,
                onDismiss = { showAccountSheet = false }
            )
        }
    }
}
