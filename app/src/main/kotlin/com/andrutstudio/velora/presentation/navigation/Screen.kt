package com.andrutstudio.velora.presentation.navigation

sealed class Screen(val route: String) {
    // Onboarding flow
    data object Welcome : Screen("welcome")
    data object CreateWallet : Screen("create_wallet")
    data object RestoreWallet : Screen("restore_wallet")
    data object SetupSecurity : Screen("setup_security")
    data object MnemonicBackup : Screen("mnemonic_backup")
    data object MnemonicVerify : Screen("mnemonic_verify")
    data object ImportPrivateKey : Screen("import_private_key")
    data object WatchWallet : Screen("watch_wallet")
    data object AddWalletOptions : Screen("add_wallet_options")

    // Main app
    data object Home : Screen("home")
    data object Send : Screen("send?to={to}&amount={amount}") {
        fun withArgs(to: String = "", amount: String = "") =
            "send?to=$to&amount=$amount"
    }
    data object Receive : Screen("receive/{address}") {
        fun withAddress(address: String) = "receive/$address"
    }
    data object Stake : Screen("stake")
    data object TransactionHistory : Screen("history")
    data object TransactionDetail : Screen("tx/{txId}") {
        fun withId(txId: String) = "tx/$txId"
    }

    // Browser / Discover
    data object Browser : Screen("browser?url={url}") {
        fun withUrl(url: String = "") = "browser?url=$url"
    }

    // Node dashboard
    data object Node : Screen("node")

    // Settings
    data object Settings : Screen("settings")
    data object SecuritySettings : Screen("settings/security")
    data object NetworkSettings : Screen("settings/network")
    data object BackupSettings : Screen("settings/backup")
    data object AboutSettings : Screen("settings/about")
}
