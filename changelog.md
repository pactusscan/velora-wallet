# Changelog

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
