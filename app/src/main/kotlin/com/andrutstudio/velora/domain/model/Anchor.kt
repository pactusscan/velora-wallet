package com.andrutstudio.velora.domain.model

/** The PIP-50 anchor of one account, as the node reports it. */
data class AnchorInfo(
    val rootHash: String,
    val manifestUri: String,
    val anchorType: Int,
    val lockedDeposit: Amount,
    val updatedTime: Long,
)

/** What the wallet is about to sign. A delete only needs the sender and the fee. */
data class AnchorRequest(
    val sender: String,
    val isDelete: Boolean,
    val rootHash: String = "",
    val manifestUri: String = "",
    val anchorType: Int = 0,
    val deposit: Amount = Amount.ZERO,
    val fee: Amount = Amount.ZERO,
    val memo: String = "",
)

/** Limits from PIP-50 sections 3 and 5. Mirrors anchor.ts in the browser extension. */
object AnchorRules {
    const val MIN_DEPOSIT_NANO_PAC = 1_000_000_000L // 1 PAC
    const val MAX_URI_BYTES = 128
    const val MAX_MEMO_BYTES = 64
    private const val MIN_ROOT_BYTES = 32
    private const val MAX_ROOT_BYTES = 64

    /** The anchor types PIP-50 names. Other values up to 255 are allowed. */
    val KNOWN_TYPES = listOf(0, 1, 2, 3, 255)

    /** Returns the root in lowercase hex, or null if it is not 32 to 64 bytes of hex. */
    fun normalizeRoot(hex: String): String? {
        val root = hex.trim().lowercase()
        val isHex = root.length % 2 == 0 && root.all { it in '0'..'9' || it in 'a'..'f' }
        return root.takeIf { isHex && it.length / 2 in MIN_ROOT_BYTES..MAX_ROOT_BYTES }
    }

    fun isValidUri(uri: String): Boolean = uri.toByteArray().size <= MAX_URI_BYTES

    fun isValidMemo(memo: String): Boolean = memo.toByteArray().size <= MAX_MEMO_BYTES

    fun isValidType(type: Int): Boolean = type in 0..255

    /**
     * A Set adds to the locked deposit, so by default a new slot locks the minimum
     * and replacing a live one locks nothing more.
     */
    fun deposit(requested: Amount?, hasAnchor: Boolean): Amount =
        requested ?: if (hasAnchor) Amount.ZERO else Amount.fromNanoPac(MIN_DEPOSIT_NANO_PAC)
}
