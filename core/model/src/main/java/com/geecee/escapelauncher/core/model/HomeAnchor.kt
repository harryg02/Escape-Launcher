package com.geecee.escapelauncher.core.model

/**
 * A line the user chose to keep on the home screen, such as a commitment or a person to call
 *
 * @param text What is shown. Nothing is shown when this is blank
 * @param phoneNumber Optional number that the dialler opens with when the anchor is tapped
 */
data class HomeAnchor(
    val text: String = "",
    val phoneNumber: String = ""
) {
    val isSet: Boolean get() = text.isNotBlank()
    val hasPhoneNumber: Boolean get() = phoneNumber.isNotBlank()
}
