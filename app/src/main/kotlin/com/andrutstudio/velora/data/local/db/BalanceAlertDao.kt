package com.andrutstudio.velora.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceAlertDao {
    @Query("SELECT * FROM balance_alerts")
    fun getAllAlerts(): Flow<List<BalanceAlertEntity>>

    @Query("SELECT * FROM balance_alerts WHERE address = :address")
    suspend fun getAlertForAddress(address: String): BalanceAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: BalanceAlertEntity)

    @Delete
    suspend fun deleteAlert(alert: BalanceAlertEntity)
}
