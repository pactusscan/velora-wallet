# Changelog

## [0.1.2] - 2026-05-05

### Added
- **Price Sparkline**: Added a minimalist price trend chart to the main Balance Card using 7-day market history.
- **Component Previews**: Integrated Compose `@Preview` for all shared UI components to accelerate development and design consistency.
- **Smart RPC Selection**: Implemented a "race" mechanism to automatically connect to the RPC node with the lowest latency for faster data synchronization.

### Fixed & Improved
- **Production Build (R8)**: Fixed a critical bug where USD balance estimation was missing in Signed APKs due to code obfuscation. Updated ProGuard rules for Market and RPC models.
- **Network Resilience**: Replaced technical error blocks with professional "Offline" notifications and automated background reconnection logic.
- **Web Browser**: 
    - Significantly improved WebView capabilities to match Chrome Android features (hardware acceleration, horizontal scroll support, enhanced JavaScript).
    - Removed side drawer and kebab menu to eliminate interference with web navigation and gestures.
- **Onboarding & Backup UI**: 
    - Re-designed `RestoreWalletScreen`, `MnemonicBackupScreen`, and `BackupSettingsScreen` to feature numbered seed phrase grids.
    - Added "Smart Paste" support for entire seed phrases with automatic word distribution across fields.
- **UI/UX**: 
    - Re-designed `Switch` components for much better visibility in high-light conditions and dark mode.
    - Optimized `ReceiveScreen` by removing redundant "Tap to copy" labels.
    - Improved `ConfirmSendSheet` to intelligently hide the password field when biometric authentication is ready.
- **Project Structure**: Aligned unit test package structure with the new `com.andrutstudio.velora` namespace.

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
