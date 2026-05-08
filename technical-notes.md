# Velora Wallet: Technical Reference (v0.1.1)

**Current Version:** 0.1.1 (Alpha Release)
**Release Date:** May 7, 2026

## 1. Security Architecture
- **Non-Custodial**: BIP-39 standard for 12/24-word mnemonics.
- **Double Encryption**: PBKDF2 (120,000 iterations) + Android Keystore hardware-backed AES-256-GCM.
- **Biometric**: Integration via `BiometricPrompt` API.

## 2. Blockchain Implementation
- **Curves**: Supports **Ed25519** (Standard) and **BLS12-381** (Validator).
- **Sync**: `dataSync` Foreground Service for Android 14/15 compliance, ensuring active transaction detection.
- **Polling**: 30-second foreground polling and 5-minute background polling.

## 3. Key Logic Highlights
- **Number Formatting**: Strict adherence to `Locale.US` in `Formatter.kt` (`,` thousands, `.` decimals).
- **Anti-Spam Alerts**: Logic in `TxSyncWorker.kt` tracks `lastTriggeredValue` to prevent duplicate notification spam.
- **Transaction Flow**: Fetches block height for `lock_time`, builds payload, signs locally, and broadcasts via JSON-RPC.

## 4. Technical Stack
- **Languages/Frameworks**: Kotlin 2.0, Jetpack Compose, Hilt, Room, Retrofit.
- **Libraries**: Trust Wallet Core JNI for cryptographic operations.