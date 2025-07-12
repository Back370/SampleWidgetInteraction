package com.seo4d696b75.android.glance_widget_demo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.seo4d696b75.android.glance_widget_demo.data.WidgetImageFileProvider
import com.seo4d696b75.android.glance_widget_demo.domain.ImageCacheRepository
import com.example.core.CharacterDisplaySettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetImageManager @Inject constructor(
    private val imageCacheRepository: ImageCacheRepository
) {
    
    companion object {
        private const val TAG = "WidgetImageManager"
        private const val MAX_BITMAP_CACHE_SIZE = 20
    }
    
    // Bitmap キャッシュ（メモリ内）
    private val bitmapCache = mutableMapOf<String, Bitmap>()
    
    /**
     * キャッシュされた画像のBitmapを取得
     * @param context Context
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return Bitmap (キャッシュされていない場合はnull)
     */
    suspend fun getCachedBitmap(
        context: Context,
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = getCacheKey(characterId, animationType, frameIndex)
        
        // メモリキャッシュから確認
        bitmapCache[cacheKey]?.let { return@withContext it }
        
        // ファイルキャッシュから読み込み
        val cachedFile = imageCacheRepository.getCachedImageFile(characterId, animationType, frameIndex)
        cachedFile?.let { file ->
            try {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888 // 透明度サポート
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                bitmap?.let {
                    // メモリキャッシュに保存
                    addToMemoryCache(cacheKey, it)
                    return@withContext it
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode cached image", e)
            }
        }
        
        return@withContext null
    }
    
    /**
     * 設定値を適用してキャッシュされた画像のBitmapを取得
     * @param context Context
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return Bitmap (設定値が適用された画像、キャッシュされていない場合はnull)
     */
    suspend fun getCachedBitmapWithSettings(
        context: Context,
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 設定値を取得
            val displaySettings = CharacterDisplaySettings.getInstance(context)
            val widthScale = displaySettings.getWidthScale()
            val heightScale = displaySettings.getHeightScale()
            val qualityScale = displaySettings.getQualityScale()
            
            // 設定値を含むキャッシュキーを生成
            val settingsKey = getCacheKeyWithSettings(characterId, animationType, frameIndex, widthScale, heightScale, qualityScale)
            
            // 設定値適用済みのキャッシュから確認
            bitmapCache[settingsKey]?.let { return@withContext it }
            
            // 元の画像を取得
            val originalBitmap = getCachedBitmap(context, characterId, animationType, frameIndex)
            originalBitmap?.let { bitmap ->
                // 設定値を適用して画像を変換
                val scaledBitmap = applyDisplaySettings(bitmap, widthScale, heightScale, qualityScale)
                
                // 変換後の画像をキャッシュに保存
                addToMemoryCache(settingsKey, scaledBitmap)
                
                return@withContext scaledBitmap
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cached bitmap with settings", e)
            return@withContext null
        }
    }
    
    /**
     * 設定値を適用してBitmapを変換
     * @param originalBitmap 元の画像
     * @param widthScale 横幅の倍率
     * @param heightScale 縦幅の倍率
     * @param qualityScale 画質の倍率
     * @return 変換後のBitmap
     */
    private fun applyDisplaySettings(
        originalBitmap: Bitmap,
        widthScale: Float,
        heightScale: Float,
        qualityScale: Float
    ): Bitmap {
        try {
            val originalWidth = originalBitmap.width
            val originalHeight = originalBitmap.height
            
            // 新しいサイズを計算
            val newWidth = (originalWidth * widthScale).toInt().coerceAtLeast(1)
            val newHeight = (originalHeight * heightScale).toInt().coerceAtLeast(1)
            
            // 画質設定に基づいてBitmapConfigを決定
            val config = when {
                qualityScale >= 0.8f -> Bitmap.Config.ARGB_8888  // 高画質
                qualityScale >= 0.5f -> Bitmap.Config.ARGB_4444  // 中画質
                else -> Bitmap.Config.RGB_565  // 低画質（透明度なし）
            }
            
            // スケーリング行列を作成
            val matrix = Matrix().apply {
                setScale(widthScale, heightScale)
            }
            
            // 変換後の画像を作成
            val scaledBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, originalWidth, originalHeight, matrix, true
            )
            
            // 画質設定を適用（必要に応じて再作成）
            return if (scaledBitmap.config != config) {
                val qualityBitmap = scaledBitmap.copy(config, false)
                if (scaledBitmap != originalBitmap) {
                    scaledBitmap.recycle()
                }
                qualityBitmap
            } else {
                scaledBitmap
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply display settings", e)
            return originalBitmap
        }
    }
    
    /**
     * 画像をダウンロードしてキャッシュに保存
     * @param context Context
     * @param imageUrl 画像URL
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return Bitmap (成功時)
     */

    suspend fun downloadAndCacheImage(
        context: Context,
        imageUrl: String,
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 既にキャッシュされている場合は返す
            getCachedBitmap(context, characterId, animationType, frameIndex)?.let {
                return@withContext it
            }
            
            // 画像をダウンロード・キャッシュ
            val result = imageCacheRepository.downloadAndCacheImage(
                imageUrl, characterId, animationType, frameIndex
            )
            
            result.getOrNull()?.let { file ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888 // 透明度サポート
                }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                bitmap?.let {
                    val cacheKey = getCacheKey(characterId, animationType, frameIndex)
                    addToMemoryCache(cacheKey, it)
                    return@withContext it
                }
            }
            
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download and cache image", e)
            return@withContext null
        }
    }
    
    /**
     * Glideを使用してImageViewに画像を設定
     * @param context Context
     * @param imageView ImageView
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     */
    fun loadImageIntoView(
        context: Context,
        imageView: Any, // ImageViewまたはRemoteViews
        characterId: String,
        animationType: String,
        frameIndex: Int
    ) {
        val cachedFile = imageCacheRepository.getCachedImageFile(characterId, animationType, frameIndex)
        cachedFile?.let { file ->
            try {
                val uri = WidgetImageFileProvider.getUriForFile(context, file)
                
                // Glideで画像を読み込み
                Glide.with(context)
                    .load(uri)
                    .apply(
                        RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(false)
                    )
                    .into(imageView as android.widget.ImageView)
                    
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load image into view", e)
            }
        }
    }
    
    /**
     * キャッシュされた画像のファイルパスを取得
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return ファイルパス (存在しない場合はnull)
     */
    fun getCachedImagePath(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): String? {
        return imageCacheRepository.getCachedImageFile(characterId, animationType, frameIndex)?.absolutePath
    }
    
    /**
     * キャッシュが存在するかチェック
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return キャッシュが存在する場合はtrue
     */
    fun isCacheExists(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Boolean {
        return imageCacheRepository.isCacheExists(characterId, animationType, frameIndex)
    }
    
    /**
     * メモリキャッシュをクリア
     */
    fun clearMemoryCache() {
        bitmapCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        bitmapCache.clear()
    }
    
    /**
     * キャッシュ全体をクリア
     */
    suspend fun clearAllCache() {
        clearMemoryCache()
        imageCacheRepository.clearCache()
    }
    
    private fun getCacheKey(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): String {
        return "${characterId}_${animationType}_${frameIndex}"
    }

    private fun getCacheKeyWithSettings(
        characterId: String,
        animationType: String,
        frameIndex: Int,
        widthScale: Float,
        heightScale: Float,
        qualityScale: Float
    ): String {
        return "${characterId}_${animationType}_${frameIndex}_${widthScale}_${heightScale}_${qualityScale}"
    }
    
    private fun addToMemoryCache(key: String, bitmap: Bitmap) {
        // メモリキャッシュサイズ制限
        if (bitmapCache.size >= MAX_BITMAP_CACHE_SIZE) {
            val firstKey = bitmapCache.keys.first()
            bitmapCache[firstKey]?.let { 
                if (!it.isRecycled) {
                    it.recycle()
                }
            }
            bitmapCache.remove(firstKey)
        }
        
        bitmapCache[key] = bitmap
    }
} 