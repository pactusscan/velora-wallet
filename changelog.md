# Changelog

## [0.1.3] - 2026-05-14

### Added
- **Instant Transaction Notifications**: Improved synchronization logic to ensure you receive alerts for incoming transfers, outgoing payments, and successful staking (bond) transactions immediately.
- **Smart Filtering**: The app now automatically hides "spam" notifications like block rewards and internal transfers between your own accounts, keeping your notification tray clean.
- **Multi-Wallet Monitoring**: All your created or imported wallets are now monitored in the background for any activity.
- **Improved Visual Alerts**: Notifications now feature the official Velora logo and a custom shield icon in the status bar for a more premium and trustworthy experience.
- **Secure Backup Interface**: Enhanced security on the recovery phrase screen to prevent accidental screenshots or screen recordings of your sensitive data.
- **Swipe Actions**: Added intuitive "swipe to manage" gestures for accounts and wallets.
- **Multi-Language Support**: All transaction alerts are fully localized in English, Indonesian, German, Russian, Vietnamese, Portuguese (Brazil), Malay, French, Spanish, and Chinese.

### Fixed & Improved
- **Better Node Monitoring**: Fixed an issue where validator status wouldn't update correctly; you'll now get reliable alerts if your monitored node goes offline.
- **Accurate Version Display**: The version number in the "About" screen now perfectly matches the system information and APK filename, including the build time.
- **Transaction Reliability**: Each transaction now has its own unique notification, so you won't miss any alerts even if multiple transactions happen at once.
- **Stability Fixes**: Resolved various technical issues and crashes related to background sync and screen transitions for a smoother experience.

## [0.1.2] - 2026-05-10

### Added
- **Node Monitoring Dashboard**: Added a new dashboard to track Pactus validator nodes, including status, current block height, version, and network orientation (Inbound/Outbound).
- **Full Edge-to-Edge Experience**: Enabled a truly immersive interface where content flows behind transparent status and navigation bars for a modern, bezel-less look.
- **Floating Glassmorphism Dock**: Implemented a modern, blurred navigation bar that "floats" over content across all main screens (Home, History, Node, Browser, Settings).
- **Adaptive Theme Support**: Full compatibility for System, Light, and Dark modes. Integrated dynamic luminance detection to automatically adjust UI elements like the Glassmorphism dock tint for optimal visibility in any theme.
- **Price Sparkline**: Added a minimalist price trend chart to the main Balance Card using 7-day market history.
- **Smart RPC Selection**: Implemented a "race" mechanism to automatically connect to the RPC node with the lowest latency for faster data synchronization.
- **Legal & Compliance**: Added direct links to Terms of Service and Privacy Policy within the About settings.

### Fixed & Improved
- **Background Sync Robustness**: Optimized `TxSyncWorker` to automatically re-initialize wallet metadata when starting from a background state, ensuring reliable updates even after app process termination.
- **Intelligent Balance Alerts**: Refined alert logic to eliminate notification spam; alerts now trigger exactly once when a threshold is crossed and reset only when conditions normalize.
- **Notification Permissions**: Integrated a seamless Android 13+ permission request flow on the Home screen to ensure users receive critical transaction and balance alerts.
- **RPC Parsing**: Enhanced transaction parsing to accurately capture amounts for both standard Transfers and Bond (Stake) operations in notifications and activity history.
- **Production Build (R8/ProGuard)**: Optimized shrinking rules for WorkManager, Hilt, and TrustWalletCore to ensure maximum stability and performance in signed release APKs.
- **Automated Versioning**: Updated build system to automatically generate unique version codes and descriptive APK filenames (e.g., `VeloraWallet-0.1.2-evening-xxxx.apk`) for every build.
- **UI Refinement**: Streamlined theme and language selection dialogs by removing redundant buttons, favoring intuitive gesture-based dismissal.
- **Component Previews**: Expanded Compose `@Preview` support for complex navigation and multi-state components to ensure pixel-perfect design consistency.
- **Snappy Navigation**: Optimized screen transitions by replacing heavy horizontal slide animations with high-performance cross-fades, significantly reducing lag on devices with higher refresh rates.

## [0.1.1] - 2026-05-07

### Added
- **Core Wallet Features**: 
    - Non-custodial wallet creation and restoration using 12/24-word mnemonics.
    - Multi-account support utilizing BIP-44 and BLS standards.
    - Ability to Send and Receive PAC transactions.
    - Staking (Bonding) functionality directly to Pactus validators.
- **Web3 Browser**:
    - Integrated browser to interact with Pactus dApps.
    - JavaScript bridge (`window.pactus`) for signing transactions on external websites.
    - Bookmarks and history management for decentralized web navigation.
- **Enhanced Notifications & Alerts**:
    - Real-time notifications for incoming transfers.
    - Custom balance alerts with conditional logic ("Lower Than" / "Higher Than").
    - Audible and high-priority status bar notifications optimized for Android 14/15.
- **Home Screen Widget**:
    - Glance-based widget for real-time balance tracking directly from the Android home screen.
- **Security & Privacy**:
    - Local encryption via Android Keystore (AES-GCM).
    - Biometric authentication support (Fingerprint and Face Unlock).
    - Secure memory management with auto-clearing sensitive data.
    - Wallet data excluded from system cloud backups to protect private keys.

### Fixed & Improved
- **Browser**: Fixed layout overlap with the system status bar and improved address bar interactivity.
- **Sync Reliability**: Resolved `InvalidForegroundServiceTypeException` for Android 14+ and optimized background sync workers.
- **UI/UX**: 
    - Standardized PAC number formatting (`,` for thousands, `.` for decimals).
    - Implemented a unified `Formatter.kt` for consistent amount displays app-wide.
    - Added immediate network synchronization on app startup.
