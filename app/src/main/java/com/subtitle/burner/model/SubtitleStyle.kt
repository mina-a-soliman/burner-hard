package com.subtitle.burner.model

data class SubtitleStyle(
    val fontSize: Int = 24,
    val primaryColor: String = "#FFFFFF", // HEX
    val outlineColor: String = "#000000",
    val outlineThickness: Int = 2,
    val shadow: Boolean = true,
    val backgroundBoxColor: String? = null,
    val bold: Boolean = false,
    val fontFamily: String? = null
)
