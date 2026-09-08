package com.subtitle.burner.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.subtitle.burner.model.SubtitlePosition
import com.subtitle.burner.model.SubtitleStyle
import com.subtitle.burner.service.SubtitleBurnService
import com.subtitle.burner.storage.FileManager
import com.subtitle.burner.storage.OutputFolderManager
import com.subtitle.burner.storage.ErrorLogManager
import com.subtitle.burner.ffmpeg.FFmpegCommandBuilder
import com.subtitle.burner.model.ProcessingHistory
import com.subtitle.burner.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _videoUri = MutableLiveData<Uri?>()
    val videoUri: LiveData<Uri?> = _videoUri

    private val _subtitleUri = MutableLiveData<Uri?>()
    val subtitleUri: LiveData<Uri?> = _subtitleUri

    private val _processing = MutableLiveData<Boolean>(false)
    val processing: LiveData<Boolean> = _processing

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val context: Context = getApplication()
    private val db = AppDatabase.getInstance(context)

    fun setVideoUri(uri: Uri) {
        _videoUri.value = uri
        grantUriPermission(uri)
    }

    fun setSubtitleUri(uri: Uri) {
        _subtitleUri.value = uri
        grantUriPermission(uri)
    }

    private fun grantUriPermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun startProcessing(activityContext: Context) {
        val videoUri = _videoUri.value ?: return
        val subtitleUri = _subtitleUri.value ?: return
        _processing.value = true
        _statusMessage.value = "Preparing files..."
        viewModelScope.launch(Dispatchers.IO) {
            // Copy files to cache
            val videoFile = FileManager.copyUriToCache(context, videoUri, "input_video")
            val subtitleFile = FileManager.copyUriToCache(context, subtitleUri, "subtitle")
            // Determine output folder & file
            val outputFolder = OutputFolderManager.getOutputFolder(context)
            val outputFile = OutputFolderManager.generateUniqueFile(outputFolder, videoFile.nameWithoutExtension + "-subtitled", "mp4")
            // Build command
            val style = SubtitleStyle() // defaults, can be parameterized later
            val position = SubtitlePosition.BOTTOM_CENTER
            val ffmpegCmd = FFmpegCommandBuilder.buildCommand(
                inputVideo = videoFile,
                subtitleFile = subtitleFile,
                outputVideo = outputFile,
                style = style,
                position = position
            )
            // Prepare intent for service
            val serviceIntent = Intent(context, SubtitleBurnService::class.java).apply {
                putExtra(SubtitleBurnService.EXTRA_INPUT_VIDEO, videoFile.absolutePath)
                putExtra(SubtitleBurnService.EXTRA_SUBTITLE, subtitleFile.absolutePath)
                putExtra(SubtitleBurnService.EXTRA_OUTPUT_VIDEO, outputFile.absolutePath)
                putExtra(SubtitleBurnService.EXTRA_FFMPEG_COMMAND, ffmpegCmd)
                putExtra(SubtitleBurnService.EXTRA_ERROR_LOG_FOLDER, OutputFolderManager.getLogFolderUriString(context))
            }
            context.startForegroundService(serviceIntent)
        }
    }

    fun onProcessingComplete(success: Boolean, outputPath: String?, logPath: String?) {
        _processing.postValue(false)
        if (success) {
            _statusMessage.postValue("Processing completed: $outputPath")
            // Save to history
            viewModelScope.launch(Dispatchers.IO) {
                val history = ProcessingHistory(
                    videoUri = _videoUri.value.toString(),
                    subtitleUri = _subtitleUri.value.toString(),
                    outputPath = outputPath ?: "",
                    logPath = logPath ?: "",
                    timestamp = System.currentTimeMillis(),
                    status = "COMPLETED"
                )
                db.processingHistoryDao().insert(history)
            }
        } else {
            _statusMessage.postValue("Processing failed. Log saved at $logPath")
        }
    }
}
