package edu.istu.achipiga.coursework.data

data class HourForecast(
    val clockLabel: String,
    val tempC: Int,
    val wmoCode: Int
)

sealed class WeatherLoadResult {
    data class Ok(
        val tempC: Int,
        val cityNameFromApi: String,
        val wmoCode: Int,
        val highC: Int,
        val lowC: Int,
        val hourly: List<HourForecast>
    ) : WeatherLoadResult()
    data object CityNotFound : WeatherLoadResult()
    data object NoForecastData : WeatherLoadResult()
}
