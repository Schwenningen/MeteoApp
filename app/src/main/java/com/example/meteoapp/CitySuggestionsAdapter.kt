package com.example.meteoapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meteoapp.model.LocationResult

// Adaptateur pour afficher les suggestions de villes lors de la recherche
class CitySuggestionsAdapter(private val onCitySelected: (LocationResult) -> Unit) : 
    RecyclerView.Adapter<CitySuggestionsAdapter.CityViewHolder>() {
    
    // Liste des villes suggérées
    private var cities = listOf<LocationResult>()

    // Met à jour la liste des villes et rafraîchit l'affichage
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newCities: List<LocationResult>) {
        cities = newCities
        notifyDataSetChanged()
    }

    // Crée une nouvelle vue pour chaque élément de la liste
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_suggestion, parent, false)
        return CityViewHolder(view)
    }

    // Lie les données d'une ville à la vue correspondante
    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    // Retourne le nombre total de villes dans la liste
    override fun getItemCount() = cities.size

    // ViewHolder pour afficher les informations d'une ville
    inner class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView as TextView

        // Initialise le gestionnaire de clics
        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCitySelected(cities[position])
                }
            }
        }

        // Configure l'affichage des informations de la ville
        fun bind(city: LocationResult) {
            // Construit le texte à afficher avec le nom de la ville, la région et le pays
            val locationText = buildString {
                append(city.name)
                if (!city.admin1.isNullOrEmpty()) {
                    append(", ${city.admin1}")
                }
                append(", ${city.country}")
            }
            textView.text = locationText
        }
    }
} 