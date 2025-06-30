package com.seo4d696b75.android.glance_widget_demo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews

class CounterWidgetProvider : AppWidgetProvider() {
    
    // アニメーション用のHandler
    private val animationHandler = Handler(Looper.getMainLooper())
    
    // 画像キャッシュ
    private val imageCache = mutableMapOf<Int, Bitmap>()
    
    // アニメーション状態管理
    private val animationStates = mutableMapOf<Int, AnimationState>()
    
    // 画像リソースIDのキャッシュ
    private val imageResourceIds = mutableListOf<Int>()

    companion object {
        // アニメーション設定
        private const val ANIMATION_FRAME_DELAY = 80L // 80ms間隔（より滑らかなアニメーション）
        private const val TOTAL_FRAMES = 40 // 40枚の画像
        private const val MAX_CACHE_SIZE = 20 // キャッシュする画像の最大数
    }
    
    // アニメーション状態を管理するデータクラス
    private data class AnimationState(
        var isAnimating: Boolean = false,
        var currentFrame: Int = 0,
        var animationRunnable: Runnable? = null,
        var frameDelay: Long = ANIMATION_FRAME_DELAY
    )

    //AppWidgetProviderクラスのouUpdate関数をオーバーライド
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        // 初回実行時に画像リソースIDを初期化
        if (imageResourceIds.isEmpty()) {
            initializeImageResourceIds()
        }

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun initializeImageResourceIds() {
        // 画像リソースIDを事前に計算してキャッシュ
        for (i in 0 until TOTAL_FRAMES) {
            val resourceId = getImageResourceId(i)
            imageResourceIds.add(resourceId)
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
        
        // 現在のフレームの画像を設定
        val currentFrame = animationStates[appWidgetId]?.currentFrame ?: 0
        val imageResId = imageResourceIds.getOrNull(currentFrame) ?: imageResourceIds[0]
        views.setImageViewResource(R.id.background_image, imageResId)

        // ウィジェットを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
        
        // アニメーションがまだ開始されていない場合は開始
        if (!(animationStates[appWidgetId]?.isAnimating ?: false)) {
            startAnimation(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun startAnimation(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val animationState = animationStates[appWidgetId] ?: AnimationState()
        animationState.isAnimating = true
        
        val animationRunnable = object : Runnable {
            override fun run() {
                if (animationState.isAnimating) {
                    // 次のフレームに進む
                    animationState.currentFrame = (animationState.currentFrame + 1) % TOTAL_FRAMES
                    
                    // ウィジェットを更新
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                    
                    // 次のフレームをスケジュール（動的な遅延時間を使用）
                    animationHandler.postDelayed(this, animationState.frameDelay)
                }
            }
        }
        
        animationState.animationRunnable = animationRunnable
        animationStates[appWidgetId] = animationState
        
        // アニメーション開始
        animationHandler.post(animationRunnable)
    }
    
    private fun stopAnimation(appWidgetId: Int) {
        val animationState = animationStates[appWidgetId]
        animationState?.let {
            it.isAnimating = false
            it.animationRunnable?.let { runnable ->
                animationHandler.removeCallbacks(runnable)
            }
        }
    }
    
    // アニメーション速度を調整する関数
    private fun setAnimationSpeed(appWidgetId: Int, frameDelay: Long) {
        val animationState = animationStates[appWidgetId]
        animationState?.let {
            it.frameDelay = frameDelay
        }
    }
    
    // メモリ使用量を最適化する関数
    private fun optimizeMemoryUsage() {
        if (imageCache.size > MAX_CACHE_SIZE) {
            // 古いキャッシュエントリを削除
            val entriesToRemove = imageCache.entries.take(imageCache.size - MAX_CACHE_SIZE)
            entriesToRemove.forEach { (key, bitmap) ->
                bitmap.recycle()
                imageCache.remove(key)
            }
        }
    }
    
    private fun getImageResourceId(frame: Int): Int {
        // フレーム番号に基づいて画像リソースIDを返す
        return when (frame) {
            0 -> R.drawable.testanims_001
            1 -> R.drawable.testanims_002
            2 -> R.drawable.testanims_003
            3 -> R.drawable.testanims_004
            4 -> R.drawable.testanims_005
            5 -> R.drawable.testanims_006
            6 -> R.drawable.testanims_007
            7 -> R.drawable.testanims_008
            8 -> R.drawable.testanims_009
            9 -> R.drawable.testanims_010
            10 -> R.drawable.testanims_011
            11 -> R.drawable.testanims_012
            12 -> R.drawable.testanims_013
            13 -> R.drawable.testanims_014
            14 -> R.drawable.testanims_015
            15 -> R.drawable.testanims_016
            16 -> R.drawable.testanims_017
            17 -> R.drawable.testanims_018
            18 -> R.drawable.testanims_019
            19 -> R.drawable.testanims_020
            20 -> R.drawable.testanims_021
            21 -> R.drawable.testanims_022
            22 -> R.drawable.testanims_023
            23 -> R.drawable.testanims_024
            24 -> R.drawable.testanims_025
            25 -> R.drawable.testanims_026
            26 -> R.drawable.testanims_027
            27 -> R.drawable.testanims_028
            28 -> R.drawable.testanims_029
            29 -> R.drawable.testanims_030
            30 -> R.drawable.testanims_031
            31 -> R.drawable.testanims_032
            32 -> R.drawable.testanims_033
            33 -> R.drawable.testanims_034
            34 -> R.drawable.testanims_035
            35 -> R.drawable.testanims_036
            36 -> R.drawable.testanims_037
            37 -> R.drawable.testanims_038
            38 -> R.drawable.testanims_039
            39 -> R.drawable.testanims_040
            else -> R.drawable.testanims_001
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        // ウィジェットが削除されたときにアニメーションを停止
        for (appWidgetId in appWidgetIds) {
            stopAnimation(appWidgetId)
            animationStates.remove(appWidgetId)
        }
        
        // メモリ最適化
        optimizeMemoryUsage()
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // すべてのアニメーションを停止
        animationStates.values.forEach { state ->
            state.isAnimating = false
            state.animationRunnable?.let { runnable ->
                animationHandler.removeCallbacks(runnable)
            }
        }
        animationStates.clear()
        
        // 画像キャッシュをクリア
        imageCache.values.forEach { bitmap ->
            bitmap.recycle()
        }
        imageCache.clear()
        imageResourceIds.clear()
    }
}