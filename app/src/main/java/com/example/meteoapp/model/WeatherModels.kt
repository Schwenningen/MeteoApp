package com.example.meteoapp.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: String,
    @SerializedName("timezone_abbreviation") 
    val timezoneAbbreviation: String,
    @SerializedName("utc_offset_seconds")
    val utcOffsetSeconds: Int,
    val hourly: HourlyData,
    @SerializedName("hourly_units")
    val hourlyUnits: HourlyUnits
)

data class HourlyUnits(
    @SerializedName("temperature_2m") 
    val temperatureUnit: String,
    @SerializedName("relative_humidity_2m") 
    val humidityUnit: String,
    @SerializedName("apparent_temperature") 
    val apparentTemperatureUnit: String,
    @SerializedName("rain") 
    val rainUnit: String,
    @SerializedName("wind_speed_10m") 
    val windSpeedUnit: String
)

data class HourlyData(
    @SerializedName("time") 
    val times: List<String>,
    @SerializedName("temperature_2m") 
    val temperatures: List<Double>,
    @SerializedName("relative_humidity_2m") 
    val humidity: List<Double>,
    @SerializedName("apparent_temperature") 
    val apparentTemperatures: List<Double>,
    @SerializedName("rain") 
    val rain: List<Double>,
    @SerializedName("wind_speed_10m") 
    val windSpeed: List<Double>
) 