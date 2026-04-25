package com.shestikpetr.meteoapp.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserSessionStore(private val context: Context) {

    companion object {
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val USERNAME_KEY = stringPreferencesKey("username")
    }

    val username: Flow<String?> = context.dataStore.data.map { it[USERNAME_KEY] }

    suspend fun save(userId: String, username: String) {
        context.dataStore.edit {
            it[USER_ID_KEY] = userId
            it[USERNAME_KEY] = username
        }
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(USER_ID_KEY)
            it.remove(USERNAME_KEY)
        }
    }
}
