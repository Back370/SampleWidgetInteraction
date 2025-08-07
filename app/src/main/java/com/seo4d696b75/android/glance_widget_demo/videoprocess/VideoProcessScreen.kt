
package com.seo4d696b75.android.glance_widget_demo.videoprocess

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.glance_widget_demo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream

@Composable
fun VideoProcessScreen() {
    val context = LocalContext.current
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    val totalFrames = 50

    // This helper function starts the processing and handles the UI state
    fun startProcessing(videoUri: Uri) {
        isLoading = true
        frames = emptyList()
        progress = 0
        CoroutineScope(Dispatchers.IO).launch {
            val extractedFrames = getEvenlySpacedFrames(context, videoUri, totalFrames) { frameNumber ->
                // This is our progress callback, updating the state on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    progress = frameNumber
                }
            }
            // Save the files in the background
            extractedFrames.forEachIndexed { index, bitmap ->
                val fileName = "frame_${String.format("%03d", index)}.png"
                saveBitmapToPictures(context, bitmap, fileName)
            }
            // Switch back to the main thread to update the final UI
            withContext(Dispatchers.Main) {
                frames = extractedFrames
                isLoading = false
                Log.d("VideoProcess", "Processed and saved ${frames.size} frames.")
            }
        }
    }

    // This launcher now correctly calls our processing function
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { videoUri: Uri? ->
            videoUri?.let {
                startProcessing(it)
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // This button is now fully functional again
            Button(onClick = {
                videoPickerLauncher.launch("video/*")
            }, enabled = !isLoading) { // Disable buttons while loading
                Text("Select Video")
            }
            Button(onClick = {
                val debugVideoUri = getUriFromRaw(context, R.raw.debug_video)
                startProcessing(debugVideoUri)
            }, enabled = !isLoading) { // Disable buttons while loading
                Text("Debug Process")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            // NEW: Progress bar and text
            Text(text = "Processing frame: $progress / $totalFrames")
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress / totalFrames.toFloat() },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        } else if (frames.isNotEmpty()) {
            Text("Extracted Frames (${frames.size}枚):")
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(frames) { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Extracted Frame",
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .border(1.dp, Color.Gray),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
        }
    }
}

// vvv This function now takes a progress callback vvv
private fun getEvenlySpacedFrames(
    context: Context,
    videoUri: Uri,
    frameCount: Int,
    onProgress: (Int) -> Unit // The new callback parameter
): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    val frames = mutableListOf<Bitmap>()
    try {
        retriever.setDataSource(context, videoUri)
        val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationString?.toLongOrNull() ?: 0
        if (durationMs > 0) {
            val intervalUs = (durationMs * 1000) / frameCount
            for (i in 1..frameCount) {
                val timeUs = i * intervalUs
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                frame?.let { frames.add(it) }
                onProgress(i) // Report progress after each frame
            }
        }
    } catch (e: Exception) {
        Log.e("VideoProcess", "フレームの取得に失敗しました", e)
    } finally {
        retriever.release()
    }
    return frames
}

// Helper functions (these are unchanged)
private fun getUriFromRaw(context: Context, resId: Int): Uri {
    return Uri.parse("android.resource://${context.packageName}/$resId")
}

private fun saveBitmapToPictures(context: Context, bitmap: Bitmap, displayName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/VideoFrames")
    }
    var uri: Uri? = null
    try {
        uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    } catch (e: Exception) {
        Log.e("VideoProcess", "Failed to save bitmap", e)
    }
    return uri
}