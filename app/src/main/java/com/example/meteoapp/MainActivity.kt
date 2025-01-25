package com.example.meteoapp

// Importations nécessaires pour l'application
import android.Manifest
import android.annotation.SuppressLint
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
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DividerItemDecoration
import android.content.Intent
import com.example.meteoapp.storage.FavoriteCitiesStorage
import java.util.Calendar

private const val TAG = "MainActivity"

// Énumération des types de météo pour différentes conditions météorologiques
enum class WeatherType {
    SUNNY, CLOUDY, RAINY
}

// Activité principale de l'application
class MainActivity : ComponentActivity() {
    // Variables pour stocker les données météo et les favoris
    private var weatherText = "Chargement..."
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val favoriteLocations = mutableListOf<WeatherInfo>()
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var citySuggestionsAdapter: CitySuggestionsAdapter
    private lateinit var citySuggestionsRecyclerView: RecyclerView
    private lateinit var suggestionsCard: CardView
    private val favoriteLocationsCoordinates = mutableMapOf<String, Pair<Double, Double>>()
    private lateinit var favoriteCitiesStorage: FavoriteCitiesStorage

    // Gestion des permissions de localisation
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

    // Méthode appelée lors de la création de l'activité
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des services de localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        favoriteCitiesStorage = FavoriteCitiesStorage(this)

        // Configuration du RecyclerView pour les villes favorites
        val recyclerView = findViewById<RecyclerView>(R.id.favoritesRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        favoritesAdapter = FavoritesAdapter()
        setupFavoritesAdapter()
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
                val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                    putExtra(WeatherDetailActivity.EXTRA_LATITUDE, city.latitude)
                    putExtra(WeatherDetailActivity.EXTRA_LONGITUDE, city.longitude)
                    putExtra(WeatherDetailActivity.EXTRA_CITY_NAME, buildString {
                        append(city.name)
                        if (!city.admin1.isNullOrEmpty()) {
                            append(", ${city.admin1}")
                        }
                        append(", ${city.country}")
                    })
                }
                startActivity(intent)
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

