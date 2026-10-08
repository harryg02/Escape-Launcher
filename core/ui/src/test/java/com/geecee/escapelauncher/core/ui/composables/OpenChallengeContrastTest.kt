package com.geecee.escapelauncher.core.ui.composables

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Checks the colours of the pause (OpenChallenge) and the closed notice against WCAG 2.2 AAA:
 * 7:1 for text (1.4.6, the normal text minimum, used for all text here) and 3:1 for the edges and
 * selected state of controls (1.4.11). The content scrolls, so every point of the background
 * gradient is checked. The dimmed countdown is an inactive control, which WCAG exempts.
 */
class OpenChallengeContrastTest {
    private val textMinimum = 7.0
    private val nonTextMinimum = 3.0

    @Test
    fun `contrast formula matches known WCAG values`() {
        assertEquals(21.0, contrast(Color.White, Color.Black), 0.001)
        assertEquals(1.0, contrast(Color.White, Color.White), 0.001)
        assertEquals(9.82, contrast(Color.White, pauseDark), 0.01)
    }

    @Test
    fun `white text passes AAA on every point of the background`() {
        // App name, questions, unselected answers, Open, closed message and Open anyway
        backgroundSamples().forEach { background ->
            assertAtLeast(textMinimum, contrast(Color.White, background), "white text on $background")
        }
    }

    @Test
    fun `dark text passes AAA on white`() {
        // Selected answer and the Go back button
        assertAtLeast(textMinimum, contrast(pauseDark, Color.White), "dark text on white")
    }

    @Test
    fun `white controls stand out from every point of the background`() {
        // Go back button, selected answer fill and the outline of unselected answers
        backgroundSamples().forEach { background ->
            assertAtLeast(nonTextMinimum, contrast(Color.White, background), "white control on $background")
        }
    }

    /**
     * Points along the background gradient. Android draws gradients by interpolating the encoded
     * sRGB channels, so this does the same.
     */
    private fun backgroundSamples(): List<Rgb> {
        val start = Rgb(pauseLight)
        val end = Rgb(pauseDark)
        return (0..20).map { step ->
            val fraction = step / 20.0
            Rgb(
                red = start.red + (end.red - start.red) * fraction,
                green = start.green + (end.green - start.green) * fraction,
                blue = start.blue + (end.blue - start.blue) * fraction
            )
        }
    }

    private fun assertAtLeast(minimum: Double, actual: Double, what: String) {
        assertTrue("$what is $actual:1, needs $minimum:1", actual >= minimum)
    }

    private fun contrast(first: Color, second: Color): Double = contrast(Rgb(first), Rgb(second))

    private fun contrast(first: Color, second: Rgb): Double = contrast(Rgb(first), second)

    private fun contrast(first: Rgb, second: Rgb): Double {
        val lighter = max(first.luminance(), second.luminance())
        val darker = min(first.luminance(), second.luminance())
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Encoded sRGB channels from 0 to 1 */
    private data class Rgb(val red: Double, val green: Double, val blue: Double) {
        constructor(color: Color) : this(
            color.red.toDouble(),
            color.green.toDouble(),
            color.blue.toDouble()
        )

        /** WCAG relative luminance */
        fun luminance(): Double = 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)

        private fun linear(channel: Double): Double =
            if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
    }
}
