package com.subtitle.burner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import com.subtitle.burner.R
import com.subtitle.burner.storage.ErrorLogManager
import com.subtitle.burner.storage.FileManager
import com.subtitle.burner.util.DateUtils
import java.io.File

class SubtitleBurnService : Service() {
    private val CHANNEL_ID = "subtitle_burn_service"
    private var ffmpegSession: FFmpegSession? = null
    private var isCancelled = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val inputVideoPath = intent?.getStringExtra(EXTRA_INPUT_VIDEO) ?: return START_NOT_STICKY
        val subtitlePath = intent.getStringExtra(EXTRA_SUBTITLE) ?: return START_NOT_STICKY
        val outputVideoPath = intent.getStringExtra(EXTRA_OUTPUT_VIDEO) ?: return START_NOT_STICKY
        val ffmpegCommand = intent.getStringExtra(EXTRA_FFMPEG_COMMAND) ?: return START_NOT_STICKY
        val errorLogFolder = intent.getStringExtra(EXTRA_ERROR_LOG_FOLDER)

        startForeground(NOTIFICATION_ID, buildNotification("Preparing..."))
        // Run FFmpeg in background thread
        Thread {
            ffmpegSession = FFmpegKit.executeAsync(ffmpegCommand) { session ->
                // Called when FFmpeg finishes
                val returnCode = session.returnCode
                if (isCancelled) {
                    stopSelf()
                } else if (returnCode.isSuccess) {
                    // Success – notify UI via broadcast
                    broadcastResult(true, outputVideoPath, null)
                } else {
                    // Failure – write error log
                    val logFile = ErrorLogManager.writeErrorLog(
                        context = this@SubtitleBurnService,
                        errorFolderUriString = errorLogFolder,
                        videoPath = inputVideoPath,
                        subtitlePath = subtitlePath,
                        outputPath = outputVideoPath,
                        ffmpegCommand = ffmpegCommand,
                        ffmpegOutput = session.allLogsAsString,
                        errorMessage = returnCode
                    )
                    broadcastResult(false, null, logFile?.absolutePath)
                }
                stopSelf()
            }
        }.start()
        return START_REDELIVER_INTENT
    }

    private fun broadcastResult(success: Boolean, outputPath: String?, logPath: String?) {
        val resultIntent = Intent(ACTION_PROCESSING_COMPLETE)
        resultIntent.putExtra(EXTRA_SUCCESS, success)
        resultIntent.putExtra(EXTRA_OUTPUT_PATH, outputPath)
        resultIntent.putExtra(EXTRA_LOG_PATH, logPath)
        sendBroadcast(resultIntent)
    }

    fun cancelProcessing() {
        isCancelled = true
        ffmpegSession?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Subtitle Burn Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val cancelIntent = Intent(this, SubtitleBurnService::class.java).apply {
            action = ACTION_CANCEL
        }
        val pendingCancel = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(R.drawable.ic_cancel, "Cancel", pendingCancel)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val ACTION_PROCESSING_COMPLETE = "com.subtitle.burner.PROCESSING_COMPLETE"
        const val ACTION_CANCEL = "com.subtitle.burner.ACTION_CANCEL"
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_OUTPUT_PATH = "extra_output_path"
        const val EXTRA_LOG_PATH = "extra_log_path"
        const val EXTRA_INPUT_VIDEO = "extra_input_video"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_OUTPUT_VIDEO = "extra_output_video"
        const val EXTRA_FFMPEG_COMMAND = "extra_ffmpeg_command"
        const val EXTRA_ERROR_LOG_FOLDER = "extra_error_log_folder"
        const val NOTIFICATION_ID = 101
    }
}
