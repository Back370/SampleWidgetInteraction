package com.seo4d696b75.android.glance_widget_demo.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.seo4d696b75.android.glance_widget_demo.data.WidgetImageFileProvider
import com.seo4d696b75.android.glance_widget_demo.domain.ImageCacheRepository
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