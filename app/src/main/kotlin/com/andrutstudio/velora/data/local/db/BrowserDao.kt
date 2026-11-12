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

    @Query("DELETE FROM browser_history WHERE url NOT LIKE 'http%'")
    suspend fun deleteNonWebHistory()

    @Query("DELETE FROM browser_history WHERE url = :url")
    suspend fun deleteHistoryUrl(url: String)

    // One row per URL with its latest visit, and a bounded table
    @Transaction
    suspend fun recordVisit(entry: BrowserHistoryEntity) {
        deleteHistoryUrl(entry.url)
        insertHistory(entry)
        pruneHistory()
    }

    @Query("DELETE FROM browser_history WHERE id NOT IN (SELECT id FROM browser_history ORDER BY visitedAt DESC LIMIT 200)")
    suspend fun pruneHistory()

    // Bookmarks

    // The user's own bookmarks first, newest first, then the presets
    @Query("SELECT * FROM bookmarks ORDER BY isPreset ASC, createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url IN (:urls))")
    suspend fun isBookmarked(urls: List<String>): Boolean

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun bookmarkCount(): Int

    // Connected sites (dApp connect() approvals)

    @Query("SELECT * FROM connected_sites ORDER BY connectedAt DESC")
    fun observeConnectedSites(): Flow<List<ConnectedSiteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM connected_sites WHERE host = :host)")
    suspend fun isSiteConnected(host: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectedSite(site: ConnectedSiteEntity)

    @Query("DELETE FROM connected_sites WHERE host = :host")
    suspend fun deleteConnectedSite(host: String)
}
