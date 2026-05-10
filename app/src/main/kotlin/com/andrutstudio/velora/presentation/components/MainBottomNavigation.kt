package com.andrutstudio.velora.presentation.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.andrutstudio.velora.R
import com.andrutstudio.velora.presentation.navigation.Screen
import com.andrutstudio.velora.presentation.theme.BrandTeal
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@Composable
fun MainBottomNavigation(
    navController: NavController,
    currentRoute: String?
) {
    val isHomeSelected = currentRoute == Screen.Home.route
    
    // Deteksi mode gelap berdasarkan luminance background tema saat ini
    // Ini lebih akurat daripada isSystemInDarkTheme() karena mendukung override tema aplikasi
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    // Glassmorphism colors
    val navBarBackground = if (isDark) {
        Color(0xCC0B0E1E) // Navy Gelap dengan 0.8 alpha
    } else {
        Color(0xB3FFFFFF) // Putih Murni dengan 0.7 alpha
    }
    
    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.25f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(Color.Transparent)
            .padding(start = 24.dp, end = 24.dp, bottom = 20.dp) // Floating style
    ) {
        // 1. Layer Glass Background (Hanya layer ini yang diblur)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        renderEffect = RenderEffect
                            .createBlurEffect(30f, 30f, Shader.TileMode.CLAMP)
                            .asComposeRenderEffect()
                    }
                    clip = true
                    shape = RoundedCornerShape(100)
                }
                .background(navBarBackground, shape = RoundedCornerShape(100))
                .shadow(
                    elevation = if (isDark) 16.dp else 8.dp, 
                    shape = RoundedCornerShape(100),
                    ambientColor = if (isDark) Color.Black else Color.Gray.copy(alpha = 0.5f),
                    spotColor = if (isDark) Color.Black else Color.Gray.copy(alpha = 0.5f)
                )
        )

        // 2. Layer Border Glow (Agar border tetap tajam)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .border(
                    width = 0.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            borderColor,
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(100)
                )
        )

        // 3. Layer Konten (Ikon dan Teks - Harus Tajam)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavDockItem(
                    selected = currentRoute == Screen.Node.route,
                    onClick = {
                        if (currentRoute != Screen.Node.route) {
                            navController.navigate(Screen.Node.route) { launchSingleTop = true }
                        }
                    },
                    icon = Icons.Rounded.Hub,
                    label = stringResource(R.string.nav_node)
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavDockItem(
                    selected = currentRoute == Screen.TransactionHistory.route,
                    onClick = {
                        if (currentRoute != Screen.TransactionHistory.route) {
                            navController.navigate(Screen.TransactionHistory.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = Icons.Rounded.SwapHoriz,
                    label = stringResource(R.string.nav_activity)
                )
            }
            
            // Gap for elevated center button
            Spacer(Modifier.weight(0.8f))
            
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavDockItem(
                    selected = currentRoute?.startsWith("browser") == true,
                    onClick = {
                        if (currentRoute?.startsWith("browser") != true) {
                            navController.navigate(Screen.Browser.withUrl()) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = Icons.Rounded.Explore,
                    label = stringResource(R.string.nav_discover)
                )
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                NavDockItem(
                    selected = currentRoute == Screen.Settings.route,
                    onClick = {
                        if (currentRoute != Screen.Settings.route) {
                            navController.navigate(Screen.Settings.route) {
                                launchSingleTop = true
                            }
                        }
                    },
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.nav_settings)
                )
            }
        }

        // Elevated center Wallet button (Capsule with Gradient)
        val walletGradient = Brush.linearGradient(
            colors = listOf(Color(0xFF23C4B2), Color(0xFF7B78FF))
        )
        
        Surface(
            modifier = Modifier
                .offset(y = (-16).dp)
                .size(width = 72.dp, height = 56.dp)
                .align(Alignment.TopCenter)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = Color(0xFF23C4B2).copy(alpha = 0.5f),
                    spotColor = Color(0xFF7B78FF).copy(alpha = 0.8f),
                ),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent, 
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(walletGradient)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (currentRoute != Screen.Home.route) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = stringResource(R.string.nav_wallet),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun NavDockItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) BrandTeal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 0.2.sp
            ),
            color = contentColor,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF060E1A)
@Composable
private fun PreviewMainBottomNavigation() {
    VeloraTheme(darkTheme = true) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Dark Mode", color = Color.White, style = MaterialTheme.typography.labelMedium)
            MainBottomNavigation(navController = rememberNavController(), currentRoute = Screen.Home.route)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun PreviewMainBottomNavigationLight() {
    VeloraTheme(darkTheme = false) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Light Mode", color = Color.Black, style = MaterialTheme.typography.labelMedium)
            MainBottomNavigation(navController = rememberNavController(), currentRoute = Screen.Home.route)
        }
    }
}
