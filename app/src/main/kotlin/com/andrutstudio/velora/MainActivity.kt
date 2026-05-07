package com.andrutstudio.velora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.andrutstudio.velora.presentation.onboarding.SplashScreen
import com.andrutstudio.velora.presentation.main.MainViewModel
import com.andrutstudio.velora.presentation.navigation.AppNavGraph
import com.andrutstudio.velora.presentation.theme.VeloraTheme

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep system splash screen visible until we're ready to show Compose SplashScreen
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.startDestination.value == null
        }

        enableEdgeToEdge()

        setContent {
            VeloraTheme {
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
