package com.subtitle.burner.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OutputFolderManager {
    private const val DEFAULT_OUTPUT_SUBDIR = "SubtitleBurner"
    private const val DEFAULT_LOG_SUBDIR = "SubtitleBurner/Logs"
    private const val PREFS_NAME = "output_folder_prefs"
    private const val KEY_OUTPUT_URI = "key_output_uri"
    private const val KEY_LOG_URI = "key_log_uri"

    fun getOutputFolder(context: Context): DocumentFile {
        val uriString = getPrefs(context).getString(KEY_OUTPUT_URI, null)
        return uriString?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }
            ?: getDefaultOutputFolder(context)
    }

    fun getLogFolder(context: Context): DocumentFile {
        val uriString = getPrefs(context).getString(KEY_LOG_URI, null)
        return uriString?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }
            ?: getDefaultLogFolder(context)
    }

    fun getLogFolderUriString(context: Context): String? =
        getPrefs(context).getString(KEY_LOG_URI, null)

    fun setOutputFolder(context: Context, uri: Uri) {
        getPrefs(context).edit().putString(KEY_OUTPUT_URI, uri.toString()).apply()
        // persist permission
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    fun setLogFolder(context: Context, uri: Uri) {
        getPrefs(context).edit().putString(KEY_LOG_URI, uri.toString()).apply()
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    private fun getDefaultOutputFolder(context: Context): DocumentFile {
        val musicDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(musicDir, DEFAULT_OUTPUT_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return DocumentFile.fromFile(dir)
    }

    private fun getDefaultLogFolder(context: Context): DocumentFile {
        val downloadDir = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(downloadDir, DEFAULT_LOG_SUBDIR)
        if (!dir.exists()) dir.mkdirs()
        return DocumentFile.fromFile(dir)
    }

    fun generateUniqueFile(folder: DocumentFile, baseName: String, extension: String): File {
        var candidate = File(folder.uri.path ?: "", "$baseName.$extension")
        var index = 1
        while (candidate.exists()) {
            candidate = File(folder.uri.path ?: "", "$baseName-$index.$extension")
            index++
        }
        return candidate
    }

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
