package com.andrutstudio.velora.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncSettings @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private companion object {
        val KEY_LAST_NOTIFIED_HEIGHT = longPreferencesKey("last_notified_height")
    }

    suspend fun getLastNotifiedHeight(): Long {
        return dataStore.data.map { it[KEY_LAST_NOTIFIED_HEIGHT] }.first() ?: 0L
    }

    suspend fun setLastNotifiedHeight(height: Long) {
        dataStore.edit { it[KEY_LAST_NOTIFIED_HEIGHT] = height }
    }
}
