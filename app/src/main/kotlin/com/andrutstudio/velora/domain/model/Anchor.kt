package com.andrutstudio.velora.domain.model

/** The PIP-50 anchor of one account, as the node reports it. */
data class AnchorInfo(
    val rootHash: String,
    val manifestUri: String,
    val anchorType: Int,
    val lockedDeposit: Amount,
    val updatedTime: Long,
)
