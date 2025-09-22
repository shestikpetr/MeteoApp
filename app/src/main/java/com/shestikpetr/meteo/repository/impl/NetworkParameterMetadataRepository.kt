package com.shestikpetr.meteo.repository.impl

import android.util.Log
import com.shestikpetr.meteo.network.AuthManager
import com.shestikpetr.meteo.network.MeteoApiService
import com.shestikpetr.meteo.network.ParameterMetadata
import com.shestikpetr.meteo.repository.interfaces.ParameterMetadataRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network implementation of ParameterMetadataRepository interface.
 * Since API v1 doesn't have dedicated parameter metadata endpoints,
 * this implementation provides static parameter metadata for common weather parameters.
 */
@Singleton
class NetworkParameterMetadataRepository @Inject constructor(
    private val meteoApiService: MeteoApiService,
    private val authManager: AuthManager
) : ParameterMetadataRepository {

    companion object {
        private const val TAG = "ParameterMetadataRepo"

        // Static parameter metadata for common weather parameters
        private val STATIC_PARAMETER_METADATA = mapOf(
            "T" to ParameterMetadata("Температура", "°C", "Температура воздуха"),
            "H" to ParameterMetadata("Влажность", "%", "Относительная влажность воздуха"),
            "P" to ParameterMetadata("Давление", "кПа", "Атмосферное давление"),
            "WS" to ParameterMetadata("Скорость ветра", "м/с", "Скорость ветра"),
            "WD" to ParameterMetadata("Направление ветра", "°", "Направление ветра"),
            "R" to ParameterMetadata("Осадки", "мм", "Количество осадков"),
            "temperature" to ParameterMetadata("Температура", "°C", "Температура воздуха"),
            "humidity" to ParameterMetadata("Влажность", "%", "Относительная влажность воздуха"),
            "pressure" to ParameterMetadata("Давление", "кПа", "Атмосферное давление"),
            "wind_speed" to ParameterMetadata("Скорость ветра", "м/с", "Скорость ветра"),
            "wind_direction" to ParameterMetadata("Направление ветра", "°", "Направление ветра"),
            "rainfall" to ParameterMetadata("Осадки", "мм", "Количество осадков")
        )
    }

    // Cache for parameter metadata to avoid repeated network calls
    private var cachedParametersMetadata: Map<String, ParameterMetadata>? = null
    private val cacheMutex = Mutex()

    override suspend fun getParametersMetadata(): Map<String, ParameterMetadata> {
        return cacheMutex.withLock {
            if (cachedParametersMetadata == null) {
                try {
                    // Since API v1 doesn't have parameter metadata endpoints,
                    // we return our static metadata
                    cachedParametersMetadata = STATIC_PARAMETER_METADATA
                    Log.d(TAG, "Loaded ${cachedParametersMetadata?.size} parameter metadata (static)")
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading parameter metadata: ${e.message}", e)
                    throw e
                }
            }
            cachedParametersMetadata ?: emptyMap()
        }
    }

    override suspend fun getParameterMetadata(parameterId: String): ParameterMetadata {
        val allMetadata = getParametersMetadata()
        return allMetadata[parameterId]
            ?: throw IllegalArgumentException("Parameter metadata not found for: $parameterId")
    }

    override suspend fun getMultipleParametersMetadata(parameterIds: List<String>): Map<String, ParameterMetadata> {
        val allMetadata = getParametersMetadata()
        return parameterIds.mapNotNull { parameterId ->
            allMetadata[parameterId]?.let { metadata ->
                parameterId to metadata
            }
        }.toMap()
    }

    override suspend fun parameterExists(parameterId: String): Boolean {
        return try {
            val allMetadata = getParametersMetadata()
            allMetadata.containsKey(parameterId)
        } catch (e: Exception) {
            Log.w("ParameterMetadataRepository", "Error checking parameter existence for $parameterId: ${e.message}")
            false
        }
    }

    override suspend fun getAvailableParameterIds(): Set<String> {
        val allMetadata = getParametersMetadata()
        return allMetadata.keys.toSet()
    }

    override suspend fun searchParameters(query: String): Map<String, ParameterMetadata> {
        val allMetadata = getParametersMetadata()
        val lowercaseQuery = query.lowercase()

        return allMetadata.filter { (parameterId, metadata) ->
            parameterId.lowercase().contains(lowercaseQuery) ||
                    metadata.name.lowercase().contains(lowercaseQuery) ||
                    metadata.description.lowercase().contains(lowercaseQuery)
        }
    }

    override suspend fun getParametersByUnit(): Map<String, List<ParameterMetadata>> {
        val allMetadata = getParametersMetadata()
        return allMetadata.values.groupBy { it.unit }
    }

    override suspend fun getParameterDisplayName(parameterId: String): String {
        return try {
            val metadata = getParameterMetadata(parameterId)
            metadata.name
        } catch (e: Exception) {
            Log.w("ParameterMetadataRepository", "Error getting display name for $parameterId: ${e.message}")
            parameterId // Fallback to parameter ID
        }
    }

    override suspend fun getParameterUnit(parameterId: String): String {
        return try {
            val metadata = getParameterMetadata(parameterId)
            metadata.unit
        } catch (e: Exception) {
            Log.w("ParameterMetadataRepository", "Error getting unit for $parameterId: ${e.message}")
            "" // Fallback to empty string
        }
    }

    override suspend fun getParameterDescription(parameterId: String): String {
        return try {
            val metadata = getParameterMetadata(parameterId)
            metadata.description
        } catch (e: Exception) {
            Log.w("ParameterMetadataRepository", "Error getting description for $parameterId: ${e.message}")
            "" // Fallback to empty string
        }
    }

    override suspend fun refreshParameterMetadata() {
        cacheMutex.withLock {
            cachedParametersMetadata = null
        }
        // Preload fresh data
        getParametersMetadata()
    }
}