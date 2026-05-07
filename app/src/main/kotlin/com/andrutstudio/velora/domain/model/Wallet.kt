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

enum class AccountType {
    /** Ed25519 — m/44'/coinType'/3'/index' — Ed25519 account (pc1r…). */
    ED25519,
    /** BLS — m/12381'/coinType'/2'/index' — BLS account (pc1z…). */
    BLS,
}

enum class Network(
    val coinType: Int,
    val rpcUrl: String,
    val displayName: String,
    val hrp: String,
) {
    MAINNET(
        coinType = 21888,
        rpcUrl = "https://bootstrap1.pactus.org/jsonrpc",
        displayName = "Mainnet",
        hrp = "pc",
    ),
    TESTNET(
        coinType = 21777,
        rpcUrl = "https://testnet1.pactus.org/jsonrpc",
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
