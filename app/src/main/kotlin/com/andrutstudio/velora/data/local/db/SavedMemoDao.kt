package com.andrutstudio.velora.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedMemoDao {
    @Query("SELECT * FROM saved_memos ORDER BY text ASC")
    fun getAll(): Flow<List<SavedMemoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SavedMemoEntity)

    @Delete
    suspend fun delete(entity: SavedMemoEntity)
}
