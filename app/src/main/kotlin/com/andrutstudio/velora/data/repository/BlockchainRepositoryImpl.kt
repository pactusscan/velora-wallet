package com.andrutstudio.velora.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.andrutstudio.velora.data.local.db.TransactionDao
import com.andrutstudio.velora.data.local.db.TransactionEntity
import com.andrutstudio.velora.data.rpc.PactusScanApiService
import com.andrutstudio.velora.data.rpc.RpcCaller
import com.andrutstudio.velora.data.rpc.RpcException
import com.andrutstudio.velora.data.sync.TxSyncWorker
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.RawTransaction
import com.andrutstudio.velora.domain.model.Transaction
import com.andrutstudio.velora.domain.model.TransactionStatus
import com.andrutstudio.velora.domain.model.TransactionType
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockchainRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rpc: RpcCaller,
    private val pactusScan: PactusScanApiService,
    private val transactionDao: TransactionDao,
) : BlockchainRepository {

    override suspend fun getBalance(address: String): Amount = withContext(Dispatchers.IO) {
        try {
            rpc.getAccount(address).balance
        } catch (e: CancellationException) {
            throw e
        } catch (_: RpcException) {
            // RPC "account not found" — address hasn't received any tx yet
            Amount.ZERO
        }
        // Network/IO exceptions propagate so HomeViewModel can show the error banner
    }

    override suspend fun getBlockHeight(): Long = withContext(Dispatchers.IO) {
        rpc.getBlockHeight()
    }

    override suspend fun calculateFee(amount: Amount): Amount = withContext(Dispatchers.IO) {
        rpc.calculateFee(amount.nanoPac)
    }

    override suspend fun buildTransferTransaction(
        from: String,
        to: String,
        amount: Amount,
        fee: Amount,
        memo: String?,
    ): RawTransaction = withContext(Dispatchers.IO) {
        val lockTime = rpc.getBlockHeight()
        val raw = rpc.getRawTransferTransaction(
            lockTime = lockTime,
            sender = from,
            receiver = to,
            amountNanoPac = amount.nanoPac,
            feeNanoPac = fee.nanoPac,
            memo = memo,
        )
        RawTransaction(
            hex = raw.rawTransactionHex,
            signingMessageHex = raw.signingHashHex,
            lockTime = lockTime,
            fee = fee,
        )
    }

    override suspend fun buildBondTransaction(
        from: String,
        to: String,
        stake: Amount,
        publicKey: String?,
        fee: Amount,
        memo: String?
    ): RawTransaction = withContext(Dispatchers.IO) {
        val lockTime = rpc.getBlockHeight()
        val raw = rpc.getRawBondTransaction(
            lockTime = lockTime,
            sender = from,
            receiver = to,
            stakeNanoPac = stake.nanoPac,
            publicKey = publicKey,
            feeNanoPac = fee.nanoPac,
            memo = memo,
        )
        RawTransaction(
            hex = raw.rawTransactionHex,
            signingMessageHex = raw.signingHashHex,
            lockTime = lockTime,
            fee = fee,
        )
    }

    override suspend fun buildUnbondTransaction(
        validator: String,
        memo: String?
    ): RawTransaction = withContext(Dispatchers.IO) {
        val lockTime = rpc.getBlockHeight() + 1
        val raw = rpc.getRawUnbondTransaction(
            lockTime = lockTime,
            validator = validator,
            memo = memo,
        )
        RawTransaction(
            hex = raw.rawTransactionHex,
            signingMessageHex = raw.signingHashHex,
            lockTime = lockTime,
            fee = Amount.ZERO,
        )
    }

    override suspend fun broadcastTransaction(signedHex: String): String =
        withContext(Dispatchers.IO) { rpc.broadcastTransaction(signedHex) }

    override suspend fun getTransactionHistory(address: String, page: Int): List<Transaction> = withContext(Dispatchers.IO) {
        try {
            val response = pactusScan.getTransactions(address, page = page, limit = 20)
            val zeroAddress = "000000000000000000000000000000000000000000"
            val entities = response.txs
                .filter { it.sender != zeroAddress }
                .map { tx ->
                    TransactionEntity(
                        id = tx.id,
                        type = tx.payloadType.toTransactionType().name,
                        fromAddress = tx.sender ?: "",
                        toAddress = tx.receiver,
                        amountNanoPac = tx.amountNanoPac,
                        feeNanoPac = tx.feeNanoPac,
                        memo = tx.memo?.trim()?.takeIf { it.isNotEmpty() },
                        blockHeight = tx.blockHeight,
                        blockTime = tx.blockTime,
                        status = TransactionStatus.CONFIRMED.name,
                        direction = tx.direction,
                        involvedAddress = address
                    )
                }
            transactionDao.insertAll(entities)
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            // Fallback to local cache if offline or API fails
            emptyList()
        }
    }

    override fun observeTransactionHistory(addresses: List<String>): Flow<List<Transaction>> =
        transactionDao.observeByAddresses(addresses).map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun getTransaction(txId: String): Transaction = withContext(Dispatchers.IO) {
        val cached = transactionDao.getById(txId)
        if (cached != null && cached.blockTime > 0) {
            return@withContext cached.toDomain()
        }

        try {
            val tx = pactusScan.getTransaction(txId)
            TransactionEntity(
                id = tx.id,
                type = tx.payloadType.toTransactionType().name,
                fromAddress = tx.sender ?: "",
                toAddress = tx.receiver,
                amountNanoPac = tx.amountNanoPac,
                feeNanoPac = tx.feeNanoPac,
                memo = tx.memo?.trim()?.takeIf { it.isNotEmpty() },
                blockHeight = tx.blockHeight,
                blockTime = tx.blockTime,
                status = TransactionStatus.CONFIRMED.name,
                direction = tx.direction,
                involvedAddress = tx.sender ?: tx.receiver ?: "",
            ).also { transactionDao.insertIfNew(it) }.toDomain()
        } catch (e: Exception) {
            error("Transaction not found: $txId")
        }
    }

    override suspend fun getValidatorPeer(address: String): com.andrutstudio.velora.data.rpc.PeerDetailResponse = withContext(Dispatchers.IO) {
        pactusScan.getValidatorPeer(address)
    }

    override fun triggerSync() {
        val request = OneTimeWorkRequestBuilder<TxSyncWorker>()
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "one_time_tx_sync",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun TransactionEntity.toDomain() = Transaction(
        id = id,
        type = TransactionType.valueOf(type),
        from = fromAddress,
        to = toAddress,
        amount = Amount.fromNanoPac(amountNanoPac),
        fee = Amount.fromNanoPac(feeNanoPac),
        memo = memo,
        blockHeight = blockHeight,
        blockTime = blockTime,
        status = TransactionStatus.valueOf(status),
        direction = direction,
        involvedAddress = involvedAddress,
    )

    private fun Int.toTransactionType(): TransactionType = when (this) {
        0 -> TransactionType.SELF
        1 -> TransactionType.TRANSFER
        2 -> TransactionType.BOND
        3 -> TransactionType.SORTITION
        4 -> TransactionType.UNBOND
        5 -> TransactionType.WITHDRAW
        6 -> TransactionType.BATCH_TRANSFER
        else -> TransactionType.TRANSFER
    }

    private fun String.toTransactionType(): TransactionType = when {
        contains("UNBOND", ignoreCase = true) -> TransactionType.UNBOND
        contains("BOND", ignoreCase = true) -> TransactionType.BOND
        contains("SORTITION", ignoreCase = true) -> TransactionType.SORTITION
        else -> TransactionType.TRANSFER
    }
}
