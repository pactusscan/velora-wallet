package com.andrutstudio.velora.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BrowserHistoryEntity::class,
        BookmarkEntity::class,
        BalanceAlertEntity::class,
        MonitoredNodeEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun browserDao(): BrowserDao
    abstract fun balanceAlertDao(): BalanceAlertDao
    abstract fun monitoredNodeDao(): MonitoredNodeDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS monitored_nodes (
                        validatorAddress TEXT PRIMARY KEY NOT NULL,
                        addedAt INTEGER NOT NULL
                    )"""
                )
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS balance_alerts (
                        address TEXT PRIMARY KEY NOT NULL,
                        thresholdNanoPac INTEGER NOT NULL,
                        isEnabled INTEGER NOT NULL
                    )"""
                )
            }
        }
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS browser_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        visitedAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS bookmarks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT NOT NULL,
                        isPreset INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_bookmarks_url` ON `bookmarks` (`url`)"
                )
            }
        }
    }
}
