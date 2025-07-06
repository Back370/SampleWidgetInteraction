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
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_DOWNLOAD_CHARACTER -> {
                val characterId = intent.getStringExtra(EXTRA_CHARACTER_ID) ?: return
                val animationType = intent.getStringExtra(EXTRA_ANIMATION_TYPE)
                downloadCharacterImages(characterId, animationType)
            }
            ACTION_DOWNLOAD_ALL -> {
                downloadAllImages()
            }
            ACTION_CLEAR_CACHE -> {
                clearCache()
            }
        }
    }
    
    private fun downloadCharacterImages(characterId: String, animationType: String?) {
        serviceScope.launch {
            try {
                showNotification("画像ダウンロード中", "$characterId の画像をダウンロードしています...")
                
                val config = SampleCharacterConfigs.getCharacterById(characterId)
                if (config == null) {
                    Log.e(TAG, "Character config not found: $characterId")
                    stopSelf()
                    return@launch
                }
                
                val animationsToDownload = if (animationType != null) {
                    config.animations.filter { it.animationType == animationType }
                } else {
                    config.animations
                }
                
                var totalDownloaded = 0
                var totalFailed = 0
                
                for (animation in animationsToDownload) {
                    Log.d(TAG, "Downloading animation: ${animation.animationType}")
                    
                    for (frameIndex in 0 until animation.frameCount) {
                        try {
                            val imageUrl = ImageUrlGenerator.generateImageUrl(
                                config,
                                animation.animationType,
                                frameIndex
                            )
                            
                            val result = imageCacheRepository.downloadAndCacheImage(
                                imageUrl,
                                config.characterId,
                                animation.animationType,
                                frameIndex
                            )
                            
                            if (result.isSuccess) {
                                totalDownloaded++
                                Log.d(TAG, "Downloaded: ${animation.animationType} frame $frameIndex")
                            } else {
                                totalFailed++
                                Log.w(TAG, "Failed to download: ${animation.animationType} frame $frameIndex")
                            }
                            
                            // 進捗通知を更新
                            updateNotification(
                                "画像ダウンロード中",
                                "${animation.animationType} ${frameIndex + 1}/${animation.frameCount}"
                            )
                            
                        } catch (e: Exception) {
                            totalFailed++
                            Log.e(TAG, "Error downloading frame $frameIndex", e)
                        }
                    }
                }
                
                showNotification(
                    "ダウンロード完了",
                    "成功: $totalDownloaded, 失敗: $totalFailed"
                )
                
                Log.d(TAG, "Download completed: $totalDownloaded successful, $totalFailed failed")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in downloadCharacterImages", e)
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
} 