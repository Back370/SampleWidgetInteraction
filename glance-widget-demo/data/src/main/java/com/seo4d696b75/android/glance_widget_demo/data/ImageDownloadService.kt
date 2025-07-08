package com.seo4d696b75.android.glance_widget_demo.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.seo4d696b75.android.glance_widget_demo.domain.CharacterImageConfig
import com.seo4d696b75.android.glance_widget_demo.domain.ImageCacheRepository
import com.seo4d696b75.android.glance_widget_demo.domain.ImageUrlGenerator
import com.seo4d696b75.android.glance_widget_demo.domain.SampleCharacterConfigs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 画像ダウンロード用のバックグラウンドサービス
 */
@AndroidEntryPoint
class ImageDownloadService : Service() {
    
    @Inject
    lateinit var imageCacheRepository: ImageCacheRepository
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "ImageDownloadService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "image_download_channel"
        
        const val ACTION_DOWNLOAD_CHARACTER = "download_character"
        const val ACTION_DOWNLOAD_ALL = "download_all"
        const val ACTION_CLEAR_CACHE = "clear_cache"
        
        const val EXTRA_CHARACTER_ID = "character_id"
        const val EXTRA_ANIMATION_TYPE = "animation_type"
        
        /**
         * 特定キャラクターの画像をダウンロード
         */
        fun downloadCharacterImages(
            context: Context,
            characterId: String,
            animationType: String? = null
        ) {
            val intent = Intent(context, ImageDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_CHARACTER
                putExtra(EXTRA_CHARACTER_ID, characterId)
                animationType?.let { putExtra(EXTRA_ANIMATION_TYPE, it) }
            }
            context.startService(intent)
        }
        
        /**
         * 全キャラクターの画像をダウンロード
         */
        fun downloadAllImages(context: Context) {
            val intent = Intent(context, ImageDownloadService::class.java).apply {
                action = ACTION_DOWNLOAD_ALL
            }
            context.startService(intent)
        }
        
        /**
         * キャッシュをクリア
         */
        fun clearCache(context: Context) {
            val intent = Intent(context, ImageDownloadService::class.java).apply {
                action = ACTION_CLEAR_CACHE
            }
            context.startService(intent)
        }
        
        /**
         * 実際のフレーム数を取得（他クラスから呼び出し可能）
         */
        fun getActualFrameCount(context: Context, characterId: String, animationType: String, defaultCount: Int): Int {
            return try {
                val prefs = context.getSharedPreferences("actual_frame_counts", Context.MODE_PRIVATE)
                val key = "${characterId}_${animationType}"
                val actualCount = prefs.getInt(key, defaultCount)
                Log.d(TAG, "📖 Retrieved actual frame count: $key = $actualCount")
                actualCount
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error retrieving actual frame count", e)
                defaultCount
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔧 ImageDownloadService onCreate")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 ImageDownloadService onStartCommand called")
        Log.d(TAG, "Intent: ${intent?.toString()}")
        Log.d(TAG, "Action: ${intent?.action}")
        
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "=== ImageDownloadService handleIntent ===")
        Log.d(TAG, "Action: ${intent.action}")
        
        when (intent.action) {
            ACTION_DOWNLOAD_CHARACTER -> {
                val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID) ?: return
                val animationType = intent.getStringExtra(EXTRA_ANIMATION_TYPE)
                Log.d(TAG, "📥 Download character request: $characterId, animation: $animationType")
                downloadCharacterImages(characterId, animationType)
            }
            ACTION_DOWNLOAD_ALL -> {
                Log.d(TAG, "📥 Download all images request")
                downloadAllImages()
            }
            ACTION_CLEAR_CACHE -> {
                Log.d(TAG, "📥 Clear cache request")
                clearCache()
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }
    
