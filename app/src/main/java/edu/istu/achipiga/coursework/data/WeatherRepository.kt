package edu.istu.achipiga.coursework.data

import edu.istu.achipiga.coursework.BuildConfig
import edu.istu.achipiga.coursework.data.remote.CityGeocodingApi
import edu.istu.achipiga.coursework.data.remote.WeatherForecastApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherRepository {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request()
            val next = if (req.url.host == "api.api-ninjas.com" && BuildConfig.NINJAS_API_KEY.isNotEmpty()) {
                req.newBuilder().header("X-Api-Key", BuildConfig.NINJAS_API_KEY).build()
            } else {
                req
            }
            chain.proceed(next)
        }
        .build()

    private val gsonFactory = GsonConverterFactory.create()

    private val cityGeocoding: CityGeocodingApi = Retrofit.Builder()
        .baseUrl("https://api.api-ninjas.com/")
        .client(client)
        .addConverterFactory(gsonFactory)
        .build()
        .create(CityGeocodingApi::class.java)

    private val weatherForecast: WeatherForecastApi = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client)
        .addConverterFactory(gsonFactory)
        .build()
        .create(WeatherForecastApi::class.java)

    suspend fun loadCityWeather(cityApiName: String): WeatherLoadResult = withContext(Dispatchers.IO) {
        var list = cityGeocoding.searchCity(cityApiName, COUNTRY_RU)
        if (list.isEmpty() && cityApiName.contains('-')) {
            list = cityGeocoding.searchCity(cityApiName.replace('-', ' '), COUNTRY_RU)
        }
        val city = list.firstOrNull() ?: return@withContext WeatherLoadResult.CityNotFound
        val fc = weatherForecast.forecast(
            latitude = city.latitude,
            longitude = city.longitude,
            timezone = "auto"
        )
        val hourly = fc.hourly ?: return@withContext WeatherLoadResult.NoForecastData
        val times = hourly.time.orEmpty()
        val temps = hourly.temperature2m.orEmpty()
        val codes = hourly.weatherCode.orEmpty()
        if (times.isEmpty() || temps.isEmpty()) return@withContext WeatherLoadResult.NoForecastData
        val zoneId = fc.timezone?.takeIf { it.isNotBlank() } ?: "UTC"
        val nowIso = nowIsoInTimeZone(zoneId)
        var startIdx = 0
        for (i in times.indices) {
            if (times[i] <= nowIso) {
                startIdx = i
            } else {
                break
            }
        }
        val span = minOf(48, times.size - startIdx, temps.size - startIdx)
        if (span <= 0) return@withContext WeatherLoadResult.NoForecastData
        val sliceTemps = (0 until span).map { temps[startIdx + it] }
        val high = sliceTemps.maxOrNull()?.toInt() ?: temps[startIdx].toInt()
        val low = sliceTemps.minOrNull()?.toInt() ?: temps[startIdx].toInt()
        val hours = ArrayList<HourForecast>(span)
        for (i in 0 until span) {
            val idx = startIdx + i
            val wmo = codes.getOrElse(idx) { 0.0 }.toInt()
            hours.add(HourForecast(clockLabel(times[idx]), temps[idx].toInt(), wmo))
        }
        val cur = fc.current
        val tempC = cur?.temperature2m?.let { kotlin.math.round(it).toInt() }
            ?: temps[startIdx].toInt()
        val wmo0 = cur?.weatherCode?.toInt() ?: codes.getOrElse(startIdx) { 0.0 }.toInt()
        WeatherLoadResult.Ok(
            tempC = tempC,
            cityNameFromApi = city.name,
            wmoCode = wmo0,
            highC = high,
            lowC = low,
            hourly = hours
        )
    }

    private companion object {
        const val COUNTRY_RU = "RU"
    }
}

private fun nowIsoInTimeZone(zoneId: String): String {
    val tz = TimeZone.getTimeZone(zoneId)
    val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = tz }
    return fmt.format(Date())
}

private fun clockLabel(iso: String): String {
    val i = iso.indexOf('T')
    if (i >= 0 && iso.length >= i + 6) {
        return iso.substring(i + 1, i + 6)
    }
    return iso
}
