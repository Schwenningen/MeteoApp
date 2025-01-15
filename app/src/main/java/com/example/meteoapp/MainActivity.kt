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
import android.widget.ImageView
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
import com.example.meteoapp.model.LocationResult
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration

private const val TAG = "MainActivity"

// Énumération des types de météo pour différentes conditions météorologiques
enum class WeatherType {
    SUNNY, CLOUDY, RAINY
}

class MainActivity : ComponentActivity() {
    private var weatherText = "Chargement..."
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val favoriteLocations = mutableListOf<WeatherInfo>()
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var citySuggestionsAdapter: CitySuggestionsAdapter
    private lateinit var citySuggestionsRecyclerView: RecyclerView
    private lateinit var suggestionsCard: CardView

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Accès à la localisation précise accordé
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Accès à la localisation approximative uniquement accordé
                getCurrentLocation()
            }
            else -> {
                // Aucun accès à la localisation accordé
                Toast.makeText(this, "L'autorisation de localisation est requise", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configuration du RecyclerView pour les villes favorites
        val recyclerView = findViewById<RecyclerView>(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        favoritesAdapter = FavoritesAdapter()
        recyclerView.adapter = favoritesAdapter

        // Configuration du champ de recherche avec le listener IME
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                lifecycleScope.launch {
                    searchCities(searchEditText.text.toString())
                }
                true
            } else {
                false
            }
        }

        // Demande des autorisations de localisation au démarrage
        requestLocationPermission()

        // Настройка списка предложений городов
        suggestionsCard = findViewById(R.id.suggestionsCard)
        citySuggestionsRecyclerView = findViewById(R.id.citySuggestionsRecyclerView)
        citySuggestionsRecyclerView.layoutManager = LinearLayoutManager(this)
        citySuggestionsAdapter = CitySuggestionsAdapter { city ->
            lifecycleScope.launch {
                fetchWeatherData(city)
                suggestionsCard.visibility = View.GONE
                searchEditText.setText("")
            }
        }
        citySuggestionsRecyclerView.adapter = citySuggestionsAdapter

        citySuggestionsRecyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        // Добавляем обработчик клика вне списка для его скрытия
        findViewById<ConstraintLayout>(R.id.rootLayout).setOnClickListener {
            suggestionsCard.visibility = View.GONE
        }

        // Добавляем обработчик клика для закрытия списка
        findViewById<TextView>(R.id.favoritesTitle).setOnClickListener {
            suggestionsCard.visibility = View.GONE
        }
        findViewById<RecyclerView>(R.id.favoritesRecyclerView).setOnClickListener {
            suggestionsCard.visibility = View.GONE
        }
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
                    // Obtenir l'heure actuelle et trouver l'index correspondant dans les données horaires
                    val currentHour = java.time.LocalTime.now().hour
                    val currentIndex = it.hourly.times.indexOfFirst { time ->
                        time.substring(11, 13).toInt() == currentHour
                    }.coerceAtLeast(0)
                    
                    // Déterminer le type de météo en fonction des précipitations et de la couverture nuageuse
                    val weatherType = when {
                        it.hourly.precipitation[currentIndex] > 0.1 -> WeatherType.RAINY
                        it.hourly.cloudCover[currentIndex] > 60 -> WeatherType.CLOUDY
                        else -> WeatherType.SUNNY
                    }

                    val weatherInfo = WeatherInfo(
                        cityName = "Position Actuelle",
                        temperature = "${it.hourly.temperatures[currentIndex]}${it.hourlyUnits.temperatureUnit}",
                        weatherType = weatherType,
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

    private suspend fun fetchWeatherData(city: LocationResult) {
        try {
            val weatherResponse = ApiClient.weatherApi.getWeatherForecast(
                latitude = city.latitude,
                longitude = city.longitude
            )
            
            if (weatherResponse.isSuccessful) {
                val weather = weatherResponse.body()
                weather?.let {
                    // Obtenir l'heure actuelle et trouver l'index correspondant dans les données horaires
                    val currentHour = java.time.LocalTime.now().hour
                    val currentIndex = it.hourly.times.indexOfFirst { time ->
                        time.substring(11, 13).toInt() == currentHour
                    }.coerceAtLeast(0)
                    
                    // Déterminer le type de météo en fonction des précipitations et de la couverture nuageuse
                    val weatherType = when {
                        it.hourly.precipitation[currentIndex] > 0.1 -> WeatherType.RAINY
                        it.hourly.cloudCover[currentIndex] > 60 -> WeatherType.CLOUDY
                        else -> WeatherType.SUNNY
                    }

                    val weatherInfo = WeatherInfo(
                        cityName = buildString {
                            append(city.name)
                            if (!city.admin1.isNullOrEmpty()) {
                                append(", ${city.admin1}")
                            }
                            append(", ${city.country}")
                        },
                        temperature = "${it.hourly.temperatures[currentIndex]}${it.hourlyUnits.temperatureUnit}",
                        weatherType = weatherType,
                        isCurrentLocation = false
                    )
                    updateFavorites(weatherInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching weather", e)
            Toast.makeText(this, "Erreur météo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Mettre à jour la liste des favoris, en gardant la position actuelle en haut
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

    // Новый метод для поиска городов
    private suspend fun searchCities(query: String) {
        try {
            val response = ApiClient.geocodingApi.searchLocation(query)
            if (response.isSuccessful) {
                response.body()?.results?.let { cities ->
                    if (cities.isNotEmpty()) {
                        citySuggestionsAdapter.submitList(cities)
                        suggestionsCard.visibility = View.VISIBLE
                    } else {
                        suggestionsCard.visibility = View.GONE
                        Toast.makeText(this, "Aucune ville trouvée", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching cities", e)
            Toast.makeText(this, "Erreur de recherche: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

data class WeatherInfo(
    val cityName: String,
    val temperature: String,
    val weatherType: WeatherType,
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

    // ViewHolder pour afficher les informations météorologiques dans RecyclerView
    class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cityNameText: TextView = itemView.findViewById(R.id.cityNameText)
        private val temperatureText: TextView = itemView.findViewById(R.id.temperatureText)
        private val weatherIcon: ImageView = itemView.findViewById(R.id.weatherIcon)

        fun bind(weatherInfo: WeatherInfo) {
            cityNameText.text = weatherInfo.cityName
            temperatureText.text = weatherInfo.temperature
            
            // Définir l'icône météo en fonction du type de météo
            weatherIcon.setImageResource(
                when (weatherInfo.weatherType) {
                    WeatherType.SUNNY -> R.drawable.ic_sunny
                    WeatherType.CLOUDY -> R.drawable.ic_cloudy
                    WeatherType.RAINY -> R.drawable.ic_rainy
                }
            )
        }
    }
}