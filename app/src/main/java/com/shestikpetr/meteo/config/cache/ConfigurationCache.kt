package com.shestikpetr.meteo.config.cache

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe cache for configuration data with TTL (Time To Live) support.
 * Follows Single Responsibility Principle by handling only caching concerns.
 */
@Singleton
class ConfigurationCache @Inject constructor() {

    private val cache = mutableMapOf<String, CacheEntry<*>>()
    private val mutex = Mutex()

    /**
     * Represents a cached entry with TTL support
     */
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() - timestamp > ttlMs
    }

    /**
     * Puts data into cache with specified TTL
     */
    suspend fun <T> put(key: String, data: T, ttlMs: Long = DEFAULT_TTL_MS) {
        mutex.withLock {
            cache[key] = CacheEntry(data, System.currentTimeMillis(), ttlMs)
            Log.d("ConfigurationCache", "Cached data for key: $key, TTL: ${ttlMs}ms")
        }
    }

    /**
     * Gets data from cache if not expired
     */
    suspend fun <T> get(key: String): T? {
        return mutex.withLock {
            val entry = cache[key] as? CacheEntry<T>
            if (entry != null && !entry.isExpired) {
                Log.d("ConfigurationCache", "Cache hit for key: $key")
                entry.data
            } else {
                if (entry?.isExpired == true) {
                    Log.d("ConfigurationCache", "Cache expired for key: $key")
                    cache.remove(key)
                } else {
                    Log.d("ConfigurationCache", "Cache miss for key: $key")
                }
                null
            }
        }
    }

    /**
     * Checks if a key exists and is not expired
     */
    suspend fun contains(key: String): Boolean {
        return mutex.withLock {
            val entry = cache[key]
            entry != null && !entry.isExpired
        }
    }

    /**
     * Invalidates a specific cache entry
     */
    suspend fun invalidate(key: String) {
        mutex.withLock {
            cache.remove(key)
            Log.d("ConfigurationCache", "Invalidated cache for key: $key")
        }
    }

    /**
     * Clears all cache entries
     */
    suspend fun clear() {
        mutex.withLock {
            val size = cache.size
            cache.clear()
            Log.d("ConfigurationCache", "Cleared all cache entries (was $size entries)")
        }
    }

    /**
     * Removes expired entries from cache
     */
    suspend fun cleanupExpired() {
        mutex.withLock {
            val iterator = cache.entries.iterator()
            var removed = 0
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if ((entry.value as CacheEntry<*>).isExpired) {
                    iterator.remove()
                    removed++
                }
            }
            if (removed > 0) {
                Log.d("ConfigurationCache", "Cleaned up $removed expired cache entries")
            }
        }
    }

    /**
     * Gets cache statistics for debugging
     */
    suspend fun getStats(): CacheStats {
        return mutex.withLock {
            val expired = cache.values.count { (it as CacheEntry<*>).isExpired }
            CacheStats(
                totalEntries = cache.size,
                expiredEntries = expired,
                validEntries = cache.size - expired
            )
        }
    }

    data class CacheStats(
        val totalEntries: Int,
        val expiredEntries: Int,
        val validEntries: Int
    )

    companion object {
        // Default TTL: 5 minutes for configuration data
        const val DEFAULT_TTL_MS = 5 * 60 * 1000L

        // Cache keys
        const val VALIDATION_CONFIG_KEY = "validation_config"
        const val RETRY_CONFIG_KEY = "retry_config"
        const val THEME_CONFIG_KEY = "theme_config"
        const val DEMO_CONFIG_KEY = "demo_config"
    }
}