package com.andrutstudio.velora.domain.model

import com.andrutstudio.velora.data.crypto.Bech32m
import kotlin.math.roundToLong

/** A validator as the node reports it (pactus.blockchain.get_validator). */
data class ValidatorInfo(
    val address: String,
    val stake: Amount,
    val unbondingHeight: Long,
    val delegated: Boolean,
    val delegateOwner: String,
    val delegateShareNanoPac: Long,
    val delegateExpiry: Long,
) {
    val isUnbonded: Boolean get() = unbondingHeight > 0
}

/** PIP-49 fields of a delegated bond: the sending account stays the Stake Owner. */
data class Delegation(
    val owner: String,
    val shareNanoPac: Long,
    val expiry: Long,
)

/**
 * PIP-49 rules, verified against pactus-project/pactus (main). Mirrors bond.ts in
 * the Velora browser extension.
 * - execution/executor/bond.go: a delegated bond stakes exactly the maximum
 *   stake (1,000 PAC) and its expiry must be above the current height; the
 *   public key goes only with a validator that was never bonded, and an
 *   unbonded validator cannot be bonded again.
 * - types/tx/payload/bond.go: the share is 0 to 0.7 PAC in nano PAC and a
 *   multiple of 32.
 */
object DelegationRules {
    const val MAX_STAKE_NANO_PAC = 1_000_000_000_000L // 1,000 PAC, also the delegated stake
    const val MAX_SHARE_NANO_PAC = 700_000_000L // 0.7 PAC per block
    const val MAX_SHARE_PERCENT = 70.0
    const val BLOCKS_PER_DAY = 8_640L // 10 s blocks
    const val BLOCK_SECONDS = 10L
    private const val SHARE_STEP = 32L
    private const val VALIDATOR_TYPE: Byte = 1
    private const val BLS_PUBLIC_KEY_BYTES = 96

    /** Term choices in months and their length in days, as in the extension. */
    val TERM_DAYS = linkedMapOf(3 to 91, 6 to 182, 12 to 365)

    fun checkShare(share: Long) {
        require(share in 0..MAX_SHARE_NANO_PAC) { "Delegate share must be 0 to 0.7 PAC" }
        require(share % SHARE_STEP == 0L) { "Delegate share must be a multiple of 32 nano PAC" }
    }

    /**
     * Share in percent of the 1 PAC base reward (0 to 70) to nano PAC per block,
     * rounded down to the protocol's multiple of 32.
     */
    fun percentToShare(percent: Double): Long = (percent * 1e7).roundToLong() / SHARE_STEP * SHARE_STEP

    fun shareToPercent(share: Long): Double = share / 1e7

    fun isValidatorAddress(address: String, network: Network): Boolean =
        runCatching { Bech32m.decode(address) }.getOrNull()
            ?.let { (hrp, type, hash) -> hrp == network.hrp && type == VALIDATOR_TYPE && hash.size == 20 } == true

    fun publicKeyHrp(network: Network): String = if (network == Network.MAINNET) "public" else "tpublic"

    fun isPublicKeyFor(key: String, network: Network): Boolean =
        runCatching { Bech32m.decode(key) }.getOrNull()
            ?.let { (hrp, _, data) -> hrp == publicKeyHrp(network) && data.size == BLS_PUBLIC_KEY_BYTES } == true

    fun expiryForDays(height: Long, days: Int): Long = height + days * BLOCKS_PER_DAY

    /** Seconds from now until [expiry], at 10 s per block. */
    fun secondsUntil(expiry: Long, height: Long): Long = (expiry - height).coerceAtLeast(0) * BLOCK_SECONDS

    /**
     * Checks what a dApp sent with window.pactus.bond. The height-dependent checks
     * (expiry above the current block, validator known or not) need the node.
     */
    fun parseDappBond(
        validator: String,
        publicKey: String,
        share: String,
        expiry: String,
        memo: String,
        network: Network,
    ): DappBond {
        val target = validator.trim()
        require(isValidatorAddress(target, network)) { "Invalid validator address" }
        val key = publicKey.trim()
        require(key.isEmpty() || isPublicKeyFor(key, network)) { "Invalid validator public key" }
        val shareNano = requireNotNull(share.trim().takeIf { it.all(Char::isDigit) && it.isNotEmpty() }?.toLongOrNull()) {
            "Delegate share must be a whole number of nano PAC"
        }
        checkShare(shareNano)
        val expiryBlock = expiry.trim().toLongOrNull()
        require(expiryBlock != null && expiryBlock in 1..0xFFFF_FFFFL) { "Invalid delegate expiry" }
        require(memo.toByteArray().size <= AnchorRules.MAX_MEMO_BYTES) { "Memo is too long" }
        return DappBond(target, key, shareNano, expiryBlock, memo)
    }
}

/** A delegated bond a dApp asked for, before the wallet picks the signer. */
data class DappBond(
    val validator: String,
    val publicKey: String,
    val shareNanoPac: Long,
    val expiry: Long,
    val memo: String,
)
