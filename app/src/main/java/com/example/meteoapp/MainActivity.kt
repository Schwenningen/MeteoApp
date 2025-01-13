package com.example.meteoapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.meteoapp.api.GeocodingApi
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(GeocodingApi.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val geocodingApi by lazy {
        retrofit.create(GeocodingApi::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GeocodingTest(geocodingApi)
                }
            }
        }
    }
}

@Composable
fun GeocodingTest(api: GeocodingApi) {
    var result by remember { mutableStateOf("Press the button to test") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                scope.launch {
                    try {
                        val response = api.searchLocation("Corte")
                        if (response.isSuccessful) {
                            val data = response.body()
                            result = "Success!\nFirst result:\n" + 
                                    (data?.results?.firstOrNull()?.let { location ->
                                        """
                                        City: ${location.name}
                                        Country: ${location.country}
                                        Latitude: ${location.latitude}
                                        Longitude: ${location.longitude}
                                        """.trimIndent()
                                    } ?: "No results")
                        } else {
                            result = "Error: ${response.code()} ${response.message()}"
                        }
                    } catch (e: Exception) {
                        Log.e("GeocodingTest", "Error", e)
                        result = "Error: ${e.message}"
                    }
                }
            }
        ) {
            Text("Test API (search 'Corte')")
        }

        Text(
            text = result,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}