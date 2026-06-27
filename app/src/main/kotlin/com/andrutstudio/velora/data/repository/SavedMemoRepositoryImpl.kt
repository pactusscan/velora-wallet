package com.andrutstudio.velora.data.repository

import com.andrutstudio.velora.data.local.db.SavedMemoDao
import com.andrutstudio.velora.data.local.db.SavedMemoEntity
import com.andrutstudio.velora.domain.repository.SavedMemoRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedMemoRepositoryImpl @Inject constructor(
    private val savedMemoDao: SavedMemoDao
) : SavedMemoRepository {
    override fun getMemos(): Flow<List<SavedMemoEntity>> = savedMemoDao.getAll()

    override suspend fun addMemo(text: String) {
        savedMemoDao.insert(SavedMemoEntity(text = text))
    }

    override suspend fun deleteMemo(entity: SavedMemoEntity) {
        savedMemoDao.delete(entity)
    }
}
