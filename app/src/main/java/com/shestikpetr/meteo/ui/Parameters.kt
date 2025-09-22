package com.shestikpetr.meteo.ui

/**
 * Enum representing the different weather parameters that can be displayed.
 * Each parameter has an associated unit for display purposes.
 */
enum class Parameters(private val unit: String) {
    TEMPERATURE("°C"),
    HUMIDITY("%"),
    PRESSURE("гПа");

    /**
     * Gets the unit string for this parameter.
     *
     * @return The unit string (e.g., "°C", "%", "гПа")
     */
    fun getUnit(): String {
        return unit
    }
}