package edu.istu.achipiga.coursework.data

import edu.istu.achipiga.coursework.BuildConfig
import edu.istu.achipiga.coursework.data.remote.CityGeocodingApi
import edu.istu.achipiga.coursework.data.remote.WeatherForecastApi
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
        val temps = fc.hourly?.temperature2m
        val t0 = temps?.firstOrNull() ?: return@withContext WeatherLoadResult.NoForecastData
        WeatherLoadResult.Ok(t0.toInt(), city.name)
    }

    private companion object {
        const val COUNTRY_RU = "RU"
    }
}
