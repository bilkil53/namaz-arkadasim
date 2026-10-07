package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GpsLocationHelper {

    private const val TAG = "GpsLocationHelper"

    data class LocationResult(
        val cityName: String,
        val latitude: Double,
        val longitude: Double,
        val isSuccess: Boolean
    )

    @SuppressLint("MissingPermission")
    suspend fun getDeviceLocation(context: Context): LocationResult = withContext(Dispatchers.IO) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                return@withContext LocationResult("İstanbul", 41.0082, 28.9784, false)
            }

            var bestLocation: Location? = null

            // Check GPS provider
            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (loc != null) {
                        bestLocation = loc
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "GPS provider check: ${e.message}")
            }

            // Check Network provider if GPS was null
            if (bestLocation == null) {
                try {
                    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        val loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (loc != null) {
                            bestLocation = loc
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Network provider check: ${e.message}")
                }
            }

            // Check Passive provider if still null
            if (bestLocation == null) {
                try {
                    val loc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                    if (loc != null) {
                        bestLocation = loc
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Passive provider check: ${e.message}")
                }
            }

            val lat = bestLocation?.latitude ?: 41.0082
            val lng = bestLocation?.longitude ?: 28.9784

            // Reverse geocode to find real city name
            var resolvedCity = "İstanbul"
            try {
                val geocoder = Geocoder(context, Locale("tr", "TR"))
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                val address = addresses?.firstOrNull()
                if (address != null) {
                    resolvedCity = address.adminArea
                        ?: address.subAdminArea
                        ?: address.locality
                        ?: address.subLocality
                        ?: "Mevcut Konum"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Geocoder error: ${e.message}")
                if (bestLocation != null) {
                    resolvedCity = "GPS Konumu"
                }
            }

            return@withContext LocationResult(
                cityName = resolvedCity,
                latitude = lat,
                longitude = lng,
                isSuccess = bestLocation != null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining device location: ${e.message}")
            return@withContext LocationResult("İstanbul", 41.0082, 28.9784, false)
        }
    }
}
