package com.andrutstudio.velora.domain.repository

import kotlinx.coroutines.flow.Flow
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.RawTransaction
import com.andrutstudio.velora.domain.model.Transaction

interface BlockchainRepository {
    suspend fun getBalance(address: String): Amount
    suspend fun getBlockHeight(): Long
    suspend fun getTransactionHistory(address: String, page: Int): List<Transaction>
    fun observeTransactionHistory(addresses: List<String>): Flow<List<Transaction>>
    suspend fun calculateFee(amount: Amount): Amount
    suspend fun buildTransferTransaction(
        from: String,
        to: String,
        amount: Amount,
        fee: Amount,
        memo: String?,
    ): RawTransaction
    suspend fun buildBondTransaction(
        from: String,
        to: String,
        stake: Amount,
        publicKey: String?,
        fee: Amount,
        memo: String?,
    ): RawTransaction
    suspend fun buildUnbondTransaction(
        validator: String,
        memo: String?,
    ): RawTransaction
    suspend fun broadcastTransaction(signedHex: String): String
    suspend fun getTransaction(txId: String): Transaction
    fun triggerSync()
}
