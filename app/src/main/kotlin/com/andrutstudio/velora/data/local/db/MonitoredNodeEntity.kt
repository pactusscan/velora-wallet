package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monitored_nodes")
data class MonitoredNodeEntity(
    @PrimaryKey val validatorAddress: String,
    val addedAt: Long = System.currentTimeMillis(),
)
