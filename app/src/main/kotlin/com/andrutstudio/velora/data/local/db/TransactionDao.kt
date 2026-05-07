package com.andrutstudio.velora.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE involvedAddress IN (:addresses) ORDER BY blockTime DESC")
    fun observeByAddresses(addresses: List<String>): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT MAX(blockHeight) FROM transactions WHERE involvedAddress IN (:addresses)")
    suspend fun getLastSyncedHeight(addresses: List<String>): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txs: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(tx: TransactionEntity): Long
}
