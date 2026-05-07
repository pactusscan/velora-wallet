package com.andrutstudio.velora.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BrowserDao {

    // History

    @Query("SELECT * FROM browser_history ORDER BY visitedAt DESC LIMIT 100")
    fun observeHistory(): Flow<List<BrowserHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: BrowserHistoryEntity)

    @Query("DELETE FROM browser_history")
    suspend fun clearHistory()

    // Bookmarks

    @Query("SELECT * FROM bookmarks ORDER BY isPreset DESC, createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    suspend fun isBookmarked(url: String): Boolean

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun bookmarkCount(): Int
}
