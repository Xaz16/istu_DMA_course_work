package edu.istu.achipiga.coursework.data.remote

import edu.istu.achipiga.coursework.data.remote.dto.CityDto
import retrofit2.http.GET
import retrofit2.http.Query

interface CityGeocodingApi {
    @GET("v1/city")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("country") country: String
    ): List<CityDto>
}
