package edu.istu.achipiga.coursework.data

sealed class WeatherLoadResult {
    data class Ok(val tempC: Int, val cityNameFromApi: String) : WeatherLoadResult()
    data object CityNotFound : WeatherLoadResult()
    data object NoForecastData : WeatherLoadResult()
}
