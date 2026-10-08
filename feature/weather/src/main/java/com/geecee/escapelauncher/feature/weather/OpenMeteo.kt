package com.geecee.escapelauncher.feature.weather

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Builds Open-Meteo requests and reads the replies. No Android APIs, so it can be unit tested.
 */
object OpenMeteo {
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"

    /**
     * Rounds a coordinate to 2 decimal places (about 1 km), which is all a temperature needs.
     */
    fun roundCoordinate(value: Double): Double = Math.round(value * 100) / 100.0

    /**
     * The URL for the current temperature at the rounded location.
     */
    fun buildUrl(latitude: Double, longitude: Double, useFahrenheit: Boolean): String {
        val unitParam = if (useFahrenheit) "&temperature_unit=fahrenheit" else ""
        return "$FORECAST_URL?latitude=${formatCoordinate(latitude)}" +
                "&longitude=${formatCoordinate(longitude)}&current_weather=true" +
                unitParam
    }

    /**
     * The current temperature in an Open-Meteo reply, or null if the reply doesn't have one.
     */
    fun parseTemperature(json: String): Double? {
        val root = try {
            Json.parseToJsonElement(json)
        } catch (e: IllegalArgumentException) {
            // Not JSON. Json throws SerializationException, which is an IllegalArgumentException
            return null
        }
        val currentWeather = (root as? JsonObject)?.get("current_weather") as? JsonObject
        return (currentWeather?.get("temperature") as? JsonPrimitive)?.doubleOrNull
    }

    /**
     * The temperature as shown on the home screen, e.g. "12°C".
     */
    fun formatTemperature(temperature: Double, useFahrenheit: Boolean): String {
        val unitSymbol = if (useFahrenheit) "°F" else "°C"
        return "${temperature.roundToInt()}$unitSymbol"
    }

    private fun formatCoordinate(value: Double): String =
        String.format(Locale.ROOT, "%.2f", roundCoordinate(value))
}
