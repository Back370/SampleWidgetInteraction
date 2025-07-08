package com.seo4d696b75.android.glance_widget_demo

import androidx.compose.runtime.Composable
import java.io.File



//// ウィジェットを強制的にリフレッシュする関数
//fun forceRefreshWidget() {
//    android.util.Log.d("MainActivity", "=== Force Refresh Widget ===")
//
//    // 現在の画像数をログ出力
//    val baseDir = getExternalFilesDir(null)
//    if (baseDir != null) {
//        val widgetImagesDir = File(baseDir, "WidgetImages")
//        val imageFiles = getExistingImageFiles(widgetImagesDir)
//        android.util.Log.d("MainActivity", "Current images in storage: ${imageFiles.size}")
//        imageFiles.forEachIndexed { index, file ->
//            android.util.Log.d("MainActivity", "Image $index: ${file.name}")
//        }
//    }
//
//    // ウィジェット更新を通知
//    notifyWidgetUpdate()
//}


//private fun clearExistingImages(directory: File) {
//    try {
//        directory.listFiles()?.forEach { file ->
//            if (file.isFile && file.extension.lowercase() in listOf(
//                    "jpg",
//                    "jpeg",
//                    "png",
//                    "webp"
//                )
//            ) {
//                file.delete()
//                android.util.Log.d("MainActivity", "Deleted existing image: ${file.name}")
//            }
//        }
//    } catch (e: Exception) {
//        android.util.Log.e("MainActivity", "Error clearing existing images", e)
//    }
//}


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
// UI呼び出し用のFirebase画像ダウンロード関数
//fun downloadImagesFromFirebase() {
//    android.util.Log.d("MainActivity", "=== Firebase Images Download Demo ===")
//    // Firebase接続テスト機能を使う
//    testFirebaseConnection()
//}
// サーバーから画像をダウンロードする関数（改善・エラーハンドリング強化版）
//fun downloadImagesFromServer(imageUrls: List<String>) {
//    CoroutineScope(Dispatchers.IO).launch {
//        try {
//            val baseDir = getExternalFilesDir(null)
//            if (baseDir != null) {
//                val widgetImagesDir = File(baseDir, "Mao/State/Adle")
//
//                if (!widgetImagesDir.exists()) {
//                    widgetImagesDir.mkdirs()
//                }
//
//                // 既存画像をクリア
//                clearExistingImages(widgetImagesDir)
//
//                var successCount = 0
//                imageUrls.forEachIndexed { index, imageUrl ->
//                    try {
//                        withContext(Dispatchers.Main) {
//                            android.util.Log.d(
//                                "MainActivity",
//                                "Downloading image ${index + 1}/${imageUrls.size} from: $imageUrl"
//                            )
//                        }
//
//                        val url = URL(imageUrl)
//                        val connection = url.openConnection().apply {
//                            connectTimeout = 10000 // 10秒タイムアウト
//                            readTimeout = 15000 // 15秒読み取りタイムアウト
//                            setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
//                        }
//                        val inputStream = connection.getInputStream()
//
//                        // ファイル名を決定（URL末尾の拡張子でなく picsum のようなパラメータ付URLの場合は jpg 等デフォルトを使う）
//                        val hasImageExtension = Regex(
//                            ".*\\.(jpg|jpeg|png|webp)\$",
//                            RegexOption.IGNORE_CASE
//                        ).matches(url.path)
//                        val extension = when {
//                            url.path.endsWith(".jpg", ignoreCase = true) -> "jpg"
//                            url.path.endsWith(".jpeg", ignoreCase = true) -> "jpeg"
//                            url.path.endsWith(".png", ignoreCase = true) -> "png"
//                            url.path.endsWith(".webp", ignoreCase = true) -> "webp"
//                            else -> "jpg" // デフォルトは jpg (picsum とか)
//                        }
//                        val fileName = String.format("%03d.%s", index + 1, extension)
//                        val outputFile = File(widgetImagesDir, fileName)
//
//                        // ファイルにダウンロード
//                        FileOutputStream(outputFile).use { outputStream ->
//                            inputStream.copyTo(outputStream)
//                        }
//                        inputStream.close()
//
//                        withContext(Dispatchers.Main) {
//                            android.util.Log.d(
//                                "MainActivity",
//                                "Successfully downloaded: ${outputFile.absolutePath} (${outputFile.length()} bytes)"
//                            )
//                        }
//                        successCount++
//
//                    } catch (e: Exception) {
//                        withContext(Dispatchers.Main) {
//                            android.util.Log.e(
//                                "MainActivity",
//                                "Failed to download image ${index + 1}: $imageUrl - ${e.message}",
//                                e
//                            )
//                        }
//                    }
//                }
//
//                withContext(Dispatchers.Main) {
//                    android.util.Log.d(
//                        "MainActivity",
//                        "Image download completed: $successCount/${imageUrls.size} successful"
//                    )
//                    if (successCount > 0) {
//                        // ダウンロード完了後にウィジェットを更新
//                        android.util.Log.d(
//                            "MainActivity",
//                            "Updating widget with $successCount images"
//                        )
//                        notifyWidgetUpdate()
//                    } else {
//                        android.util.Log.w("MainActivity", "No images downloaded successfully")
//                    }
//                }
//            }
//        } catch (e: Exception) {
//            withContext(Dispatchers.Main) {
//                android.util.Log.e("MainActivity", "Error in download process", e)
//            }
//        }
//    }
//}