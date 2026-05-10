package com.andrutstudio.velora.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredNodeDao {
    @Query("SELECT * FROM monitored_nodes ORDER BY addedAt ASC")
    fun getAll(): Flow<List<MonitoredNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(node: MonitoredNodeEntity)

    @Delete
    suspend fun delete(node: MonitoredNodeEntity)
}
