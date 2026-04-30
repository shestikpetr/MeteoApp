package com.shestikpetr.meteoapp.domain.repository

import com.shestikpetr.meteoapp.domain.model.ParameterHistory
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest

/**
 * Доступ к станциям пользователя и их данным.
 */
interface StationRepository {

    suspend fun getUserStations(): Result<List<Station>>

    suspend fun getStationParameters(stationNumber: String): Result<List<ParameterMeta>>

    suspend fun getStationLatest(stationNumber: String): Result<StationLatest>

    suspend fun getParameterHistory(
        stationNumber: String,
        parameterCode: Int,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<ParameterHistory>

    suspend fun attachStation(stationNumber: String, customName: String? = null): Result<Station>

    suspend fun detachStation(stationNumber: String): Result<Unit>

    suspend fun renameStation(stationNumber: String, customName: String?): Result<Station>
}
