package com.geecee.escapelauncher.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAnchorTest {

    @Test
    fun `default anchor is not shown`() {
        assertFalse(HomeAnchor().isSet)
        assertFalse(HomeAnchor().hasPhoneNumber)
    }

    @Test
    fun `blank text is not shown`() {
        assertFalse(HomeAnchor(text = "  ", phoneNumber = "0123").isSet)
    }

    @Test
    fun `text with a number is shown and callable`() {
        val anchor = HomeAnchor(text = "Call Sam", phoneNumber = "0123")

        assertTrue(anchor.isSet)
        assertTrue(anchor.hasPhoneNumber)
    }
}
