package edu.istu.achipiga.coursework.ui

import android.app.Application
import android.content.res.Resources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.istu.achipiga.coursework.R
import edu.istu.achipiga.coursework.data.WeatherLoadResult
import edu.istu.achipiga.coursework.data.WeatherRepository
import java.util.Locale
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CityWeatherRow(
    val city: String,
    val text: String
)

data class WeatherUiState(
    val loading: Boolean = false,
    val rows: List<CityWeatherRow> = emptyList(),
    val error: String? = null
)

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository
) : AndroidViewModel(application) {

    private val cityQueries = listOf(
        "Izhevsk",
        "Moscow",
        "Saint Petersburg",
        "Novosibirsk",
        "Yekaterinburg",
        "Kazan",
        "Chelyabinsk",
        "Samara",
        "Omsk",
        "Ufa",
        "Krasnoyarsk",
        "Voronezh",
        "Perm",
        "Volgograd"
    )

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, rows = emptyList()) }
            val app = getApplication<Application>()
            try {
                val rows = coroutineScope {
                    cityQueries.map { apiName ->
                        async {
                            val label = cityDisplayName(app.resources, app.packageName, apiName)
                            when (val r = repository.loadCityWeather(apiName)) {
                                is WeatherLoadResult.Ok -> {
                                    val name = cityDisplayName(app.resources, app.packageName, apiName, r.cityNameFromApi)
                                    CityWeatherRow(
                                        name,
                                        app.getString(R.string.weather_detail, r.tempC)
                                    )
                                }
                                WeatherLoadResult.CityNotFound ->
                                    CityWeatherRow(label, app.getString(R.string.error_city_not_found))
                                WeatherLoadResult.NoForecastData ->
                                    CityWeatherRow(label, app.getString(R.string.error_no_forecast))
                            }
                        }
                    }.awaitAll()
                }
                _state.update { it.copy(loading = false, rows = rows, error = null) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        loading = false,
                        rows = emptyList(),
                        error = e.message ?: app.getString(R.string.error_generic)
                    )
                }
            }
        }
    }

    private fun cityDisplayName(
        resources: Resources,
        appPackageName: String,
        apiQuery: String,
        apiReturnedName: String? = null
    ): String {
        val slug = apiQuery.lowercase(Locale.US).replace('-', '_').replace(' ', '_')
        val resId = resources.getIdentifier("city_$slug", "string", appPackageName)
        if (resId != 0) {
            return resources.getString(resId)
        }
        return apiReturnedName ?: apiQuery
    }

    class Factory(
        private val application: Application,
        private val repository: WeatherRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(application, repository) as T
            }
            throw IllegalArgumentException()
        }
    }
}
