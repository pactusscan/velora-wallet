package com.andrutstudio.velora.domain.model

import java.util.UUID

data class Wallet(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val accounts: List<Account> = emptyList(),
    val network: Network = Network.MAINNET,
    val createdAt: Long = System.currentTimeMillis(),
    val isWatchOnly: Boolean = false,
    val isPrivateKeyImport: Boolean = false,
)

data class Account(
    val address: String,
    val label: String,
    val type: AccountType,
    val derivationIndex: Int,
    val balanceNanoPac: Long = 0L,
)

/**
 * Account key types. [addressType] is the bech32m type byte of the address,
 * [signatureType] the one of secret1…/public1… key strings (they differ for BLS).
 */
enum class AccountType(val addressType: Byte, val signatureType: Int) {
    /** Ed25519, m/44'/coinType'/3'/index', address pc1r…, secret key SECRET1R…. */
    ED25519(3, 3),
    /** BLS12-381, m/12381'/coinType'/2'/index (index not hardened), address pc1z…, secret key SECRET1P…. */
    BLS(2, 1),
    /** Secp256k1, m/44'/coinType'/4'/index', address pc1y…, secret key SECRET1Y…. */
    SECP256K1(4, 4);

    companion object {
        fun fromSignatureType(type: Int): AccountType? = entries.firstOrNull { it.signatureType == type }
    }
}

enum class Network(
    val coinType: Int,
    val rpcUrl: String,
    val explorerUrl: String,
    val displayName: String,
    val hrp: String,
) {
    MAINNET(
        coinType = 21888,
        rpcUrl = "https://bootstrap1.pactus.org/jsonrpc",
        explorerUrl = "https://pactusscan.com",
        displayName = "Mainnet",
        hrp = "pc",
    ),
    TESTNET(
        coinType = 21777,
        rpcUrl = "https://testnet1.pactus.org/jsonrpc",
        explorerUrl = "https://phoenix.pactusscan.com",
        displayName = "Testnet",
        hrp = "tpc",
    ),
}

/** Immutable value type. All internal math in NanoPAC (Long). */
@JvmInline
value class Amount(val nanoPac: Long) {
    val pac: Double get() = nanoPac / 1_000_000_000.0
    operator fun plus(other: Amount) = Amount(nanoPac + other.nanoPac)
    operator fun minus(other: Amount) = Amount(nanoPac - other.nanoPac)
    operator fun compareTo(other: Amount) = nanoPac.compareTo(other.nanoPac)

    companion object {
        val ZERO = Amount(0L)
        fun fromPac(pac: Double) = Amount((pac * 1_000_000_000).toLong())
        fun fromNanoPac(nanoPac: Long) = Amount(nanoPac)
    }
}
