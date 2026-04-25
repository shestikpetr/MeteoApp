package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationAllData
import com.shestikpetr.meteoapp.data.model.StationParameterValue
import com.shestikpetr.meteoapp.data.model.StationWithData
import com.shestikpetr.meteoapp.data.model.UserStationResponse

class StationDataAggregator(private val stationRepository: StationRepository) {

    suspend fun getAllParameters(
        stations: List<UserStationResponse>
    ): Result<List<ParameterMetadata>> {
        return try {
            val allParameters = mutableMapOf<String, ParameterMetadata>()

            for (station in stations) {
                val stationNumber = station.station?.stationNumber ?: continue
                stationRepository.getStationParameters(stationNumber)
                    .getOrNull()?.forEach { param ->
                        if (!allParameters.containsKey(param.code)) {
                            allParameters[param.code] = param
                        }
                    }
            }

            Result.success(allParameters.values.toList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStationsWithData(
        stations: List<UserStationResponse>,
        selectedParameterCode: String?
    ): List<StationWithData> {
        return stations.mapNotNull { userStation ->
            val station = userStation.station ?: return@mapNotNull null
            val lat = station.latitude ?: return@mapNotNull null
            val lon = station.longitude ?: return@mapNotNull null

            var paramValue: Double? = null
            var unit: String? = null

            if (selectedParameterCode != null) {
                val paramsResult = stationRepository.getStationParameters(station.stationNumber)
                val hasParameter = paramsResult.getOrNull()
                    ?.any { it.code == selectedParameterCode } == true

                if (hasParameter) {
                    val dataResult = stationRepository.getLatestParameterValue(
                        station.stationNumber,
                        selectedParameterCode
                    )
                    dataResult.getOrNull()?.let { (value, paramUnit) ->
                        paramValue = value
                        unit = paramUnit
                    }
                }
            }

            StationWithData(
                stationNumber = station.stationNumber,
                name = station.name,
                customName = userStation.customName,
                latitude = lat,
                longitude = lon,
                parameterValue = paramValue,
                unit = unit
            )
        }
    }

    suspend fun getStationAllData(stationWithData: StationWithData): StationAllData {
        val parameters = mutableListOf<StationParameterValue>()

        stationRepository.getStationParameters(stationWithData.stationNumber)
            .getOrNull()?.forEach { param ->
                stationRepository.getLatestParameterValue(stationWithData.stationNumber, param.code)
                    .getOrNull()?.let { (value, unit) ->
                        parameters.add(
                            StationParameterValue(
                                code = param.code,
                                name = param.name,
                                value = value,
                                unit = unit
                            )
                        )
                    }
            }

        return StationAllData(
            stationNumber = stationWithData.stationNumber,
            name = stationWithData.name,
            customName = stationWithData.customName,
            latitude = stationWithData.latitude,
            longitude = stationWithData.longitude,
            parameters = parameters
        )
    }
}
