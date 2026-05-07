package com.andrutstudio.velora.data.local.model

import kotlinx.serialization.Serializable

/**
 * Persisted wallet vault. The mnemonic is AES-256-GCM encrypted with a
 * PBKDF2-derived key (user password + salt). The payload field contains
 * IV (12 bytes) + ciphertext, base64-encoded.
 */
@Serializable
data class WalletVault(
    val id: String,
    val name: String,
    /** Base64(IV + AES-GCM ciphertext of UTF-8 mnemonic). */
    val encryptedMnemonic: String,
    /** Base64 PBKDF2 salt. */
    val salt: String,
    val network: String,
    val accounts: List<VaultAccount>,
    val createdAt: Long,
    val version: Int = VAULT_VERSION,
    /** True when wallet was created by importing a raw private key (not HD mnemonic). */
    val isPrivateKeyImport: Boolean = false,
    /** True for read-only wallets. These have empty encryptedMnemonic/salt. */
    val isWatchOnly: Boolean = false,
) {
    companion object {
        const val VAULT_VERSION = 1
    }
}

@Serializable
data class VaultAccount(
    val address: String,
    val label: String,
    val type: String,
    val derivationPath: String,
    val derivationIndex: Int,
)

/**
 * Top-level container for multi-wallet storage. Holds an ordered list of
 * vaults plus a pointer to the currently active one.
 */
@Serializable
data class MultiVault(
    val wallets: List<WalletVault>,
    val activeWalletId: String,
    val version: Int = MULTI_VAULT_VERSION,
) {
    companion object {
        const val MULTI_VAULT_VERSION = 1
    }
}
