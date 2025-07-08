package com.seo4d696b75.android.glance_widget_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.glance_widget_demo.ui.MainScreen
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay
import android.content.Intent
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService


class MainActivity : ComponentActivity() {

    private lateinit var firebaseStorage: FirebaseStorage
    // Firebase匿名認証を行うサスペンド関数
    private suspend fun signInAnonymouslyIfNeeded() {
        android.util.Log.d("MainActivity", "Firebase anonymous authentication skipped")
    }
    // 権限名のリスト（Android 13+はREAD_MEDIA_IMAGESを使う）
    private val storagePermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    // 権限リクエストランチャ
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            android.util.Log.d("MainActivity", "All needed storage permissions granted")
            onStoragePermissionGranted()
        } else {
            android.util.Log.w("MainActivity", "Storage permissions denied")
            // 必要なら説明ダイアログなど出す
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        android.util.Log.d("MainActivity", "=== MainActivity onCreate ===")

        // Firebase Storage初期化
        firebaseStorage = FirebaseStorage.getInstance()

        // UIを常に表示（権限に依存せず）
        setContent {
            MainScreen()
        }

        // 権限チェックと初期化は並行して実行
        checkAndRequestStoragePermission()

        android.util.Log.d("MainActivity", "MainActivity setup completed")
    }


    // 手動で全権限をリクエストする関数（UI から呼び出し用）
    fun requestAllRequiredPermissions() {
        android.util.Log.d("MainActivity", "=== Manual Permission Request ===")

        // ストレージ権限をチェック・リクエスト
        checkAndRequestStoragePermission()

        // Firebase Storage接続テスト
        android.util.Log.d("MainActivity", "Testing Firebase Storage connectivity...")
        testFirebaseConnection()
    }

    // ストレージ権限チェック・リクエスト関数
    private fun checkAndRequestStoragePermission() {
        // Android 10+ の分離ストレージ対応…本アプリの own sandbox 配下のみなら権限不要だが、念のため
        val permissionsToRequest = storagePermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isEmpty()) {
            android.util.Log.d("MainActivity", "All storage permissions already granted")
            onStoragePermissionGranted()
        } else {
            android.util.Log.d(
                "MainActivity",
                "Requesting storage permissions: $permissionsToRequest"
            )
            storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // 権限OK時の初期化処理
    private fun onStoragePermissionGranted() {
        android.util.Log.d("MainActivity", "Storage permissions granted - initializing folders")

        // ウィジェット用の画像フォルダを作成
        createWidgetImageFolder()

        // 詳細なデバッグ情報を出力
        debugExternalStorage()
        
        // 画像ダウンロードを開始
        startImageDownloads()
    }

    private fun createWidgetImageFolder() {
        try {
            val baseDir = getExternalFilesDir(null)
            android.util.Log.d("MainActivity", "=== Widget Folder Creation ===")
            android.util.Log.d("MainActivity", "Base external files dir: ${baseDir?.absolutePath}")

            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")
                android.util.Log.d(
                    "MainActivity",
                    "Target WidgetImages dir: ${widgetImagesDir.absolutePath}"
                )

                if (!widgetImagesDir.exists()) {
                    val created = widgetImagesDir.mkdirs()
                    android.util.Log.d("MainActivity", "WidgetImages folder created: $created")
                    android.util.Log.d(
                        "MainActivity",
                        "Folder path: ${widgetImagesDir.absolutePath}"
                    )
                } else {
                    android.util.Log.d("MainActivity", "WidgetImages folder already exists")
                }

                // フォルダの存在と権限を確認
                android.util.Log.d("MainActivity", "Folder exists: ${widgetImagesDir.exists()}")
                android.util.Log.d(
                    "MainActivity",
                    "Folder is directory: ${widgetImagesDir.isDirectory}"
                )
                android.util.Log.d("MainActivity", "Folder readable: ${widgetImagesDir.canRead()}")
                android.util.Log.d("MainActivity", "Folder writable: ${widgetImagesDir.canWrite()}")

            } else {
                android.util.Log.e("MainActivity", "External files directory is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error creating WidgetImages folder", e)
        }
    }

    // Firebase Storageから指定されたローカルパスに画像をダウンロードする関数（リトライ機能付き）
    fun downloadImagesFromFirebaseStorageToPath(imagePaths: List<String>, localPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val maxRetries = 5
            val retryDelayMs = 2000L // 2秒間隔
            var currentRetry = 0
            
            android.util.Log.d("MainActivity", "🚀 Starting download to local path: $localPath")
            
            while (currentRetry < maxRetries) {
                try {
                    // Firebase匿名認証を確実に実施
                    signInAnonymouslyIfNeeded()

                    val baseDir = getExternalFilesDir(null)
                    if (baseDir != null) {
                        val widgetImagesDir = File(baseDir, localPath)

                        if (!widgetImagesDir.exists()) {
                            widgetImagesDir.mkdirs()
                            android.util.Log.d("MainActivity", "📁 Created directory: ${widgetImagesDir.absolutePath}")
                        }

                        // 既存の画像ファイルをチェック
                        val existingFiles = getExistingImageFiles(widgetImagesDir)
                        android.util.Log.d(
                            "MainActivity",
                            "Retry ${currentRetry + 1}/${maxRetries} for $localPath: Found ${existingFiles.size} existing image files"
                        )

                        var downloadCount = 0
                        var skipCount = 0
                        var failedCount = 0

                        imagePaths.forEachIndexed { index, imagePath ->
                            try {
                                // ファイル名を生成
                                val lowerName = imagePath.lowercase()
                                val extension = when {
                                    lowerName.endsWith(".jpg") -> "jpg"
                                    lowerName.endsWith(".jpeg") -> "jpeg"
                                    lowerName.endsWith(".png") -> "png"
                                    lowerName.endsWith(".webp") -> "webp"
                                    else -> "png"
                                }
                                val fileName = String.format("%03d.%s", index + 1, extension)
                                val outputFile = File(widgetImagesDir, fileName)

                                // 既にファイルが存在し、サイズが0でない場合はスキップ
                                if (outputFile.exists() && outputFile.length() > 0) {
                                    skipCount++
                                    return@forEachIndexed
                                }

                                withContext(Dispatchers.Main) {
                                    android.util.Log.d("MainActivity", "Downloading to $localPath: ${index + 1}/${imagePaths.size} from Firebase Storage: $imagePath")
                                }

                                val storageRef = firebaseStorage.reference.child(imagePath)
                                val maxDownloadSize = 50L * 1024 * 1024 // 50MB
                                val bytes = storageRef.getBytes(maxDownloadSize).await()

                                // ファイルに書き込み
                                outputFile.writeBytes(bytes)
                                downloadCount++

                                withContext(Dispatchers.Main) {
                                    android.util.Log.d(
                                        "MainActivity",
                                        "Downloaded to $localPath: ${outputFile.name} (${outputFile.length()} bytes)"
                                    )
                                }

                            } catch (e: Exception) {
                                failedCount++
                                withContext(Dispatchers.Main) {
                                    android.util.Log.e("MainActivity", "Failed to download to $localPath: ${index + 1}: $imagePath", e)
                                }
                            }
                        }

                        val totalSuccessful = downloadCount + skipCount
                        val targetCount = imagePaths.size

                        withContext(Dispatchers.Main) {
                            android.util.Log.d(
                                "MainActivity",
                                "Download attempt ${currentRetry + 1} for $localPath: Downloaded: $downloadCount, Skipped: $skipCount, Failed: $failedCount, Total: $totalSuccessful/$targetCount"
                            )
                        }

                        // すべてダウンロード完了チェック
                        if (totalSuccessful >= targetCount) {
                            withContext(Dispatchers.Main) {
                                android.util.Log.d(
                                    "MainActivity",
                                    "✅ All ${targetCount} images downloaded successfully to $localPath!"
                                )
                                
                                // ダウンロード完了をログに記録
                                android.util.Log.d("MainActivity", "📁 Final directory state for $localPath:")
                                android.util.Log.d("MainActivity", "  - Directory: ${widgetImagesDir.absolutePath}")
                                android.util.Log.d("MainActivity", "  - Exists: ${widgetImagesDir.exists()}")
                                android.util.Log.d("MainActivity", "  - Files count: ${widgetImagesDir.listFiles()?.size ?: 0}")
                                
                                widgetImagesDir.listFiles()?.take(5)?.forEach { file ->
                                    android.util.Log.d("MainActivity", "    - ${file.name} (${file.length()} bytes)")
                                }
                                
                                // WidgetAnimationServiceにキャッシュ再構築を指示
                                rebuildWidgetAnimationCache()
                            }
                            return@launch // 成功したので終了
                        } else {
                            withContext(Dispatchers.Main) {
                                android.util.Log.w(
                                    "MainActivity",
                                    "⚠️ Only $totalSuccessful/${targetCount} images available for $localPath. Retrying in ${retryDelayMs}ms..."
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("MainActivity", "Error in Firebase download process for $localPath (retry ${currentRetry + 1})", e)
                    }
                }
                
                currentRetry++
                
                // 最大リトライ回数に達していない場合は待機
                if (currentRetry < maxRetries) {
                    delay(retryDelayMs)
                }
            }
            
            // 最大リトライ回数に達した場合
            withContext(Dispatchers.Main) {
                android.util.Log.e(
                    "MainActivity",
                    "❌ Failed to download all images to $localPath after ${maxRetries} attempts"
                )
            }
        }
    }

    // Firebase Storageから画像をダウンロードする関数（リトライ機能付き）- 後方互換性のために保持
    fun downloadImagesFromFirebaseStorage(imagePaths: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            val maxRetries = 5
            val retryDelayMs = 2000L // 2秒間隔
            var currentRetry = 0
            
            while (currentRetry < maxRetries) {
                try {
                    // Firebase匿名認証を確実に実施
                    signInAnonymouslyIfNeeded()

                    val baseDir = getExternalFilesDir(null)
                    if (baseDir != null) {
                        val widgetImagesDir = File(baseDir, "Mao/State/Adle")

                        if (!widgetImagesDir.exists()) {
                            widgetImagesDir.mkdirs()
                        }

                        // 既存の画像ファイルをチェック
                        val existingFiles = getExistingImageFiles(widgetImagesDir)
                        android.util.Log.d(
                            "MainActivity",
                            "Retry ${currentRetry + 1}/${maxRetries}: Found ${existingFiles.size} existing image files"
                        )

                        var downloadCount = 0
                        var skipCount = 0
                        var failedCount = 0

                        imagePaths.forEachIndexed { index, imagePath ->
                            try {
                                // ファイル名を生成
                                val lowerName = imagePath.lowercase()
                                val extension = when {
                                    lowerName.endsWith(".jpg") -> "jpg"
                                    lowerName.endsWith(".jpeg") -> "jpeg"
                                    lowerName.endsWith(".png") -> "png"
                                    lowerName.endsWith(".webp") -> "webp"
                                    else -> "png"
                                }
                                val fileName = String.format("%03d.%s", index + 1, extension)
                                val outputFile = File(widgetImagesDir, fileName)

                                // 既にファイルが存在し、サイズが0でない場合はスキップ
                                if (outputFile.exists() && outputFile.length() > 0) {
                                    skipCount++
                                    return@forEachIndexed
                                }

                                withContext(Dispatchers.Main) {
                                    android.util.Log.d("MainActivity", "Downloading image ${index + 1}/${imagePaths.size} from Firebase Storage: $imagePath")
                                }

                                val storageRef = firebaseStorage.reference.child(imagePath)
                                val maxDownloadSize = 50L * 1024 * 1024 // 50MB
                                val bytes = storageRef.getBytes(maxDownloadSize).await()

                                // ファイルに書き込み
                                outputFile.writeBytes(bytes)
                                downloadCount++

                                withContext(Dispatchers.Main) {
                                    android.util.Log.d(
                                        "MainActivity",
                                        "Downloaded from Firebase: ${outputFile.name} (${outputFile.length()} bytes)"
                                    )
                                }

                            } catch (e: Exception) {
                                failedCount++
                                withContext(Dispatchers.Main) {
                                    android.util.Log.e("MainActivity", "Failed to download image ${index + 1}: $imagePath", e)
                                }
                            }
                        }

                        val totalSuccessful = downloadCount + skipCount
                        val targetCount = imagePaths.size

                        withContext(Dispatchers.Main) {
                            android.util.Log.d(
                                "MainActivity",
                                "Firebase download attempt ${currentRetry + 1}: Downloaded: $downloadCount, Skipped: $skipCount, Failed: $failedCount, Total: $totalSuccessful/$targetCount"
                            )
                        }

                        // 50枚すべてダウンロード完了チェック
                        if (totalSuccessful >= targetCount) {
                            withContext(Dispatchers.Main) {
                                android.util.Log.d(
                                    "MainActivity",
                                    "✅ All ${targetCount} images downloaded successfully!"
                                )
                                notifyWidgetUpdate()
                                
                                // WidgetAnimationServiceにキャッシュ再構築を指示
                                rebuildWidgetAnimationCache()
                            }
                            return@launch // 成功したので終了
                        } else {
                            withContext(Dispatchers.Main) {
                                android.util.Log.w(
                                    "MainActivity",
                                    "⚠️ Only $totalSuccessful/${targetCount} images available. Retrying in ${retryDelayMs}ms..."
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.e("MainActivity", "Error in Firebase download process (retry ${currentRetry + 1})", e)
                    }
                }
                
                currentRetry++
                
                // 最大リトライ回数に達していない場合は待機
                if (currentRetry < maxRetries) {
                    delay(retryDelayMs)
                }
            }
            
            // 最大リトライ回数に達した場合
            withContext(Dispatchers.Main) {
                android.util.Log.e(
                    "MainActivity",
                    "❌ Failed to download all images after ${maxRetries} attempts"
                )
                // 部分的なダウンロードでもウィジェットを更新
                notifyWidgetUpdate()
            }
        }
    }

    // 既存の画像ファイルリストを取得する関数
    private fun getExistingImageFiles(directory: File): List<File> {
        return try {
            directory.listFiles()?.filter { file ->
                file.isFile && file.extension.lowercase() in listOf(
                    "jpg",
                    "jpeg",
                    "png",
                    "webp"
                ) && file.length() > 0
            }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error getting existing image files", e)
            emptyList()
        }
    }


    // Firebase接続テスト関数（複数パス対応）
    fun testFirebaseConnection() {
        android.util.Log.d("MainActivity", "=== Firebase Connection Test (Multiple Paths) ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firebase匿名認証を確実に実施
                signInAnonymouslyIfNeeded()

                // Storage参照のテスト
                val storageRef = firebaseStorage.reference
                android.util.Log.d("MainActivity", "Storage reference created: ${storageRef.path}")

                // ダウンロード対象のパスリスト
                val downloadPaths = listOf(
                    "Mao/State/Adle",
                    "Mao/State/Flow"
                )

                var totalDownloadTasks = 0

                for (path in downloadPaths) {
                    try {
                        android.util.Log.d("MainActivity", "🔍 Checking path: $path")
                        
                        val pathRef = storageRef.child(path)
                        val listResult = pathRef.listAll().await()

                        withContext(Dispatchers.Main) {
                            android.util.Log.d(
                                "MainActivity",
                                "Files found in $path/: ${listResult.items.size}"
                            )
                            listResult.items.take(5).forEach { item ->
                                android.util.Log.d("MainActivity", "  - ${item.name}")
                            }
                            if (listResult.items.size > 5) {
                                android.util.Log.d("MainActivity", "  - ... and ${listResult.items.size - 5} more files")
                            }
                        }

                        if (listResult.items.isNotEmpty()) {
                            totalDownloadTasks++
                            android.util.Log.d("MainActivity", "✅ Found ${listResult.items.size} images in $path")
                            
                            // 実際の画像ダウンロードを実行
                            val imagePaths = listResult.items.map { "$path/${it.name}" }
                            val localPath = path // Firebase Storageパスをそのままローカルパスとして使用
                            downloadImagesFromFirebaseStorageToPath(imagePaths, localPath)
                        } else {
                            android.util.Log.w(
                                "MainActivity",
                                "⚠️ No images found in Firebase Storage /$path/ folder"
                            )
                        }

                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "❌ Error processing path $path", e)
                    }
                }

                if (totalDownloadTasks == 0) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.w("MainActivity", "⚠️ No images found in any Firebase Storage paths")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("MainActivity", "Firebase connection test failed", e)
                    android.util.Log.w("MainActivity", "Falling back to HTTP image download")
                   // downloadTestImagesFromInternet()
                }
            }
        }
    }



    private fun rebuildWidgetAnimationCache() {
        try {
            android.util.Log.d("MainActivity", "🔄 Requesting WidgetAnimationService to rebuild cache...")
            
            val serviceIntent = Intent(this, com.seo4d696b75.android.glance_widget_demo.widget.WidgetAnimationService::class.java).apply {
                action = "REBUILD_CACHE"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            android.util.Log.d("MainActivity", "✅ Cache rebuild request sent to WidgetAnimationService")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Failed to request cache rebuild", e)
        }
    }

    private fun notifyWidgetUpdate() {
        try {
            android.util.Log.d("MainActivity", "=== Notifying Widget Update ===")

            // AppWidgetManagerを使用してウィジェットを強制更新
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(this)
            val componentName = android.content.ComponentName(
                this,
                com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider::class.java
            )

            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            android.util.Log.d("MainActivity", "Found ${appWidgetIds.size} widgets to update")

            if (appWidgetIds.isNotEmpty()) {
                // 直接onUpdateを呼び出す
                val widgetProvider =
                    com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider()
                widgetProvider.onUpdate(this, appWidgetManager, appWidgetIds)
                android.util.Log.d("MainActivity", "Direct widget update invoked")

                // ブロードキャストも送信（追加保険）
                val intent = android.content.Intent().apply {
                    component = android.content.ComponentName(
                        "com.seo4d696b75.android.glance_widget_demo",
                        "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
                    )
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                sendBroadcast(intent)
                android.util.Log.d(
                    "MainActivity",
                    "Widget update broadcast sent for ${appWidgetIds.size} widgets"
                )

                // 強制的にウィジェットを再初期化するための追加ブロードキャスト
                val forceRefreshIntent =
                    android.content.Intent("com.seo4d696b75.android.glance_widget_demo.FORCE_REFRESH")
                sendBroadcast(forceRefreshIntent)
                android.util.Log.d("MainActivity", "Force refresh broadcast sent")

            } else {
                android.util.Log.w(
                    "MainActivity",
                    "No widgets found - please add widget to home screen"
                )
            }

        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error notifying widget update", e)
        }
    }

    // ウィジェット用画像を全てクリアする関数
    fun clearAllWidgetImages() {
        android.util.Log.d("MainActivity", "=== Clearing All Widget Images ===")

        try {
            val baseDir = getExternalFilesDir(null)
            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")

                if (widgetImagesDir.exists() && widgetImagesDir.isDirectory) {
                    val allFiles = widgetImagesDir.listFiles()
                    android.util.Log.d(
                        "MainActivity",
                        "Found ${allFiles?.size ?: 0} files to delete"
                    )

                    var deletedCount = 0
                    allFiles?.forEach { file ->
                        if (file.isFile) {
                            val deleted = file.delete()
                            if (deleted) {
                                deletedCount++
                                android.util.Log.d("MainActivity", "Deleted: ${file.name}")
                            } else {
                                android.util.Log.w("MainActivity", "Failed to delete: ${file.name}")
                            }
                        }
                    }

                    android.util.Log.d("MainActivity", "Successfully deleted $deletedCount files")

                    // ウィジェットを更新してクリア状態を反映
                    notifyWidgetUpdate()

                } else {
                    android.util.Log.w("MainActivity", "WidgetImages directory does not exist")
                }
            } else {
                android.util.Log.e("MainActivity", "External files directory is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error clearing widget images", e)
        }
    }

    private fun startImageDownloads() {
        android.util.Log.d("MainActivity", "=== Starting Image Downloads ===")
        
        try {
            // 現在のディレクトリ状況を確認
            val baseDir = getExternalFilesDir(null)
            android.util.Log.d("MainActivity", "Base directory: ${baseDir?.absolutePath}")
            
            // Mao/State/Adle と Mao/State/Flow ディレクトリの状況を確認
            val maoDir = File(baseDir, "Mao")
            val stateDir = File(maoDir, "State")
            val adleDir = File(stateDir, "Adle")
            val flowDir = File(stateDir, "Flow")
            
            android.util.Log.d("MainActivity", "📁 Directory status:")
            android.util.Log.d("MainActivity", "  - Mao: exists=${maoDir.exists()}")
            android.util.Log.d("MainActivity", "  - State: exists=${stateDir.exists()}")
            android.util.Log.d("MainActivity", "  - Adle: exists=${adleDir.exists()}, files=${adleDir.listFiles()?.size ?: 0}")
            android.util.Log.d("MainActivity", "  - Flow: exists=${flowDir.exists()}, files=${flowDir.listFiles()?.size ?: 0}")
            
            // Maoキャラクターの画像をダウンロード
            android.util.Log.d("MainActivity", "🚀 Starting download for Mao character")
            
            // 全てのアニメーションタイプをダウンロード
            ImageDownloadService.downloadCharacterImages(this, "Mao")
            
            android.util.Log.d("MainActivity", "✅ Image download service started for Mao character")
            
            // 5秒後にダウンロード状況を再確認
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(5000)
                checkDownloadStatus()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "❌ Error starting image downloads", e)
        }
    }
    
    private fun checkDownloadStatus() {
        android.util.Log.d("MainActivity", "=== Download Status Check ===")
        
        val baseDir = getExternalFilesDir(null)
        val adleDir = File(baseDir, "Mao/State/Adle")
        val flowDir = File(baseDir, "Mao/State/Flow")
        
        android.util.Log.d("MainActivity", "📊 Download results:")
        android.util.Log.d("MainActivity", "  - Adle images: ${adleDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        android.util.Log.d("MainActivity", "  - Flow images: ${flowDir.listFiles()?.count { it.isFile && it.extension.lowercase() == "png" } ?: 0}")
        
        if (adleDir.exists()) {
            adleDir.listFiles()?.take(3)?.forEach { file ->
                android.util.Log.d("MainActivity", "  - Adle file: ${file.name} (${file.length()} bytes)")
            }
        }
        
        if (flowDir.exists()) {
            flowDir.listFiles()?.take(3)?.forEach { file ->
                android.util.Log.d("MainActivity", "  - Flow file: ${file.name} (${file.length()} bytes)")
            }
        }
    }
    
    private fun debugExternalStorage() {
        try {
            android.util.Log.d("MainActivity", "=== External Storage Debug ===")

            val baseDir = getExternalFilesDir(null)
            if (baseDir != null) {
                val widgetImagesDir = File(baseDir, "WidgetImages")

                // ディレクトリ内のファイルをすべて確認
                if (widgetImagesDir.exists() && widgetImagesDir.isDirectory) {
                    val allFiles = widgetImagesDir.listFiles()
                    android.util.Log.d(
                        "MainActivity",
                        "Total files in WidgetImages: ${allFiles?.size ?: 0}"
                    )

                    allFiles?.forEach { file ->
                        android.util.Log.d("MainActivity", "File found: ${file.name}")
                        android.util.Log.d("MainActivity", "  - Extension: ${file.extension}")
                        android.util.Log.d("MainActivity", "  - Is file: ${file.isFile}")
                        android.util.Log.d("MainActivity", "  - Size: ${file.length()} bytes")
                        android.util.Log.d("MainActivity", "  - Full path: ${file.absolutePath}")

                        // 画像ファイルかチェック
                        val isImageFile =
                            file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                        android.util.Log.d("MainActivity", "  - Is image file: $isImageFile")
                    }

                    // 画像ファイルのみを抽出
                    val imageFiles = allFiles?.filter { file ->
                        file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                    }?.sortedBy { it.name }

                    android.util.Log.d(
                        "MainActivity",
                        "Total image files: ${imageFiles?.size ?: 0}"
                    )
                    imageFiles?.forEachIndexed { index, file ->
                        android.util.Log.d("MainActivity", "Image $index: ${file.name}")
                    }
                } else {
                    android.util.Log.w(
                        "MainActivity",
                        "WidgetImages directory does not exist or is not a directory"
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error debugging external storage", e)
        }
    }
}

