package com.shestikpetr.meteoapp.data.repository

import com.shestikpetr.meteoapp.data.mapper.toDomain
import com.shestikpetr.meteoapp.data.remote.api.MeteoApi
import com.shestikpetr.meteoapp.data.remote.dto.UserStationRequest
import com.shestikpetr.meteoapp.data.remote.dto.UserStationUpdateRequest
import com.shestikpetr.meteoapp.domain.model.ParameterHistory
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.domain.repository.StationRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StationRepositoryImpl @Inject constructor(
    private val api: MeteoApi
) : StationRepository {

    override suspend fun getUserStations(): Result<List<Station>> = runCatching {
        unwrap(api.getUserStations()) { it.data }.orEmpty().map { it.toDomain() }
    }

    override suspend fun getStationParameters(stationNumber: String): Result<List<ParameterMeta>> =
        runCatching {
            val parameters = unwrap(api.getStationParameters(stationNumber)) { it.data }?.parameters
            parameters?.map { it.toDomain() } ?: emptyList()
        }

    override suspend fun getStationLatest(stationNumber: String): Result<StationLatest> = runCatching {
        unwrap(api.getStationData(stationNumber)) { it.data }?.toDomain()
            ?: error("Нет данных")
    }

    override suspend fun getParameterHistory(
        stationNumber: String,
        parameterCode: Int,
        startTime: Long?,
        endTime: Long?
    ): Result<ParameterHistory> = runCatching {
        unwrap(
            api.getParameterHistory(stationNumber, parameterCode, startTime, endTime)
        ) { it.data }?.toDomain() ?: error("Не удалось загрузить историю")
    }

    override suspend fun attachStation(stationNumber: String, customName: String?): Result<Station> =
        runCatching {
            val response = api.attachStation(UserStationRequest(stationNumber, customName))
            unwrap(response) { it.data }?.toDomain() ?: error("Не удалось добавить станцию")
        }

    override suspend fun detachStation(stationNumber: String): Result<Unit> = runCatching {
        val response = api.detachStation(stationNumber)
        val body = response.body()
        if (!response.isSuccessful || body == null || !body.success) {
            error(body?.error ?: response.errorBody()?.string() ?: "Не удалось удалить станцию")
        }
    }

    override suspend fun renameStation(stationNumber: String, customName: String?): Result<Station> =
        runCatching {
            val response = api.renameStation(stationNumber, UserStationUpdateRequest(customName))
            unwrap(response) { it.data }?.toDomain() ?: error("Не удалось переименовать станцию")
        }

    private fun <B, T> unwrap(response: Response<B>, extractor: (B) -> T?): T? {
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            error(response.errorBody()?.string() ?: "Request failed")
        }
        val errorField = (body as? HasError)?.error
        if (errorField != null) error(errorField)
        return extractor(body)
    }

    private interface HasError { val error: String? }
}