        // Загружаем сохраненные города при запуске
        loadSavedCities()
    }

    // Méthode appelée lors de la reprise de l'activité
    override fun onResume() {
        super.onResume()
        // Очищаем текущий список избранного
        favoriteLocations.clear()
        // Загружаем актуальный список избранных городов
        loadSavedCities()
    }

    // Demande des permissions de localisation
    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Obtient la localisation actuelle de l'utilisateur
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

    // Récupère les données météo pour une localisation donnée
    private suspend fun fetchWeatherDataByCoordinates(
        latitude: Double,
        longitude: Double,
        cityName: String? = null,
        isCurrentLocation: Boolean = false
    ) {
        try {
            val weatherResponse = ApiClient.weatherApi.getWeatherForecast(
                latitude = latitude,
                longitude = longitude
            )

            if (weatherResponse.isSuccessful) {
                val weather = weatherResponse.body()
                weather?.let {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val currentIndex = it.hourly.times.indexOfFirst { time ->
                        time.substring(11, 13).toInt() == currentHour
                    }.coerceAtLeast(0)

                    val weatherType = when {
                        it.hourly.precipitation[currentIndex] > 0.1 -> WeatherType.RAINY
                        it.hourly.cloudCover[currentIndex] > 60 -> WeatherType.CLOUDY
                        else -> WeatherType.SUNNY
                    }

                    val name = cityName ?: "Position Actuelle"
                    val weatherInfo = WeatherInfo(
                        cityName = name,
                        temperature = "${it.hourly.temperatures[currentIndex]}${it.hourlyUnits.temperatureUnit}",
                        weatherType = weatherType,
                        isCurrentLocation = isCurrentLocation
                    )

                    favoriteLocationsCoordinates[name] = Pair(latitude, longitude)
                    updateFavorites(weatherInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing request", e)
            updateWeatherText("Erreur de récupération de la météo: ${e.message}")
        }
    }

    // Met à jour la liste des favoris, en gardant la position actuelle en haut
    private fun updateFavorites(weatherInfo: WeatherInfo) {
        if (weatherInfo.isCurrentLocation) {
            favoriteLocations.removeAll { it.isCurrentLocation }
            favoriteLocations.add(0, weatherInfo)
        } else {
            if (!favoriteLocations.any { it.cityName == weatherInfo.cityName }) {
                favoriteLocations.add(weatherInfo)
            }
        }
        favoritesAdapter.submitList(favoriteLocations.toList())
    }

    // Met à jour le texte de la météo
    private fun updateWeatherText(newText: String) {
        weatherText = newText
        // Implementation of updateWeatherText method
    }

    // Nouveau méthode pour rechercher des villes
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

    // Charge les villes sauvegardées
    private fun loadSavedCities() {
        lifecycleScope.launch {
            // Сначала загружаем текущую локацию
            getCurrentLocation()

            // Затем загружаем сохраненные города
            val favorites = favoriteCitiesStorage.getFavoriteCities()
            favorites.forEach { city ->
                fetchWeatherDataByCoordinates(
                    latitude = city.latitude,
                    longitude = city.longitude,
                    cityName = city.name,
                    isCurrentLocation = city.isCurrentLocation
                )
            }
        }
    }

    // Обновим FavoritesAdapter для поддержки удаления
    private fun setupFavoritesAdapter() {
        favoritesAdapter = FavoritesAdapter().apply {
            setOnItemClickListener { weatherInfo ->
                val coordinates = favoriteLocationsCoordinates[weatherInfo.cityName]
                coordinates?.let {
                    val intent = Intent(this@MainActivity, WeatherDetailActivity::class.java).apply {
                        putExtra(WeatherDetailActivity.EXTRA_LATITUDE, it.first)
                        putExtra(WeatherDetailActivity.EXTRA_LONGITUDE, it.second)
                        putExtra(WeatherDetailActivity.EXTRA_CITY_NAME, weatherInfo.cityName)
                    }
                    startActivity(intent)
                }
            }

            setOnItemLongClickListener { weatherInfo ->
                if (!weatherInfo.isCurrentLocation) {
                    favoriteCitiesStorage.removeFavoriteCity(weatherInfo.cityName)
                    favoriteLocations.removeAll { it.cityName == weatherInfo.cityName }
                    submitList(favoriteLocations.toList())
                }
            }
        }
    }
}

// Classe de données pour stocker les informations météo
data class WeatherInfo(
    val cityName: String,
    val temperature: String,
    val weatherType: WeatherType,
    val isCurrentLocation: Boolean
)

// Adaptateur pour afficher les villes favorites
class FavoritesAdapter : RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder>() {
    private var items = listOf<WeatherInfo>()
    private var onItemClickListener: ((WeatherInfo) -> Unit)? = null
    private var onItemLongClickListener: ((WeatherInfo) -> Unit)? = null

    // Définit le gestionnaire de clics pour les éléments
    fun setOnItemClickListener(listener: (WeatherInfo) -> Unit) {
        onItemClickListener = listener
    }

    // Définit le gestionnaire de clics longs pour les éléments
    fun setOnItemLongClickListener(listener: (WeatherInfo) -> Unit) {
        onItemLongClickListener = listener
    }

    // Met à jour la liste des éléments et rafraîchit l'affichage
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<WeatherInfo>) {
        items = newItems
        notifyDataSetChanged()
    }

    // Crée une nouvelle vue pour chaque élément de la liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite, parent, false)
        return FavoriteViewHolder(view, onItemClickListener, onItemLongClickListener)
    }

    // Lie les données d'un élément à la vue correspondante
    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(items[position])
    }

    // Retourne le nombre total d'éléments dans la liste
    override fun getItemCount() = items.size

    // ViewHolder pour afficher les informations d'un élément favori
    class FavoriteViewHolder(
        itemView: View,
        private val onItemClickListener: ((WeatherInfo) -> Unit)?,
        private val onItemLongClickListener: ((WeatherInfo) -> Unit)?
    ) : RecyclerView.ViewHolder(itemView) {
        private val cityNameText: TextView = itemView.findViewById(R.id.cityNameText)
        private val temperatureText: TextView = itemView.findViewById(R.id.temperatureText)
        private val weatherIcon: ImageView = itemView.findViewById(R.id.weatherIcon)

        // Configure l'affichage des informations de l'élément
        fun bind(weatherInfo: WeatherInfo) {
            cityNameText.text = weatherInfo.cityName
            temperatureText.text = weatherInfo.temperature

            weatherIcon.setImageResource(
                when (weatherInfo.weatherType) {
                    WeatherType.SUNNY -> R.drawable.day_clear
                    WeatherType.CLOUDY -> R.drawable.day_cloudy
                    WeatherType.RAINY -> R.drawable.day_rain
                }
            )

            itemView.setOnClickListener {
                onItemClickListener?.invoke(weatherInfo)
            }

            itemView.setOnLongClickListener {
                onItemLongClickListener?.invoke(weatherInfo)
                true
            }
        }
    }
}