    private fun downloadCharacterImages(characterId: String, animationType: String?) {
        Log.d(TAG, "🚀 downloadCharacterImages called: characterId=$characterId, animationType=$animationType")
        
        serviceScope.launch {
            try {
                showNotification("画像ダウンロード中", "$characterId の画像をダウンロードしています...")
                
                val config = SampleCharacterConfigs.getCharacterById(characterId)
                if (config == null) {
                    Log.e(TAG, "❌ Character config not found: $characterId")
                    showNotification("エラー", "キャラクター設定が見つかりません: $characterId")
                    stopSelf()
                    return@launch
                }
                
                Log.d(TAG, "✅ Character config found: ${config.characterId}")
                Log.d(TAG, "📁 Base URL: ${config.baseUrl}")
                Log.d(TAG, "🎬 Available animations: ${config.animations.map { it.animationType }}")
                
                val animationsToDownload = if (animationType != null) {
                    config.animations.filter { it.animationType == animationType }
                } else {
                    config.animations
                }
                
                Log.d(TAG, "📋 Animations to download: ${animationsToDownload.map { it.animationType }}")
                
                var totalDownloaded = 0
                var totalFailed = 0
                
                for (animation in animationsToDownload) {
                    Log.d(TAG, "🎬 Downloading animation: ${animation.animationType} (${animation.frameCount} frames)")
                    
                    var actualFrameCount = 0
                    var consecutiveFailures = 0
                    
                    for (frameIndex in 0 until animation.frameCount) {
                        try {
                            val imageUrl = ImageUrlGenerator.generateImageUrl(
                                config,
                                animation.animationType,
                                frameIndex
                            )
                            
                            Log.d(TAG, "🔗 Frame $frameIndex URL: $imageUrl")
                            
                            val result = imageCacheRepository.downloadAndCacheImage(
                                imageUrl,
                                config.characterId,
                                animation.animationType,
                                frameIndex
                            )
                            
                            if (result.isSuccess) {
                                totalDownloaded++
                                actualFrameCount = frameIndex + 1
                                consecutiveFailures = 0
                                if (frameIndex % 10 == 0) {  // 10フレームごとにログ出力
                                    Log.d(TAG, "✅ Downloaded: ${animation.animationType} frame $frameIndex")
                                }
                            } else {
                                totalFailed++
                                consecutiveFailures++
                                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                Log.w(TAG, "❌ Failed to download: ${animation.animationType} frame $frameIndex - $errorMessage")
                                
                                // 404エラーが2回連続で発生した場合、実際のフレーム数を検出したと判断
                                if (errorMessage.contains("Object does not exist") && consecutiveFailures >= 2) {
                                    Log.i(TAG, "🔍 Detected actual frame count for ${animation.animationType}: $actualFrameCount frames (instead of ${animation.frameCount})")
                                    
                                    // SharedPreferencesに実際のフレーム数を保存
                                    saveActualFrameCount(config.characterId, animation.animationType, actualFrameCount)
                                    
                                    // 残りのフレームダウンロードをスキップ
                                    Log.i(TAG, "⏭️ Skipping remaining frames for ${animation.animationType} (${frameIndex + 1} to ${animation.frameCount - 1})")
                                    break
                                }
                            }
                            
                            // 進捗通知を更新
                            updateNotification(
                                "画像ダウンロード中",
                                "${animation.animationType} ${frameIndex + 1}/${animation.frameCount}"
                            )
                            
                        } catch (e: Exception) {
                            totalFailed++
                            consecutiveFailures++
                            Log.e(TAG, "❌ Error downloading frame $frameIndex for ${animation.animationType}", e)
                        }
                    }
                    
                    Log.d(TAG, "🏁 Animation ${animation.animationType} completed: ${animation.frameCount} frames processed (actual: $actualFrameCount)")
                }
                
                showNotification(
                    "ダウンロード完了",
                    "成功: $totalDownloaded, 失敗: $totalFailed"
                )
                
                Log.d(TAG, "🎯 Download completed: $totalDownloaded successful, $totalFailed failed")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in downloadCharacterImages", e)
                showNotification("エラー", "画像ダウンロードに失敗しました")
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun downloadAllImages() {
        serviceScope.launch {
            try {
                showNotification("全画像ダウンロード中", "全キャラクターの画像をダウンロードしています...")
                
                val allCharacters = SampleCharacterConfigs.getAllCharacters()
                var totalDownloaded = 0
                var totalFailed = 0
                
                for (character in allCharacters) {
                    Log.d(TAG, "Downloading character: ${character.characterId}")
                    
                    for (animation in character.animations) {
                        for (frameIndex in 0 until animation.frameCount) {
                            try {
                                val imageUrl = ImageUrlGenerator.generateImageUrl(
                                    character,
                                    animation.animationType,
                                    frameIndex
                                )
                                
                                val result = imageCacheRepository.downloadAndCacheImage(
                                    imageUrl,
                                    character.characterId,
                                    animation.animationType,
                                    frameIndex
                                )
                                
                                if (result.isSuccess) {
                                    totalDownloaded++
                                } else {
                                    totalFailed++
                                }
                                
                                // 進捗通知を更新
                                updateNotification(
                                    "全画像ダウンロード中",
                                    "${character.characterId} ${animation.animationType}"
                                )
                                
                            } catch (e: Exception) {
                                totalFailed++
                                Log.e(TAG, "Error downloading frame $frameIndex", e)
                            }
                        }
                    }
                }
                
                showNotification(
                    "全ダウンロード完了",
                    "成功: $totalDownloaded, 失敗: $totalFailed"
                )
                
                Log.d(TAG, "All download completed: $totalDownloaded successful, $totalFailed failed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadAllImages", e)
                showNotification("エラー", "全画像ダウンロードに失敗しました")
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun clearCache() {
        serviceScope.launch {
            try {
                showNotification("キャッシュクリア中", "画像キャッシュをクリアしています...")
                
                imageCacheRepository.clearCache()
                
                showNotification("キャッシュクリア完了", "画像キャッシュをクリアしました")
                
                Log.d(TAG, "Cache cleared successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing cache", e)
                showNotification("エラー", "キャッシュクリアに失敗しました")
            } finally {
                stopSelf()
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "画像ダウンロード",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "画像ダウンロードの進捗"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 実際のフレーム数をSharedPreferencesに保存
     */
    private fun saveActualFrameCount(characterId: String, animationType: String, actualFrameCount: Int) {
        try {
            val prefs = getSharedPreferences("actual_frame_counts", Context.MODE_PRIVATE)
            val key = "${characterId}_${animationType}"
            prefs.edit().putInt(key, actualFrameCount).apply()
            Log.d(TAG, "💾 Saved actual frame count: $key = $actualFrameCount")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving actual frame count", e)
        }
    }
    
    /**
     * 実際のフレーム数をSharedPreferencesから取得
     */
    private fun getActualFrameCount(characterId: String, animationType: String, defaultCount: Int): Int {
        return try {
            val prefs = getSharedPreferences("actual_frame_counts", Context.MODE_PRIVATE)
            val key = "${characterId}_${animationType}"
            val actualCount = prefs.getInt(key, defaultCount)
            Log.d(TAG, "📖 Retrieved actual frame count: $key = $actualCount")
            actualCount
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error retrieving actual frame count", e)
            defaultCount
        }
    }
} 