package com.antigravity.remote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "antigravity_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        const val DEFAULT_URL = "https://antigravity.google.com/remote/"
        private val KEY_CURRENT_URL = stringPreferencesKey("current_url")
        private val KEY_DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        private val KEY_URL_HISTORY = stringPreferencesKey("url_history")
        private const val HISTORY_DELIMITER = "|"
        private const val MAX_HISTORY_SIZE = 5
    }

    val currentUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENT_URL] ?: DEFAULT_URL
    }

    val isDesktopMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DESKTOP_MODE] ?: false
    }

    val urlHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_URL_HISTORY].orEmpty()
        if (raw.isBlank()) {
            listOf(DEFAULT_URL)
        } else {
            raw.split(HISTORY_DELIMITER).filter { it.isNotBlank() }
        }
    }

    suspend fun setCurrentUrl(url: String) {
        val cleanUrl = url.trim().ifEmpty { DEFAULT_URL }
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENT_URL] = cleanUrl
            val currentList = prefs[KEY_URL_HISTORY]
                ?.split(HISTORY_DELIMITER)
                ?.filter { it.isNotBlank() && it != cleanUrl }
                .orEmpty()
            val updatedList = (listOf(cleanUrl) + currentList).take(MAX_HISTORY_SIZE)
            prefs[KEY_URL_HISTORY] = updatedList.joinToString(HISTORY_DELIMITER)
        }
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DESKTOP_MODE] = enabled
        }
    }

    suspend fun resetToDefault() {
        setCurrentUrl(DEFAULT_URL)
    }

    suspend fun clearHistory() {
        context.dataStore.edit { prefs ->
            prefs[KEY_URL_HISTORY] = DEFAULT_URL
        }
    }
}
