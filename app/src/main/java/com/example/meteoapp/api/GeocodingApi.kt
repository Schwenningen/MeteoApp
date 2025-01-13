package com.example.meteoapp.api

import com.example.meteoapp.model.GeocodingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") cityName: String
    ): Response<GeocodingResponse>

    companion object {
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
} 