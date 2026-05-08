package com.andrutstudio.velora.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import com.andrutstudio.velora.data.local.db.AppDatabase
import com.andrutstudio.velora.data.local.db.BalanceAlertDao
import com.andrutstudio.velora.data.local.db.BrowserDao
import com.andrutstudio.velora.data.local.db.TransactionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pactus_wallet.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideBrowserDao(db: AppDatabase): BrowserDao = db.browserDao()

    @Provides
    fun provideBalanceAlertDao(db: AppDatabase): BalanceAlertDao = db.balanceAlertDao()
}
