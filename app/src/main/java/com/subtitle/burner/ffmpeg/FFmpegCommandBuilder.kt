package com.subtitle.burner.ffmpeg

import com.subtitle.burner.model.SubtitlePosition
import com.subtitle.burner.model.SubtitleStyle
import java.io.File

object FFmpegCommandBuilder {
    /**
     * Converts a hex color string like "#RRGGBB" to ASS color format "&H00BBGGRR".
     */
    fun convertHexToAssColor(hex: String): String {
        var clean = hex.removePrefix("#")
        if (clean.length == 6) {
            // ASS expects BBGGRR order and prefixed with &H00
            val r = clean.substring(0, 2)
            val g = clean.substring(2, 4)
            val b = clean.substring(4, 6)
            return "&H00${b}${g}${r}"
        }
        // fallback to white if malformed
        return "&H00FFFFFF"
    }

    /**
     * Builds the FFmpeg command string for burning subtitles with the given parameters.
     * Returns a list suitable for passing to FFmpegKit.execute.
     */
    fun buildCommand(
        inputVideo: File,
        subtitleFile: File,
        outputVideo: File,
        style: SubtitleStyle,
        position: SubtitlePosition
    ): String {
        // Build force_style parameter
        val primaryColor = convertHexToAssColor(style.primaryColor)
        val outlineColor = convertHexToAssColor(style.outlineColor)
        val backgroundBox = style.backgroundBoxColor?.let { "BackColour=${convertHexToAssColor(it)}" } ?: ""
        val bold = if (style.bold) "Bold=1" else "Bold=0"
        val shadow = if (style.shadow) "Shadow=1" else "Shadow=0"
        val fontFamily = style.fontFamily?.let { "Fontname=$it" } ?: ""
        val fontSize = "FontSize=${style.fontSize}"
        val outline = "Outline=${style.outlineThickness}"
        val alignment = "Alignment=${position.alignment}"

        val forceStyleComponents = listOfNotNull(
            fontFamily,
            fontSize,
            "PrimaryColour=$primaryColor",
            "OutlineColour=$outlineColor",
            outline,
            bold,
            shadow,
            backgroundBox,
            alignment
        ).joinToString(",")

        // Escape paths that may contain spaces or special characters
        val videoPath = inputVideo.absolutePath.replace("\\", "\\\\")
        val subtitlePath = subtitleFile.absolutePath.replace("\\", "\\\\")
        val outPath = outputVideo.absolutePath.replace("\\", "\\\\")

        val vf = "subtitles='$subtitlePath':force_style='${forceStyleComponents}'"
        return "-i '$videoPath' -vf \"$vf\" -c:a copy '$outPath'"
    }
}
