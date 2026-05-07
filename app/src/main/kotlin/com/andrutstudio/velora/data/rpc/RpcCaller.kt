package com.andrutstudio.velora.data.rpc

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import com.andrutstudio.velora.data.rpc.model.RpcRequest
import com.andrutstudio.velora.domain.model.Amount
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RpcCaller @Inject constructor(
    private val service: PactusRpcService,
    private val networkProvider: NetworkProvider,
    private val gson: Gson,
) {
    private val idCounter = AtomicInteger(1)

    suspend fun getBlockHeight(): Long {
        val result = call("pactus.blockchain.get_blockchain_info", emptyMap<String, Any>())
        return result.asJsonObjectOrNull()?.get("last_block_height")?.asLongSafe() ?: 0L
    }

    suspend fun getAccount(address: String): AccountRpcData {
        val result = call("pactus.blockchain.get_account", mapOf("address" to address))
        val obj = result.asJsonObjectOrNull()
        val account = obj?.getAsJsonObject("account")
        
        return AccountRpcData(
            address = account?.get("address")?.asStringSafe() ?: address,
            balance = Amount.fromNanoPac(account?.get("balance")?.asLongSafe() ?: 0L),
            sequence = account?.get("sequence")?.asIntSafe() ?: 0,
        )
    }

    suspend fun calculateFee(amountNanoPac: Long): Amount {
        val params = mapOf(
            "amount" to amountNanoPac,
            "payload_type" to 1
        )
        val result = call("pactus.transaction.calculate_fee", params)
        
        val fee = if (result.isJsonPrimitive) {
            result.asLongSafe() ?: MIN_FEE_NANO_PAC
        } else {
            val obj = result.asJsonObjectOrNull()
            obj?.get("fee")?.asLongSafe() ?: MIN_FEE_NANO_PAC
        }
        return Amount.fromNanoPac(fee)
    }

    suspend fun getRawTransferTransaction(
        lockTime: Long,
        sender: String,
        receiver: String,
        amountNanoPac: Long,
        feeNanoPac: Long,
        memo: String?,
    ): RawTransferRpcData {
        val params = mutableMapOf<String, Any>(
            "lock_time" to lockTime,
            "sender" to sender,
            "receiver" to receiver,
            "amount" to amountNanoPac,
            "fee" to feeNanoPac
        )
        if (!memo.isNullOrBlank()) params["memo"] = memo

        Log.d("RpcCaller", "get_raw_transfer_transaction params: $params")
        val result = call("pactus.transaction.get_raw_transfer_transaction", params)
        Log.d("RpcCaller", "get_raw_transfer_transaction result: $result")
        
        val obj = result.asJsonObjectOrNull()
        val rawTx = obj?.get("transaction")?.asStringSafe() 
            ?: obj?.get("raw_transaction")?.asStringSafe()
            ?: result.asStringSafe()
            
        val hash = obj?.get("id")?.asStringSafe() 
            ?: obj?.get("hash")?.asStringSafe() 
            ?: obj?.get("transaction_id")?.asStringSafe()
        
        return RawTransferRpcData(
            rawTransactionHex = rawTx ?: error("Missing transaction data in RPC response"),
            signingHashHex = hash ?: "0000000000000000000000000000000000000000000000000000000000000000"
        )
    }

    suspend fun getRawBondTransaction(
        lockTime: Long,
        sender: String,
        receiver: String,
        stakeNanoPac: Long,
        publicKey: String?,
        feeNanoPac: Long,
        memo: String?,
    ): RawTransferRpcData {
        val params = mutableMapOf<String, Any>(
            "lock_time" to lockTime,
            "sender" to sender,
            "receiver" to receiver,
            "stake" to stakeNanoPac,
            "fee" to feeNanoPac
        )
        if (!publicKey.isNullOrBlank()) params["public_key"] = publicKey
        if (!memo.isNullOrBlank()) params["memo"] = memo

        val result = call("pactus.transaction.get_raw_bond_transaction", params)
        val obj = result.asJsonObjectOrNull()
        
        val rawTx = obj?.get("transaction")?.asStringSafe() 
            ?: obj?.get("raw_transaction")?.asStringSafe()
            ?: result.asStringSafe()
            
        val hash = obj?.get("id")?.asStringSafe() 
            ?: obj?.get("hash")?.asStringSafe() 
            ?: obj?.get("transaction_id")?.asStringSafe()

        return RawTransferRpcData(
            rawTransactionHex = rawTx ?: error("Missing transaction data in RPC response"),
            signingHashHex = hash ?: "0000000000000000000000000000000000000000000000000000000000000000"
        )
    }

    suspend fun getRawUnbondTransaction(
        lockTime: Long,
        validator: String,
        memo: String?,
    ): RawTransferRpcData {
        val params = mutableMapOf<String, Any>(
            "lock_time" to lockTime,
            "validator" to validator
        )
        if (!memo.isNullOrBlank()) params["memo"] = memo

        val result = call("pactus.transaction.get_raw_unbond_transaction", params)
        val obj = result.asJsonObjectOrNull()
        
        val rawTx = obj?.get("transaction")?.asStringSafe() 
            ?: obj?.get("raw_transaction")?.asStringSafe()
            ?: result.asStringSafe()
            
        val hash = obj?.get("id")?.asStringSafe() 
            ?: obj?.get("hash")?.asStringSafe() 
            ?: obj?.get("transaction_id")?.asStringSafe()

        return RawTransferRpcData(
            rawTransactionHex = rawTx ?: error("Missing transaction data in RPC response"),
            signingHashHex = hash ?: "0000000000000000000000000000000000000000000000000000000000000000"
        )
    }

    suspend fun broadcastTransaction(signedHex: String): String {
        val params = mapOf("signed_raw_transaction" to signedHex)
        Log.d("RpcCaller", "broadcast_transaction params: $params")
        val result = call("pactus.transaction.broadcast_transaction", params)
        Log.d("RpcCaller", "broadcast_transaction result: $result")

        return if (result.isJsonPrimitive) {
            result.asStringSafe() ?: error("Empty broadcast response")
        } else {
            val obj = result.asJsonObjectOrNull()
            obj?.get("hash")?.asStringSafe() 
                ?: obj?.get("id")?.asStringSafe() 
                ?: error("No transaction hash in broadcast response")
        }
    }

    suspend fun getBlock(height: Long): BlockRpcData {
        val result = call(
            "pactus.blockchain.get_block",
            mapOf("height" to height, "verbosity" to 2),
        )
        val obj = result.asJsonObjectOrNull()
        val blockTime = obj?.get("block_time")?.asLongSafe() ?: 0L
        val txArray = obj?.getAsJsonArray("txs") ?: return BlockRpcData(height, blockTime, emptyList())
        val txs = txArray.mapNotNull { el ->
            if (el.isJsonObject) {
                runCatching { parseTxElement(el.asJsonObject, blockTime, height) }.getOrNull()
            } else null
        }
        return BlockRpcData(height, blockTime, txs)
    }

    suspend fun getTransaction(txId: String): TxRpcData? {
        return runCatching {
            val result = call(
                "pactus.blockchain.get_transaction",
                mapOf("id" to txId, "verbosity" to 2),
            )
            val obj = result.asJsonObjectOrNull()
            val blockHeight = obj?.get("block_height")?.asLongSafe() ?: 0L
            val blockTime = obj?.get("block_time")?.asLongSafe() ?: 0L
            val txObj = obj?.getAsJsonObject("transaction") ?: return@runCatching null
            parseTxElement(txObj, blockTime, blockHeight)
        }.getOrNull()
    }

    private fun parseTxElement(obj: JsonObject, blockTime: Long, blockHeight: Long): TxRpcData {
        val id = obj.get("hash")?.asStringSafe() ?: obj.get("id")?.asStringSafe() ?: ""
        val typeStr = obj.get("type")?.asStringSafe() ?: "PAYLOAD_TRANSFER"
        val fee = obj.get("fee")?.asLongSafe() ?: 0L
        
        // Robust memo extraction: handles null, empty, and blank strings
        val memo = obj.get("memo")?.asStringSafe()?.trim()?.takeIf { it.isNotEmpty() }

        val payload = obj.getAsJsonObject("payload")
        val sender = payload?.get("sender")?.asStringSafe()
        val receiver = payload?.get("receiver")?.asStringSafe()
        val amount = payload?.get("amount")?.asLongSafe() ?: 0L
        return TxRpcData(
            id = id,
            typeStr = typeStr,
            sender = sender,
            receiver = receiver,
            amountNanoPac = amount,
            feeNanoPac = fee,
            memo = memo,
            blockTime = blockTime,
            blockHeight = blockHeight,
        )
    }

    private suspend fun call(method: String, params: Any): JsonElement {
        val urls = networkProvider.getCurrentUrls()
        var lastException: Exception = RpcException(-1, "No endpoints configured")
        for (url in urls) {
            try {
                val request = RpcRequest(
                    method = method,
                    params = params,
                    id = idCounter.getAndIncrement(),
                )
                val response = service.call(url, request)
                if (response.error != null) {
                    throw RpcException(response.error.code, response.error.message)
                }
                return response.result ?: throw RpcException(-1, "Empty result for method: $method")
            } catch (e: CancellationException) {
                throw e
            } catch (e: RpcException) {
                throw e
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException
    }

    // --- Safe Helpers to prevent NPE and ClassCastException ---

    private fun JsonElement?.asJsonObjectOrNull(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonElement?.asStringSafe(): String? =
        if (this != null && this.isJsonPrimitive && !this.isJsonNull) {
            runCatching { this.asString }.getOrNull()
        } else null

    private fun JsonElement?.asLongSafe(): Long? =
        if (this != null && this.isJsonPrimitive && !this.isJsonNull) {
            runCatching { this.asLong }.getOrNull()
        } else null

    private fun JsonElement?.asIntSafe(): Int? =
        if (this != null && this.isJsonPrimitive && !this.isJsonNull) {
            runCatching { this.asInt }.getOrNull()
        } else null
}

data class AccountRpcData(
    val address: String,
    val balance: Amount,
    val sequence: Int,
)

data class RawTransferRpcData(
    val rawTransactionHex: String,
    val signingHashHex: String,
)

const val MIN_FEE_NANO_PAC = 10_000_000L // 0.01 PAC

class RpcException(val code: Int, override val message: String) : Exception(message)

data class TxRpcData(
    val id: String,
    val typeStr: String,
    val sender: String?,
    val receiver: String?,
    val amountNanoPac: Long,
    val feeNanoPac: Long,
    val memo: String?,
    val blockTime: Long,
    val blockHeight: Long,
)

data class BlockRpcData(
    val height: Long,
    val blockTime: Long,
    val txs: List<TxRpcData>,
)
