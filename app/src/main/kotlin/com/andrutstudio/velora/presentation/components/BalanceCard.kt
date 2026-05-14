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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    priceHistory: List<Double> = emptyList(),
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
                    .size(160.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .alpha(0.2f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(Color.White)
            )

            if (!isLoading && priceHistory.size >= 2) {
                PriceSparkline(
                    data = priceHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .alpha(0.4f)
                )
            }

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
                        stringResource(R.string.balance_total_label),
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
                            stringResource(R.string.send_unit_pac),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(bottom = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    pacPriceUsd?.let { price ->
                        val usdValue = totalBalance.pac * price
                        val symbols = DecimalFormatSymbols(Locale.US)
                        val formattedUsd = DecimalFormat("#,##0.00", symbols).format(usdValue)
                        Text(
                            text = stringResource(R.string.balance_usd_estimate, formattedUsd),
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

@Composable
private fun PriceSparkline(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White,
) {
    Canvas(modifier = modifier) {
        val min = data.minOrNull() ?: 0.0
        val max = data.maxOrNull() ?: 0.0
        val range = (max - min).coerceAtLeast(0.0001)

        val path = Path()
        val stepX = size.width / (data.size - 1)

        data.forEachIndexed { index, price ->
            val x = index * stepX
            val y = size.height - ((price - min) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.3f), Color.Transparent)
            )
        )
    }
}

@Preview
@Composable
private fun BalanceCardPreview() {
    VeloraTheme {
        val mockHistory = listOf(0.022, 0.021, 0.018, 0.019, 0.025, 0.023, 0.024, 0.026, 0.024)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BalanceCard(
                totalBalance = Amount.fromPac(12345.6789),
                network = Network.MAINNET,
                isLoading = false,
                pacPriceUsd = 0.42,
                priceHistory = mockHistory
            )
            BalanceCard(
                totalBalance = Amount.fromPac(0.0),
                network = Network.TESTNET,
                isLoading = false
            )
        }
    }
}
