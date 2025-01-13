package com.example.meteoapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meteoapp.api.ApiClient
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherScreen() {
    var weatherText by remember { mutableStateOf("Loading...") }
    
    LaunchedEffect(Unit) {
        try {
            // First, get city coordinates
            val geoResponse = ApiClient.geocodingApi.searchLocation("Paris")
            
            if (geoResponse.isSuccessful) {
                val location = geoResponse.body()?.results?.firstOrNull()
                
                if (location != null) {
                    weatherText = "Location found: ${location.name}, ${location.country}\n\nFetching weather data..."
                    
                    // Now get weather for the coordinates
                    val weatherResponse = ApiClient.weatherApi.getWeatherForecast(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    
                    if (weatherResponse.isSuccessful) {
                        val weather = weatherResponse.body()
                        weather?.let {
                            weatherText = buildString {
                                append("Location: ${location.name}, ${location.country}\n\n")
                                append("Weather Data:\n")
                                append("Coordinates: ${it.latitude}°N ${it.longitude}°E\n")
                                append("Elevation: ${it.elevation}m\n")
                                append("Timezone: ${it.timezone} ${it.timezoneAbbreviation}\n")
                                append("UTC offset: ${it.utcOffsetSeconds}s\n\n")
                                
                                append("Temperature (next 5 hours):\n")
                                it.hourly.times.zip(it.hourly.temperatures)
                                    .take(5)
                                    .forEach { (time, temp) ->
                                        append("$time: $temp${it.hourlyUnits.temperatureUnit}\n")
                                    }
                                
                                append("\nHumidity (next 5 hours):\n")
                                it.hourly.times.zip(it.hourly.humidity)
                                    .take(5)
                                    .forEach { (time, humidity) ->
                                        append("$time: $humidity${it.hourlyUnits.humidityUnit}\n")
                                    }
                                
                                append("\nWind Speed (next 5 hours):\n")
                                it.hourly.times.zip(it.hourly.windSpeed)
                                    .take(5)
                                    .forEach { (time, speed) ->
                                        append("$time: $speed${it.hourlyUnits.windSpeedUnit}\n")
                                    }
                            }
                        }
                    } else {
                        weatherText = "Error getting weather: ${weatherResponse.errorBody()?.string()}"
                    }
                } else {
                    weatherText = "City not found"
                }
            } else {
                weatherText = "Geocoding error: ${geoResponse.errorBody()?.string()}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing request", e)
            weatherText = "Error: ${e.message}"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = weatherText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}