package com.seo4d696b75.android.glance_widget_demo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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

    // Firebase Storageから画像をダウンロードする関数
    fun downloadImagesFromFirebaseStorage(imagePaths: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firebase匿名認証を確実に実施
                signInAnonymouslyIfNeeded()

                val baseDir = getExternalFilesDir(null)
                if (baseDir != null) {
                    val widgetImagesDir = File(baseDir, "WidgetImages")

                    if (!widgetImagesDir.exists()) {
                        widgetImagesDir.mkdirs()
                    }

                    // 既存の画像ファイルをチェック
                    val existingFiles = getExistingImageFiles(widgetImagesDir)
                    android.util.Log.d(
                        "MainActivity",
                        "Found ${existingFiles.size} existing image files"
                    )

                    var downloadCount = 0
                    var skipCount = 0

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
                                withContext(Dispatchers.Main) {
                                    android.util.Log.d(
                                        "MainActivity",
                                        "Skipping existing file: ${outputFile.name} (${outputFile.length()} bytes)"
                                    )
                                }
                                skipCount++
                                return@forEachIndexed
                            }

                            withContext(Dispatchers.Main) {
                                android.util.Log.d("MainActivity", "Downloading image $index from Firebase Storage: $imagePath")
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
                                    "Downloaded from Firebase: ${outputFile.absolutePath} (${outputFile.length()} bytes)"
                                )
                            }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.util.Log.e("MainActivity", "Failed to download image $index: $imagePath", e)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        android.util.Log.d(
                            "MainActivity",
                            "Firebase image download completed - Downloaded: $downloadCount, Skipped: $skipCount"
                        )
                        notifyWidgetUpdate()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("MainActivity", "Error in Firebase download process", e)
                }
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

    // UI呼び出し用のFirebase画像ダウンロード関数
    fun downloadImagesFromFirebase() {
        android.util.Log.d("MainActivity", "=== Firebase Images Download Demo ===")
        // Firebase接続テスト機能を使う
        testFirebaseConnection()
    }

    // Firebase接続テスト関数
    fun testFirebaseConnection() {
        android.util.Log.d("MainActivity", "=== Firebase Connection Test ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Firebase匿名認証を確実に実施
                signInAnonymouslyIfNeeded()

                // Storage参照のテスト
                val storageRef = firebaseStorage.reference
                android.util.Log.d("MainActivity", "Storage reference created: ${storageRef.path}")

                // Adleフォルダのファイル一覧取得テスト
                val adleRef = storageRef.child("Adle")
                val listResult = adleRef.listAll().await()

                withContext(Dispatchers.Main) {
                    android.util.Log.d(
                        "MainActivity",
                        "Files found in Adle/: ${listResult.items.size}"
                    )
                    listResult.items.forEach { item ->
                        android.util.Log.d("MainActivity", "  - ${item.name}")
                    }

                    if (listResult.items.isNotEmpty()) {
                        android.util.Log.d("MainActivity", "Firebase Storage access successful!")
                        // 実際の画像ダウンロードを実行
                        val imagePaths = listResult.items.map { "Adle/${it.name}" }
                        downloadImagesFromFirebaseStorage(imagePaths)
                    } else {
                        android.util.Log.w(
                            "MainActivity",
                            "No images found in Firebase Storage /Adle/ folder"
                        )
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

    // インターネットから実際の画像をダウンロードする関数
//    fun downloadTestImagesFromInternet() {
//        android.util.Log.d("MainActivity", "=== Downloading Test Images from Internet ===")
//
//        // 実際に動作する画像URLのリスト
//        val workingImageUrls = listOf(
////            "https://picsum.photos/300/300?random=1",
////            "https://picsum.photos/300/300?random=2",
////            "https://picsum.photos/300/300?random=3",
////            "https://picsum.photos/300/300?random=4",
////            "https://picsum.photos/300/300?random=5",
////            "https://picsum.photos/300/300?random=6",
////            "https://picsum.photos/300/300?random=7",
////            "https://picsum.photos/300/300?random=8"
//        )
//
//        android.util.Log.d(
//            "MainActivity",
//            "Starting download of ${workingImageUrls.size} test images"
//        )
//        downloadImagesFromServer(workingImageUrls)
//    }

    private fun clearExistingImages(directory: File) {
        try {
            directory.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() in listOf(
                        "jpg",
                        "jpeg",
                        "png",
                        "webp"
                    )
                ) {
                    file.delete()
                    android.util.Log.d("MainActivity", "Deleted existing image: ${file.name}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error clearing existing images", e)
        }
    }

    // サーバーから画像をダウンロードする関数（改善・エラーハンドリング強化版）
    fun downloadImagesFromServer(imageUrls: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val baseDir = getExternalFilesDir(null)
                if (baseDir != null) {
                    val widgetImagesDir = File(baseDir, "WidgetImages")

                    if (!widgetImagesDir.exists()) {
                        widgetImagesDir.mkdirs()
                    }

                    // 既存画像をクリア
                    clearExistingImages(widgetImagesDir)

                    var successCount = 0
                    imageUrls.forEachIndexed { index, imageUrl ->
                        try {
                            withContext(Dispatchers.Main) {
                                android.util.Log.d(
                                    "MainActivity",
                                    "Downloading image ${index + 1}/${imageUrls.size} from: $imageUrl"
                                )
                            }

                            val url = URL(imageUrl)
                            val connection = url.openConnection().apply {
                                connectTimeout = 10000 // 10秒タイムアウト
                                readTimeout = 15000 // 15秒読み取りタイムアウト
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                            }
                            val inputStream = connection.getInputStream()

                            // ファイル名を決定（URL末尾の拡張子でなく picsum のようなパラメータ付URLの場合は jpg 等デフォルトを使う）
                            val hasImageExtension = Regex(
                                ".*\\.(jpg|jpeg|png|webp)\$",
                                RegexOption.IGNORE_CASE
                            ).matches(url.path)
                            val extension = when {
                                url.path.endsWith(".jpg", ignoreCase = true) -> "jpg"
                                url.path.endsWith(".jpeg", ignoreCase = true) -> "jpeg"
                                url.path.endsWith(".png", ignoreCase = true) -> "png"
                                url.path.endsWith(".webp", ignoreCase = true) -> "webp"
                                else -> "jpg" // デフォルトは jpg (picsum とか)
                            }
                            val fileName = String.format("%03d.%s", index + 1, extension)
                            val outputFile = File(widgetImagesDir, fileName)

                            // ファイルにダウンロード
                            FileOutputStream(outputFile).use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                            inputStream.close()

                            withContext(Dispatchers.Main) {
                                android.util.Log.d(
                                    "MainActivity",
                                    "Successfully downloaded: ${outputFile.absolutePath} (${outputFile.length()} bytes)"
                                )
                            }
                            successCount++

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                android.util.Log.e(
                                    "MainActivity",
                                    "Failed to download image ${index + 1}: $imageUrl - ${e.message}",
                                    e
                                )
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        android.util.Log.d(
                            "MainActivity",
                            "Image download completed: $successCount/${imageUrls.size} successful"
                        )
                        if (successCount > 0) {
                            // ダウンロード完了後にウィジェットを更新
                            android.util.Log.d(
                                "MainActivity",
                                "Updating widget with $successCount images"
                            )
                            notifyWidgetUpdate()
                        } else {
                            android.util.Log.w("MainActivity", "No images downloaded successfully")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("MainActivity", "Error in download process", e)
                }
            }
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

    // ウィジェットを強制的にリフレッシュする関数
    fun forceRefreshWidget() {
        android.util.Log.d("MainActivity", "=== Force Refresh Widget ===")

        // 現在の画像数をログ出力
        val baseDir = getExternalFilesDir(null)
        if (baseDir != null) {
            val widgetImagesDir = File(baseDir, "WidgetImages")
            val imageFiles = getExistingImageFiles(widgetImagesDir)
            android.util.Log.d("MainActivity", "Current images in storage: ${imageFiles.size}")
            imageFiles.forEachIndexed { index, file ->
                android.util.Log.d("MainActivity", "Image $index: ${file.name}")
            }
        }

        // ウィジェット更新を通知
        notifyWidgetUpdate()
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
