package com.shestikpetr.meteoapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shestikpetr.meteoapp.util.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface TokenStorage {
    val accessToken: Flow<String?>
    val refreshToken: Flow<String?>

    suspend fun saveTokens(accessToken: String, refreshToken: String)
    suspend fun updateAccessToken(accessToken: String)
    suspend fun clear()

    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun isLoggedIn(): Boolean
}

class TokenStorageDataStore(private val context: Context) : TokenStorage {

    override val accessToken: Flow<String?> =
        context.dataStore.data.map { it[ACCESS_TOKEN_KEY] }

    override val refreshToken: Flow<String?> =
        context.dataStore.data.map { it[REFRESH_TOKEN_KEY] }

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[ACCESS_TOKEN_KEY] = accessToken
            it[REFRESH_TOKEN_KEY] = refreshToken
        }
    }

    override suspend fun updateAccessToken(accessToken: String) {
        context.dataStore.edit { it[ACCESS_TOKEN_KEY] = accessToken }
    }

    override suspend fun clear() {
        context.dataStore.edit {
            it.remove(ACCESS_TOKEN_KEY)
            it.remove(REFRESH_TOKEN_KEY)
        }
    }

    override suspend fun getAccessToken(): String? = accessToken.first()
    override suspend fun getRefreshToken(): String? = refreshToken.first()
    override suspend fun isLoggedIn(): Boolean = getAccessToken() != null

    private companion object {
        val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token")
    }
}
