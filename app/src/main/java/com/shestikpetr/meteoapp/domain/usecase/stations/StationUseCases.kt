package com.shestikpetr.meteoapp.domain.usecase.stations

import com.shestikpetr.meteoapp.domain.model.ParameterHistory
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.domain.repository.StationRepository
import javax.inject.Inject

class GetUserStationsUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(): Result<List<Station>> = repo.getUserStations()
}

class GetStationParametersUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(stationNumber: String): Result<List<ParameterMeta>> =
        repo.getStationParameters(stationNumber)
}

class GetStationLatestUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(stationNumber: String): Result<StationLatest> =
        repo.getStationLatest(stationNumber)
}

class GetParameterHistoryUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(
        stationNumber: String,
        parameterCode: Int,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<ParameterHistory> =
        repo.getParameterHistory(stationNumber, parameterCode, startTime, endTime)
}

class AttachStationUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(stationNumber: String, customName: String? = null): Result<Station> =
        repo.attachStation(stationNumber, customName)
}

class DetachStationUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(stationNumber: String): Result<Unit> =
        repo.detachStation(stationNumber)
}

class RenameStationUseCase @Inject constructor(
    private val repo: StationRepository
) {
    suspend operator fun invoke(stationNumber: String, customName: String?): Result<Station> =
        repo.renameStation(stationNumber, customName)
}

/**
 * Объединяет метаданные параметров со всех станций пользователя в один уникальный список.
 * Бизнес-логика, не относящаяся к самому репозиторию.
 */
class GetAllParametersUseCase @Inject constructor(
    private val getStationParameters: GetStationParametersUseCase
) {
    suspend operator fun invoke(stations: List<Station>): Result<List<ParameterMeta>> = runCatching {
        val byCode = mutableMapOf<Int, ParameterMeta>()
        for (station in stations) {
            getStationParameters(station.stationNumber).getOrNull()?.forEach { p ->
                byCode.putIfAbsent(p.code, p)
            }
        }
        byCode.values.toList()
    }
}
