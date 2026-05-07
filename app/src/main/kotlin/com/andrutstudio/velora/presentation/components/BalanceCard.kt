package com.andrutstudio.velora.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.andrutstudio.velora.R
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.presentation.theme.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun BalanceCard(
    totalBalance: Amount,
    network: Network,
    isLoading: Boolean,
    pacPriceUsd: Double? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BrandTeal, BrandPurple)
                    )
                )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_pactus_symbol),
                contentDescription = null,
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp, y = 20.dp)
                    .alpha(0.2f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Balance",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    NetworkBadge(network)
                }

                Spacer(Modifier.weight(1f))

                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedBalance(totalBalance)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "PAC",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    pacPriceUsd?.let { price ->
                        val usdValue = totalBalance.pac * price
                        val symbols = DecimalFormatSymbols(Locale.US)
                        Text(
                            text = "≈ $${DecimalFormat("#,##0.00", symbols).format(usdValue)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedBalance(balance: Amount) {
    Text(
        text = formatPac(balance).removeSuffix(" PAC"),
        style = MaterialTheme.typography.headlineLarge,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
}

@Composable
private fun NetworkBadge(network: Network) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (network == Network.MAINNET) BrandTeal else Color(0xFFFF9800))
            )
            Text(
                text = network.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
private fun BalanceCardPreview() {
    VeloraTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BalanceCard(
                totalBalance = Amount.fromPac(12345.6789),
                network = Network.MAINNET,
                isLoading = false,
                pacPriceUsd = 0.42
            )
            BalanceCard(
                totalBalance = Amount.fromPac(0.0),
                network = Network.TESTNET,
                isLoading = false
            )
        }
    }
}
