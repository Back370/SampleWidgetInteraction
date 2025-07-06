package com.seo4d696b75.android.glance_widget_demo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.widget.RemoteViews
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class CounterWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_WIDGET_UPDATE = "com.seo4d696b75.android.glance_widget_demo.WIDGET_UPDATE"
        const val ACTION_START_ANIMATION = "com.seo4d696b75.android.glance_widget_demo.START_ANIMATION"
        const val ACTION_STOP_ANIMATION = "com.seo4d696b75.android.glance_widget_demo.STOP_ANIMATION"
        
        private const val ANIMATION_INTERVAL_MS = 16L // 60fps - ゲームレベルの滑らかさ
        private const val TOTAL_FRAMES = 50
        
        // ウィジェットに最適化されたサイズ
        private const val WIDGET_IMAGE_SIZE = 120 // dpに基づく最適サイズ
        
        private var isAnimationRunning = false
        private var currentFrame = 0
        private var alarmManager: AlarmManager? = null
        private var animationPendingIntent: PendingIntent? = null
        private var frameUpdateCount = 0
        private var lastFrameTime = 0L
        private var averageFrameTime = 0L
        private var maxFrameTime = 0L
        private var minFrameTime = Long.MAX_VALUE
        
        // 高精度タイマーシステム
        private var scheduledExecutor: ScheduledExecutorService? = null
        private var expectedTriggerTime = 0L
        private var actualTriggerTime = 0L
        private var triggerDelayTotal = 0L
        private var triggerDelayCount = 0
        
        // 自動再開システム
        private var autoRestartEnabled = true
        private var autoRestartExecutor: ScheduledExecutorService? = null
        private var lastSuccessfulFrameTime = 0L
        private var autoRestartCount = 0
        private const val AUTO_RESTART_DELAY_MS = 3000L // 3秒後に自動再開
        private const val MAX_AUTO_RESTART_COUNT = 10 // 最大再開回数
        
        private val imageFilePaths = mutableListOf<String>()
        private val optimizedImageCache = mutableListOf<Bitmap>()
        private var isCacheReady = false
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.v("WidgetProvider", "🎯 onReceive: ${intent.action}")
        
        when (intent.action) {
            ACTION_WIDGET_UPDATE -> {
                android.util.Log.d("WidgetProvider", "📡 AlarmManager triggered update")
                handleAlarmManagerUpdate(context)
            }
            ACTION_START_ANIMATION -> {
                android.util.Log.d("WidgetProvider", "▶️ Start animation command received")
                startAlarmBasedAnimation(context)
            }
            ACTION_STOP_ANIMATION -> {
                android.util.Log.d("WidgetProvider", "⏹️ Stop animation command received")
                stopAlarmBasedAnimation(context)
            }
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        android.util.Log.d("WidgetProvider", "🚀 Widget enabled - Starting AlarmManager animation")
        initializeImagePaths(context)
        startAlarmBasedAnimation(context)
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        android.util.Log.d("WidgetProvider", "🛑 Widget disabled - Stopping AlarmManager animation")
        stopAlarmBasedAnimation(context)
        cleanupImageCache()
    }
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("WidgetProvider", "🔄 onUpdate called for ${appWidgetIds.size} widgets")
        
        // 画像パスの初期化
        if (imageFilePaths.isEmpty()) {
            initializeImagePaths(context)
        }
        
        // すべてのウィジェットを更新
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
        
        // アニメーションが停止している場合は開始
        if (!isAnimationRunning && imageFilePaths.size >= TOTAL_FRAMES) {
            android.util.Log.d("WidgetProvider", "🔄 Restarting animation from onUpdate")
            startAlarmBasedAnimation(context)
        }
    }
    
    private fun startAlarmBasedAnimation(context: Context) {
        android.util.Log.d("WidgetProvider", "🎬 === STARTING HIGH-PRECISION ANIMATION ===")
        
        try {
            // 既存のアニメーションを停止
            stopAlarmBasedAnimation(context)
            
            // 高精度タイマーシステムの初期化
            scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
            
            // アニメーション状態の初期化
            isAnimationRunning = true
            currentFrame = 0
            frameUpdateCount = 0
            lastFrameTime = System.currentTimeMillis()
            expectedTriggerTime = System.currentTimeMillis() + ANIMATION_INTERVAL_MS
            triggerDelayTotal = 0L
            triggerDelayCount = 0
            
            // 自動再開システムの初期化
            lastSuccessfulFrameTime = System.currentTimeMillis()
            autoRestartCount = 0
            startAutoRestartMonitoring(context)
            
            // AlarmManagerをバックアップとして初期化
            initializeAlarmManagerBackup(context)
            
            // 高精度タイマーでアニメーション開始
            scheduleHighPrecisionFrame(context)
            
            android.util.Log.d("WidgetProvider", "✅ High-precision animation started successfully")
            android.util.Log.d("WidgetProvider", "  - Frame interval: ${ANIMATION_INTERVAL_MS}ms")
            android.util.Log.d("WidgetProvider", "  - Total frames: $TOTAL_FRAMES")
            android.util.Log.d("WidgetProvider", "  - Available images: ${imageFilePaths.size}")
            android.util.Log.d("WidgetProvider", "  - Using ScheduledExecutorService for precision")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to start high-precision animation", e)
            isAnimationRunning = false
        }
    }
    
    private fun stopAlarmBasedAnimation(context: Context) {
        android.util.Log.d("WidgetProvider", "⏹️ Stopping high-precision animation")
        
        try {
            // 高精度タイマーを停止
            scheduledExecutor?.shutdown()
            scheduledExecutor = null
            
            // 自動再開システムを停止
            stopAutoRestartMonitoring()
            
            // AlarmManagerバックアップを停止
            animationPendingIntent?.let { pendingIntent ->
                alarmManager?.cancel(pendingIntent)
                pendingIntent.cancel()
                android.util.Log.d("WidgetProvider", "✅ AlarmManager backup stopped")
            }
            
            isAnimationRunning = false
            animationPendingIntent = null
            
            android.util.Log.d("WidgetProvider", "✅ High-precision animation stopped")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error stopping high-precision animation", e)
        }
    }
    
    private fun scheduleHighPrecisionFrame(context: Context) {
        try {
            if (!isAnimationRunning) {
                android.util.Log.v("WidgetProvider", "⚠️ Animation stopped, not scheduling next frame")
                return
            }
            
            scheduledExecutor?.schedule({
                // 実際のトリガー時間を記録
                actualTriggerTime = System.currentTimeMillis()
                val delay = actualTriggerTime - expectedTriggerTime
                
                if (delay > 0) {
                    triggerDelayTotal += delay
                    triggerDelayCount++
                    android.util.Log.v("WidgetProvider", "⏱️ Trigger delay: ${delay}ms")
                }
                
                // フレーム更新を実行
                handleHighPrecisionUpdate(context)
                
                // 次のフレームの期待時間を設定
                expectedTriggerTime = System.currentTimeMillis() + ANIMATION_INTERVAL_MS
                
                // 次のフレームをスケジュール
                scheduleHighPrecisionFrame(context)
                
            }, ANIMATION_INTERVAL_MS, TimeUnit.MILLISECONDS)
            
            android.util.Log.v("WidgetProvider", "⚡ High-precision frame scheduled in ${ANIMATION_INTERVAL_MS}ms")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to schedule high-precision frame", e)
            // フォールバックでAlarmManagerを使用
            fallbackToAlarmManager(context)
        }
    }
    
    private fun initializeAlarmManagerBackup(context: Context) {
        try {
            // AlarmManagerをバックアップとして初期化
            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            val intent = Intent(context, CounterWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_UPDATE
            }
            
            animationPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            android.util.Log.d("WidgetProvider", "✅ AlarmManager backup initialized")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to initialize AlarmManager backup", e)
        }
    }
    
    private fun fallbackToAlarmManager(context: Context) {
        android.util.Log.w("WidgetProvider", "⚠️ Falling back to AlarmManager due to ScheduledExecutor failure")
        
        try {
            val nextTriggerTime = SystemClock.elapsedRealtime() + ANIMATION_INTERVAL_MS
            val pendingIntent = animationPendingIntent
            
            if (pendingIntent != null && alarmManager != null) {
                val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager!!.canScheduleExactAlarms()
                } else {
                    true
                }
                
                if (canScheduleExactAlarms) {
                    alarmManager!!.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                    android.util.Log.v("WidgetProvider", "⚡ AlarmManager fallback scheduled (exact)")
                } else {
                    alarmManager!!.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                    android.util.Log.v("WidgetProvider", "⚡ AlarmManager fallback scheduled (inexact)")
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ AlarmManager fallback failed", e)
            isAnimationRunning = false
        }
    }
    
    private fun handlePermissionError(context: Context) {
        android.util.Log.w("WidgetProvider", "⚠️ Exact alarm permission not available, stopping animation")
        isAnimationRunning = false
        
        // ユーザーに権限の必要性を通知するためのトーストやノーティフィケーションを表示できる
        // ここでは簡単にアニメーションを停止するだけにしている
        
        // 必要に応じて、設定画面に誘導するインテントを作成することもできる
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.data = Uri.parse("package:${context.packageName}")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
        */
    }
    
    private fun handleAlarmManagerUpdate(context: Context) {
        android.util.Log.v("WidgetProvider", "🔄 === AlarmManager Backup Update Frame $currentFrame ===")
        handleHighPrecisionUpdate(context)
        
        // AlarmManagerバックアップの場合は次のフレームをスケジュール
        fallbackToAlarmManager(context)
    }
    
    private fun handleHighPrecisionUpdate(context: Context) {
        val startTime = System.currentTimeMillis()
        
        try {
            android.util.Log.v("WidgetProvider", "⚡ === High-Precision Update Frame $currentFrame ===")
            
            // WakeLockを取得して確実な実行を保証
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WidgetProvider:HighPrecisionUpdate"
            )
            
            wakeLock.acquire(5000) // 最大5秒間
            
            try {
                if (!isAnimationRunning) {
                    android.util.Log.w("WidgetProvider", "⚠️ Animation stopped, skipping update")
                    return
                }
                
                // 画像の確認
                if (imageFilePaths.isEmpty()) {
                    android.util.Log.w("WidgetProvider", "⚠️ No images available, reinitializing")
                    initializeImagePaths(context)
                    if (imageFilePaths.isEmpty()) {
                        android.util.Log.e("WidgetProvider", "❌ Still no images after reinitialization")
                        stopAlarmBasedAnimation(context)
                        return
                    }
                }
                
                // ウィジェットの更新
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, CounterWidgetProvider::class.java)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (widgetIds.isNotEmpty()) {
                    updateWidgetsWithCurrentFrame(context, appWidgetManager, widgetIds)
                    
                    // フレームカウンターの更新
                    currentFrame = (currentFrame + 1) % TOTAL_FRAMES
                    frameUpdateCount++
                    lastFrameTime = System.currentTimeMillis()
                    
                    // 自動再開システム用の成功時間更新
                    lastSuccessfulFrameTime = System.currentTimeMillis()
                    
                    // 超高速アニメーション用パフォーマンス監視
                    val frameTime = System.currentTimeMillis() - startTime
                    updatePerformanceMetrics(frameTime)
                    
                    // 遅延統計の更新
                    if (triggerDelayCount > 0) {
                        val avgDelay = triggerDelayTotal / triggerDelayCount
                        android.util.Log.v("WidgetProvider", "⚡ Frame completed in ${frameTime}ms (avg: ${averageFrameTime}ms, delay: ${avgDelay}ms)")
                    } else {
                        android.util.Log.v("WidgetProvider", "⚡ Frame completed in ${frameTime}ms (avg: ${averageFrameTime}ms)")
                    }
                } else {
                    android.util.Log.w("WidgetProvider", "⚠️ No active widgets, stopping animation")
                    stopAlarmBasedAnimation(context)
                }
                
            } finally {
                wakeLock.release()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Critical error in high-precision update", e)
            stopAlarmBasedAnimation(context)
        }
    }
    
    private fun updateWidgetsWithCurrentFrame(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        try {
            // キャッシュの確認
            if (!isCacheReady) {
                android.util.Log.w("WidgetProvider", "⚠️ Image cache not ready, building cache...")
                buildImageCache(context)
                if (!isCacheReady) {
                    android.util.Log.e("WidgetProvider", "❌ Failed to build image cache")
                    return
                }
            }
            
            if (currentFrame >= optimizedImageCache.size) {
                android.util.Log.w("WidgetProvider", "⚠️ Frame index out of bounds: $currentFrame >= ${optimizedImageCache.size}")
                return
            }
            
            android.util.Log.v("WidgetProvider", "📸 Using cached frame $currentFrame")
            
            val bitmap = optimizedImageCache[currentFrame]
            if (bitmap.isRecycled) {
                android.util.Log.w("WidgetProvider", "⚠️ Bitmap is recycled, rebuilding cache")
                buildImageCache(context)
                if (currentFrame >= optimizedImageCache.size) return
            }
            
            android.util.Log.v("WidgetProvider", "✅ Using optimized image: ${bitmap.width}x${bitmap.height}")
            
            // すべてのウィジェットを更新
            widgetIds.forEach { widgetId ->
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                remoteViews.setImageViewBitmap(R.id.widget_image, bitmap)
                
                // アニメーション制御ボタンの設定
                setupButtons(context, remoteViews)
                
                appWidgetManager.updateAppWidget(widgetId, remoteViews)
            }
            
            android.util.Log.v("WidgetProvider", "✅ Updated ${widgetIds.size} widgets with cached frame $currentFrame")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error updating widgets with frame", e)
        }
    }
    
    private fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            android.util.Log.d("WidgetProvider", "🔄 Updating ${appWidgetIds.size} widgets")
            
            // キャッシュの確認
            if (!isCacheReady) {
                buildImageCache(context)
            }
            
            appWidgetIds.forEach { widgetId ->
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout)
                
                // キャッシュされた画像を使用
                if (isCacheReady && optimizedImageCache.isNotEmpty() && currentFrame < optimizedImageCache.size) {
                    val bitmap = optimizedImageCache[currentFrame]
                    if (!bitmap.isRecycled) {
                        remoteViews.setImageViewBitmap(R.id.widget_image, bitmap)
                    }
                }
                
                // ボタンの設定
                setupButtons(context, remoteViews)
                
                appWidgetManager.updateAppWidget(widgetId, remoteViews)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error updating widgets", e)
        }
    }
    
    private fun setupButtons(context: Context, remoteViews: RemoteViews) {
        try {
            // 開始ボタン
            val startIntent = Intent(context, CounterWidgetProvider::class.java).apply {
                action = ACTION_START_ANIMATION
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context, 1, startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.button_start, startPendingIntent)
            
            // 停止ボタン
            val stopIntent = Intent(context, CounterWidgetProvider::class.java).apply {
                action = ACTION_STOP_ANIMATION
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context, 2, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.button_stop, stopPendingIntent)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error setting up buttons", e)
        }
    }
    
    private fun initializeImagePaths(context: Context) {
        android.util.Log.d("WidgetProvider", "🔍 Initializing image paths")
        
        try {
            imageFilePaths.clear()
            
            val externalFilesDir = context.getExternalFilesDir("WidgetImages")
            if (externalFilesDir?.exists() == true) {
                val files = externalFilesDir.listFiles { file ->
                    file.isFile && file.name.endsWith(".png", ignoreCase = true)
                }
                
                files?.sortedBy { it.name }?.forEach { file ->
                    imageFilePaths.add(file.absolutePath)
                }
                
                android.util.Log.d("WidgetProvider", "✅ Found ${imageFilePaths.size} image files")
                
                // 画像パスが見つかったら、最適化キャッシュを構築
                if (imageFilePaths.isNotEmpty()) {
                    buildImageCache(context)
                }
            } else {
                android.util.Log.w("WidgetProvider", "⚠️ WidgetImages directory not found")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error initializing image paths", e)
        }
    }
    
    private fun buildImageCache(context: Context) {
        android.util.Log.d("WidgetProvider", "🏗️ Building optimized image cache...")
        val startTime = System.currentTimeMillis()
        
        try {
            // 既存のキャッシュをクリア
            optimizedImageCache.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            optimizedImageCache.clear()
            isCacheReady = false
            
            if (imageFilePaths.isEmpty()) {
                android.util.Log.w("WidgetProvider", "⚠️ No image paths available for caching")
                return
            }
            
            // 密度に基づいた最適サイズの計算
            val displayMetrics = context.resources.displayMetrics
            val scaledSize = (WIDGET_IMAGE_SIZE * displayMetrics.density).toInt()
            
            android.util.Log.d("WidgetProvider", "📏 Optimizing images to ${scaledSize}x${scaledSize} pixels")
            
            imageFilePaths.forEachIndexed { index, imagePath ->
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    val optimizedBitmap = createScaledBitmap(imagePath, scaledSize, scaledSize)
                    if (optimizedBitmap != null) {
                        optimizedImageCache.add(optimizedBitmap)
                        android.util.Log.v("WidgetProvider", "✅ Cached image $index: ${optimizedBitmap.width}x${optimizedBitmap.height}")
                    } else {
                        android.util.Log.w("WidgetProvider", "⚠️ Failed to create scaled bitmap for: $imagePath")
                    }
                } else {
                    android.util.Log.w("WidgetProvider", "⚠️ Image file not found: $imagePath")
                }
            }
            
            isCacheReady = optimizedImageCache.isNotEmpty()
            val duration = System.currentTimeMillis() - startTime
            android.util.Log.d("WidgetProvider", "✅ Image cache built: ${optimizedImageCache.size} images in ${duration}ms")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error building image cache", e)
            isCacheReady = false
        }
    }
    
    private fun createScaledBitmap(imagePath: String, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            // 元の画像サイズを確認
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imagePath, options)
            
            // サンプルサイズを計算
            val sampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            
            // 最適化された画像を読み込み
            val loadOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // メモリ効率を改善
            }
            
            val bitmap = BitmapFactory.decodeFile(imagePath, loadOptions)
            if (bitmap != null) {
                // 正確なサイズにスケール
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                    if (it != bitmap) {
                        bitmap.recycle()
                    }
                }
            } else {
                null
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error creating scaled bitmap", e)
            null
        }
    }
    
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    private fun updatePerformanceMetrics(frameTime: Long) {
        // 平均フレーム時間を計算
        averageFrameTime = if (frameUpdateCount == 0) {
            frameTime
        } else {
            (averageFrameTime + frameTime) / 2
        }
        
        // 最大・最小フレーム時間を更新
        maxFrameTime = maxOf(maxFrameTime, frameTime)
        minFrameTime = minOf(minFrameTime, frameTime)
        
        // 100フレームごとに詳細統計をログ出力
        if (frameUpdateCount % 100 == 0) {
            val avgDelay = if (triggerDelayCount > 0) triggerDelayTotal / triggerDelayCount else 0
            val maxDelay = if (triggerDelayCount > 0) "Max delay: ${maxFrameTime}ms" else "No delays"
            
            android.util.Log.i("WidgetProvider", "📊 High-Precision Performance Stats:")
            android.util.Log.i("WidgetProvider", "  - Frame time: Avg ${averageFrameTime}ms, Max ${maxFrameTime}ms, Min ${minFrameTime}ms")
            android.util.Log.i("WidgetProvider", "  - Trigger delay: Avg ${avgDelay}ms, Count ${triggerDelayCount}/${frameUpdateCount}")
            android.util.Log.i("WidgetProvider", "  - Precision: ${(frameUpdateCount - triggerDelayCount) * 100 / frameUpdateCount}% on-time")
            
            // 遅延統計をリセット
            triggerDelayTotal = 0L
            triggerDelayCount = 0
        }
    }
    
    private fun logFinalStatistics() {
        if (frameUpdateCount > 0) {
            val avgDelay = if (triggerDelayCount > 0) triggerDelayTotal / triggerDelayCount else 0
            val precisionPercent = (frameUpdateCount - triggerDelayCount) * 100 / frameUpdateCount
            
            android.util.Log.i("WidgetProvider", "📊 Final High-Precision Animation Statistics:")
            android.util.Log.i("WidgetProvider", "  - Total frames: $frameUpdateCount")
            android.util.Log.i("WidgetProvider", "  - Average frame time: ${averageFrameTime}ms")
            android.util.Log.i("WidgetProvider", "  - Frame time range: ${minFrameTime}ms - ${maxFrameTime}ms")
            android.util.Log.i("WidgetProvider", "  - Average trigger delay: ${avgDelay}ms")
            android.util.Log.i("WidgetProvider", "  - Precision rate: ${precisionPercent}% on-time")
            android.util.Log.i("WidgetProvider", "  - Target interval: ${ANIMATION_INTERVAL_MS}ms (${1000/ANIMATION_INTERVAL_MS}fps)")
        }
    }
    
    private fun cleanupImageCache() {
        android.util.Log.d("WidgetProvider", "🧹 Cleaning up image cache")
        
        try {
            // 統計情報を出力
            logFinalStatistics()
            
            optimizedImageCache.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            optimizedImageCache.clear()
            isCacheReady = false
            
            android.util.Log.d("WidgetProvider", "✅ Image cache cleaned up")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error cleaning up image cache", e)
        }
    }
}