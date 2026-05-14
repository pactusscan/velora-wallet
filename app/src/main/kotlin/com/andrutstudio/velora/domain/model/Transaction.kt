package com.andrutstudio.velora.domain.model

data class Transaction(
    val id: String,
    val type: TransactionType,
    val from: String,
    val to: String?,
    val amount: Amount,
    val fee: Amount,
    val memo: String?,
    val blockHeight: Long,
    val blockTime: Long,
    val status: TransactionStatus,
    val direction: Int = 0, // 0: Self, 1: In, 2: Out
    val involvedAddress: String = "",
)

enum class TransactionType { 
    SELF, 
    TRANSFER, 
    BOND, 
    SORTITION, 
    UNBOND, 
    WITHDRAW, 
    BATCH_TRANSFER 
}

enum class TransactionStatus { PENDING, CONFIRMED, FAILED }

data class RawTransaction(
    /** Hex of the unsigned transaction body returned by the node. */
    val hex: String,
    /**
     * Hex of the 32-byte SHA-256 hash that the wallet must sign. Pactus exposes
     * this via the `id` field of `pactus.transaction.get_raw_transfer_transaction`
     * so clients don't have to recompute SignBytes locally.
     */
    val signingMessageHex: String,
    val lockTime: Long,
    val fee: Amount,
)
