package com.geecee.escapelauncher.feature.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class OpenMeteoTest {

    @Test
    fun `coordinates are rounded to two decimal places`() {
        assertEquals(51.51, OpenMeteo.roundCoordinate(51.50735), 0.0)
        assertEquals(-0.13, OpenMeteo.roundCoordinate(-0.12776), 0.0)
        assertEquals(40.71, OpenMeteo.roundCoordinate(40.7128), 0.0)
        assertEquals(0.0, OpenMeteo.roundCoordinate(-0.001), 0.0)
    }

    @Test
    fun `url sends only the rounded location`() {
        assertEquals(
            "https://api.open-meteo.com/v1/forecast?latitude=51.51&longitude=-0.13&current_weather=true",
            OpenMeteo.buildUrl(51.50735, -0.12776, useFahrenheit = false)
        )
    }

    @Test
    fun `url asks for fahrenheit when it is chosen`() {
        assertEquals(
            "https://api.open-meteo.com/v1/forecast?latitude=40.71&longitude=-74.01&current_weather=true&temperature_unit=fahrenheit",
            OpenMeteo.buildUrl(40.7128, -74.006, useFahrenheit = true)
        )
    }

    @Test
    fun `url always has two decimals`() {
        assertEquals(
            "https://api.open-meteo.com/v1/forecast?latitude=10.00&longitude=-20.50&current_weather=true",
            OpenMeteo.buildUrl(10.0, -20.5, useFahrenheit = false)
        )
    }

    @Test
    fun `url uses a decimal point whatever the device language`() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(
                "https://api.open-meteo.com/v1/forecast?latitude=51.51&longitude=-0.13&current_weather=true",
                OpenMeteo.buildUrl(51.50735, -0.12776, useFahrenheit = false)
            )
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun `temperature is read from current_weather`() {
        val json = """
            {
              "latitude": 51.5,
              "longitude": -0.13,
              "current_weather": {
                "time": "2026-10-07T12:00",
                "temperature": 12.3,
                "windspeed": 10.1,
                "weathercode": 3
              }
            }
        """.trimIndent()

        assertEquals(12.3, OpenMeteo.parseTemperature(json)!!, 0.0)
    }

    @Test
    fun `negative and whole temperatures are read`() {
        assertEquals(-4.0, OpenMeteo.parseTemperature("""{"current_weather":{"temperature":-4}}""")!!, 0.0)
    }

    @Test
    fun `reply without a temperature gives null`() {
        assertNull(OpenMeteo.parseTemperature("""{"latitude":51.5}"""))
        assertNull(OpenMeteo.parseTemperature("""{"current_weather":{}}"""))
        assertNull(OpenMeteo.parseTemperature("""{"current_weather":{"temperature":null}}"""))
        assertNull(OpenMeteo.parseTemperature("""{"current_weather":"none"}"""))
        assertNull(OpenMeteo.parseTemperature("""{"error":true,"reason":"Latitude must be in range of -90 to 90°."}"""))
    }

    @Test
    fun `reply that is not a JSON object gives null`() {
        assertNull(OpenMeteo.parseTemperature(""))
        assertNull(OpenMeteo.parseTemperature("not json"))
        assertNull(OpenMeteo.parseTemperature("[]"))
        assertNull(OpenMeteo.parseTemperature("""{"current_weather":"""))
    }

    @Test
    fun `temperature is shown rounded with its unit`() {
        assertEquals("13°C", OpenMeteo.formatTemperature(12.6, useFahrenheit = false))
        assertEquals("0°C", OpenMeteo.formatTemperature(-0.4, useFahrenheit = false))
        assertEquals("-7°C", OpenMeteo.formatTemperature(-6.8, useFahrenheit = false))
        assertEquals("54°F", OpenMeteo.formatTemperature(54.4, useFahrenheit = true))
    }
}
