package com.example.meteoapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meteoapp.api.ApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    private var weatherText = "Chargement..."
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val favoriteLocations = mutableListOf<WeatherInfo>()
    private lateinit var favoritesAdapter: FavoritesAdapter

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Точное местоположение разрешено
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Только приблизительное местоположение разрешено
                getCurrentLocation()
            }
            else -> {
                // Нет разрешений на местоположение
                Toast.makeText(this, "L'autorisation de localisation est requise", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Setup RecyclerView for favorite cities
        val recyclerView = findViewById<RecyclerView>(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        favoritesAdapter = FavoritesAdapter()
        recyclerView.adapter = favoritesAdapter

        // Setup search field
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                lifecycleScope.launch {
                    fetchWeatherData(searchEditText.text.toString())
                }
                true
            } else {
                false
            }
        }

        // Request location permissions
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    lifecycleScope.launch {
                        fetchWeatherDataByCoordinates(it.latitude, it.longitude)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erreur de localisation: ${e.message}", Toast.LENGTH_LONG).show()
                updateWeatherText("Erreur: ${e.message}")
                updateWeatherText("Ville non trouvée")
                updateWeatherText("Erreur de géocodage: ${e.message}")
            }
    }

    private suspend fun fetchWeatherDataByCoordinates(latitude: Double, longitude: Double) {
        try {
            val weatherResponse = ApiClient.weatherApi.getWeatherForecast(
                latitude = latitude,
                longitude = longitude
            )
            
            if (weatherResponse.isSuccessful) {
                val weather = weatherResponse.body()
                weather?.let {
                    val weatherInfo = WeatherInfo(
                        cityName = "Position Actuelle",
                        temperature = "${it.hourly.temperatures[0]}${it.hourlyUnits.temperatureUnit}",
                        humidity = "${it.hourly.humidity[0]}${it.hourlyUnits.humidityUnit}",
                        windSpeed = "${it.hourly.windSpeed[0]}${it.hourlyUnits.windSpeedUnit}",
                        isCurrentLocation = true
                    )
                    updateFavorites(weatherInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing request", e)
            updateWeatherText("Erreur de récupération de la météo: ${e.message}")
        }
    }

    private suspend fun fetchWeatherData(city: String) {
        try {
            val geoResponse = ApiClient.geocodingApi.searchLocation(city)
            
            if (geoResponse.isSuccessful) {
                val location = geoResponse.body()?.results?.firstOrNull()
                
                if (location != null) {
                    val weatherResponse = ApiClient.weatherApi.getWeatherForecast(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                    
                    if (weatherResponse.isSuccessful) {
                        val weather = weatherResponse.body()
                        weather?.let {
                            val weatherInfo = WeatherInfo(
                                cityName = "${location.name}, ${location.country}",
                                temperature = "${it.hourly.temperatures[0]}${it.hourlyUnits.temperatureUnit}",
                                humidity = "${it.hourly.humidity[0]}${it.hourlyUnits.humidityUnit}",
                                windSpeed = "${it.hourly.windSpeed[0]}${it.hourlyUnits.windSpeedUnit}",
                                isCurrentLocation = false
                            )
                            updateFavorites(weatherInfo)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing request", e)
            updateWeatherText("Erreur de géocodage: ${e.message}")
            updateWeatherText("Erreur de récupération de la météo: ${e.message}")
        }
    }

    private fun updateFavorites(weatherInfo: WeatherInfo) {
        if (weatherInfo.isCurrentLocation) {
            favoriteLocations.removeAll { it.isCurrentLocation }
            favoriteLocations.add(0, weatherInfo)
        } else {
            favoriteLocations.add(weatherInfo)
        }
        favoritesAdapter.submitList(favoriteLocations.toList())
    }

    private fun updateWeatherText(newText: String) {
        weatherText = newText
        // Implementation of updateWeatherText method
    }
}

data class WeatherInfo(
    val cityName: String,
    val temperature: String,
    val humidity: String,
    val windSpeed: String,
    val isCurrentLocation: Boolean
)

class FavoritesAdapter : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {
    private var items = listOf<WeatherInfo>()

    fun submitList(newItems: List<WeatherInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cityNameText: TextView = itemView.findViewById(R.id.cityNameText)
        private val temperatureText: TextView = itemView.findViewById(R.id.temperatureText)
        private val humidityText: TextView = itemView.findViewById(R.id.humidityText)
        private val windSpeedText: TextView = itemView.findViewById(R.id.windSpeedText)

        fun bind(weatherInfo: WeatherInfo) {
            cityNameText.text = weatherInfo.cityName
            temperatureText.text = "Température: ${weatherInfo.temperature}"
            humidityText.text = "Humidité: ${weatherInfo.humidity}"
            windSpeedText.text = "Vitesse du vent: ${weatherInfo.windSpeed}"
        }
    }
}