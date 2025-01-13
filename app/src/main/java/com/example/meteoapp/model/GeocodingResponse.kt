package com.example.meteoapp.model

data class GeocodingResponse(
    val results: List<LocationResult>? = null,
    val generationtime_ms: Double
)

data class LocationResult(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val feature_code: String,
    val country_code: String,
    val timezone: String,
    val population: Int,
    val country_id: Long,
    val country: String,
    val admin1: String? = null,
    val admin2: String? = null,
    val admin3: String? = null,
    val admin4: String? = null,
    val admin1_id: Long? = null,
    val admin2_id: Long? = null,
    val admin3_id: Long? = null,
    val admin4_id: Long? = null,
    val postcodes: List<String>? = null
) 