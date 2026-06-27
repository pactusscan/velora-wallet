package com.andrutstudio.velora.domain.repository

import com.andrutstudio.velora.data.local.db.SavedMemoEntity
import kotlinx.coroutines.flow.Flow

interface SavedMemoRepository {
    fun getMemos(): Flow<List<SavedMemoEntity>>
    suspend fun addMemo(text: String)
    suspend fun deleteMemo(entity: SavedMemoEntity)
}
