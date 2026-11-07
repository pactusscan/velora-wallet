package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.AnchorRequest
import com.andrutstudio.velora.domain.model.Delegation

/**
 * Reads a raw transaction returned by the node, so the wallet can check what it
 * is about to sign instead of trusting the node. Mirrors txDecode.ts in the
 * Velora browser extension.
 *
 * Wire format, from pactus-project/pactus types/tx/tx.go:
 *   flags(1) | version(1) | lockTime(4, little-endian) | fee(varint)
 *   | memo(varString) | payloadType(1) | payload
 * Transfer payload: from(21) | to(21) | amount(varint)
 * Anchor payload (PIP-50): from(21) | action(1), then for Set only
 *   hashLen(1) | hash | uriLen(1) | uri | anchorType(1) | deposit(varint)
 * Bond payload: from(21) | to(21) | keyLen(varint, 0 or 96) | key | stake(varint),
 *   then with flag 0x04 (PIP-49) owner(21) | share(varint) | expiry(u32 LE)
 * Unbond payload: validator(21), then with flag 0x04 owner(21)
 * Addresses are a type byte plus a 20-byte hash. Integers are unsigned LEB128.
 */
object TxDecoder {

    const val PAYLOAD_TRANSFER = 1
    const val PAYLOAD_BOND = 2
    const val PAYLOAD_UNBOND = 4
    const val PAYLOAD_ANCHOR = 7

    private const val ACTION_SET = 0
    private const val ACTION_DELETE = 1

    private const val ADDRESS_SIZE = 21
    private const val BLS_PUBLIC_KEY_SIZE = 96

    data class Bond(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val from: ByteArray,
        val to: ByteArray,
        val publicKey: String, // hex, empty without a key
        val stake: Long,
        val delegateOwner: ByteArray?, // non-null for a PIP-49 delegated bond
        val delegateShare: Long,
        val delegateExpiry: Long,
    )

    data class Unbond(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val validator: ByteArray,
        val delegateOwner: ByteArray?, // the Stake Owner signs a delegated unbond
    )

    fun decodeBond(rawHex: String): Bond {
        val reader = Reader(hexToBytes(rawHex))
        val header = reader.header()
        check(header.payloadType == PAYLOAD_BOND) { "Not a bond transaction (payload type ${header.payloadType})" }
        val from = reader.take(ADDRESS_SIZE)
        val to = reader.take(ADDRESS_SIZE)
        val keySize = reader.varint().toInt()
        check(keySize == 0 || keySize == BLS_PUBLIC_KEY_SIZE) { "Invalid public key size $keySize" }
        val key = reader.take(keySize).joinToString("") { "%02x".format(it) }
        val stake = reader.varint()
        var tx = Bond(header.lockTime, header.fee, header.memo, from, to, key, stake, null, 0, 0)
        if (header.flags and SignedTx.FLAG_WITH_DELEGATION != 0) {
            tx = tx.copy(
                delegateOwner = reader.take(ADDRESS_SIZE),
                delegateShare = reader.varint(),
                delegateExpiry = reader.u32le(),
            )
        }
        check(reader.atEnd()) { "Unexpected bytes after bond payload" }
        return tx
    }

    fun decodeUnbond(rawHex: String): Unbond {
        val reader = Reader(hexToBytes(rawHex))
        val header = reader.header()
        check(header.payloadType == PAYLOAD_UNBOND) { "Not an unbond transaction (payload type ${header.payloadType})" }
        val validator = reader.take(ADDRESS_SIZE)
        val owner = if (header.flags and SignedTx.FLAG_WITH_DELEGATION != 0) reader.take(ADDRESS_SIZE) else null
        check(reader.atEnd()) { "Unexpected bytes after unbond payload" }
        return Unbond(header.lockTime, header.fee, header.memo, validator, owner)
    }

    /**
     * Fail-closed like [assertTransferMatches]: also checks the PIP-49 fields, and
     * that a plain bond carries no delegation (and the other way round).
     */
    fun assertBondMatches(
        rawHex: String,
        sender: String,
        validator: String,
        stake: Long,
        publicKey: String?, // public1p... string, only for a validator never bonded
        fee: Long,
        memo: String?,
        delegation: Delegation?,
    ) {
        val tx = decodeBond(rawHex)
        val key = publicKey?.takeIf { it.isNotEmpty() }?.let { publicKeyHex(it) }
        val checks = mutableListOf(
            "sender" to addressMatches(tx.from, sender),
            "validator" to addressMatches(tx.to, validator),
            "stake" to (tx.stake == stake),
            "public key" to if (publicKey.isNullOrEmpty()) tx.publicKey.isEmpty() else (key != null && tx.publicKey == key),
            "fee" to (tx.fee == fee),
            "memo" to (tx.memo == memo.orEmpty()),
            "delegation" to ((tx.delegateOwner != null) == (delegation != null)),
        )
        if (delegation != null) {
            checks += "delegate owner" to (tx.delegateOwner?.let { addressMatches(it, delegation.owner) } == true)
            checks += "delegate share" to (tx.delegateShare == delegation.shareNanoPac)
            checks += "delegate expiry" to (tx.delegateExpiry == delegation.expiry)
        }
        throwOnMismatch(checks)
    }

