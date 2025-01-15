package com.example.meteoapp.api

import com.example.meteoapp.model.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {
    // Récupère les prévisions météorologiques pour une localisation donnée
    @GET("v1/forecast")
    suspend fun getWeatherForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        // Paramètres pour les données horaires: température, humidité, vent, précipitations et couverture nuageuse
        @Query("hourly") hourly: String = "temperature_2m,relativehumidity_2m,windspeed_10m,precipitation,cloud_cover",
        @Query("timezone") timezone: String = "auto"
    ): Response<WeatherResponse>

    companion object {
        // URL de base pour l'API météo
        const val BASE_URL = "https://api.open-meteo.com/"
    }
} 