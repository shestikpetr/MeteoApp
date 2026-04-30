package com.shestikpetr.meteoapp.domain.model

/** Описание параметра: код, имя, единица измерения. Без значения. */
data class ParameterMeta(
    val code: Int,
    val name: String,
    val unit: String? = null,
    val description: String? = null
)

/** Метаданные параметра + значение в момент измерения. value=null — нет данных. */
data class ParameterReading(
    val code: Int,
    val name: String,
    val unit: String?,
    val description: String?,
    val value: Double?
) {
    val meta: ParameterMeta get() = ParameterMeta(code, name, unit, description)
}

/** Снимок последних показаний станции: единое время + список значений активных параметров. */
data class StationLatest(
    val stationNumber: String,
    val name: String,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val time: Long?,
    val parameters: List<ParameterReading>
) {
    fun valueOf(code: Int): Double? = parameters.firstOrNull { it.code == code }?.value
    fun unitOf(code: Int): String? = parameters.firstOrNull { it.code == code }?.unit
}

/** Точка временного ряда. */
data class TimeSeriesPoint(
    val time: Long,
    val value: Double
)

/** История значений одного параметра. */
data class ParameterHistory(
    val parameter: ParameterMeta,
    val points: List<TimeSeriesPoint>
)
