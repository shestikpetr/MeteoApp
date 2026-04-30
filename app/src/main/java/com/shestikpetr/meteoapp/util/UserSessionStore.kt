package com.shestikpetr.meteoapp.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSessionStore(private val context: Context) {

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val EMAIL_KEY = stringPreferencesKey("email")
    }

    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }
    val email: Flow<String?> = context.dataStore.data.map { it[EMAIL_KEY] }

    suspend fun save(username: String, email: String? = null) {
        context.dataStore.edit {
            it[USERNAME_KEY] = username
            if (email != null) it[EMAIL_KEY] = email
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(USERNAME_KEY)
            it.remove(EMAIL_KEY)
        }
    }
}
