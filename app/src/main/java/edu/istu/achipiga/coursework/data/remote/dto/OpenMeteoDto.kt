package edu.istu.achipiga.coursework.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoForecastDto(
    val timezone: String?,
    val current: OpenMeteoCurrentDto?,
    val hourly: OpenMeteoHourlyDto?
)

data class OpenMeteoCurrentDto(
    val time: String?,
    @SerializedName("temperature_2m")
    val temperature2m: Double?,
    @SerializedName("weather_code")
    val weatherCode: Double?
)

data class OpenMeteoHourlyDto(
    val time: List<String>?,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>?,
    @SerializedName("weather_code")
    val weatherCode: List<Double>?
)
