package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationAllData
import com.shestikpetr.meteoapp.data.model.StationParameterValue
import com.shestikpetr.meteoapp.data.model.StationWithData
import com.shestikpetr.meteoapp.data.model.UserStationResponse

class StationDataAggregator(private val stationRepository: StationRepository) {

    /**
     * Объединяет метаданные параметров со всех станций пользователя в один уникальный список.
     */
    suspend fun getAllParameters(
        stations: List<UserStationResponse>
    ): Result<List<ParameterMetadata>> {
        return try {
            val allParameters = mutableMapOf<Int, ParameterMetadata>()

            for (station in stations) {
                stationRepository.getStationParameters(station.stationNumber)
                    .getOrNull()?.forEach { param ->
                        allParameters.putIfAbsent(param.code, param)
                    }
            }

            Result.success(allParameters.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Для списка станций возвращает данные для отображения на карте.
     * Если задан [selectedParameterCode] — заполняет [StationWithData.parameterValue]
     * последним известным значением этого параметра (или null, если параметра нет/значение пустое).
     */
    suspend fun getStationsWithData(
        stations: List<UserStationResponse>,
        selectedParameterCode: Int?
    ): List<StationWithData> {
        return stations.mapNotNull { station ->
            val lat = station.latitude ?: return@mapNotNull null
            val lon = station.longitude ?: return@mapNotNull null

            var paramValue: Double? = null
            var unit: String? = null

            if (selectedParameterCode != null) {
                stationRepository.getStationData(station.stationNumber)
                    .getOrNull()
                    ?.parameters
                    ?.firstOrNull { it.code == selectedParameterCode }
                    ?.let { param ->
                        paramValue = param.value
                        unit = param.unit
                    }
            }

            StationWithData(
                stationNumber = station.stationNumber,
                name = station.name,
                latitude = lat,
                longitude = lon,
                parameterValue = paramValue,
                unit = unit
            )
        }
    }

    /**
     * Возвращает все актуальные показатели станции одним запросом /data.
     * Параметры с `value == null` отбрасываются.
     */
    suspend fun getStationAllData(stationWithData: StationWithData): StationAllData {
        val data = stationRepository.getStationData(stationWithData.stationNumber).getOrNull()

        val parameters = data?.parameters?.mapNotNull { param ->
            val value = param.value ?: return@mapNotNull null
            StationParameterValue(
                code = param.code,
                name = param.name,
                value = value,
                unit = param.unit
            )
        } ?: emptyList()

        return StationAllData(
            stationNumber = stationWithData.stationNumber,
            name = stationWithData.name,
            latitude = stationWithData.latitude,
            longitude = stationWithData.longitude,
            time = data?.time,
            parameters = parameters
        )
    }
}
