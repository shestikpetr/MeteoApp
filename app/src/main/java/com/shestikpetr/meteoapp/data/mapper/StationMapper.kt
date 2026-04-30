package com.shestikpetr.meteoapp.data.mapper

import com.shestikpetr.meteoapp.data.remote.dto.ParameterHistoryResponseDto
import com.shestikpetr.meteoapp.data.remote.dto.ParameterMetadataDto
import com.shestikpetr.meteoapp.data.remote.dto.ParameterWithValueDto
import com.shestikpetr.meteoapp.data.remote.dto.StationDataResponseDto
import com.shestikpetr.meteoapp.data.remote.dto.TimeSeriesPointDto
import com.shestikpetr.meteoapp.data.remote.dto.UserStationResponseDto
import com.shestikpetr.meteoapp.domain.model.ParameterHistory
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.ParameterReading
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint

fun UserStationResponseDto.toDomain(): Station = Station(
    stationNumber = stationNumber,
    name = name,
    location = location,
    latitude = latitude,
    longitude = longitude,
    altitude = altitude
)

fun ParameterMetadataDto.toDomain(): ParameterMeta = ParameterMeta(
    code = code,
    name = name,
    unit = unit,
    description = description
)

fun ParameterWithValueDto.toDomain(): ParameterReading = ParameterReading(
    code = code,
    name = name,
    unit = unit,
    description = description,
    value = value
)

fun StationDataResponseDto.toDomain(): StationLatest = StationLatest(
    stationNumber = stationNumber,
    name = name,
    location = location,
    latitude = latitude,
    longitude = longitude,
    time = time,
    parameters = parameters.map { it.toDomain() }
)

fun TimeSeriesPointDto.toDomain(): TimeSeriesPoint = TimeSeriesPoint(time = time, value = value)

fun ParameterHistoryResponseDto.toDomain(): ParameterHistory = ParameterHistory(
    parameter = parameter.toDomain(),
    points = data.map { it.toDomain() }
)
