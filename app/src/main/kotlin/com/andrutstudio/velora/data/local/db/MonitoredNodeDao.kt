package com.andrutstudio.velora.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredNodeDao {
    @Query("SELECT * FROM monitored_nodes ORDER BY displayOrder ASC, addedAt ASC")
    fun getAll(): Flow<List<MonitoredNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(node: MonitoredNodeEntity)

    @Update
    suspend fun update(node: MonitoredNodeEntity)

    @Delete
    suspend fun delete(node: MonitoredNodeEntity)

    @Transaction
    suspend fun updateOrder(nodes: List<MonitoredNodeEntity>) {
        nodes.forEach { update(it) }
    }
}
