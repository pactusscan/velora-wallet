package com.andrutstudio.velora.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "address_book")
data class AddressBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val address: String,
    val addedAt: Long = System.currentTimeMillis()
)
