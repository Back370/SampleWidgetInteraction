package com.seo4d696b75.android.glance_widget_demo.data

import android.content.Context
import android.util.Log
import com.seo4d696b75.android.glance_widget_demo.domain.ImageCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCacheRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) : ImageCacheRepository {
    
    private val firebaseStorage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    companion object {
        private const val TAG = "ImageCacheRepository"
        private const val CACHE_DIR_NAME = "widget_assets"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024 // 100MB
        private const val CACHE_FILE_EXTENSION = ".png"
    }

    private val cacheDir: File by lazy {
        // 外部ファイルディレクトリを使用（ウィジェットアニメーションと同じ場所）
        File(context.getExternalFilesDir(null), "").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override suspend fun downloadAndCacheImage(
        imageUrl: String,
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(characterId, animationType, frameIndex)
            
            Log.d(TAG, "📁 Attempting to cache image: $characterId/$animationType/$frameIndex")
            Log.d(TAG, "📁 Cache file path: ${cacheFile.absolutePath}")
            Log.d(TAG, "📁 Parent directory: ${cacheFile.parentFile?.absolutePath}")
            
            // 既にキャッシュが存在する場合は返す
            if (cacheFile.exists()) {
                Log.d(TAG, "✅ Image already cached: ${cacheFile.absolutePath}")
                return@withContext Result.success(cacheFile)
            }

            // ディレクトリが存在しない場合は作成
            val parentDir = cacheFile.parentFile
            if (parentDir != null && !parentDir.exists()) {
                val mkdirResult = parentDir.mkdirs()
                Log.d(TAG, "📁 Created directory: ${parentDir.absolutePath} - Success: $mkdirResult")
            }

            Log.d(TAG, "Downloading image from Firebase Storage: $imageUrl")
            Log.d(TAG, "Saving to: ${cacheFile.absolutePath}")

            // Firebase Storageからダウンロード
            if (imageUrl.startsWith("gs://")) {
                // Firebase Storage URL の場合
                val storageRef = firebaseStorage.getReferenceFromUrl(imageUrl)
                
                // ファイルにダウンロード
                storageRef.getFile(cacheFile).await()
                
                Log.d(TAG, "✅ Firebase image cached successfully: ${cacheFile.absolutePath}")
                Log.d(TAG, "📏 File size: ${cacheFile.length()} bytes")
                Log.d(TAG, "📂 File exists: ${cacheFile.exists()}")
                
            } else {
                // HTTP URL の場合（フォールバック）
                val request = Request.Builder()
                    .url(imageUrl)
                    .build()

                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("Failed to download image: ${response.code}")
                }

                response.body?.let { body ->
                    FileOutputStream(cacheFile).use { output ->
                        body.byteStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                } ?: throw IOException("Empty response body")
                
                Log.d(TAG, "HTTP image cached successfully: ${cacheFile.absolutePath}")
            }

            // キャッシュサイズチェック
            cleanupOldCache()
            
            Result.success(cacheFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and cache image: $imageUrl", e)
            Log.e(TAG, "Target file: ${getCacheFile(characterId, animationType, frameIndex).absolutePath}")
            Result.failure(e)
        }
    }

    override fun getCachedImageFile(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): File? {
        val cacheFile = getCacheFile(characterId, animationType, frameIndex)
        return if (cacheFile.exists()) cacheFile else null
    }

    override fun isCacheExists(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Boolean {
        return getCacheFile(characterId, animationType, frameIndex).exists()
    }

    override fun getCacheSize(): Long {
        return calculateDirectorySize(cacheDir)
    }

    override suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            Log.d(TAG, "Cache cleared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
        Unit
    }

    override suspend fun cleanupOldCache(): Unit = withContext(Dispatchers.IO) {
        try {
            val currentSize = getCacheSize()
            if (currentSize > MAX_CACHE_SIZE) {
                // 古いファイルから削除
                val files = cacheDir.walkTopDown()
                    .filter { it.isFile }
                    .sortedBy { it.lastModified() }
                    .toList()

                var deletedSize = 0L
                for (file in files) {
                    if (currentSize - deletedSize <= MAX_CACHE_SIZE * 0.8) {
                        break
                    }
                    deletedSize += file.length()
                    file.delete()
                }

                Log.d(TAG, "Cleanup completed: deleted $deletedSize bytes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup cache", e)
        }
        Unit
    }

    private fun getCacheFile(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): File {
        // ウィジェットアニメーションが期待するディレクトリ構造に合わせる
        val subDir = File(cacheDir, "$characterId/State/$animationType")
        val filename = "${(frameIndex + 1).toString().padStart(3, '0')}.png"  // 001.png, 002.png, ... のフォーマット
        return File(subDir, filename)
    }

    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists()) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    size += file.length()
                }
            }
        }
        return size
    }

    private fun generateCacheKey(url: String): String {
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(url.toByteArray())
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
} 