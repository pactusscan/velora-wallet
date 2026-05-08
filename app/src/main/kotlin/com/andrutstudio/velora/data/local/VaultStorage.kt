package com.andrutstudio.velora.data.local

import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.andrutstudio.velora.data.crypto.CryptoUtils
import com.andrutstudio.velora.data.crypto.CryptoUtils.zeroOut
import com.andrutstudio.velora.data.local.model.MultiVault
import com.andrutstudio.velora.data.local.model.WalletVault
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

private const val KEY_VAULT = "vault_data"           // legacy single-vault key
private const val KEY_MULTI_VAULT = "multi_vault_data"
private const val KEY_BIOMETRIC = "biometric_enabled"

/**
 * Persists wallet vaults in EncryptedSharedPreferences.
 * The mnemonic inside each vault is double-encrypted:
 *   Layer 1 — PBKDF2(password) via [CryptoUtils.deriveKey]
 *   Layer 2 — Android Keystore AES key via EncryptedSharedPreferences itself
 */
@Singleton
class VaultStorage @Inject constructor(
    private val prefs: SharedPreferences,
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun hasVault(): Boolean =
        prefs.contains(KEY_MULTI_VAULT) || prefs.contains(KEY_VAULT)

    /** Returns the multi-vault, migrating any legacy single-vault on first read. */
    fun loadMultiVault(): MultiVault? {
        prefs.getString(KEY_MULTI_VAULT, null)?.let {
            return json.decodeFromString(it)
        }
        val legacy = prefs.getString(KEY_VAULT, null) ?: return null
        val legacyVault: WalletVault = json.decodeFromString(legacy)
        val migrated = MultiVault(
            wallets = listOf(legacyVault),
            activeWalletId = legacyVault.id,
        )
        saveMultiVault(migrated)
        prefs.edit().remove(KEY_VAULT).apply()
        return migrated
    }

    fun saveMultiVault(multi: MultiVault) {
        prefs.edit()
            .putString(KEY_MULTI_VAULT, json.encodeToString(multi))
            .apply()
    }

    /** Active wallet (the one currently selected). */
    fun loadVault(): WalletVault? {
        val multi = loadMultiVault() ?: return null
        return multi.wallets.find { it.id == multi.activeWalletId }
            ?: multi.wallets.firstOrNull()
    }

    /** Updates an existing wallet by id (matched on [vault.id]); inserts if not present. */
    fun saveVault(vault: WalletVault) {
        val current = loadMultiVault()
        val updated = if (current == null) {
            MultiVault(wallets = listOf(vault), activeWalletId = vault.id)
        } else {
            val replaced = current.wallets.map { if (it.id == vault.id) vault else it }
            val merged = if (replaced.any { it.id == vault.id }) replaced else replaced + vault
            current.copy(wallets = merged)
        }
        saveMultiVault(updated)
    }

    /** Adds a brand-new wallet and makes it active. */
    fun addWallet(vault: WalletVault) {
        val current = loadMultiVault()
        val updated = if (current == null) {
            MultiVault(wallets = listOf(vault), activeWalletId = vault.id)
        } else {
            current.copy(
                wallets = current.wallets + vault,
                activeWalletId = vault.id,
            )
        }
        saveMultiVault(updated)
    }

    fun selectWallet(walletId: String) {
        val current = loadMultiVault() ?: return
        if (current.wallets.any { it.id == walletId }) {
            saveMultiVault(current.copy(activeWalletId = walletId))
        }
    }

    fun removeWallet(walletId: String) {
        val current = loadMultiVault() ?: return
        val remaining = current.wallets.filterNot { it.id == walletId }
        if (remaining.isEmpty()) {
            deleteVault()
        } else {
            val newActive = if (current.activeWalletId == walletId) {
                remaining.first().id
            } else current.activeWalletId
            saveMultiVault(MultiVault(wallets = remaining, activeWalletId = newActive))
        }
    }

    fun deleteVault() {
        prefs.edit()
            .remove(KEY_MULTI_VAULT)
            .remove(KEY_VAULT)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    /** Encrypts [mnemonic] with the user [password], returns base64 ciphertext and base64 salt. */
    fun encryptMnemonic(mnemonic: String, password: CharArray): Pair<String, String> {
        val salt = CryptoUtils.generateSalt()
        val iv = CryptoUtils.generateIv()
        val key = CryptoUtils.deriveKey(password, salt)
        val plainBytes = mnemonic.toByteArray(Charsets.UTF_8)
        val encrypted = CryptoUtils.encrypt(plainBytes, key, iv)
        plainBytes.zeroOut()
        return Base64.getEncoder().encodeToString(encrypted) to
                Base64.getEncoder().encodeToString(salt)
    }

    /**
     * Decrypts mnemonic. Throws [WrongPasswordException] if password is incorrect
     * (AES-GCM authentication tag will fail).
     */
    fun decryptMnemonic(encryptedMnemonic: String, salt: String, password: CharArray): String {
        return try {
            val saltBytes = Base64.getDecoder().decode(salt)
            val cipherBytes = Base64.getDecoder().decode(encryptedMnemonic)
            val key = CryptoUtils.deriveKey(password, saltBytes)
            val plain = CryptoUtils.decrypt(cipherBytes, key)
            String(plain, Charsets.UTF_8).also { plain.zeroOut() }
        } catch (e: Exception) {
            throw WrongPasswordException()
        }
    }
}

class WrongPasswordException : Exception("Incorrect password")
