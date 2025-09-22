package com.shestikpetr.meteo.network.interfaces

/**
 * Abstract secure storage interface to abstract away direct SharedPreferences dependency.
 * This interface follows the Dependency Inversion Principle by providing an abstraction
 * over secure storage operations, making the code testable and allowing for different
 * storage implementations (SharedPreferences, encrypted storage, etc.).
 */
interface SecureStorage {

    /**
     * Stores a string value securely.
     *
     * @param key The key to store the value under
     * @param value The string value to store
     */
    suspend fun putString(key: String, value: String)

    /**
     * Retrieves a string value from secure storage.
     *
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return The stored string value or the default value
     */
    suspend fun getString(key: String, defaultValue: String? = null): String?

    /**
     * Stores an integer value securely.
     *
     * @param key The key to store the value under
     * @param value The integer value to store
     */
    suspend fun putInt(key: String, value: Int)

    /**
     * Retrieves an integer value from secure storage.
     *
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return The stored integer value or the default value
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int

    /**
     * Stores a boolean value securely.
     *
     * @param key The key to store the value under
     * @param value The boolean value to store
     */
    suspend fun putBoolean(key: String, value: Boolean)

    /**
     * Retrieves a boolean value from secure storage.
     *
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return The stored boolean value or the default value
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * Stores a long value securely.
     *
     * @param key The key to store the value under
     * @param value The long value to store
     */
    suspend fun putLong(key: String, value: Long)

    /**
     * Retrieves a long value from secure storage.
     *
     * @param key The key to retrieve the value for
     * @param defaultValue The default value to return if key doesn't exist
     * @return The stored long value or the default value
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long

    /**
     * Checks if a key exists in secure storage.
     *
     * @param key The key to check for existence
     * @return true if the key exists, false otherwise
     */
    suspend fun contains(key: String): Boolean

    /**
     * Removes a value from secure storage.
     *
     * @param key The key to remove
     */
    suspend fun remove(key: String)

    /**
     * Clears all values from secure storage.
     */
    suspend fun clear()

    /**
     * Applies multiple operations atomically.
     * This allows for batch operations to be performed as a single transaction.
     *
     * @param operations The operations to perform atomically
     */
    suspend fun edit(operations: suspend StorageEditor.() -> Unit)
}

/**
 * Interface for batch storage operations.
 */
interface StorageEditor {
    /**
     * Puts a string value in the batch operation.
     */
    fun putString(key: String, value: String): StorageEditor

    /**
     * Puts an integer value in the batch operation.
     */
    fun putInt(key: String, value: Int): StorageEditor

    /**
     * Puts a boolean value in the batch operation.
     */
    fun putBoolean(key: String, value: Boolean): StorageEditor

    /**
     * Puts a long value in the batch operation.
     */
    fun putLong(key: String, value: Long): StorageEditor

    /**
     * Removes a key in the batch operation.
     */
    fun remove(key: String): StorageEditor

    /**
     * Clears all data in the batch operation.
     */
    fun clear(): StorageEditor
}