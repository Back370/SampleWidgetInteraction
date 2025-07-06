package com.seo4d696b75.android.glance_widget_demo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import com.seo4d696b75.android.glance_widget_demo.data.WidgetImageFileProvider
import com.seo4d696b75.android.glance_widget_demo.domain.ImageUrlGenerator
import com.seo4d696b75.android.glance_widget_demo.domain.SampleCharacterConfigs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 画像キャッシュを使用する拡張ウィジェット
 * 
 * 使用例：
 * - 外部URLから画像をダウンロード
 * - キャッシュシステムを使用した効率的な画像管理
 * - FileProvider経由での安全な画像アクセス
 */
@AndroidEntryPoint
class EnhancedCounterWidgetProvider : AppWidgetProvider() {
    
    @Inject
    lateinit var widgetImageManager: WidgetImageManager
    
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    companion object {
        private const val TAG = "EnhancedCounterWidget"
        private const val ANIMATION_FRAME_DELAY = 100L
        private const val TOTAL_FRAMES = 10
    }
    
    // アニメーション状態管理
    private val animationStates = mutableMapOf<Int, AnimationState>()
    private val animationHandler = Handler(Looper.getMainLooper())
    
    private data class AnimationState(
        var isAnimating: Boolean = false,
        var currentFrame: Int = 0,
        var animationRunnable: Runnable? = null,
        var characterId: String = "character001",
        var animationType: String = "idle"
    )

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            
            // 画像の事前ダウンロード（非同期）
            preloadImages(context, appWidgetId)
        }
    }
    
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout_wall)
        
        // アニメーション状態を初期化
        if (!animationStates.containsKey(appWidgetId)) {
            animationStates[appWidgetId] = AnimationState()
        }
        
        val animationState = animationStates[appWidgetId]!!
        
        // キャッシュされた画像を設定
        setCachedImage(context, views, animationState)
        
        // ウィジェットを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // アニメーション開始
        if (!animationState.isAnimating) {
            startAnimation(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun setCachedImage(
        context: Context,
        views: RemoteViews,
        animationState: AnimationState
    ) {
        try {
            // キャッシュされた画像のURIを取得
            val uri = WidgetImageFileProvider.getCachedImageUri(
                context,
                animationState.characterId,
                animationState.animationType,
                animationState.currentFrame
            )

            if (uri != null) {
                // キャッシュされた画像を設定
                views.setImageViewUri(R.id.background_image, uri)
                Log.d(
                    TAG,
                    "Set cached image: ${animationState.characterId}_${animationState.animationType}_${animationState.currentFrame}"
                )
            } else {
                // キャッシュされていない場合、フォールバック画像を設定し、ダウンロードを開始
                setFallbackImage(views, animationState.currentFrame)
                Log.d(
                    TAG,
                    "Using fallback image for frame ${animationState.currentFrame}, will attempt download"
                )

                // 非同期でダウンロードを開始
                downloadMissingImage(context, animationState)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set cached image", e)
            setFallbackImage(views, animationState.currentFrame)
        }
    }
    
    private fun startAnimation(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val animationState = animationStates[appWidgetId] ?: return
        animationState.isAnimating = true
        
        val animationRunnable = object : Runnable {
            override fun run() {
                if (animationState.isAnimating) {
                    // 次のフレームに進む
                    animationState.currentFrame = (animationState.currentFrame + 1) % TOTAL_FRAMES
                    
                    // ウィジェットを更新
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    
                    // 次のフレームをスケジュール
                    animationHandler.postDelayed(this, ANIMATION_FRAME_DELAY)
                }
            }
        }
        
        animationState.animationRunnable = animationRunnable
        animationHandler.post(animationRunnable)
    }
    
    private fun preloadImages(context: Context, appWidgetId: Int) {
        val animationState = animationStates[appWidgetId] ?: return
        
        widgetScope.launch {
            try {
                Log.d(TAG, "Starting image preload for widget $appWidgetId")
                
                // 全フレームの画像をダウンロード・キャッシュ
                for (frameIndex in 0 until TOTAL_FRAMES) {
                    val imageUrl = generateImageUrl(
                        animationState.characterId,
                        animationState.animationType,
                        frameIndex
                    )
                    
                    // 既にキャッシュされている場合はスキップ
                    if (widgetImageManager.isCacheExists(
                        animationState.characterId,
                        animationState.animationType,
                        frameIndex
                    )) {
                        Log.d(TAG, "Image already cached: $frameIndex")
                        continue
                    }
                    
                    // 画像をダウンロード・キャッシュ
                    val bitmap = widgetImageManager.downloadAndCacheImage(
                        context,
                        imageUrl,
                        animationState.characterId,
                        animationState.animationType,
                        frameIndex
                    )
                    
                    if (bitmap != null) {
                        Log.d(TAG, "Successfully cached image: $frameIndex")
                    } else {
                        Log.w(TAG, "Failed to cache image: $frameIndex")
                    }
                }
                
                Log.d(TAG, "Image preload completed for widget $appWidgetId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to preload images", e)
            }
        }
    }
    
    private fun generateImageUrl(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): String {
        // SampleCharacterConfigs からキャラクター設定を取得し、ImageUrlGenerator で正しいURLを生成
        val config = SampleCharacterConfigs.getCharacterById(characterId)
            ?: SampleCharacterConfigs.CHARACTER_001 // フォールバック
        return ImageUrlGenerator.generateImageUrl(
            config,
            animationType,
            frameIndex
        )
    }
    
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        
        for (appWidgetId in appWidgetIds) {
            stopAnimation(appWidgetId)
            animationStates.remove(appWidgetId)
        }
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        
        // 全アニメーションを停止
        animationStates.values.forEach { state ->
            state.isAnimating = false
            state.animationRunnable?.let { runnable ->
                animationHandler.removeCallbacks(runnable)
            }
        }
        animationStates.clear()
        
        // メモリキャッシュをクリア
        widgetImageManager.clearMemoryCache()
    }
    
    private fun stopAnimation(appWidgetId: Int) {
        animationStates[appWidgetId]?.let { state ->
            state.isAnimating = false
            state.animationRunnable?.let { runnable ->
                animationHandler.removeCallbacks(runnable)
            }
        }
    }
    
    /**
     * drawableリソースの代わりに使うフォールバック画像設定
     */
    private fun setFallbackImage(views: RemoteViews, frameIndex: Int) {
        // drawableリソースを使用しない - 透明背景のみ使用
        views.setImageViewResource(R.id.background_image, android.R.color.transparent)
        Log.w(TAG, "Using transparent fallback for frame $frameIndex - no drawable resources")
    }

    /**
     * キャッシュされていない画像のダウンロード処理
     */
    private fun downloadMissingImage(context: Context, animationState: AnimationState) {
        widgetScope.launch {
            try {
                val imageUrl = generateImageUrl(
                    animationState.characterId,
                    animationState.animationType,
                    animationState.currentFrame
                )

                val bitmap = widgetImageManager.downloadAndCacheImage(
                    context,
                    imageUrl,
                    animationState.characterId,
                    animationState.animationType,
                    animationState.currentFrame
                )

                if (bitmap != null) {
                    Log.d(
                        TAG,
                        "Successfully downloaded missing image: ${animationState.currentFrame}"
                    )
                    // 画像がダウンロードできた場合、ウィジェットを更新
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        android.content.ComponentName(
                            context,
                            EnhancedCounterWidgetProvider::class.java
                        )
                    )
                    for (id in appWidgetIds) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download missing image", e)
            }
        }
    }

    /**
     * キャッシュ管理機能
     */
    fun clearCache(context: Context) {
        widgetScope.launch {
            widgetImageManager.clearAllCache()
            Log.d(TAG, "All cache cleared")
        }
    }
    
    /**
     * キャラクターを変更
     */
    fun changeCharacter(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newCharacterId: String
    ) {
        animationStates[appWidgetId]?.let { state ->
            state.characterId = newCharacterId
            state.currentFrame = 0

            // 新しいキャラクターの画像をプリロード
            preloadImages(context, appWidgetId)

            // ウィジェットを更新
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
} 