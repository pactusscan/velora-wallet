package com.andrutstudio.velora.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.andrutstudio.velora.presentation.browser.BrowserScreen
import com.andrutstudio.velora.presentation.history.TransactionDetailScreen
import com.andrutstudio.velora.presentation.settings.AboutSettingsScreen
import com.andrutstudio.velora.presentation.settings.BackupSettingsScreen
import com.andrutstudio.velora.presentation.settings.NetworkSettingsScreen
import com.andrutstudio.velora.presentation.settings.SecuritySettingsScreen
import com.andrutstudio.velora.presentation.settings.SettingsScreen
import com.andrutstudio.velora.presentation.settings.SettingsViewModel
import com.andrutstudio.velora.presentation.history.TransactionHistoryScreen
import com.andrutstudio.velora.presentation.home.HomeScreen
import com.andrutstudio.velora.presentation.home.AddWalletOptionsScreen
import com.andrutstudio.velora.presentation.onboarding.OnboardingViewModel
import com.andrutstudio.velora.presentation.onboarding.backup.MnemonicBackupScreen
import com.andrutstudio.velora.presentation.onboarding.create.CreateWalletScreen
import com.andrutstudio.velora.presentation.onboarding.restore.RestoreWalletScreen
import com.andrutstudio.velora.presentation.onboarding.security.SetupSecurityScreen
import com.andrutstudio.velora.presentation.onboarding.ImportPrivateKeyScreen
import com.andrutstudio.velora.presentation.onboarding.WatchWalletScreen
import com.andrutstudio.velora.presentation.onboarding.WelcomeScreen
import com.andrutstudio.velora.presentation.receive.ReceiveScreen
import com.andrutstudio.velora.presentation.send.SendScreen
import com.andrutstudio.velora.presentation.stake.StakeScreen
import com.andrutstudio.velora.presentation.theme.VeloraTheme

