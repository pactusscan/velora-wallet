package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val fromAddress: String,
    val toAddress: String?,
    val amountNanoPac: Long,
    val feeNanoPac: Long,
    val memo: String?,
    val blockHeight: Long,
    val blockTime: Long,
    val status: String,
    val direction: Int = 0,
    /** Which of our wallet addresses is involved in this TX. */
    val involvedAddress: String,
    /** Whether the user has been notified about this transaction. */
    val isNotified: Boolean = false,
)
