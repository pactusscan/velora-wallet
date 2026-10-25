# Changelog

## [Unreleased]

### Fixed & Improved
- **Unlock Dialog Wording**: The unlock dialog on Home and Settings now offers "Cancel" instead of "Reject". In the dApp browser it still says "Reject", because there it also declines the site's request.
- **All Languages in System Settings**: German, Portuguese (Brazil), Russian and Chinese can now also be chosen in Android's per-app language settings (Android 13+), and the Settings screen shows their name instead of "English".
- **Complete Indonesian Translation**: The 94 texts that still appeared in English in Indonesian (Settings, dApp browser, Stake, balance alerts, Receive) are now translated, and Recover addresses, Connected sites and the theme selector are translated in every language.
- **Translatable Screens**: Transaction details, history filters and status badges, the dApp connect sheet, password strength, node monitor labels and several buttons were hardcoded in English and can now be translated. In the Send and Stake confirmation, addresses are shortened and shown in monospace in every language (previously this only worked for some English labels).

## [0.1.6] - 2026-10-25

This release hardens how dApps ask for signatures. If you use the in-app dApp browser, updating is recommended.

### Added
- **Choose the Account That Signs a Message**: dApps can ask for a signature from a specific address with `window.pactus.signMessage(message, { address })`, for example to prove ownership of that account. The approval sheet shows the requested account and it cannot be changed; if the wallet does not hold it, the request is refused. Without an address, the sheet now lets you pick which account signs (the first one is preselected).

### Fixed & Improved
- **dApp Signing After Opening the App**: Approving a dApp message or transaction right after opening Velora failed with "Wallet is locked". The approval now asks for your password (or fingerprint, when enabled) first and then completes the signing.
- **Clear Message Signing Title**: The approval sheet for signing a message is now titled "Sign Message" instead of "Sign Transaction", so a text message is never mistaken for a transaction.
- **Safer dApp Approvals**: A site can no longer replace a signing request while you are reviewing it. Only one request is shown at a time, further requests are refused until you approve or reject it, and only the exact request you confirmed is signed.

## [0.1.5] - 2026-10-25

### Added
- **dApp Connection Approval**: dApps can now request a connection via `window.pactus.connect()`. A native approval sheet shows the site and your account before anything is shared, and approved sites connect automatically on the next visit.
- **Connected Sites Management**: New "Connected sites" screen in Settings (matching the Velora browser extension) to review every dApp you have approved and revoke access with one tap.
- **Instant Provider Detection**: The `window.pactus` provider is now injected before any page script runs, so dApps like play.pactusscan detect Velora immediately instead of missing it during startup.
- **Wallet Discovery Protocol**: The browser now answers the multi-wallet announce protocol (`pactus:requestProvider` / `pactus:announceProvider`), presenting itself as "Velora Wallet" with the official logo in dApp wallet pickers.
- **Web3 Authentication (Sign-In with Pactus)**: Added support for signing arbitrary messages, enabling users to log in to decentralized applications (dApps) securely without spending coins.
- **Enhanced JavaScript Bridge**: Expanded the `window.pactus` API with `connect`, `signMessage`, `signTransaction`, and `getAccounts` to allow deep integration for web-based Pactus tools.
- **Global Deep Linking**: Implemented a comprehensive deep link system (`pactus://`) to trigger wallet actions like sending coins, staking, or opening the browser from external links. Websites opened in Chrome can hand themselves over to Velora's dApp browser through `pactus://browser`.
- **Offline Balance Caching**: The app now stores and displays your last known balance even when offline, providing immediate feedback instead of showing zero.
- **PIP-41 Address Recovery**: Restoring a wallet from a mnemonic now automatically rediscovers previously used accounts by scanning the chain (gap limit 8, per [PIP-41](https://pips.pactus.org/PIPs/pip-41)). A manual "Recover addresses" scan is also available in Settings for wallets restored while offline.

### Fixed & Improved
- **Visible Send and Stake Errors**: When a transaction fails, the reason is now shown inside the confirmation sheet instead of a snackbar hidden behind it, so you can read it and retry or cancel.
- **Imported Private Key Wallets Can Send Again**: Unlocking a wallet imported from a private key treated the key as a seed phrase and failed, so every send (and stake) from that wallet stopped at the password step. The address is now computed from the key itself, also when switching networks, and "Add account" is hidden for these single-account wallets.
- **secp256k1 (pc1y) Recipient Support**: Sending to the new secp256k1 addresses (`pc1y…`, introduced in Pactus 1.16.0 via PIP-53) now works. Recipient validation was also upgraded from a prefix check to full bech32m checksum validation, catching typos before a transaction is built.
- **Multi-Network Compatibility**: Fixed an issue where the coin type was hardcoded to Mainnet (21888) in `PactusKeyManager`; key derivation now correctly uses dynamic coin types, enabling full support for Testnet accounts.
- **Dynamic Explorer Integration**: Added automatic explorer switching; the app now correctly points to `phoenix.pactusscan.com` when on Testnet and `pactusscan.com` on Mainnet for accurate transaction history.
- **Intelligent Layout Scaling**: Fixed a UI issue where extremely long PAC balances would cause text overlapping on the main dashboard card.
- **Background Robustness**: Improved the reliability of balance updates to ensure they happen quietly in the background without UI flickering.
- **Developer Guide**: Included a technical integration guide for dApp developers to easily connect their web applications to Velora Wallet.
- **Safe Database Upgrade**: Local database migrated to version 8 for connected-site storage with a proper migration path, keeping bookmarks, history, and contacts intact when updating from older versions.

## [0.1.4] - 2026-06-27

### Added
- **Address Book**: A permanent storage for your favorite wallet addresses. Save frequent recipients with custom names to avoid manual typing and potential errors.
- **Saved Memos**: Quick-access storage for transaction notes. Save common memos and fill them instantly using the new picker interface.
- **Upgraded Send Experience**:
    - Integrated "Address Book" and "QR Scan" shortcuts directly into the recipient field.
    - Added a "Saved Memos" button to the transaction note field.
    - All new features are fully localized in all supported languages.
- **Smart Connectivity Guard**: The app now verifies an active internet connection before checking node status, preventing false "Node Offline" alerts during general network outages.
- **Local Contact Database**: Implemented a secure, offline database to ensure your contacts and memos stay on your device and load instantly.
- **Universal Localized Buttons**: Main action buttons (Send, Receive, Stake) are now fully localized across all 10 supported languages.

### Fixed & Improved
- **Enhanced QR Scannability**: Optimized QR code generation with a higher error correction level (Level H) to guarantee reliable scanning, even with the Velora brand logo overlay.
- **UI Refinement**: Optimized the "Send" screen layout for better accessibility and a cleaner look.
- **Theme Stability**: Fixed styling and font inconsistencies in the "Restore Wallet" and "Import Private Key" screens for a seamless experience in System, Light, and Dark modes.
- **Navigation Fluidity**: Smoother Bottom Sheet animations when selecting contacts or memos.
- **Consistency**: Standardized font styles and icons across all input fields during the transaction flow.

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