internal const val ONBOARDING_GRAPH = "onboarding_graph"
private const val SETTINGS_GRAPH = "settings_graph"

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { it } + fadeIn() },
        exitTransition = { slideOutHorizontally { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally { -it } + fadeIn() },
        popExitTransition = { slideOutHorizontally { it } + fadeOut() },
    ) {

        // ── Onboarding (nested graph — shares one OnboardingViewModel instance) ──
        navigation(
            startDestination = Screen.Welcome.route,
            route = ONBOARDING_GRAPH,
        ) {
            composable(Screen.Welcome.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToBackup ->
                                navController.navigate(Screen.MnemonicBackup.route)
                            OnboardingViewModel.Effect.NavigateToSecurity ->
                                navController.navigate(Screen.SetupSecurity.route)
                            OnboardingViewModel.Effect.NavigateToHome ->
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            else -> {}
                        }
                    }
                }

                WelcomeScreen(
                    onCreateWallet = {
                        vm.setWalletName("")
                        navController.navigate(Screen.CreateWallet.route)
                    },
                    onRestoreWallet = {
                        vm.startRestore()
                        navController.navigate(Screen.RestoreWallet.route)
                    },
                    onImportPrivateKey = {
                        vm.startPrivateKeyImport()
                        navController.navigate(Screen.ImportPrivateKey.route)
                    }
                )
            }

            composable(Screen.WatchWallet.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToHome ->
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            else -> {}
                        }
                    }
                }

                WatchWalletScreen(
                    state = state,
                    onNameChange = vm::setWalletName,
                    onAddressChange = vm::setWatchAddress,
                    onSubmit = { vm.submitWatchWallet() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.CreateWallet.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToBackup ->
                                navController.navigate(Screen.MnemonicBackup.route)
                            else -> {}
                        }
                    }
                }

                CreateWalletScreen(
                    state = state,
                    onNameChange = vm::setWalletName,
                    onContinue = {
                        if (state.walletName.isNotBlank()) vm.startCreate()
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.MnemonicBackup.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToSecurity ->
                                navController.navigate(Screen.SetupSecurity.route)
                            OnboardingViewModel.Effect.NavigateToHome ->
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            else -> {}
                        }
                    }
                }

                MnemonicBackupScreen(
                    state = state,
                    onContinue = { vm.proceedToVerify() },
                    onBack = { navController.popBackStack() },
                )
            }


            composable(Screen.RestoreWallet.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToSecurity ->
                                navController.navigate(Screen.SetupSecurity.route)
                            else -> {}
                        }
                    }
                }

                RestoreWalletScreen(
                    state = state,
                    onNameChange = vm::setWalletName,
                    onMnemonicTextChange = vm::setMnemonicText,
                    onSubmit = { vm.submitRestore() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.ImportPrivateKey.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToSecurity ->
                                navController.navigate(Screen.SetupSecurity.route)
                            else -> {}
                        }
                    }
                }

                ImportPrivateKeyScreen(
                    state = state,
                    onNameChange = vm::setWalletName,
                    onPrivateKeyChange = vm::setPrivateKeyHex,
                    onSubmit = { vm.submitPrivateKey() },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Screen.SetupSecurity.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, ONBOARDING_GRAPH)
                val vm: OnboardingViewModel = hiltViewModel(parentEntry)
                val state by vm.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    vm.effect.collect { effect ->
                        when (effect) {
                            OnboardingViewModel.Effect.NavigateToHome ->
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            else -> {}
                        }
                    }
                }

                SetupSecurityScreen(
                    state = state,
                    onFinish = { password, confirm, biometric ->
                        vm.finishSetup(password, confirm, biometric)
                    },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // ── Main app ──────────────────────────────────────────────────────────

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.AddWalletOptions.route) {
            AddWalletOptionsScreen(
                onBack = { navController.popBackStack() },
                onOptionSelected = { option ->
                    when (option) {
                        "create" -> {
                            // Using a simple navigate here; if it needs specific VM state, 
                            // we might need to adjust OnboardingViewModel or use HomeViewModel
                            navController.navigate(Screen.CreateWallet.route)
                        }
                        "restore" -> navController.navigate(Screen.RestoreWallet.route)
                        "import" -> navController.navigate(Screen.ImportPrivateKey.route)
                        "watch" -> {
                            // Need to initialize OnboardingViewModel state for watch wallet
                            // This is a bit tricky since we're navigating between graphs,
                            // but Screen.WatchWallet is part of ONBOARDING_GRAPH.
                            navController.navigate(Screen.WatchWallet.route)
                        }
                    }
                }
            )
        }

        composable(
            route = Screen.Send.route,
            arguments = listOf(
                navArgument("to") { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.StringType; defaultValue = "" },
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "pactus://send?to={to}&amount={amount}" },
            ),
        ) {
            SendScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Receive.route,
            arguments = listOf(navArgument("address") { type = NavType.StringType }),
        ) { backStackEntry ->
            val address = backStackEntry.arguments?.getString("address").orEmpty()
            ReceiveScreen(
                address = address,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Stake.route) {
            StakeScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TransactionHistory.route) {
            TransactionHistoryScreen(navController = navController)
        }

        composable(
            route = Screen.TransactionDetail.route,
            arguments = listOf(navArgument("txId") { type = NavType.StringType }),
        ) {
            TransactionDetailScreen(navController = navController)
        }

        composable(
            route = Screen.Browser.route,
            arguments = listOf(navArgument("url") { type = NavType.StringType; defaultValue = "" }),
        ) {
            BrowserScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ── Settings (nested graph — shares one SettingsViewModel instance) ──
        navigation(
            startDestination = Screen.Settings.route,
            route = SETTINGS_GRAPH,
        ) {
            composable(Screen.Settings.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, SETTINGS_GRAPH)
                val vm: SettingsViewModel = hiltViewModel(parentEntry)
                SettingsScreen(
                    navController = navController,
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSecuritySettings = { navController.navigate(Screen.SecuritySettings.route) },
                    onNetworkSettings = { navController.navigate(Screen.NetworkSettings.route) },
                    onBackupSettings = { navController.navigate(Screen.BackupSettings.route) },
                    onAboutSettings = { navController.navigate(Screen.AboutSettings.route) },
                    onWalletReset = {
                        navController.navigate(ONBOARDING_GRAPH) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.SecuritySettings.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, SETTINGS_GRAPH)
                val vm: SettingsViewModel = hiltViewModel(parentEntry)
                SecuritySettingsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.NetworkSettings.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, SETTINGS_GRAPH)
                val vm: SettingsViewModel = hiltViewModel(parentEntry)
                NetworkSettingsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.BackupSettings.route) { entry ->
                val parentEntry = entry.rememberParentEntry(navController, SETTINGS_GRAPH)
                val vm: SettingsViewModel = hiltViewModel(parentEntry)
                BackupSettingsScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.AboutSettings.route) {
                AboutSettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

/** Helper to get a parent NavBackStackEntry for a named graph. */
@Composable
private fun androidx.navigation.NavBackStackEntry.rememberParentEntry(
    navController: NavHostController,
    route: String,
) = androidx.compose.runtime.remember(this) {
    navController.getBackStackEntry(route)
}

@Preview(showBackground = true)
@Composable
private fun AppNavGraphPreview() {
    VeloraTheme {
        val navController = rememberNavController()
        AppNavGraph(
            navController = navController,
            startDestination = Screen.Welcome.route
        )
    }
}
