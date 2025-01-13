package com.example.meteoapp.api

import com.example.meteoapp.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("hourly") hourly: String = "temperature_2m,relative_humidity_2m,apparent_temperature,rain,wind_speed_10m",
        @Query("models") models: String = "meteofrance_seamless",
        @Query("timezone") timezone: String = "auto"
    ): Response<WeatherResponse>

    companion object {
        const val BASE_URL = "https://api.open-meteo.com/"
    }
} 