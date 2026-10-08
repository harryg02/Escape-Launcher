package com.geecee.escapelauncher.feature.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresPermission
import jakarta.inject.Inject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Weather without Google Play Services. Takes one approximate location from [LocationManager]
 * (a recent last known one if there is one, otherwise a single fix, never continuous updates),
 * rounds it to about 1 km and asks Open-Meteo for the current temperature.
 *
 * Shows nothing ("") when there is no location permission. If there is no fix or the request
 * fails, the last temperature stays for a while and is then hidden.
 */
class WeatherImpl @Inject constructor() : WeatherProxy {
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var cache: CachedWeather? = null

    override fun getWeather(context: Context, useFarenheit: Boolean, callback: (String) -> Unit) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            cache = null
            callback("")
            return
        }

        cache?.takeIf { it.isFresh(System.currentTimeMillis(), useFarenheit) }?.let {
            callback(it.text)
            return
        }

        val locationManager = context.getSystemService(LocationManager::class.java)
        if (locationManager == null) {
            callback(fallbackText(useFarenheit))
            return
        }

        getLocation(locationManager) { location ->
            executor.execute {
                if (location == null) {
                    callback(fallbackText(useFarenheit))
                    return@execute
                }
                val url = OpenMeteo.buildUrl(location.latitude, location.longitude, useFarenheit)
                val temperature = fetchTemperature(url)
                if (temperature == null) {
                    callback(fallbackText(useFarenheit))
                    return@execute
                }
                val text = OpenMeteo.formatTemperature(temperature, useFarenheit)
                cache = CachedWeather(text, useFarenheit, System.currentTimeMillis())
                callback(text)
            }
        }
    }

    /**
     * The text to show when fetching failed: the last temperature if it isn't too old, else
     * nothing.
     */
    private fun fallbackText(useFahrenheit: Boolean): String =
        cache?.takeIf {
            it.isFresh(
                System.currentTimeMillis(),
                useFahrenheit,
                CachedWeather.KEEP_AFTER_FAILURE_MILLIS
            )
        }?.text ?: ""

    /**
     * Gives [onResult] a recent last known location if there is one, otherwise asks for one fix.
     * [onResult] gets null if there is no location, and is called once.
     */
    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun getLocation(locationManager: LocationManager, onResult: (Location?) -> Unit) {
        try {
            // Only the providers this app is allowed to use. With approximate location, Android 11
            // and older leave out GPS.
            val providers = locationManager.getProviders(true)

            val lastKnown = providers
                .mapNotNull { locationManager.getLastKnownLocation(it) }
                .maxByOrNull { it.elapsedRealtimeNanos }
            if (lastKnown != null && isRecent(lastKnown)) {
                onResult(lastKnown)
                return
            }

            // Network is quick and approximate, which is all this needs. Passive never asks for
            // a fix of its own.
            val provider = PROVIDER_PREFERENCE.firstOrNull { it in providers }
            if (provider == null) {
                onResult(lastKnown)
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(provider, null, executor) { location ->
                    onResult(location ?: lastKnown)
                }
            } else {
                requestSingleFix(locationManager, provider) { location ->
                    onResult(location ?: lastKnown)
                }
            }
        } catch (e: SecurityException) {
            // The permission was taken away while this was running
            Log.w("Weather", "Location permission missing", e)
            onResult(null)
        } catch (e: IllegalArgumentException) {
            // The provider went away between listing it and using it
            Log.w("Weather", "Location provider unavailable", e)
            onResult(null)
        }
    }

    /**
     * Below Android 11 there is no getCurrentLocation, so this listens for the first update and
     * stops listening straight away, or after [FIX_TIMEOUT_MILLIS] with no fix.
     */
    @RequiresPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    private fun requestSingleFix(
        locationManager: LocationManager,
        provider: String,
        onResult: (Location?) -> Unit
    ) {
        val handler = Handler(Looper.getMainLooper())
        val done = AtomicBoolean(false)

        // Not a lambda: below Android 11 the other three methods have no default and are called
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = finish(location)
            override fun onProviderDisabled(provider: String) = finish(null)
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("Called by Android 10 and older only")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            fun finish(location: Location?) {
                if (!done.compareAndSet(false, true)) return
                handler.removeCallbacksAndMessages(null)
                locationManager.removeUpdates(this)
                onResult(location)
            }
        }

        handler.postDelayed({ listener.finish(null) }, FIX_TIMEOUT_MILLIS)
        locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
    }

    private fun isRecent(location: Location): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return ageNanos in 0..LOCATION_MAX_AGE_NANOS
    }

    /**
     * The current temperature from Open-Meteo, or null if the request or the reply failed.
     * Blocks, so only call it on [executor].
     */
    private fun fetchTemperature(url: String): Double? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URI(url).toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
            connection.readTimeout = NETWORK_TIMEOUT_MILLIS
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                Log.w("Weather", "Open-Meteo replied ${connection.responseCode}")
                null
            } else {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                OpenMeteo.parseTemperature(body)
            }
        } catch (e: IOException) {
            Log.w("Weather", "Weather request failed", e)
            null
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        val PROVIDER_PREFERENCE = listOf(
            LocationManager.NETWORK_PROVIDER,
            "fused", // LocationManager.FUSED_PROVIDER, which is only defined from Android 12
            LocationManager.GPS_PROVIDER
        )
        const val LOCATION_MAX_AGE_NANOS = 30 * 60 * 1_000_000_000L
        const val FIX_TIMEOUT_MILLIS = 30_000L
        const val NETWORK_TIMEOUT_MILLIS = 10_000
    }
}
