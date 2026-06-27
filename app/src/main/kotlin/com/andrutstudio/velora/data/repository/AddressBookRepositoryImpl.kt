package com.andrutstudio.velora.data.repository

import com.andrutstudio.velora.data.local.db.AddressBookDao
import com.andrutstudio.velora.data.local.db.AddressBookEntity
import com.andrutstudio.velora.domain.repository.AddressBookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AddressBookRepositoryImpl @Inject constructor(
    private val addressBookDao: AddressBookDao
) : AddressBookRepository {
    override fun getAddresses(): Flow<List<AddressBookEntity>> = addressBookDao.getAll()

    override suspend fun addAddress(label: String, address: String) {
        addressBookDao.insert(AddressBookEntity(label = label, address = address))
    }

    override suspend fun deleteAddress(entity: AddressBookEntity) {
        addressBookDao.delete(entity)
    }

    override suspend fun getByAddress(address: String): AddressBookEntity? {
        return addressBookDao.getByAddress(address)
    }
}
