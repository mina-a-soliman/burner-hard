package com.subtitle.burner

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import com.subtitle.burner.ui.MainScreen
import com.subtitle.burner.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    private val videoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.setVideoUri(it) }
    }

    private val subtitlePicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.setSubtitleUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request persistable URI permissions when activity finishes picking
        setContent {
            MaterialTheme {
                MainScreen(
                    viewModel = viewModel,
                    onSelectVideo = { videoPicker.launch(arrayOf("video/*")) },
                    onSelectSubtitle = { subtitlePicker.launch(arrayOf("text/*", "application/octet-stream")) },
                    onStartProcessing = { viewModel.startProcessing(this) }
                )
            }
        }
    }
}
