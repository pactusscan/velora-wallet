package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "balance_alerts")
data class BalanceAlertEntity(
    @PrimaryKey val address: String,
    val thresholdNanoPac: Long,
    val isEnabled: Boolean,
    val type: AlertType = AlertType.LOWER_THAN,
    val lastTriggeredValue: Long? = null
)

enum class AlertType {
    LOWER_THAN,
    HIGHER_THAN
}
