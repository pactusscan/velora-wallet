package com.andrutstudio.velora.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import com.andrutstudio.velora.data.crypto.PactusKeyManager
import com.andrutstudio.velora.data.local.BiometricHelper
import com.andrutstudio.velora.data.local.VaultStorage
import com.andrutstudio.velora.data.local.WrongPasswordException
import com.andrutstudio.velora.data.local.model.MultiVault
import com.andrutstudio.velora.data.local.model.VaultAccount
import com.andrutstudio.velora.data.local.model.WalletVault
import com.andrutstudio.velora.data.rpc.NetworkProvider
import com.andrutstudio.velora.domain.model.Account
import com.andrutstudio.velora.domain.model.AccountType
import com.andrutstudio.velora.domain.model.Network
import com.andrutstudio.velora.domain.model.Wallet
import com.andrutstudio.velora.domain.repository.WalletRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val vaultStorage: VaultStorage,
    private val keyManager: PactusKeyManager,
    private val networkProvider: NetworkProvider,
    private val biometricHelper: BiometricHelper,
) : WalletRepository {

    // Active wallet — null when locked or no wallet exists.
    private val _walletState = MutableStateFlow<Wallet?>(null)
    // All wallets in storage.
    private val _walletsState = MutableStateFlow<List<Wallet>>(emptyList())

    // Unlocked mnemonic (or imported private-key hex) for the ACTIVE wallet only.
    // Switching wallets clears this.
    private var unlockedMnemonic: String? = null

    override fun observeWallet(): Flow<Wallet?> = _walletState
    override fun observeWallets(): Flow<List<Wallet>> = _walletsState

    override suspend fun hasWallet(): Boolean = withContext(Dispatchers.IO) {
        vaultStorage.hasVault()
    }

    override suspend fun loadLockedWallet() = withContext(Dispatchers.IO) {
        val multi = vaultStorage.loadMultiVault() ?: return@withContext
        val active = multi.wallets.find { it.id == multi.activeWalletId } ?: multi.wallets.firstOrNull()
        if (active != null) {
            networkProvider.setNetwork(Network.valueOf(active.network))
            _walletState.value = active.toDomain()
        }
        _walletsState.value = multi.wallets.map { it.toDomain() }
    }

    override suspend fun createWallet(
        name: String,
        mnemonic: String,
        password: String,
    ): Wallet = withContext(Dispatchers.IO) {
        val passwordChars = password.toCharArray()
        val network = Network.MAINNET

        val firstKey = keyManager.deriveAccount(mnemonic, network, AccountType.ED25519, 0)

        val (encryptedMnemonic, salt) = vaultStorage.encryptMnemonic(mnemonic, passwordChars)
        passwordChars.fill(' ')

        val vault = WalletVault(
            id = UUID.randomUUID().toString(),
            name = name,
            encryptedMnemonic = encryptedMnemonic,
            salt = salt,
            network = network.name,
            accounts = listOf(
                VaultAccount(
                    address = firstKey.address,
                    label = "Account 1",
                    type = AccountType.ED25519.name,
                    derivationPath = firstKey.derivationPath,
                    derivationIndex = 0,
                )
            ),
            createdAt = System.currentTimeMillis(),
        )

        vaultStorage.addWallet(vault)
        unlockedMnemonic = mnemonic
        publishState()

        vault.toDomain()
    }

    override suspend fun restoreWallet(
        name: String,
        mnemonic: String,
        password: String,
    ): Wallet = createWallet(name, mnemonic, password)

    override suspend fun importPrivateKey(
        name: String,
        privateKeyHex: String,
        password: String,
        type: AccountType,
    ): Wallet = withContext(Dispatchers.IO) {
        val passwordChars = password.toCharArray()
        val network = networkProvider.current
        val privateKeyBytes = privateKeyHex.hexDecode()
        val address = keyManager.addressFromPrivateKey(privateKeyBytes, network, type)
        privateKeyBytes.fill(0)

        val (encryptedKey, salt) = vaultStorage.encryptMnemonic(privateKeyHex, passwordChars)
        passwordChars.fill(' ')

        val vault = WalletVault(
            id = UUID.randomUUID().toString(),
            name = name,
            encryptedMnemonic = encryptedKey,
            salt = salt,
            network = network.name,
            accounts = listOf(
                VaultAccount(
                    address = address,
                    label = "Imported Account",
                    type = type.name,
                    derivationPath = "m/imported",
                    derivationIndex = 0,
                )
            ),
            createdAt = System.currentTimeMillis(),
            isPrivateKeyImport = true,
        )
        vaultStorage.addWallet(vault)
        unlockedMnemonic = privateKeyHex
        publishState()

        vault.toDomain()
    }

    override suspend fun watchWallet(
        name: String,
        address: String,
    ): Wallet = withContext(Dispatchers.IO) {
        val network = Network.MAINNET
        val vault = WalletVault(
            id = UUID.randomUUID().toString(),
            name = name,
            encryptedMnemonic = "",
            salt = "",
            network = network.name,
            accounts = listOf(
                VaultAccount(
                    address = address,
                    label = "Watched Account",
                    type = AccountType.ED25519.name,
                    derivationPath = "m/watched",
                    derivationIndex = 0,
                )
            ),
            createdAt = System.currentTimeMillis(),
            isWatchOnly = true,
        )
        vaultStorage.addWallet(vault)
        publishState()
        vault.toDomain()
    }

    override suspend fun selectWallet(walletId: String) = withContext(Dispatchers.IO) {
        val multi = vaultStorage.loadMultiVault() ?: return@withContext
        if (multi.activeWalletId == walletId) return@withContext
        if (multi.wallets.none { it.id == walletId }) return@withContext
        vaultStorage.selectWallet(walletId)
        // Switching wallets requires re-authentication.
        unlockedMnemonic = null
        val active = multi.wallets.first { it.id == walletId }
        networkProvider.setNetwork(Network.valueOf(active.network))
        publishState()
    }

    override suspend fun renameWallet(walletId: String, name: String) = withContext(Dispatchers.IO) {
        val multi = vaultStorage.loadMultiVault() ?: return@withContext
        val wallet = multi.wallets.find { it.id == walletId } ?: return@withContext
        val updated = wallet.copy(name = name)
        vaultStorage.saveVault(updated)
        publishState()
    }

    override suspend fun deleteWallet(walletId: String) = withContext(Dispatchers.IO) {
        val multi = vaultStorage.loadMultiVault() ?: return@withContext
        if (multi.activeWalletId == walletId) {
            unlockedMnemonic = null
        }
        vaultStorage.removeWallet(walletId)
        publishState()
    }

    override suspend fun resetWallet() = withContext(Dispatchers.IO) {
        vaultStorage.deleteVault()
        unlockedMnemonic = null
        _walletState.value = null
        _walletsState.value = emptyList()
    }

    override suspend fun addAccount(type: AccountType, label: String): Account =
        withContext(Dispatchers.IO) {
            val mnemonic = requireUnlocked()
            val vault = vaultStorage.loadVault() ?: error("No vault found")
            val network = Network.valueOf(vault.network)

            val nextIndex = vault.accounts.count { it.type == type.name }
            val derived = keyManager.deriveAccount(mnemonic, network, type, nextIndex)

            val updatedVault = vault.copy(
                accounts = vault.accounts + VaultAccount(
                    address = derived.address,
                    label = label,
                    type = type.name,
                    derivationPath = derived.derivationPath,
                    derivationIndex = nextIndex,
                )
            )
            vaultStorage.saveVault(updatedVault)
            publishState()

            Account(
                address = derived.address,
                label = label,
                type = type,
                derivationIndex = nextIndex,
            )
        }

    override suspend fun renameAccount(address: String, label: String) =
        withContext(Dispatchers.IO) {
            val vault = vaultStorage.loadVault() ?: return@withContext
            val updated = vault.copy(
                accounts = vault.accounts.map {
                    if (it.address == address) it.copy(label = label) else it
                }
            )
            vaultStorage.saveVault(updated)
            publishState()
        }

    override suspend fun deleteAccount(address: String) = withContext(Dispatchers.IO) {
        val vault = vaultStorage.loadVault() ?: return@withContext
        if (vault.accounts.size <= 1) error("Cannot delete the last account in a wallet")
        val updated = vault.copy(accounts = vault.accounts.filterNot { it.address == address })
        vaultStorage.saveVault(updated)
        publishState()
    }

    override suspend fun generateMnemonic(wordCount: Int): String =
        withContext(Dispatchers.IO) {
            val strength = if (wordCount == 12) 128 else 256
            wallet.core.jni.HDWallet(strength, "").mnemonic()
        }

    override suspend fun validateMnemonic(mnemonic: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching { wallet.core.jni.Mnemonic.isValid(mnemonic) }.getOrDefault(false)
        }

    override suspend fun exportEncrypted(password: String): String =
        withContext(Dispatchers.IO) {
            vaultStorage.loadVault()?.toString() ?: error("No vault")
        }

    override suspend fun lockWallet() {
        unlockedMnemonic = null
        _walletState.value = _walletState.value?.copy()
    }

    override suspend fun unlockWallet(password: String): Boolean =
        withContext(Dispatchers.IO) {
            val vault = vaultStorage.loadVault() ?: return@withContext false
            if (vault.isWatchOnly) return@withContext true
            return@withContext try {
                val mnemonic = vaultStorage.decryptMnemonic(
                    vault.encryptedMnemonic,
                    vault.salt,
                    password.toCharArray(),
                )
                unlockedMnemonic = mnemonic
                
                // Fix legacy addresses if needed
                val network = Network.valueOf(vault.network)
                val updatedAccounts = vault.accounts.map { acc ->
                    val derived = keyManager.deriveAccount(mnemonic, network, AccountType.valueOf(acc.type), acc.derivationIndex)
                    acc.copy(address = derived.address)
                }
                if (updatedAccounts != vault.accounts) {
                    val updatedVault = vault.copy(accounts = updatedAccounts)
                    vaultStorage.saveVault(updatedVault)
                    _walletState.value = updatedVault.toDomain()
                } else {
                    _walletState.value = vault.toDomain()
                }

                true
            } catch (e: WrongPasswordException) {
                false
            }
        }

    override fun isUnlocked(): Boolean = unlockedMnemonic != null

    override suspend fun signRawTransaction(
        rawHex: String,
        signingMessageHex: String,
        fromAddress: String,
    ): String = withContext(Dispatchers.IO) {
        Log.d("Pactus", "Signing transaction: raw=$rawHex")
        // Official pactus-wallet (web) logic:
        // 1. rawTx from RPC starts with a flag (e.g. 0x02 for unsigned)
        // 2. Data to sign is everything from index 1 onwards
        // 3. Final signed tx is [0x00] + data_to_sign + signature + publicKey

        val rawBytes = rawHex.hexDecode()
        if (rawBytes.isEmpty()) error("Raw transaction hex is empty")
        
        // Prepare bytes to sign (removing the first byte/flag)
        val bytesToSign = rawBytes.copyOfRange(1, rawBytes.size)

        val secret = requireUnlocked()
        val vault = vaultStorage.loadVault() ?: error("No vault found")
        val vaultAccount = vault.accounts.find { it.address == fromAddress }
            ?: error("Account not found: $fromAddress")
        val network = Network.valueOf(vault.network)
        val type = AccountType.valueOf(vaultAccount.type)

        val (signatureBytes, publicKeyBytes) = if (vault.isPrivateKeyImport) {
            val privateKeyBytes = secret.hexDecode()
            val sig = keyManager.sign(bytesToSign, privateKeyBytes, AccountType.ED25519)
            val pub = keyManager.publicKeyBytesFromPrivateKey(privateKeyBytes)
            privateKeyBytes.fill(0)
            sig to pub
        } else {
            val privateKeyBytes = keyManager.derivePrivateKeyBytes(secret, network, type, vaultAccount.derivationIndex)
            val sig = keyManager.sign(bytesToSign, privateKeyBytes, type)
            val pub = keyManager.deriveAccount(secret, network, type, vaultAccount.derivationIndex).publicKeyBytes
            privateKeyBytes.fill(0)
            sig to pub
        }

        // Construct final signed transaction: [0x00] + [bytesToSign] + [Signature] + [PublicKey]
        val signedTxHeader = byteArrayOf(0x00) // 0x00 indicates signed
        val finalHex = (signedTxHeader + bytesToSign + signatureBytes + publicKeyBytes).hexEncode()

        Log.d("Pactus", "Final signed transaction: $finalHex")
        finalHex
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) =
        withContext(Dispatchers.IO) {
            val vault = vaultStorage.loadVault() ?: error("No vault")
            val mnemonic = vaultStorage.decryptMnemonic(
                vault.encryptedMnemonic, vault.salt, oldPassword.toCharArray()
            )
            val (newEncrypted, newSalt) = vaultStorage.encryptMnemonic(mnemonic, newPassword.toCharArray())
            vaultStorage.saveVault(vault.copy(encryptedMnemonic = newEncrypted, salt = newSalt))
            publishState()
        }

    override suspend fun setNetwork(network: Network) = withContext(Dispatchers.IO) {
        val vault = vaultStorage.loadVault() ?: error("No vault")
        if (Network.valueOf(vault.network) == network) return@withContext
        val mnemonic = requireUnlocked()
        val reDerived = vault.accounts.map { acc ->
            val type = AccountType.valueOf(acc.type)
            val derived = keyManager.deriveAccount(mnemonic, network, type, acc.derivationIndex)
            acc.copy(address = derived.address, derivationPath = derived.derivationPath)
        }
        val updated = vault.copy(network = network.name, accounts = reDerived)
        vaultStorage.saveVault(updated)
        publishState()
    }

    override suspend fun revealMnemonic(password: String): String = withContext(Dispatchers.IO) {
        val vault = vaultStorage.loadVault() ?: error("No vault")
        vaultStorage.decryptMnemonic(vault.encryptedMnemonic, vault.salt, password.toCharArray())
    }

    override fun setBiometricEnabled(enabled: Boolean) {
        vaultStorage.setBiometricEnabled(enabled)
        if (!enabled) {
            biometricHelper.clearStoredPassword()
        }
    }

    override fun isBiometricEnabled(): Boolean = vaultStorage.isBiometricEnabled()

    fun getUnlockedMnemonic(): String = requireUnlocked()

    private fun requireUnlocked(): String =
        unlockedMnemonic ?: error("Wallet is locked. Call unlockWallet() first.")

    private fun publishState() {
        val multi: MultiVault? = vaultStorage.loadMultiVault()
        if (multi == null) {
            _walletState.value = null
            _walletsState.value = emptyList()
        } else {
            _walletState.value = multi.wallets
                .find { it.id == multi.activeWalletId }
                ?.toDomain()
            _walletsState.value = multi.wallets.map { it.toDomain() }
        }
    }

    private fun String.hexDecode(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    private fun ByteArray.hexEncode(): String =
        joinToString("") { "%02x".format(it) }

    private fun WalletVault.toDomain(): Wallet {
        val network = Network.valueOf(network)
        return Wallet(
            id = id,
            name = name,
            network = network,
            accounts = accounts.mapNotNull {
                val type = runCatching { AccountType.valueOf(it.type) }.getOrNull() ?: return@mapNotNull null
                Account(
                    address = it.address,
                    label = it.label,
                    type = type,
                    derivationIndex = it.derivationIndex,
                )
            },
            createdAt = createdAt,
            isWatchOnly = isWatchOnly,
            isPrivateKeyImport = isPrivateKeyImport,
        )
    }
}
