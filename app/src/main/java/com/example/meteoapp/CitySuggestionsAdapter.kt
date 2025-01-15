package com.example.meteoapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.meteoapp.model.LocationResult

class CitySuggestionsAdapter(private val onCitySelected: (LocationResult) -> Unit) : 
    RecyclerView.Adapter<CitySuggestionsAdapter.CityViewHolder>() {
    
    private var cities = listOf<LocationResult>()

    fun submitList(newCities: List<LocationResult>) {
        cities = newCities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_city_suggestion, parent, false)
        return CityViewHolder(view)
    }

    override fun onBindViewHolder(holder: CityViewHolder, position: Int) {
        holder.bind(cities[position])
    }

    override fun getItemCount() = cities.size

    inner class CityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView as TextView

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onCitySelected(cities[position])
                }
            }
        }

        fun bind(city: LocationResult) {
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