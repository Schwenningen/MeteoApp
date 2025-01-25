packagepackage com.example.meteoapp.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// Classe de données représentant une ville favorite
data class FavoriteCity(
    val name: String,          // Nom de la ville
    val latitude: Double,      // Latitude de la ville
    val longitude: Double,     // Longitude de la ville
    val isCurrentLocation: Boolean = false  // Indique si c'est la position actuelle
)

// Classe de gestion du stockage des villes favorites
class FavoriteCitiesStorage(context: Context) {
    // Initialisation des SharedPreferences et de Gson pour la sérialisation
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        // Constantes pour le stockage
        private const val PREFS_NAME = "FavoriteCities"
        private const val KEY_FAVORITES = "favorites"
    }

    // Ajoute une ville aux favoris si elle n'existe pas déjà
    fun saveFavoriteCity(city: FavoriteCity) {
        val favorites = getFavoriteCities().toMutableList()
        if (!favorites.any { it.name == city.name }) {
            // Si c'est la position actuelle, on l'ajoute au début
            if (city.isCurrentLocation) {
                favorites.add(0, city)
            } else {
                // Sinon on l'ajoute après la position actuelle s'il y en a une
                val currentLocationIndex = favorites.indexOfFirst { it.isCurrentLocation }
                if (currentLocationIndex != -1) {
                    favorites.add(currentLocationIndex + 1, city)
                } else {
                    favorites.add(city)
                }
            }
            saveFavoritesList(favorites)
        }
    }

    // Supprime une ville des favoris
    fun removeFavoriteCity(cityName: String) {
        val favorites = getFavoriteCities().toMutableList()
        // On ne supprime pas la position actuelle
        if (!favorites.any { it.name == cityName && it.isCurrentLocation }) {
            favorites.removeAll { it.name == cityName }
            saveFavoritesList(favorites)
        }
    }

    // Récupère la liste des villes favorites
    fun getFavoriteCities(): List<FavoriteCity> {
        val favoritesJson = sharedPreferences.getString(KEY_FAVORITES, null)
        val favorites = if (favoritesJson != null) {
            // Conversion du JSON en liste d'objets FavoriteCity
            val type = object : TypeToken<List<FavoriteCity>>() {}.type
            gson.fromJson<List<FavoriteCity>>(favoritesJson, type)
        } else {
            emptyList()
        }

        // On s'assure que la position actuelle est toujours en première position
        return favorites.sortedBy { !it.isCurrentLocation }
    }

    // Sauvegarde la liste des favoris dans les SharedPreferences
    private fun saveFavoritesList(favorites: List<FavoriteCity>) {
        // On s'assure que la position actuelle est en première position avant la sauvegarde
        val sortedFavorites = favorites.sortedBy { !it.isCurrentLocation }
        // Conversion de la liste en JSON et sauvegarde
        val favoritesJson = gson.toJson(sortedFavorites)
        sharedPreferences.edit().putString(KEY_FAVORITES, favoritesJson).apply()
    }
} 