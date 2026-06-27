package com.andrutstudio.velora.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.andrutstudio.velora.data.repository.BlockchainRepositoryImpl
import com.andrutstudio.velora.data.repository.MarketRepositoryImpl
import com.andrutstudio.velora.data.repository.AddressBookRepositoryImpl
import com.andrutstudio.velora.data.repository.SavedMemoRepositoryImpl
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.MarketRepository
import com.andrutstudio.velora.domain.repository.AddressBookRepository
import com.andrutstudio.velora.domain.repository.SavedMemoRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBlockchainRepository(impl: BlockchainRepositoryImpl): BlockchainRepository

    @Binds
    @Singleton
    abstract fun bindMarketRepository(impl: MarketRepositoryImpl): MarketRepository

    @Binds
    @Singleton
    abstract fun bindAddressBookRepository(impl: AddressBookRepositoryImpl): AddressBookRepository

    @Binds
    @Singleton
    abstract fun bindSavedMemoRepository(impl: SavedMemoRepositoryImpl): SavedMemoRepository
}
