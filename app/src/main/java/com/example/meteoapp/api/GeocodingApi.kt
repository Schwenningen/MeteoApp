package com.example.meteoapp.api

import com.example.meteoapp.model.GeocodingResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApi {
    // Recherche une ville par son nom et retourne ses coordonnées géographiques
    @GET("v1/search")
    suspend fun searchLocation(
        @Query("name") cityName: String
    ): Response<GeocodingResponse>

    companion object {
        // URL de base pour l'API de géocodage
        const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    }
} 