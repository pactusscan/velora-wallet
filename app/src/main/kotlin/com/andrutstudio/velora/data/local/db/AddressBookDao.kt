package com.andrutstudio.velora.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressBookDao {
    @Query("SELECT * FROM address_book ORDER BY label ASC")
    fun getAll(): Flow<List<AddressBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AddressBookEntity)

    @Delete
    suspend fun delete(entity: AddressBookEntity)

    @Query("SELECT * FROM address_book WHERE address = :address LIMIT 1")
    suspend fun getByAddress(address: String): AddressBookEntity?
}
