package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_memos")
data class SavedMemoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val addedAt: Long = System.currentTimeMillis()
)
