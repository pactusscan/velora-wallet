package com.andrutstudio.velora.presentation.mockups

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrutstudio.velora.presentation.theme.VeloraTheme

/**
 * MOCKUP 1: IDENTICON
 * A unique visual pattern generated from an address.
 */
@Composable
fun Identicon(address: String, size: Int = 40) {
    // Dynamic logic: determine colors based on address hash
    val hash = address.hashCode()
    val hue1 = (hash % 360).let { if (it < 0) it + 360 else it }.toFloat()
    val hue2 = ((hash / 360) % 360).let { if (it < 0) it + 360 else it }.toFloat()
    
    val color1 = Color.hsv(hue1, 0.6f, 0.8f)
    val color2 = Color.hsv(hue2, 0.7f, 0.6f)
    
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(color1, color2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        val icon = when (hash % 3) {
            0 -> Icons.Rounded.AccountBalanceWallet
            1 -> Icons.Rounded.Token
            else -> Icons.Rounded.Fingerprint
        }
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size((size * 0.5).dp)
        )
    }
}

/**
 * MOCKUP 2: STAKE DASHBOARD
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StakeDashboardMockup() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staking Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {}) { Icon(Icons.Rounded.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Stake More") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Total Staked Assets", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.7f))
                        Text("1,240.50 PAC", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoItemWhite("Active Nodes", "2 Owned")
                            InfoItemWhite("Delegations", "1 External")
                            InfoItemWhite("Avg. Score", "98.5%")
                        }
                    }
                }
            }

            // Group 1: My Own Validators
            item {
                SectionHeader("My Validators", "Nodes you control")
            }
            items(listOf("Personal-Node-01", "Home-Server")) { name ->
                ValidatorStakeItem(name, "Owned Node", isOwned = true)
            }

            // Group 2: Delegations (External Validators)
            item {
                SectionHeader("Delegations", "Staked to others")
            }
            items(listOf("Pactus-Global-Pool")) { name ->
                ValidatorStakeItem(name, "External Validator", isOwned = false)
            }
            
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoItemWhite(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}

@Composable
fun ValidatorStakeItem(name: String, type: String, isOwned: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Identicon(name, 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (isOwned) Color(0xFF11B9A5) else MaterialTheme.colorScheme.secondary
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("400 PAC", fontWeight = FontWeight.Bold)
                Text("Staked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStakeDashboard() {
    VeloraTheme {
        StakeDashboardMockup()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewIdenticon() {
    VeloraTheme {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Identicon("address1")
            Identicon("address2")
            Identicon("address3")
        }
    }
}
