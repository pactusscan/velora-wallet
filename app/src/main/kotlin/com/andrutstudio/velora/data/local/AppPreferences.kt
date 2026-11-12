package com.andrutstudio.velora.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemePreference { SYSTEM, LIGHT, DARK }

private val KEY_THEME = stringPreferencesKey("app_theme")
private val KEY_BOOKMARKS_SEEDED = booleanPreferencesKey("browser_bookmarks_seeded")

@Singleton
class AppPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val themeFlow: Flow<ThemePreference> = dataStore.data.map { prefs ->
        prefs[KEY_THEME]
            ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
            ?: ThemePreference.SYSTEM
    }

    suspend fun setTheme(theme: ThemePreference) {
        dataStore.edit { it[KEY_THEME] = theme.name }
    }

    /** The preset bookmarks are added once, so deleting them all does not bring them back. */
    suspend fun bookmarksSeeded(): Boolean = dataStore.data.first()[KEY_BOOKMARKS_SEEDED] == true

    suspend fun setBookmarksSeeded() {
        dataStore.edit { it[KEY_BOOKMARKS_SEEDED] = true }
    }
}
