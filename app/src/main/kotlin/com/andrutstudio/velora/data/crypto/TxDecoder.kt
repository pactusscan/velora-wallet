package com.andrutstudio.velora.data.crypto

/**
 * Reads a raw transaction returned by the node, so the wallet can check what it
 * is about to sign instead of trusting the node. Mirrors txDecode.ts in the
 * Velora browser extension.
 *
 * Wire format, from pactus-project/pactus types/tx/tx.go:
 *   flags(1) | version(1) | lockTime(4, little-endian) | fee(varint)
 *   | memo(varString) | payloadType(1) | payload
 * Transfer payload: from(21) | to(21) | amount(varint)
 * Addresses are a type byte plus a 20-byte hash. Integers are unsigned LEB128.
 */
object TxDecoder {

    const val PAYLOAD_TRANSFER = 1

    private const val ADDRESS_SIZE = 21

    data class Transfer(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val from: ByteArray,
        val to: ByteArray,
        val amount: Long,
    )

    fun decodeTransfer(rawHex: String): Transfer {
        val reader = Reader(hexToBytes(rawHex))
        val header = reader.header()
        check(header.payloadType == PAYLOAD_TRANSFER) {
            "Not a transfer transaction (payload type ${header.payloadType})"
        }
        return Transfer(
            lockTime = header.lockTime,
            fee = header.fee,
            memo = header.memo,
            from = reader.take(ADDRESS_SIZE),
            to = reader.take(ADDRESS_SIZE),
            amount = reader.varint(),
        )
    }

    /**
     * Throws unless the raw transaction encodes exactly what the user approved.
     * The caller must not sign when this throws.
     */
    fun assertTransferMatches(
        rawHex: String,
        sender: String,
        receiver: String,
        amount: Long,
        fee: Long,
        memo: String?,
    ) {
        val tx = decodeTransfer(rawHex)
        throwOnMismatch(
            listOf(
                "sender" to addressMatches(tx.from, sender),
                "receiver" to addressMatches(tx.to, receiver),
                "amount" to (tx.amount == amount),
                "fee" to (tx.fee == fee),
                "memo" to (tx.memo == memo.orEmpty()),
            ),
        )
    }

    private fun throwOnMismatch(checks: List<Pair<String, Boolean>>) {
        val mismatch = checks.filterNot { it.second }.map { it.first }
        check(mismatch.isEmpty()) {
            "Transaction from node does not match approved parameters: ${mismatch.joinToString()}"
        }
    }

    private fun addressMatches(decoded: ByteArray, address: String): Boolean {
        val (_, type, hash) = runCatching { Bech32m.decode(address) }.getOrNull() ?: return false
        return decoded.contentEquals(byteArrayOf(type) + hash)
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Raw transaction hex has odd length" }
        return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private data class Header(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val payloadType: Int,
    )

    private class Reader(private val bytes: ByteArray) {
        private var offset = 0

        fun u8(): Int {
            check(offset < bytes.size) { "Unexpected end of transaction bytes" }
            return bytes[offset++].toInt() and 0xFF
        }

        fun take(count: Int): ByteArray {
            check(count >= 0 && offset + count <= bytes.size) { "Unexpected end of transaction bytes" }
            return bytes.copyOfRange(offset, offset + count).also { offset += count }
        }

        fun varint(): Long {
            var value = 0L
            for (shift in 0..56 step 7) {
                val byte = u8()
                value = value or ((byte and 0x7F).toLong() shl shift)
                if (byte and 0x80 == 0) return value
            }
            error("Transaction integer is too large")
        }

        private fun u32le(): Long =
            (0..3).fold(0L) { value, index -> value or (u8().toLong() shl (8 * index)) }

        fun header(): Header {
            u8() // flags: not needed, the unsigned tx from the node marks itself
            u8() // version
            val lockTime = u32le()
            val fee = varint()
            val memo = take(varint().toInt()).toString(Charsets.UTF_8)
            return Header(lockTime, fee, memo, u8())
        }
    }
}
