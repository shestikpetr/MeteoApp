package com.shestikpetr.meteo.storage.impl

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.shestikpetr.meteo.network.interfaces.SecureStorage
import com.shestikpetr.meteo.network.interfaces.StorageEditor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SharedPreferences implementation of SecureStorage interface.
 * This implementation provides secure storage using Android's SharedPreferences,
 * adapted from the existing AuthManager implementation.
 */
@Singleton
class SharedPreferencesStorage @Inject constructor(
    @ApplicationContext private val context: Context
) : SecureStorage {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("MeteoPrefs", Context.MODE_PRIVATE)
    }

    override suspend fun putString(key: String, value: String) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putString(key, value)
            }
        }
    }

    override suspend fun getString(key: String, defaultValue: String?): String? {
        return withContext(Dispatchers.IO) {
            prefs.getString(key, defaultValue)
        }
    }

    override suspend fun putInt(key: String, value: Int) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putInt(key, value)
            }
        }
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(key, defaultValue)
        }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putBoolean(key, value)
            }
        }
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return withContext(Dispatchers.IO) {
            prefs.getBoolean(key, defaultValue)
        }
    }

    override suspend fun putLong(key: String, value: Long) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                putLong(key, value)
            }
        }
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long {
        return withContext(Dispatchers.IO) {
            prefs.getLong(key, defaultValue)
        }
    }

    override suspend fun contains(key: String): Boolean {
        return withContext(Dispatchers.IO) {
            prefs.contains(key)
        }
    }

    override suspend fun remove(key: String) {
        withContext(Dispatchers.IO) {
            prefs.edit {
                remove(key)
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            prefs.edit {
                clear()
            }
        }
    }

    override suspend fun edit(operations: suspend StorageEditor.() -> Unit) {
        withContext(Dispatchers.IO) {
            val editor = SharedPreferencesStorageEditor()
            operations(editor)
            editor.apply(prefs)
        }
    }

    /**
     * Internal implementation of StorageEditor for batch operations.
     */
    private class SharedPreferencesStorageEditor : StorageEditor {
        private val operations = mutableListOf<(SharedPreferences.Editor) -> Unit>()

        override fun putString(key: String, value: String): StorageEditor {
            operations.add { editor -> editor.putString(key, value) }
            return this
        }

        override fun putInt(key: String, value: Int): StorageEditor {
            operations.add { editor -> editor.putInt(key, value) }
            return this
        }

        override fun putBoolean(key: String, value: Boolean): StorageEditor {
            operations.add { editor -> editor.putBoolean(key, value) }
            return this
        }

        override fun putLong(key: String, value: Long): StorageEditor {
            operations.add { editor -> editor.putLong(key, value) }
            return this
        }

        override fun remove(key: String): StorageEditor {
            operations.add { editor -> editor.remove(key) }
            return this
        }

        override fun clear(): StorageEditor {
            operations.add { editor -> editor.clear() }
            return this
        }

        fun apply(prefs: SharedPreferences) {
            prefs.edit {
                operations.forEach { operation ->
                    operation(this)
                }
            }
        }
    }
}