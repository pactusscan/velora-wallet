package com.andrutstudio.velora.data.crypto

/**
 * Builds the signed transaction from the node's unsigned one. Mirrors
 * txSigner.ts in the Velora browser extension.
 *
 * The node signs only the bytes after the flags byte (Tx.SignBytes in
 * pactus-project/pactus types/tx/tx.go), so the flags are kept as the node set
 * them, minus the not-signed bit 0x02: 0x00 for a plain transaction, 0x04 for a
 * PIP-49 delegated bond or unbond. Writing 0x00 there breaks delegated ones.
 */
object SignedTx {
    const val FLAG_NOT_SIGNED = 0x02
    const val FLAG_WITH_DELEGATION = 0x04

    /** The bytes to sign: everything after the flags byte. */
    fun signBytes(raw: ByteArray): ByteArray {
        require(raw.isNotEmpty()) { "Raw transaction is empty" }
        return raw.copyOfRange(1, raw.size)
    }

    /** flags (without 0x02) || raw[1:] || signature(64) || public key(32) */
    fun assemble(raw: ByteArray, signature: ByteArray, publicKey: ByteArray): ByteArray {
        val flags = (raw[0].toInt() and 0xFF and FLAG_NOT_SIGNED.inv()).toByte()
        return byteArrayOf(flags) + signBytes(raw) + signature + publicKey
    }
}
