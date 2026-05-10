package com.andrutstudio.velora

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.andrutstudio.velora.data.local.ThemePreference
import com.andrutstudio.velora.presentation.onboarding.SplashScreen
import com.andrutstudio.velora.presentation.main.MainViewModel
import com.andrutstudio.velora.presentation.navigation.AppNavGraph
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        splashScreen.setKeepOnScreenCondition {
            mainViewModel.startDestination.value == null
        }

        setContent {
            val themePreference by mainViewModel.themePreference.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themePreference) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                ThemePreference.SYSTEM -> systemDark
            }

            VeloraTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

                if (startDestination == null) {
                    SplashScreen()
                } else {
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination!!,
                    )
                }
            }
        }
    }
}
