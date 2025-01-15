package com.example.meteoapp.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timezone") val timezone: String,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int,
    @SerializedName("elevation") val elevation: Double,
    @SerializedName("hourly") val hourly: HourlyData,
    @SerializedName("hourly_units") val hourlyUnits: HourlyUnits
)

data class HourlyUnits(
    @SerializedName("time") val timeUnit: String,
    @SerializedName("temperature_2m") val temperatureUnit: String,
    @SerializedName("relativehumidity_2m") val humidityUnit: String,
    @SerializedName("windspeed_10m") val windSpeedUnit: String,
    @SerializedName("precipitation") val precipitationUnit: String,
    @SerializedName("cloud_cover") val cloudCoverUnit: String
)

data class HourlyData(
    @SerializedName("time") val times: List<String>,
    @SerializedName("temperature_2m") val temperatures: List<Double>,
    @SerializedName("relativehumidity_2m") val humidity: List<Int>,
    @SerializedName("windspeed_10m") val windSpeed: List<Double>,
    @SerializedName("precipitation") val precipitation: List<Double>,
    @SerializedName("cloud_cover") val cloudCover: List<Int>
) 