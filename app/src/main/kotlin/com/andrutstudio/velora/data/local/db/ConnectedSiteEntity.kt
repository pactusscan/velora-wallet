package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A dApp host the user approved via window.pactus.connect() in the in-app
 * browser. Connect requests from these hosts are auto-approved until the user
 * revokes them in Settings > Connected sites (mirrors the Velora extension).
 */
@Entity(tableName = "connected_sites")
data class ConnectedSiteEntity(
    @PrimaryKey val host: String,
    val connectedAt: Long = System.currentTimeMillis(),
)
