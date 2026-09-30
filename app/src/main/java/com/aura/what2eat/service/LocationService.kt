package com.aura.what2eat.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationService {

    private const val TAG = "LocationService"
    private const val PREFS_NAME = "what2eat_location_prefs"
    private const val KEY_SAVED_CITY = "saved_city"
    private const val KEY_SAVED_COUNTRY = "saved_country"
    private const val KEY_SAVED_AREA = "saved_area"
    private const val KEY_SAVED_LAT = "saved_lat"
    private const val KEY_SAVED_LNG = "saved_lng"
    private const val KEY_IS_MANUAL_LOCATION = "is_manual_location"

    private const val DEFAULT_CITY = "Karachi"
    private const val DEFAULT_COUNTRY = "Pakistan"
    private const val DEFAULT_AREA = ""

    private val _locationChangedFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)
    val locationChangedFlow: SharedFlow<Pair<String, String>> = _locationChangedFlow.asSharedFlow()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    data class LocationDetails(
        val city: String = DEFAULT_CITY,
        val country: String = DEFAULT_COUNTRY,
        val area: String = DEFAULT_AREA,
        val lat: Double = 24.8607,
        val lng: Double = 67.0011
    )

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
    }

    /**
     * Retrieves the device's current location (City, Country, Area) using FusedLocationProviderClient.
     * Saves the location to SharedPreferences and notifies listeners if changed.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context, forceGps: Boolean = false): Pair<String, String> = withContext(Dispatchers.IO) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val isManual = getPrefs(context).getBoolean(KEY_IS_MANUAL_LOCATION, false)
        if (isManual && !forceGps) {
            Log.d(TAG, "Manual location selected by user, honoring saved location")
            return@withContext getSavedLocation(context)
        }

        if (!hasFine && !hasCoarse) {
            Log.d(TAG, "Location permission not granted, returning saved location")
            return@withContext getSavedLocation(context)
        }

        try {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Try fetching fresh location, fallback to last known location
            val priority = if (forceGps) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
            val location: Location? = try {
                fusedLocationClient.getCurrentLocation(
                    priority,
                    null
                ).await() ?: fusedLocationClient.lastLocation.await()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get current location from fused client: ${e.message}")
                try {
                    fusedLocationClient.lastLocation.await()
                } catch (e2: Exception) {
                    null
                }
            }

            if (location != null) {
                val details = reverseGeocode(context, location.latitude, location.longitude)
                val city = details.city.ifBlank { DEFAULT_CITY }
                val country = details.country.ifBlank { DEFAULT_COUNTRY }
                val area = details.area
                val newLocation = Pair(city, country)

                saveLocation(context, city, country, area, location.latitude, location.longitude, isManual = if (forceGps) false else isManual)
                _locationChangedFlow.tryEmit(newLocation)
                Log.i(TAG, "Location updated: Area=$area, City=$city, Country=$country, Lat=${location.latitude}, Lng=${location.longitude}")

                return@withContext newLocation
            } else {
                Log.d(TAG, "Location was null, returning saved location")
                return@withContext getSavedLocation(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining current location: ${e.message}", e)
            return@withContext getSavedLocation(context)
        }
    }

    /**
     * Performs reverse geocoding to retrieve detailed location information from coordinates.
     */
    @Suppress("DEPRECATION")
    private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): LocationDetails =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext LocationDetails(DEFAULT_CITY, DEFAULT_COUNTRY, DEFAULT_AREA, lat, lng)
            }

            try {
                val geocoder = Geocoder(context, Locale.getDefault())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    return@withContext suspendCancellableCoroutine { continuation ->
                        geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                val address = addresses.firstOrNull()
                                val area = address?.subLocality
                                    ?: address?.subThoroughfare
                                    ?: address?.thoroughfare
                                    ?: address?.featureName
                                    ?: ""
                                val city = address?.locality
                                    ?: address?.subAdminArea
                                    ?: address?.adminArea
                                    ?: DEFAULT_CITY
                                val country = address?.countryName ?: DEFAULT_COUNTRY
                                continuation.resume(LocationDetails(city, country, area, lat, lng))
                            }

                            override fun onError(errorMessage: String?) {
                                Log.w(TAG, "Geocoder error: $errorMessage")
                                continuation.resume(LocationDetails(DEFAULT_CITY, DEFAULT_COUNTRY, DEFAULT_AREA, lat, lng))
                            }
                        })
                    }
                } else {
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    val address = addresses?.firstOrNull()
                    val area = address?.subLocality
                        ?: address?.subThoroughfare
                        ?: address?.thoroughfare
                        ?: address?.featureName
                        ?: ""
                    val city = address?.locality
                        ?: address?.subAdminArea
                        ?: address?.adminArea
                        ?: DEFAULT_CITY
                    val country = address?.countryName ?: DEFAULT_COUNTRY
                    return@withContext LocationDetails(city, country, area, lat, lng)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocoding failed: ${e.message}", e)
                return@withContext LocationDetails(DEFAULT_CITY, DEFAULT_COUNTRY, DEFAULT_AREA, lat, lng)
            }
        }

    /**
     * Saves city and country (and optional area + coordinates) to SharedPreferences.
     */
    fun saveLocation(
        context: Context,
        city: String,
        country: String,
        area: String = "",
        lat: Double = 24.8607,
        lng: Double = 67.0011,
        isManual: Boolean = true
    ) {
        getPrefs(context).edit()
            .putString(KEY_SAVED_CITY, city)
            .putString(KEY_SAVED_COUNTRY, country)
            .putString(KEY_SAVED_AREA, area)
            .putFloat(KEY_SAVED_LAT, lat.toFloat())
            .putFloat(KEY_SAVED_LNG, lng.toFloat())
            .putBoolean(KEY_IS_MANUAL_LOCATION, isManual)
            .apply()

        _locationChangedFlow.tryEmit(Pair(city, country))

        // Also update PreferencesManager UserPreferences
        try {
            val prefsManager = com.aura.what2eat.data.local.PreferencesManager.getInstance(context)
            val userPrefs = prefsManager.loadUserPreferences()
            prefsManager.saveUserPreferences(userPrefs.copy(country = country, city = city))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update UserPreferences country/city: ${e.message}")
        }
    }

    fun clearManualLocation(context: Context) {
        getPrefs(context).edit().remove(KEY_IS_MANUAL_LOCATION).apply()
    }

    fun isManualLocation(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_MANUAL_LOCATION, false)
    }

    /**
     * Returns the saved location, or (Karachi, Pakistan) by default.
     */
    fun getSavedLocation(context: Context): Pair<String, String> {
        val prefs = getPrefs(context)
        val city = prefs.getString(KEY_SAVED_CITY, DEFAULT_CITY) ?: DEFAULT_CITY
        val country = prefs.getString(KEY_SAVED_COUNTRY, DEFAULT_COUNTRY) ?: DEFAULT_COUNTRY
        return Pair(city, country)
    }

    /**
     * Returns the saved specific area / neighborhood (e.g. Landhi 5, Buffer Zone, Clifton), or empty string if not available.
     */
    fun getSavedArea(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_SAVED_AREA, "") ?: ""
    }

    /**
     * Returns saved coordinates.
     */
    fun getSavedCoordinates(context: Context): Pair<Double, Double> {
        val prefs = getPrefs(context)
        val lat = prefs.getFloat(KEY_SAVED_LAT, 24.8607f).toDouble()
        val lng = prefs.getFloat(KEY_SAVED_LNG, 67.0011f).toDouble()
        return Pair(lat, lng)
    }

    /**
     * Checks if the newly resolved location is different from the currently saved location.
     */
    fun hasLocationChanged(context: Context, newLocation: Pair<String, String>): Boolean {
        val saved = getSavedLocation(context)
        return !saved.first.equals(newLocation.first, ignoreCase = true) ||
                !saved.second.equals(newLocation.second, ignoreCase = true)
    }

    data class DebugLocationInfo(
        val city: String,
        val country: String,
        val area: String,
        val lat: Double,
        val lng: Double,
        val isManual: Boolean,
        val hasPermission: Boolean,
        val isGpsEnabled: Boolean
    ) {
        val fullLocationText: String
            get() = buildString {
                if (area.isNotBlank()) append("$area, ")
                append("$city, $country")
            }

        val formattedCoordinates: String
            get() = String.format(Locale.US, "%.5f, %.5f", lat, lng)

        val summaryText: String
            get() = buildString {
                append("📍 Complete: $fullLocationText\n")
                append("🌐 Lat/Lng: $formattedCoordinates\n")
                append("⚙️ Mode: ${if (isManual) "Manual (User Selected)" else "GPS Auto-Detected"}")
                append(" | Perm: ${if (hasPermission) "Granted" else "Denied"}")
                append(" | GPS: ${if (isGpsEnabled) "ON" else "OFF"}")
            }
    }

    fun getDebugLocationInfo(context: Context): DebugLocationInfo {
        val (city, country) = getSavedLocation(context)
        val area = getSavedArea(context)
        val (lat, lng) = getSavedCoordinates(context)
        val isManual = isManualLocation(context)
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val isGps = isGpsEnabled(context)

        return DebugLocationInfo(
            city = city,
            country = country,
            area = area,
            lat = lat,
            lng = lng,
            isManual = isManual,
            hasPermission = hasFine || hasCoarse,
            isGpsEnabled = isGps
        )
    }
}
