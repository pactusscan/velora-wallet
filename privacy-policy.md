# Privacy Policy for Velora Wallet

**Last Updated:** May 7, 2026

Velora Wallet ("we," "us," or "our") is committed to protecting your privacy.

## 1. Zero Data Collection Policy
Velora **does not** collect, store, or transmit any personal data, recovery phrases, private keys, or IP addresses to our servers.

## 2. Local Storage and Encryption
All sensitive data is stored exclusively on your device using industry-standard encryption:
- **Keys**: Private keys are double-encrypted using **PBKDF2** (120k iterations) and **AES-256-GCM**.
- **Hardware Security**: We utilize the **Android Keystore System** to ensure keys never leave the secure hardware (TEE/SE).
- **Local Database**: Account labels and settings are stored in a local Room database and are never shared.

## 3. Use of Device Permissions
- **Camera**: For scanning Pactus QR codes.
- **Biometrics**: For secure local vault unlocking (success/fail token only).
- **Notifications & Foreground Service**: Used for transaction detection and alerts on Android 14/15.

## 4. Contact Information
For inquiries, please visit [velora.pactusscan.com](https://velora.pactusscan.com).