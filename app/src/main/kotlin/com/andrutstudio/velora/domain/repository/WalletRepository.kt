package com.andrutstudio.velora.domain.repository

import kotlinx.coroutines.flow.Flow
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet

interface WalletRepository {
    /** Active (currently selected) wallet, or null when no wallet exists. */
    fun observeWallet(): Flow<Wallet?>

    /** All wallets the user has created/imported. Empty when none exist. */
    fun observeWallets(): Flow<List<Wallet>>

    suspend fun hasWallet(): Boolean
    /** Load wallet data from storage (locked state — no mnemonic). Safe to call on every cold start. */
    suspend fun loadLockedWallet()
    suspend fun createWallet(name: String, mnemonic: String, password: String): Wallet
    suspend fun restoreWallet(name: String, mnemonic: String, password: String): Wallet
    /** Import a single Ed25519 private key (hex). Creates a non-HD single-account wallet. */
    suspend fun importPrivateKey(
        name: String,
        privateKeyHex: String,
        password: String,
        type: AccountType = AccountType.ED25519,
    ): Wallet

    /** Watch an address (read-only). No password required. */
    suspend fun watchWallet(name: String, address: String): Wallet

    /** Switch the active wallet. */
    suspend fun selectWallet(walletId: String)
    /** Update wallet name. */
    suspend fun renameWallet(walletId: String, name: String)
    /**
     * Remove a wallet. If the active wallet is deleted, the first remaining wallet becomes active;
     * if no wallets remain, behaves like [resetWallet].
     */
    suspend fun deleteWallet(walletId: String)

    /** Wipe the vault and reset to a blank state. */
    suspend fun resetWallet()
    suspend fun addAccount(type: AccountType, label: String): Account
    suspend fun renameAccount(address: String, label: String)
    /** Remove an account by address from the active wallet. The wallet must keep at least one account. */
    suspend fun deleteAccount(address: String)
    suspend fun generateMnemonic(wordCount: Int = 12): String
    suspend fun validateMnemonic(mnemonic: String): Boolean
    suspend fun exportEncrypted(password: String): String
    suspend fun lockWallet()
    suspend fun unlockWallet(password: String): Boolean
    fun isUnlocked(): Boolean
    /**
     * Signs the transaction whose SignBytes hash to-be-signed is [signingMessageHex]
     * (a 32-byte SHA-256 hex returned by the node alongside the raw_transaction)
     * and returns the broadcast-ready signed_raw_transaction hex.
     */
    suspend fun signRawTransaction(
        rawHex: String,
        signingMessageHex: String,
        fromAddress: String,
    ): String

    // M8 — Settings & Security
    /** Throws [com.andrutstudio.velora.data.local.WrongPasswordException] if oldPassword is wrong. */
    suspend fun changePassword(oldPassword: String, newPassword: String)
    /** Re-derives all account addresses for the new network. Wallet must be unlocked. */
    suspend fun setNetwork(network: Network)
    /** Decrypts and returns mnemonic. Always re-authenticates with password. */
    suspend fun revealMnemonic(password: String): String
    fun setBiometricEnabled(enabled: Boolean)
    fun isBiometricEnabled(): Boolean
}
