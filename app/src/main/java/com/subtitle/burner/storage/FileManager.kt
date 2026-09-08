package com.subtitle.burner.storage

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileManager {
    /**
     * Copies a content Uri to the app's cache directory with a given prefix.
     * Returns the resulting File.
     */
    fun copyUriToCache(context: Context, uri: Uri, prefix: String): File {
        val contentResolver = context.contentResolver
        val inputStream: InputStream = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open URI: $uri")
        val fileName = prefix + "_" + (uri.lastPathSegment?.substringAfterLast('/') ?: "temp")
        val cacheFile = File(context.cacheDir, fileName)
        FileOutputStream(cacheFile).use { out ->
            inputStream.copyTo(out)
        }
        inputStream.close()
        return cacheFile
    }
}
