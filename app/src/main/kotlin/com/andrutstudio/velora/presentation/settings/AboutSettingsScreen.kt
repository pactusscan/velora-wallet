package com.andrutstudio.velora.presentation.settings

import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrutstudio.velora.BuildConfig
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // App identity
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_velora_logo),
                    contentDescription = stringResource(R.string.app_name),
                    modifier = Modifier.size(100.dp),
                    contentScale = ContentScale.Fit
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.about_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            HorizontalDivider()

            // Links
            AboutLinkItem(
                icon = Icons.Rounded.Language,
                title = stringResource(R.string.about_website),
                subtitle = stringResource(R.string.about_website_subtitle),
                onClick = { openUrl("https://pactus.org") },
            )
            AboutLinkItem(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = stringResource(R.string.about_docs),
                subtitle = stringResource(R.string.about_docs_subtitle),
                onClick = { openUrl("https://docs.pactus.org") },
            )
            AboutLinkItem(
                icon = Icons.Rounded.Code,
                title = stringResource(R.string.about_github),
                subtitle = stringResource(R.string.about_github_subtitle),
                onClick = { openUrl("https://github.com/pactus-project") },
            )
            AboutLinkItem(
                icon = Icons.Rounded.Search,
                title = stringResource(R.string.about_explorer),
                subtitle = stringResource(R.string.about_explorer_subtitle),
                onClick = { openUrl("https://pactusscan.com") },
            )
            AboutLinkItem(
                icon = Icons.Rounded.Gavel,
                title = stringResource(R.string.about_terms),
                subtitle = stringResource(R.string.about_terms_subtitle),
                onClick = { openUrl("https://velora.pactusscan.com/terms-of-service") },
            )
            AboutLinkItem(
                icon = Icons.Rounded.PrivacyTip,
                title = stringResource(R.string.about_privacy),
                subtitle = stringResource(R.string.about_privacy_subtitle),
                onClick = { openUrl("https://velora.pactusscan.com/privacy-policy") },
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun AboutSettingsScreenPreview() {
    VeloraTheme {
        AboutSettingsScreen(onNavigateBack = {})
    }
}

@Composable
private fun AboutLinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
