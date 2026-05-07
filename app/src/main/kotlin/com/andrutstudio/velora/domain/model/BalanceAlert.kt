package com.andrutstudio.velora.domain.model

data class BalanceAlert(
    val address: String,
    val thresholdNanoPac: Long,
    val isEnabled: Boolean = true
)
