package com.subtitle.burner

import android.app.Application
import com.arthenica.ffmpegkit.FFmpegKitConfig

class SubtitleBurnerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize FFmpegKit with default configuration. This loads native libraries.
        FFmpegKitConfig.enableLogCallback { log ->
            // Optional: forward FFmpeg logs to Android logcat for debugging.
            android.util.Log.d("FFmpegKit", log.message)
        }
        FFmpegKitConfig.enableStatisticsCallback { stats ->
            // Statistics can be used for progress monitoring if needed.
        }
    }
}
