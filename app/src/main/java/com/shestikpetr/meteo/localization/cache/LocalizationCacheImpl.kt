package com.shestikpetr.meteo.localization.cache

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shestikpetr.meteo.localization.interfaces.LocalizationCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.localizationDataStore: DataStore<Preferences> by preferencesDataStore(name = "localization")

/**
 * DataStore-based implementation of LocalizationCache
 * Uses Android DataStore for efficient and safe data persistence
 */
@Singleton
class LocalizationCacheImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocalizationCache {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCachedStrings(locale: String): Map<String, String>? {
        return try {
            val stringKey = stringPreferencesKey("strings_$locale")
            val stringsJson = context.localizationDataStore.data
                .map { preferences -> preferences[stringKey] }
                .first()

            stringsJson?.let { json.decodeFromString<Map<String, String>>(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cacheStrings(locale: String, strings: Map<String, String>) {
        try {
            val stringKey = stringPreferencesKey("strings_$locale")
            val timestampKey = longPreferencesKey("timestamp_$locale")
            val stringsJson = json.encodeToString(strings)

            context.localizationDataStore.edit { preferences ->
                preferences[stringKey] = stringsJson
                preferences[timestampKey] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Log error but don't fail - caching is optional
        }
    }

    override suspend fun clearCache() {
        try {
            context.localizationDataStore.edit { preferences ->
                preferences.clear()
            }
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }

    override suspend fun isCacheValid(locale: String): Boolean {
        return try {
            val timestampKey = longPreferencesKey("timestamp_$locale")
            val timestamp = context.localizationDataStore.data
                .map { preferences -> preferences[timestampKey] }
                .first()

            timestamp != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCacheAge(locale: String): Long? {
        return try {
            val timestampKey = longPreferencesKey("timestamp_$locale")
            context.localizationDataStore.data
                .map { preferences -> preferences[timestampKey] }
                .first()
        } catch (e: Exception) {
            null
        }
    }
}