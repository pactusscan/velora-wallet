# Velora Wallet

Velora is a premium, non-custodial mobile wallet built for the **Pactus Blockchain**. Designed with a focus on security, simplicity, and real-time responsiveness, Velora provides a seamless interface for managing Pactus assets on Android.

## 🚀 Key Features

*   **Truly Non-Custodial**: Your private keys never leave your device. Encrypted using PBKDF2 and secured by the hardware-backed Android Keystore.
*   **Multi-Account Support**: Manage multiple accounts (BIP-44 Ed25519) and Validator nodes (BLS) under a single recovery phrase.
*   **Real-time Synchronization**: Powered by an Android Foreground Service (`dataSync`) for zero-latency detection of incoming transfers and audible notifications.
*   **Built-in Web3 Browser**: Interact with the Pactus ecosystem (Wrapto, Pactus Scan, etc.) via a secure JavaScript bridge (`window.pactus`).
*   **Smart Staking**: Directly bond/stake PAC to validators and monitor your rewards.
*   **Custom Balance Alerts**: Set "Lower Than" or "Higher Than" thresholds with smart anti-spam notification logic.
*   **Modern UI/UX**: Built with Jetpack Compose following Material 3 guidelines, featuring a dark theme and glassmorphism effects.
*   **Home Screen Widget**: Track your total balance at a glance without opening the app.
*   **Biometric Security**: Quick and secure access using Fingerprint or Face Unlock.

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **UI**: Jetpack Compose (Material 3)
*   **Architecture**: MVVM with MVI-style state management
*   **Dependency Injection**: Hilt
*   **Local Database**: Room
*   **Networking**: Retrofit + OKHttp (JSON-RPC)
*   **Background Tasks**: WorkManager (Foreground Service)
*   **Crypto Core**: Trust Wallet Core (JNI)
*   **Widgets**: Jetpack Glance
*   **Target SDK**: 35 (Android 15)

## 📁 Project Structure

```text
app/src/main/kotlin/com/andrutstudio/velora/
├── data/           # Repositories, DAO, RPC, and Crypto logic
├── domain/         # Models, Repository interfaces, and Use Cases
├── presentation/   # UI Screens, ViewModels, and Components
└── VeloraApp.kt    # Application class & Sync scheduling

web/                # Publication website (velora.pactusscan.com)
docs/               # Technical notes and legal documents
```

## ⚙️ Setup & Installation

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/pactusscan/pactus-wallet-app.git
    ```
2.  **Open in Android Studio**: (Ladybug or newer recommended).
3.  **Local Properties**: Add your signing configuration in `local.properties` for release builds.
4.  **Build & Run**: Use the `app` module on an emulator or physical device (API 26+).

## 🔒 Security

Velora utilizes a multi-layered security approach:
- **Seed Phrases**: Standard BIP-39 (12/24 words).
- **Encryption**: AES-256-GCM encryption with keys derived via PBKDF2 (120,000 iterations).
- **Hardware Isolation**: Encryption keys are stored in the Android Keystore's Trusted Execution Environment (TEE).
- **Privacy**: No personal data collection. External connections are limited to Pactus RPC nodes and public market APIs.

## 📄 Documentation

For more detailed information, please refer to:
- [Technical Notes](technical-notes.md) - Deep dive into code and architecture.
- [Terms of Service](terms-of-service.md) - Usage guidelines.
- [Privacy Policy](privacy-policy.md) - Data handling practices.
- [Changelog](changelog.md) - Evolution of the project.

## 🌐 Links

- **Website**: [velora.pactusscan.com](https://velora.pactusscan.com)
- **Blockchain**: [pactus.org](https://pactus.org)

---
Crafted with ❤️ by AndrutStudio for the Pactus Community.
