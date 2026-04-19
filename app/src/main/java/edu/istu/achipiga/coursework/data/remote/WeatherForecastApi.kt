package edu.istu.achipiga.coursework.data.remote

import edu.istu.achipiga.coursework.data.remote.dto.OpenMeteoForecastDto
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherForecastApi {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m",
        @Query("forecast_days") forecastDays: Int = 7,
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoForecastDto
}
