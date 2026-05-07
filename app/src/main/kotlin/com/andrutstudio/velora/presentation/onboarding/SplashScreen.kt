package com.andrutstudio.velora.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.theme.Background
import com.andrutstudio.velora.presentation.theme.VeloraTheme

/**
 * Replicates the system splash screen for preview purposes and to show attribution text.
 */
@Composable
fun SplashScreen() {
    var showText by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200) // Small delay to sync with system exit
        showText = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash_logo),
            contentDescription = "Velora Logo",
            modifier = Modifier
                .size(160.dp) // Match system icon size better
                .align(Alignment.Center)
        )

        AnimatedVisibility(
            visible = showText,
            enter = fadeIn(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Powered by Pactus Blockchain",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Dedicated to the Pactus Ecosystem",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(showSystemUi = true, name = "Splash Screen")
@Composable
private fun SplashScreenPreview() {
    VeloraTheme {
        SplashScreen()
    }
}
