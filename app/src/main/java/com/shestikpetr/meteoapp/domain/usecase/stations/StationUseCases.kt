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
