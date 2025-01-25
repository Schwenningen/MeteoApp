package com.example.meteoapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.meteoapp.api.ApiClient
import kotlinx.coroutines.launch
import com.example.meteoapp.storage.FavoriteCitiesStorage
import com.example.meteoapp.storage.FavoriteCity
import com.google.android.material.button.MaterialButton
import java.util.Calendar

// Activité pour afficher les détails de la météo d'une ville
class WeatherDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_CITY_NAME = "extra_city_name"
    }

    // Variables pour gérer les favoris et les détails de la ville
    private lateinit var favoriteCitiesStorage: FavoriteCitiesStorage
    private lateinit var favoriteIcon: ImageView
    private lateinit var favoriteButton: MaterialButton
    private var cityName: String = ""
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    // Méthode appelée lors de la création de l'activité
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_detail)

        // Initialisation des variables et récupération des données de l'intent
        favoriteCitiesStorage = FavoriteCitiesStorage(this)
        favoriteIcon = findViewById(R.id.favoriteIcon)
        favoriteButton = findViewById(R.id.favoriteButton)

        // Récupération des données de la ville
        cityName = intent.getStringExtra(EXTRA_CITY_NAME) ?: ""
        latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)

        findViewById<TextView>(R.id.cityNameTextView).text = cityName

        // Mise à jour de l'état du bouton favori
        updateFavoriteStatus()

        // Gestion du clic sur le bouton favori
        favoriteButton.setOnClickListener {
            toggleFavorite()
        }

        // Récupération des détails météo
        lifecycleScope.launch {
            fetchWeatherDetails(latitude, longitude)
        }
    }

    // Met à jour l'état du bouton favori et de l'icône
    private fun updateFavoriteStatus() {
        val isFavorite = favoriteCitiesStorage.getFavoriteCities().any { it.name == cityName }
        
        // Mise à jour de l'icône
        favoriteIcon.setImageResource(
            if (isFavorite) R.drawable.ic_favorite_filled
            else R.drawable.ic_favorite_border
        )
        favoriteIcon.setColorFilter(
            if (isFavorite) getColor(R.color.accent)
            else getColor(R.color.text_secondary)
        )

        // Mise à jour du texte du bouton
        favoriteButton.text = if (isFavorite) {
            "Retirer des favoris"
        } else {
            "Ajouter aux favoris"
        }
    }

    // Ajoute ou retire la ville des favoris
    private fun toggleFavorite() {
        val isFavorite = favoriteCitiesStorage.getFavoriteCities().any { it.name == cityName }
        if (isFavorite) {
            favoriteCitiesStorage.removeFavoriteCity(cityName)
        } else {
            favoriteCitiesStorage.saveFavoriteCity(
                FavoriteCity(
                    name = cityName,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        }
        updateFavoriteStatus()
    }

    // Récupère les détails météo pour la ville donnée
    @SuppressLint("SetTextI18n")
    private suspend fun fetchWeatherDetails(latitude: Double, longitude: Double) {
        try {
            val response = ApiClient.weatherApi.getWeatherForecast(latitude, longitude)
            
            if (response.isSuccessful) {
                val weather = response.body()
                weather?.let {
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val currentIndex = it.hourly.times.indexOfFirst { time ->
                        time.substring(11, 13).toInt() == currentHour
                    }.coerceAtLeast(0)

                    // Mise à jour de l'affichage des détails météo
                    findViewById<TextView>(R.id.currentTempTextView).text = 
                        "${it.hourly.temperatures[currentIndex]}${it.hourlyUnits.temperatureUnit}"

                    val dailyMinTemp = it.hourly.temperatures.take(24).minOrNull()
                    val dailyMaxTemp = it.hourly.temperatures.take(24).maxOrNull()
                    findViewById<TextView>(R.id.minMaxTempTextView).text = 
                        "Min: ${dailyMinTemp}° Max: ${dailyMaxTemp}°"

                    findViewById<TextView>(R.id.windSpeedTextView).text = 
                        "Vent: ${it.hourly.windSpeed[currentIndex]} ${it.hourlyUnits.windSpeedUnit}"

                    val weatherType = when {
                        it.hourly.precipitation[currentIndex] > 0.1 -> WeatherType.RAINY
                        it.hourly.cloudCover[currentIndex] > 60 -> WeatherType.CLOUDY
                        else -> WeatherType.SUNNY
                    }

                    val weatherIcon = findViewById<ImageView>(R.id.weatherIconImageView)
                    weatherIcon.setImageResource(
                        when (weatherType) {
                            WeatherType.SUNNY -> R.drawable.day_clear
                            WeatherType.CLOUDY -> R.drawable.day_cloudy
                            WeatherType.RAINY -> R.drawable.day_rain
                        }
                    )

                    findViewById<TextView>(R.id.weatherConditionTextView).text = when (weatherType) {
                        WeatherType.SUNNY -> "Ensoleillé"
                        WeatherType.CLOUDY -> "Nuageux"
                        WeatherType.RAINY -> "Pluvieux"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherDetailActivity", "Error fetching weather details", e)
        }
    }
} 