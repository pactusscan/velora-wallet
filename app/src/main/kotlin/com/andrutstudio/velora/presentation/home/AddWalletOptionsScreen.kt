package com.andrutstudio.velora.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletOptionsScreen(
    onBack: () -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Wallet") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Choose how you want to add a wallet to Velora.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OptionItem(
                title = "Create New Wallet",
                subtitle = "Generate a new recovery phrase",
                icon = Icons.Rounded.AddCard,
                iconBackground = BrandTeal,
                onClick = { onOptionSelected("create") }
            )

            OptionItem(
                title = "Restore Wallet",
                subtitle = "Use your existing recovery phrase",
                icon = Icons.Rounded.History,
                iconBackground = BrandPurple,
                onClick = { onOptionSelected("restore") }
            )

            OptionItem(
                title = "Import Wallet",
                subtitle = "Import from a private key",
                icon = Icons.Rounded.VpnKey,
                iconBackground = Color(0xFFFF9800),
                onClick = { onOptionSelected("import") }
            )

            OptionItem(
                title = "Watch Wallet",
                subtitle = "Track balance (read-only)",
                icon = Icons.Rounded.Visibility,
                iconBackground = MaterialTheme.colorScheme.outline,
                onClick = { onOptionSelected("watch") }
            )
        }
    }
}

@Composable
private fun OptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBackground: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = SurfaceContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBackground.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconBackground,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = OnBackground
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
fun AddWalletOptionsScreenPreview() {
    VeloraTheme {
        AddWalletOptionsScreen(
            onBack = {},
            onOptionSelected = {}
        )
    }
}
