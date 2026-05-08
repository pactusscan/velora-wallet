package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPickerSheet(
    wallets: List<Wallet>,
    activeWalletId: String?,
    onSelect: (walletId: String) -> Unit,
    onRequestDelete: (walletId: String) -> Unit,
    onAddWallet: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        WalletPickerSheetContent(
            wallets = wallets,
            activeWalletId = activeWalletId,
            onSelect = onSelect,
            onRequestDelete = onRequestDelete,
            onAddWallet = onAddWallet,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun WalletPickerSheetContent(
    wallets: List<Wallet>,
    activeWalletId: String?,
    onSelect: (walletId: String) -> Unit,
    onRequestDelete: (walletId: String) -> Unit,
    onAddWallet: () -> Unit,
    onDismiss: () -> Unit,
) {
    val gradient = Brush.linearGradient(
        colors = listOf(BrandTeal, BrandPurple),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_my_wallets),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.browser_close), tint = OnSurfaceVariant)
            }
        }

        HorizontalDivider(
            color = Outline.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(wallets, key = { it.id }) { wallet ->
                WalletPickerItem(
                    wallet = wallet,
                    isActive = wallet.id == activeWalletId,
                    canDelete = wallets.size > 1,
                    gradient = gradient,
                    onSelect = { onSelect(wallet.id) },
                    onDelete = { onRequestDelete(wallet.id) },
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(gradient)
                        .clickable(onClick = onAddWallet)
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.home_add_new_wallet),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun WalletPickerItem(
    wallet: Wallet,
    isActive: Boolean,
    canDelete: Boolean,
    gradient: Brush,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isActive) Modifier.background(gradient)
                else Modifier.background(Color.Transparent)
            )
            .padding(if (isActive) 1.5.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(if (isActive) 15.dp else 16.dp))
                .background(if (isActive) SurfaceVariant else SurfaceContainer)
                .clickable(onClick = onSelect)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = wallet.name.firstOrNull()?.uppercaseChar()?.toString() ?: "W",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = wallet.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = OnBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val accountCount = wallet.accounts.size
                    Text(
                        text = if (accountCount == 1) stringResource(R.string.home_accounts_count, accountCount)
                               else stringResource(R.string.home_accounts_count_plural, accountCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnSurfaceVariant,
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(OnSurfaceVariant.copy(alpha = 0.5f), CircleShape),
                    )
                    NetworkChip(network = wallet.network)
                }
            }

            Spacer(Modifier.width(8.dp))

            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Active",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            } else if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.action_clear),
                        tint = DangerRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkChip(network: Network) {
    val dotColor = when (network) {
        Network.MAINNET -> BrandTeal
        Network.TESTNET -> Color(0xFFFF9800)
    }
    val label = when (network) {
        Network.MAINNET -> stringResource(R.string.network_mainnet)
        Network.TESTNET -> stringResource(R.string.network_testnet)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(dotColor.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun WalletPickerSheetPreview() {
    val wallets = listOf(
        Wallet(
            id = "1",
            name = "Main Wallet",
            network = Network.MAINNET,
            accounts = listOf(
                Account("pc1...", "Account 1", AccountType.ED25519, 0)
            )
        ),
        Wallet(
            id = "2",
            name = "Testnet Wallet",
            network = Network.TESTNET,
            accounts = listOf(
                Account("pc1...", "Testing", AccountType.ED25519, 0)
            )
        )
    )
    VeloraTheme {
        Surface {
            WalletPickerSheetContent(
                wallets = wallets,
                activeWalletId = "1",
                onSelect = {},
                onRequestDelete = {},
                onAddWallet = {},
                onDismiss = {}
            )
        }
    }
}
