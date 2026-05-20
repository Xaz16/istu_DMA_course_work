package edu.istu.achipiga.coursework.ui

import android.app.Application
import android.content.res.Resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import edu.istu.achipiga.coursework.R
import edu.istu.achipiga.coursework.data.WeatherLoadResult
import edu.istu.achipiga.coursework.data.WeatherRepository
import edu.istu.achipiga.coursework.data.local.SavedCityDao
import edu.istu.achipiga.coursework.data.local.SavedCityEntity
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CityWeatherRow(
    val cityId: Long,
    val city: String,
    val ok: Boolean,
    val errorMessage: String?,
    val tempC: Int?,
    val conditionLabel: String?,
    val highLowLabel: String?,
    val hourly: List<HourChipUi>,
    @DrawableRes val cardBackgroundRes: Int
)

data class WeatherUiState(
    val loading: Boolean = false,
    val rows: List<CityWeatherRow> = emptyList(),
    val error: String? = null,
    val isEmpty: Boolean = false,
    val snackbarMessage: String? = null
)

class WeatherViewModel(
    application: Application,
    private val repository: WeatherRepository,
    private val savedCityDao: SavedCityDao
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            savedCityDao.observeSavedCities().collect { entities ->
                loadWeatherFor(entities)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                savedCityDao.observeSavedCities().first()
            }
            loadWeatherFor(list)
        }
    }

    private suspend fun loadWeatherFor(entities: List<SavedCityEntity>) {
        if (entities.isEmpty()) {
            _state.update {
                WeatherUiState(loading = false, rows = emptyList(), isEmpty = true, error = null)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null, isEmpty = false) }
        val app = getApplication<Application>()
        try {
            val rows = coroutineScope {
                entities.map { entity ->
                    async {
                        val apiName = entity.apiName
                        val label = cityDisplayName(app.resources, app.packageName, apiName)
                        when (val r = repository.loadCityWeather(apiName)) {
                            is WeatherLoadResult.Ok -> {
                                val name = cityDisplayName(
                                    app.resources,
                                    app.packageName,
                                    apiName,
                                    r.cityNameFromApi
                                )
                                CityWeatherRow(
                                    cityId = entity.id,
                                    city = name,
                                    ok = true,
                                    errorMessage = null,
                                    tempC = r.tempC,
                                    conditionLabel = app.getString(wmoTitleRes(r.wmoCode)),
                                    highLowLabel = app.getString(R.string.weather_high_low, r.highC, r.lowC),
                                    hourly = r.hourly.map { h ->
                                        HourChipUi(
                                            h.clockLabel,
                                            h.tempC,
                                            wmoHourIcon(h.wmoCode)
                                        )
                                    },
                                    cardBackgroundRes = wmoCardBg(r.wmoCode)
                                )
                            }
                            WeatherLoadResult.CityNotFound ->
                                CityWeatherRow(
                                    entity.id,
                                    label,
                                    ok = false,
                                    app.getString(R.string.error_city_not_found),
                                    null,
                                    null,
                                    null,
                                    emptyList(),
                                    R.drawable.weather_bg_cloudy
                                )
                            WeatherLoadResult.NoForecastData ->
                                CityWeatherRow(
                                    entity.id,
                                    label,
                                    ok = false,
                                    app.getString(R.string.error_no_forecast),
                                    null,
                                    null,
                                    null,
                                    emptyList(),
                                    R.drawable.weather_bg_cloudy
                                )
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
                    error = e.message ?: app.getString(R.string.error_generic),
                    isEmpty = false
                )
            }
        }
    }

    fun addCity(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            if (withContext(Dispatchers.IO) { savedCityDao.countByNameIgnoreCase(trimmed) } > 0) {
                _state.update {
                    it.copy(snackbarMessage = app.getString(R.string.city_already_saved))
                }
                return@launch
            }
            when (val r = withContext(Dispatchers.IO) { repository.loadCityWeather(trimmed) }) {
                is WeatherLoadResult.Ok -> {
                    val next = withContext(Dispatchers.IO) { savedCityDao.maxSortOrder() + 1 }
                    withContext(Dispatchers.IO) {
                        savedCityDao.insert(SavedCityEntity(apiName = trimmed, sortOrder = next))
                    }
                }
                else -> {
                    _state.update {
                        it.copy(snackbarMessage = app.getString(R.string.city_add_failed))
                    }
                }
            }
        }
    }

    fun clearSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
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

    @DrawableRes
    private fun wmoCardBg(code: Int): Int = when (code) {
        0, 1 -> R.drawable.weather_bg_clear
        in 51..67, in 80..99 -> R.drawable.weather_bg_rain
        else -> R.drawable.weather_bg_cloudy
    }

    @DrawableRes
    private fun wmoHourIcon(code: Int): Int = when (code) {
        0, 1 -> R.drawable.ic_wx_sun
        2, 3 -> R.drawable.ic_wx_cloud
        45, 48 -> R.drawable.ic_wx_fog
        in 51..67, in 80..82 -> R.drawable.ic_wx_rain
        in 71..77 -> R.drawable.ic_wx_snow
        in 95..99 -> R.drawable.ic_wx_storm
        else -> R.drawable.ic_wx_cloud
    }

    @StringRes
    private fun wmoTitleRes(code: Int): Int = when (code) {
        0 -> R.string.wx_clear
        1 -> R.string.wx_mainly_clear
        2 -> R.string.wx_partly_cloudy
        3 -> R.string.wx_overcast
        45, 48 -> R.string.wx_fog
        in 51..57 -> R.string.wx_drizzle
        in 61..67 -> R.string.wx_rain
        in 71..77 -> R.string.wx_snow
        in 80..82 -> R.string.wx_showers
        in 95..99 -> R.string.wx_thunder
        else -> R.string.wx_cloudy
    }

    class Factory(
        private val application: Application,
        private val repository: WeatherRepository,
        private val savedCityDao: SavedCityDao
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
                return WeatherViewModel(application, repository, savedCityDao) as T
            }
            throw IllegalArgumentException()
        }
    }
}