    fun assertUnbondMatches(rawHex: String, validator: String, delegateOwner: String?, memo: String?) {
        val tx = decodeUnbond(rawHex)
        val ownerOk = if (delegateOwner == null) {
            tx.delegateOwner == null
        } else {
            tx.delegateOwner?.let { addressMatches(it, delegateOwner) } == true
        }
        throwOnMismatch(
            listOf(
                "validator" to addressMatches(tx.validator, validator),
                "delegate owner" to ownerOk,
                "memo" to (tx.memo == memo.orEmpty()),
            ),
        )
    }

    // public1p... to the hex of its key bytes; null fails the comparison closed
    private fun publicKeyHex(key: String): String? =
        runCatching { Bech32m.decode(key).third.joinToString("") { "%02x".format(it) } }.getOrNull()

    data class Transfer(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val from: ByteArray,
        val to: ByteArray,
        val amount: Long,
    )

    data class Anchor(
        val lockTime: Long,
        val fee: Long,
        val memo: String,
        val from: ByteArray,
        val action: Int,
        val rootHash: String,
        val manifestUri: String,
        val anchorType: Int,
        val deposit: Long,
    )

    /** Bytes after the payload are refused: everything here gets signed. */
    fun decodeAnchor(rawHex: String): Anchor {
        val reader = Reader(hexToBytes(rawHex))
        val header = reader.header()
        check(header.payloadType == PAYLOAD_ANCHOR) {
            "Not an anchor transaction (payload type ${header.payloadType})"
        }
        var tx = Anchor(
            lockTime = header.lockTime,
            fee = header.fee,
            memo = header.memo,
            from = reader.take(ADDRESS_SIZE),
            action = reader.u8(),
            rootHash = "",
            manifestUri = "",
            anchorType = 0,
            deposit = 0,
        )
        when (tx.action) {
            ACTION_SET -> tx = tx.copy(
                rootHash = reader.take(reader.u8()).joinToString("") { "%02x".format(it) },
                manifestUri = reader.take(reader.u8()).toString(Charsets.UTF_8),
                anchorType = reader.u8(),
                deposit = reader.varint(),
            )
            ACTION_DELETE -> Unit
            else -> error("Unknown anchor action ${tx.action}")
        }
        check(reader.atEnd()) { "Unexpected bytes after anchor payload" }
        return tx
    }

    /**
     * Fail-closed like [assertTransferMatches]. This also catches a root the node
     * misread: it silently decodes a hex root_hash as base64 into other bytes.
     */
    fun assertAnchorMatches(rawHex: String, expected: AnchorRequest, lockTime: Long) {
        val tx = decodeAnchor(rawHex)
        throwOnMismatch(
            listOf(
                "sender" to addressMatches(tx.from, expected.sender),
                "action" to (tx.action == if (expected.isDelete) ACTION_DELETE else ACTION_SET),
                "root hash" to (tx.rootHash == expected.rootHash),
                "manifest URI" to (tx.manifestUri == expected.manifestUri),
                "anchor type" to (tx.anchorType == expected.anchorType),
                "deposit" to (tx.deposit == expected.deposit.nanoPac),
                "fee" to (tx.fee == expected.fee.nanoPac),
                "memo" to (tx.memo == expected.memo),
                "lock time" to (tx.lockTime == lockTime),
            ),
        )
    }

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

    fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Raw transaction hex has odd length" }
        return ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    private data class Header(
        val flags: Int,
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

        fun atEnd(): Boolean = offset == bytes.size

        fun u32le(): Long =
            (0..3).fold(0L) { value, index -> value or (u8().toLong() shl (8 * index)) }

        fun header(): Header {
            val flags = u8() // 0x04 marks a PIP-49 delegated bond or unbond
            u8() // version
            val lockTime = u32le()
            val fee = varint()
            val memo = take(varint().toInt()).toString(Charsets.UTF_8)
            return Header(flags, lockTime, fee, memo, u8())
        }
    }
}
