package com.shestikpetr.meteo.model

/**
 * Dynamic parameter configuration loaded from API.
 * Replaces the static Parameters enum with flexible configuration.
 */
data class ParameterConfig(
    val code: String,           // API parameter code (e.g., "T", "H", "4402")
    val name: String,           // Display name (e.g., "Температура")
    val unit: String,           // Measurement unit (e.g., "°C")
    val description: String,    // Full description
    val category: String,       // Parameter category from API
    val displayOrder: Int = 0,  // Order for UI display
    val isDefault: Boolean = false // Whether this parameter should be selected by default
) {
    /**
     * Unique identifier for this parameter configuration.
     * Used for comparison and caching.
     */
    val id: String get() = code

    /**
     * Full display text combining name and unit.
     */
    val displayText: String get() = "$name ($unit)"

    companion object {
        /**
         * Creates ParameterConfig from API ParameterInfo.
         */
        fun fromParameterInfo(
            parameterInfo: com.shestikpetr.meteo.network.ParameterInfo,
            displayOrder: Int = 0,
            isDefault: Boolean = false
        ): ParameterConfig {
            return ParameterConfig(
                code = parameterInfo.code,
                name = parameterInfo.name,
                unit = parameterInfo.unit,
                description = parameterInfo.description,
                category = parameterInfo.category,
                displayOrder = displayOrder,
                isDefault = isDefault
            )
        }

        /**
         * Default fallback parameter configurations when API is unavailable.
         * These match the legacy hardcoded values for backward compatibility.
         */
        val FALLBACK_CONFIGS = listOf(
            ParameterConfig(
                code = "4402",
                name = "Температура",
                unit = "°C",
                description = "Температура воздуха",
                category = "Meteorological",
                displayOrder = 1,
                isDefault = true
            ),
            ParameterConfig(
                code = "5402",
                name = "Влажность",
                unit = "%",
                description = "Относительная влажность воздуха",
                category = "Meteorological",
                displayOrder = 2,
                isDefault = false
            ),
            ParameterConfig(
                code = "700",
                name = "Давление",
                unit = "гПа",
                description = "Атмосферное давление",
                category = "Meteorological",
                displayOrder = 3,
                isDefault = false
            )
        )
    }
}

/**
 * Collection of parameter configurations for a specific context (station, user preferences, etc.).
 */
data class ParameterConfigSet(
    val parameters: List<ParameterConfig>,
    val defaultParameterCode: String? = null
) {
    /**
     * Gets parameter config by code.
     */
    fun getByCode(code: String): ParameterConfig? {
        return parameters.find { it.code == code }
    }

    /**
     * Gets default parameter config.
     */
    fun getDefault(): ParameterConfig? {
        return defaultParameterCode?.let { getByCode(it) }
            ?: parameters.find { it.isDefault }
            ?: parameters.firstOrNull()
    }

    /**
     * Gets parameters sorted by display order.
     */
    fun getSorted(): List<ParameterConfig> {
        return parameters.sortedBy { it.displayOrder }
    }

    /**
     * Gets parameters grouped by category.
     */
    fun getByCategory(): Map<String, List<ParameterConfig>> {
        return parameters.groupBy { it.category }
    }

    /**
     * Checks if a parameter code is available.
     */
    fun hasParameter(code: String): Boolean {
        return parameters.any { it.code == code }
    }

    companion object {
        /**
         * Creates fallback parameter set when API is unavailable.
         */
        fun fallback(): ParameterConfigSet {
            return ParameterConfigSet(
                parameters = ParameterConfig.FALLBACK_CONFIGS,
                defaultParameterCode = "4402" // Temperature as default
            )
        }

        /**
         * Creates empty parameter set.
         */
        fun empty(): ParameterConfigSet {
            return ParameterConfigSet(emptyList())
        }
    }
}