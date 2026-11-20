package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bharatDataStore by preferencesDataStore(name = "bharat_tools_prefs")

class LocalPreferencesStore(private val context: Context) {
    private val keyRecentSearches = stringPreferencesKey("recent_searches")

    val recentSearches: Flow<List<String>> = context.bharatDataStore.data.map { prefs ->
        val raw = prefs[keyRecentSearches].orEmpty()
        if (raw.isBlank()) emptyList() else raw.split("||").filter { it.isNotBlank() }
    }

    suspend fun addSearch(query: String) {
        val clean = query.trim().take(60)
        if (clean.length < 2) return
        context.bharatDataStore.edit { prefs ->
            val current = prefs[keyRecentSearches].orEmpty()
                .split("||")
                .filter { it.isNotBlank() && !it.equals(clean, ignoreCase = true) }
            val next = (listOf(clean) + current).take(8)
            prefs[keyRecentSearches] = next.joinToString("||")
        }
    }

    suspend fun clearSearches() {
        context.bharatDataStore.edit { prefs ->
            prefs.remove(keyRecentSearches)
        }
    }
}
