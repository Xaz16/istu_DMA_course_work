package edu.istu.achipiga.coursework.data.remote.dto

import com.google.gson.annotations.SerializedName

data class OpenMeteoForecastDto(
    val hourly: OpenMeteoHourlyDto?
)

data class OpenMeteoHourlyDto(
    val time: List<String>?,
    @SerializedName("temperature_2m")
    val temperature2m: List<Double>?
)
