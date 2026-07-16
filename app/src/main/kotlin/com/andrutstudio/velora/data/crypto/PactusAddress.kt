package com.andrutstudio.velora.data.crypto

/**
 * Validation for Pactus addresses. An address is bech32m-encoded as a type
 * byte plus a 20-byte public-key hash. Account-capable types (protocol v4):
 *
 *  1 — Validator (pc1p…), BLS12-381
 *  2 — BLS Account (pc1z…), BLS12-381
 *  3 — Ed25519 Account (pc1r…)
 *  4 — secp256k1 Account (pc1y…), PIP-53
 *
 * Type 0 (treasury, pc1q…) is not a valid transfer recipient.
 */
object PactusAddress {

    private const val ADDRESS_DATA_SIZE = 20
    private val KNOWN_HRPS = setOf("pc", "tpc")

    /**
     * Full checksum validation via [Bech32m.decode] — catches typos that a
     * prefix check would let through.
     */
    fun isValidAccountAddress(address: String): Boolean {
        val decoded = runCatching { Bech32m.decode(address) }.getOrNull() ?: return false
        val (hrp, type, data) = decoded
        return hrp in KNOWN_HRPS && type.toInt() in 1..4 && data.size == ADDRESS_DATA_SIZE
    }
}
