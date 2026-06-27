package com.andrutstudio.velora.domain.repository

import com.andrutstudio.velora.data.local.db.AddressBookEntity
import kotlinx.coroutines.flow.Flow

interface AddressBookRepository {
    fun getAddresses(): Flow<List<AddressBookEntity>>
    suspend fun addAddress(label: String, address: String)
    suspend fun deleteAddress(entity: AddressBookEntity)
    suspend fun getByAddress(address: String): AddressBookEntity?
}
