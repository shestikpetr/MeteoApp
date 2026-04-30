package com.shestikpetr.meteoapp.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.shestikpetr.meteoapp.util.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SessionStorage {
    val username: Flow<String?>
    val email: Flow<String?>

    suspend fun save(username: String, email: String?)
    suspend fun clear()
}

class SessionStorageDataStore(private val context: Context) : SessionStorage {

    override val username: Flow<String?> =
        context.dataStore.data.map { it[USERNAME_KEY] }

    override val email: Flow<String?> =
        context.dataStore.data.map { it[EMAIL_KEY] }

    override suspend fun save(username: String, email: String?) {
        context.dataStore.edit { prefs ->
            prefs[USERNAME_KEY] = username
            if (email != null) prefs[EMAIL_KEY] = email
        }
    }

    override suspend fun clear() {
        context.dataStore.edit {
            it.remove(USERNAME_KEY)
            it.remove(EMAIL_KEY)
        }
    }

    private companion object {
        val USERNAME_KEY = stringPreferencesKey("username")
        val EMAIL_KEY = stringPreferencesKey("email")
    }
}
