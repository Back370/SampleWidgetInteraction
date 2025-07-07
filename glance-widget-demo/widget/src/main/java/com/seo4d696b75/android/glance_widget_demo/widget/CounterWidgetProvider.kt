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
import android.os.PowerManager
import android.os.SystemClock
import android.widget.RemoteViews
import java.io.File

class CounterWidgetProvider : AppWidgetProvider() {
    
    companion object {
        const val ACTION_WIDGET_UPDATE = "com.seo4d696b75.android.glance_widget_demo.WIDGET_UPDATE"
        const val ACTION_START_ANIMATION = "com.seo4d696b75.android.glance_widget_demo.START_ANIMATION"
        const val ACTION_TOGGLE_ANIMATION = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
//        const val ACTION_STOP_ANIMATION = "com.seo4d696b75.android.glance_widget_demo.STOP_ANIMATION"
//        const val ACTION_AUTO_RESTART_CHECK = "com.seo4d696b75.android.glance_widget_demo.AUTO_RESTART_CHECK"
        
        private const val ANIMATION_INTERVAL_MS = 500L // 2fps - AlarmManagerで実現可能な間隔
        private const val DEFAULT_FRAMES = 50 // デフォルトフレーム数（実際のフレーム数が不明な場合）
        
        // ウィジェットに最適化されたサイズ
        private const val WIDGET_IMAGE_SIZE = 120 // dpに基づく最適サイズ
        
        private var isAnimationRunning = false
        private var currentFrame = 0
        private var currentTotalFrames = DEFAULT_FRAMES // 動的に決定される実際のフレーム数
        private var alarmManager: AlarmManager? = null
        private var animationPendingIntent: PendingIntent? = null
        private var frameUpdateCount = 0
        private var lastFrameTime = 0L
        private var averageFrameTime = 0L
        private var maxFrameTime = 0L
        private var minFrameTime = Long.MAX_VALUE
        
        // 2fpsタイマーシステム（AlarmManagerベース）
        private var expectedTriggerTime = 0L
        private var actualTriggerTime = 0L
        private var triggerDelayTotal = 0L
        private var triggerDelayCount = 0
        private var lastFrameScheduleTime = 0L
        
        // 自動再開システム (AlarmManagerベース)
        private var autoRestartEnabled = true
        private var autoRestartPendingIntent: PendingIntent? = null
        private var lastSuccessfulFrameTime = 0L
        private var autoRestartCount = 0
        private const val AUTO_RESTART_DELAY_MS = 1000L // 1秒間隔で監視（2fpsアニメーションに適応）
        private const val MAX_AUTO_RESTART_COUNT = 10 // 最大再開回数
        
        private val imageFilePaths = mutableListOf<String>()
        private val optimizedImageCache = mutableListOf<Bitmap>()
        private var isCacheReady = false
        
        /**
         * 実際のフレーム数を取得
         */
        private fun getActualFrameCount(context: Context): Int {
            val animationStateManager = AnimationStateManager.getInstance(context)
            val currentCharacterId = animationStateManager.getCurrentCharacterId()
            val currentAnimationType = animationStateManager.getCurrentAnimationType()
            
            return try {
                val actualFrameCount = com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService.getActualFrameCount(
                    context, currentCharacterId, currentAnimationType, DEFAULT_FRAMES
                )
                android.util.Log.d("WidgetProvider", "📊 Actual frame count for $currentCharacterId/$currentAnimationType: $actualFrameCount")
                actualFrameCount
            } catch (e: Exception) {
                android.util.Log.e("WidgetProvider", "❌ Error getting actual frame count", e)
                DEFAULT_FRAMES
            }
        }
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
                android.util.Log.d("WidgetProvider", "▶️ Start animation command received (manual)")
                // フォアグラウンドServiceでアニメーション開始
                startForegroundAnimation(context)
            }
            ACTION_TOGGLE_ANIMATION -> {
                android.util.Log.d("WidgetProvider", "🔄 Toggle animation command received")
                handleToggleAnimation(context)
            }
//            ACTION_STOP_ANIMATION -> {
//                android.util.Log.d("WidgetProvider", "⏹️ Stop animation command received")
//                // フォアグラウンドServiceでアニメーション停止
//                stopForegroundAnimation(context)
//            }
//            ACTION_AUTO_RESTART_CHECK -> {
//                android.util.Log.d("WidgetProvider", "🔍 Auto-restart check triggered")
//                handleAutoRestartCheck(context)
//            }
        }
    }
    
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        android.util.Log.d("WidgetProvider", "🚀 Widget enabled - Starting foreground 5fps animation (400x500 resolution)")
        startForegroundAnimation(context)
    }
    
//    override fun onDisabled(context: Context) {
//        super.onDisabled(context)
//        android.util.Log.d("WidgetProvider", "🛑 Widget disabled - Stopping foreground animation")
////        stopForegroundAnimation(context)
//    }
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        android.util.Log.d("WidgetProvider", "🔄 onUpdate called for ${appWidgetIds.size} widgets")
        
        // すべてのウィジェットを更新
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
        
        // フォアグラウンドアニメーションを開始
        android.util.Log.d("WidgetProvider", "🔄 Starting foreground animation from onUpdate")
        startForegroundAnimation(context)
    }
    
    private fun startForegroundAnimation(context: Context) {
        try {
            android.util.Log.d("WidgetProvider", "🚀 Starting foreground animation service")
            
            val serviceIntent = Intent(context, WidgetAnimationService::class.java).apply {
                action = "START_ANIMATION"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            android.util.Log.d("WidgetProvider", "✅ Foreground animation service started")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to start foreground animation service", e)
        }
    }
    
//    private fun stopForegroundAnimation(context: Context) {
//        try {
//            android.util.Log.d("WidgetProvider", "🛑 Stopping foreground animation service")
//
//            val serviceIntent = Intent(context, WidgetAnimationService::class.java).apply {
//                action = "STOP_ANIMATION"
//            }
//            context.startService(serviceIntent)
//
//            android.util.Log.d("WidgetProvider", "✅ Foreground animation service stop requested")
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Failed to stop foreground animation service", e)
//        }
//    }
    
//    private fun startAlarmBasedAnimation(context: Context, resetAutoRestartCounter: Boolean = false) {
//        android.util.Log.d("WidgetProvider", "🎬 === STARTING 2FPS ANIMATION ===")
//
//        try {
//            // 既存のアニメーションを停止
//            stopAlarmBasedAnimation(context)
//
//            // AlarmManagerのみを使用（バックグラウンド制約完全回避）
//            alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//            // アニメーション状態の初期化
//            isAnimationRunning = true
//            currentFrame = 0
//            frameUpdateCount = 0
//            lastFrameTime = System.currentTimeMillis()
//            expectedTriggerTime = System.currentTimeMillis() + ANIMATION_INTERVAL_MS
//            triggerDelayTotal = 0L
//            triggerDelayCount = 0
//            lastFrameScheduleTime = System.currentTimeMillis()
//
//            // 自動再開システムの初期化
//            lastSuccessfulFrameTime = System.currentTimeMillis()
//            if (resetAutoRestartCounter) {
//                autoRestartCount = 0
//                autoRestartEnabled = true
//                android.util.Log.d("WidgetProvider", "🔄 Auto-restart counter reset")
//            }
//            startAutoRestartMonitoring(context)
//
//            // AlarmManagerメインシステムの初期化
//            initializeAlarmManagerMain(context)
//
//            // AlarmManagerでアニメーション開始
//            scheduleAlarmManagerFrame(context)
//
//            android.util.Log.d("WidgetProvider", "✅ AlarmManager-only animation started successfully")
//            android.util.Log.d("WidgetProvider", "  - Frame interval: ${ANIMATION_INTERVAL_MS}ms (2fps)")
//            android.util.Log.d("WidgetProvider", "  - Total frames: $DEFAULT_FRAMES")
//            android.util.Log.d("WidgetProvider", "  - Available images: ${imageFilePaths.size}")
//            android.util.Log.d("WidgetProvider", "  - Primary: AlarmManager (background-constraint-free)")
//            android.util.Log.d("WidgetProvider", "  - Auto-restart: ${if (autoRestartEnabled) "ENABLED" else "DISABLED"} (${AUTO_RESTART_DELAY_MS}ms)")
//            android.util.Log.d("WidgetProvider", "  - Max restarts: ${MAX_AUTO_RESTART_COUNT}, Current: $autoRestartCount")
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Failed to start AlarmManager-only animation", e)
//            isAnimationRunning = false
//        }
//    }
    
    private fun stopAlarmBasedAnimation(context: Context) {
        android.util.Log.d("WidgetProvider", "⏹️ Stopping AlarmManager-only animation")

        try {
            // 自動再開システムを停止
            //stopAutoRestartMonitoring()

            // AlarmManagerメインシステムを停止
            animationPendingIntent?.let { pendingIntent ->
                alarmManager?.cancel(pendingIntent)
                pendingIntent.cancel()
                android.util.Log.d("WidgetProvider", "✅ AlarmManager main system stopped")
            }

            isAnimationRunning = false
            animationPendingIntent = null

            android.util.Log.d("WidgetProvider", "✅ AlarmManager-only animation stopped")

        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error stopping AlarmManager-only animation", e)
        }
    }
    
    private fun scheduleAlarmManagerFrame(context: Context) {
        try {
            if (!isAnimationRunning) {
                android.util.Log.v("WidgetProvider", "⚠️ Animation stopped, not scheduling next frame")
                return
            }
            
            lastFrameScheduleTime = System.currentTimeMillis()
            expectedTriggerTime = System.currentTimeMillis() + ANIMATION_INTERVAL_MS
            
            val nextTriggerTime = SystemClock.elapsedRealtime() + ANIMATION_INTERVAL_MS
            val pendingIntent = animationPendingIntent
            
            if (pendingIntent != null && alarmManager != null) {
                // Android 12+ での権限チェック
                val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager!!.canScheduleExactAlarms()
                } else {
                    true
                }
                
                if (canScheduleExactAlarms) {
                    // 最高精度のアラーム（バッテリー最適化を無視）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager!!.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            nextTriggerTime,
                            pendingIntent
                        )
                        android.util.Log.v("WidgetProvider", "⚡ AlarmManager frame scheduled (exact+idle) in ${ANIMATION_INTERVAL_MS}ms")
                } else {
                        alarmManager!!.setExact(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            nextTriggerTime,
                            pendingIntent
                        )
                        android.util.Log.v("WidgetProvider", "⚡ AlarmManager frame scheduled (exact) in ${ANIMATION_INTERVAL_MS}ms")
                    }
                } else {
                    // 近似アラームを使用
                    alarmManager!!.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        nextTriggerTime,
                        pendingIntent
                    )
                    android.util.Log.v("WidgetProvider", "⚡ AlarmManager frame scheduled (inexact) in ${ANIMATION_INTERVAL_MS}ms")
            }
        } else {
                android.util.Log.e("WidgetProvider", "❌ PendingIntent or AlarmManager is null, cannot schedule frame")
                isAnimationRunning = false
            }
            
        } catch (e: SecurityException) {
            android.util.Log.e("WidgetProvider", "❌ Security exception - missing alarm permission", e)
            handlePermissionError(context)
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to schedule AlarmManager frame", e)
            isAnimationRunning = false
        }
    }
    
//    private fun initializeAlarmManagerMain(context: Context) {
//        try {
//            // AlarmManagerメインシステムの初期化
//            val intent = Intent(context, CounterWidgetProvider::class.java).apply {
//                action = ACTION_WIDGET_UPDATE
//            }
//
//            animationPendingIntent = PendingIntent.getBroadcast(
//                context,
//                0,
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            android.util.Log.d("WidgetProvider", "✅ AlarmManager main system initialized")
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Failed to initialize AlarmManager main system", e)
//        }
//    }
    

    
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
        android.util.Log.v("WidgetProvider", "🔄 === AlarmManager Main Update Frame $currentFrame ===")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // アニメーション状態を確認・修正
            if (!isAnimationRunning) {
                android.util.Log.w("WidgetProvider", "⚠️ Animation state was false, resetting to true")
                isAnimationRunning = true
            }
            
            // 実際のトリガー時間を記録
            actualTriggerTime = System.currentTimeMillis()
            val delay = actualTriggerTime - expectedTriggerTime
            
            if (delay > 0) {
                triggerDelayTotal += delay
                triggerDelayCount++
                android.util.Log.v("WidgetProvider", "⏱️ AlarmManager delay: ${delay}ms")
            }
            
            // フレーム更新を実行
            handleHighPrecisionUpdate(context)
            
            // 次のフレームをAlarmManagerでスケジュール
            scheduleAlarmManagerFrame(context)
            
            val frameTime = System.currentTimeMillis() - startTime
            android.util.Log.v("WidgetProvider", "⚡ AlarmManager frame completed in ${frameTime}ms")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error in AlarmManager main update", e)
            stopAlarmBasedAnimation(context)
        }
    }
    
    private fun handleHighPrecisionUpdate(context: Context) {
        val startTime = System.currentTimeMillis()
        
        try {
            android.util.Log.v("WidgetProvider", "⚡ === 2fps Update Frame $currentFrame ===")
            
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
                    currentTotalFrames = getActualFrameCount(context)
                    currentFrame = (currentFrame + 1) % currentTotalFrames
                    frameUpdateCount++
                    lastFrameTime = System.currentTimeMillis()
                    
                    // 自動再開システム用の成功時間更新
                    lastSuccessfulFrameTime = System.currentTimeMillis()
                    
                    // 2fpsアニメーション用パフォーマンス監視
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
            android.util.Log.e("WidgetProvider", "❌ Critical error in 2fps update", e)
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
               // setupButtons(context, remoteViews)
                
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
                val remoteViews = RemoteViews(context.packageName, R.layout.widget_layout_wall)
                
                // キャッシュされた画像を使用
                if (isCacheReady && optimizedImageCache.isNotEmpty() && currentFrame < optimizedImageCache.size) {
                    val bitmap = optimizedImageCache[currentFrame]
                    if (!bitmap.isRecycled) {
                        remoteViews.setImageViewBitmap(R.id.background_image, bitmap)
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
            // アニメーション状態管理
            val animationStateManager = AnimationStateManager.getInstance(context)
            
            // 現在のアニメーション状態を表示
            val currentAnimationText = animationStateManager.getCurrentAnimationDisplayText()
            remoteViews.setTextViewText(R.id.animation_status_text, currentAnimationText)
            
            // アニメーション切り替えボタン
            val toggleIntent = Intent(context, CounterWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_ANIMATION
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context, 1, toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            remoteViews.setOnClickPendingIntent(R.id.toggle_animation_button, togglePendingIntent)
            

        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error setting up buttons", e)
        }
    }
    
    private fun handleToggleAnimation(context: Context) {
        try {
            android.util.Log.d("WidgetProvider", "🔄 Handling animation toggle")
            
            // アニメーション状態を切り替え
            val animationStateManager = AnimationStateManager.getInstance(context)
            val newAnimationType = animationStateManager.toggleAnimationType()
            
            android.util.Log.d("WidgetProvider", "✅ Animation switched to: $newAnimationType")
            
            // 古いキャッシュをクリア
            optimizedImageCache.forEach { bitmap ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            optimizedImageCache.clear()
            imageFilePaths.clear()
            isCacheReady = false
            
            // 新しいアニメーション種類の画像パスを取得
            android.util.Log.d("WidgetProvider", "🔄 Rebuilding image cache for new animation type")
            initializeImagePaths(context)
            
            // フレームをリセット
            currentFrame = 0
            android.util.Log.d("WidgetProvider", "🔄 Frame reset to 0 for new animation")
            
            // アニメーションサービスに切り替えを通知
            val serviceIntent = Intent(context, WidgetAnimationService::class.java).apply {
                action = "TOGGLE_ANIMATION"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            // ウィジェットの表示を更新
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, CounterWidgetProvider::class.java))
            updateAllWidgets(context, appWidgetManager, widgetIds)
            
            android.util.Log.d("WidgetProvider", "✅ Animation toggle complete - Cache status: ${if (isCacheReady) "READY" else "NOT READY"}, Images: ${optimizedImageCache.size}")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Error handling animation toggle", e)
        }
    }
    
    private fun initializeImagePaths(context: Context) {
        android.util.Log.d("WidgetProvider", "🔍 Initializing image paths")
        
        try {
            imageFilePaths.clear()
            
            // アニメーション状態管理
            val animationStateManager = AnimationStateManager.getInstance(context)
            val currentAnimationType = animationStateManager.getCurrentAnimationType()
            val currentCharacterId = animationStateManager.getCurrentCharacterId()
            
            val baseDir = context.getExternalFilesDir(null)
            
            // 複数のパスパターンを試行
            val pathPatterns = listOf(
                "$currentCharacterId/State/$currentAnimationType",  // Mao/State/Adle
                "State/$currentAnimationType",                      // State/Adle
                "$currentAnimationType",                            // Adle
                "$currentCharacterId/$currentAnimationType",        // Mao/Adle
                "WidgetImages",                                     // 従来のパス
                "$currentCharacterId/State/${currentAnimationType.lowercase()}", // 小文字版
                "State/${currentAnimationType.lowercase()}",                     // 小文字版
                currentAnimationType.lowercase()                                 // 小文字版単体
            )
            
            android.util.Log.d("WidgetProvider", "🎯 Looking for animation: $currentCharacterId/State/$currentAnimationType")
            
            var externalFilesDir: File? = null
            var foundPath: String? = null
            
            if (baseDir != null) {
                for (pattern in pathPatterns) {
                    val testDir = File(baseDir, pattern)
                    android.util.Log.d("WidgetProvider", "🔍 Testing path: $pattern -> ${testDir.absolutePath}")
                    
                    if (testDir.exists()) {
                        val imageFiles = testDir.listFiles { file ->
                            file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
                        }
                        
                        android.util.Log.d("WidgetProvider", "  - Directory exists: ${testDir.exists()}")
                        android.util.Log.d("WidgetProvider", "  - Image files found: ${imageFiles?.size ?: 0}")
                        
                        // ディレクトリが存在するが画像がない場合、全ファイルを確認
                        if (testDir.exists() && imageFiles.isNullOrEmpty()) {
                            val allFiles = testDir.listFiles()
                            android.util.Log.d("WidgetProvider", "  - All files in directory: ${allFiles?.size ?: 0}")
                            allFiles?.take(5)?.forEach { file ->
                                android.util.Log.d("WidgetProvider", "    - ${file.name} (${if (file.isDirectory) "DIR" else "FILE"}, ext: ${file.extension})")
                            }
                        }
                        
                        if (!imageFiles.isNullOrEmpty()) {
                            externalFilesDir = testDir
                            foundPath = pattern
                            android.util.Log.d("WidgetProvider", "✅ Found images in: $pattern (${imageFiles.size} files)")
                            
                            // 特にWidgetImagesディレクトリの場合、ファイル名を詳細に確認
                            if (pattern == "WidgetImages") {
                                android.util.Log.d("WidgetProvider", "📁 WidgetImages directory contents:")
                                imageFiles.take(10).forEach { file ->
                                    android.util.Log.d("WidgetProvider", "  - ${file.name}")
                                }
                                if (imageFiles.size > 10) {
                                    android.util.Log.d("WidgetProvider", "  - ... and ${imageFiles.size - 10} more files")
                                }
                            }
                            
                            break
                        }
                    }
                }
            }
            
            if (externalFilesDir?.exists() == true) {
                val files = externalFilesDir.listFiles { file ->
                    file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
                }
                
                files?.sortedBy { it.name }?.forEach { file ->
                    imageFilePaths.add(file.absolutePath)
                }
                
                android.util.Log.d("WidgetProvider", "✅ Found ${imageFilePaths.size} image files for $currentAnimationType using pattern: $foundPath")
                
                // 画像パスが見つかったら、最適化キャッシュを構築
                if (imageFilePaths.isNotEmpty()) {
                    buildImageCache(context)
                }
            } else {
                android.util.Log.w("WidgetProvider", "⚠️ No images found in any path pattern for: $currentAnimationType")
                
                // 利用可能なディレクトリを全てログ出力
                android.util.Log.d("WidgetProvider", "📁 Available directories in base:")
                baseDir?.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        android.util.Log.d("WidgetProvider", "  - ${file.name}/")
                        file.listFiles()?.forEach { subFile ->
                            if (subFile.isDirectory) {
                                android.util.Log.d("WidgetProvider", "    - ${subFile.name}/")
                            }
                        }
                    }
                }
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
            
            android.util.Log.i("WidgetProvider", "📊 2fps Performance Stats:")
            android.util.Log.i("WidgetProvider", "  - Frame time: Avg ${averageFrameTime}ms, Max ${maxFrameTime}ms, Min ${minFrameTime}ms")
            android.util.Log.i("WidgetProvider", "  - Trigger delay: Avg ${avgDelay}ms, Count ${triggerDelayCount}/${frameUpdateCount}")
            android.util.Log.i("WidgetProvider", "  - Precision: ${(frameUpdateCount - triggerDelayCount) * 100 / frameUpdateCount}% on-time")
            
            // 遅延統計をリセット
            triggerDelayTotal = 0L
            triggerDelayCount = 0
        }
    }
    
//    private fun logFinalStatistics() {
//        if (frameUpdateCount > 0) {
//            val avgDelay = if (triggerDelayCount > 0) triggerDelayTotal / triggerDelayCount else 0
//            val precisionPercent = (frameUpdateCount - triggerDelayCount) * 100 / frameUpdateCount
//
//            android.util.Log.i("WidgetProvider", "📊 Final 2fps Animation Statistics:")
//            android.util.Log.i("WidgetProvider", "  - Total frames: $frameUpdateCount")
//            android.util.Log.i("WidgetProvider", "  - Average frame time: ${averageFrameTime}ms")
//            android.util.Log.i("WidgetProvider", "  - Frame time range: ${minFrameTime}ms - ${maxFrameTime}ms")
//            android.util.Log.i("WidgetProvider", "  - Average trigger delay: ${avgDelay}ms")
//            android.util.Log.i("WidgetProvider", "  - Precision rate: ${precisionPercent}% on-time")
//            android.util.Log.i("WidgetProvider", "  - Target interval: ${ANIMATION_INTERVAL_MS}ms (${1000/ANIMATION_INTERVAL_MS}fps)")
//            android.util.Log.i("WidgetProvider", "  - Auto-restart attempts: $autoRestartCount/${MAX_AUTO_RESTART_COUNT}")
//            android.util.Log.i("WidgetProvider", "  - Auto-restart enabled: $autoRestartEnabled")
//        }
//    }
    
//    private fun startAutoRestartMonitoring(context: Context) {
//        if (!autoRestartEnabled) return
//
//        android.util.Log.d("WidgetProvider", "🔄 Starting AlarmManager-based auto-restart monitoring")
//
//        try {
//            // 既存の監視アラームを停止
//            stopAutoRestartMonitoring()
//
//            // 自動再開チェック用のPendingIntentを作成
//            val intent = Intent(context, CounterWidgetProvider::class.java).apply {
//                action = ACTION_AUTO_RESTART_CHECK
//            }
//
//            autoRestartPendingIntent = PendingIntent.getBroadcast(
//                context,
//                100, // 異なるrequestCodeを使用
//                intent,
//                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//            )
//
//            // AlarmManagerで定期チェックをスケジュール
//            scheduleAutoRestartCheck(context)
//
//            android.util.Log.d("WidgetProvider", "✅ AlarmManager-based auto-restart monitoring started")
//
//                } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Failed to start auto-restart monitoring", e)
//        }
//    }
    
//    private fun stopAutoRestartMonitoring() {
//        android.util.Log.d("WidgetProvider", "🛑 Stopping AlarmManager-based auto-restart monitoring")
//
//        try {
//            autoRestartPendingIntent?.let { pendingIntent ->
//                alarmManager?.cancel(pendingIntent)
//                pendingIntent.cancel()
//                android.util.Log.d("WidgetProvider", "✅ Auto-restart monitoring alarm cancelled")
//            }
//            autoRestartPendingIntent = null
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Error stopping auto-restart monitoring", e)
//        }
//    }
    
//    private fun scheduleAutoRestartCheck(context: Context) {
//        try {
//            val triggerTime = SystemClock.elapsedRealtime() + AUTO_RESTART_DELAY_MS
//            val pendingIntent = autoRestartPendingIntent
//
//            if (pendingIntent != null && alarmManager != null) {
//                // 近似アラームを使用（監視なので正確性はそれほど重要ではない）
//                alarmManager!!.set(
//                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                    triggerTime,
//                    pendingIntent
//                )
//                android.util.Log.v("WidgetProvider", "🔍 Auto-restart check scheduled in ${AUTO_RESTART_DELAY_MS}ms")
//            }
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Failed to schedule auto-restart check", e)
//        }
//    }
    
//    private fun handleAutoRestartCheck(context: Context) {
//        try {
//            val currentTime = System.currentTimeMillis()
//            val timeSinceLastFrame = currentTime - lastSuccessfulFrameTime
//
//            android.util.Log.d("WidgetProvider", "🔍 Auto-restart check: ${timeSinceLastFrame}ms since last frame, running: $isAnimationRunning")
//            android.util.Log.d("WidgetProvider", "  - Threshold: ${ANIMATION_INTERVAL_MS * 2}ms (2x animation interval)")
//            android.util.Log.d("WidgetProvider", "  - Should restart: ${!isAnimationRunning || timeSinceLastFrame > ANIMATION_INTERVAL_MS * 2}")
//
//            // アニメーションが停止していて、最大再開回数に達していない場合
//            // 判定：アニメーション間隔の2倍以上経過している場合（2秒）
//            if (!isAnimationRunning || timeSinceLastFrame > ANIMATION_INTERVAL_MS * 2) {
//                if (autoRestartCount < MAX_AUTO_RESTART_COUNT && autoRestartEnabled) {
//
//                    android.util.Log.w("WidgetProvider", "⚠️ Animation appears to be stopped")
//                    android.util.Log.i("WidgetProvider", "🔄 Auto-restarting animation (attempt ${autoRestartCount + 1}/${MAX_AUTO_RESTART_COUNT})")
//
//                    autoRestartCount++
//
//                    // 自動再開を実行
////                    performAutoRestart(context)
//
//                } else if (autoRestartCount >= MAX_AUTO_RESTART_COUNT) {
//                    android.util.Log.w("WidgetProvider", "⚠️ Max auto-restart attempts reached. Disabling auto-restart.")
//                    autoRestartEnabled = false
//                    stopAutoRestartMonitoring()
//                    return // 次の監視をスケジュールしない
//                }
//            } else {
//                android.util.Log.v("WidgetProvider", "✅ Animation is running normally")
//            }
//
//            // 継続監視のために次のチェックをスケジュール
//            if (autoRestartEnabled) {
//                scheduleAutoRestartCheck(context)
//            }
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Error in auto-restart check", e)
//            // エラーが発生しても監視を継続
//            if (autoRestartEnabled) {
//                scheduleAutoRestartCheck(context)
//            }
//        }
    }
    
//    private fun performAutoRestart(context: Context) {
//        try {
//            android.util.Log.i("WidgetProvider", "🚀 Performing automatic restart...")
//            android.util.Log.i("WidgetProvider", "  - Current state: isAnimationRunning=$isAnimationRunning")
//            android.util.Log.i("WidgetProvider", "  - Auto-restart attempt: $autoRestartCount/$MAX_AUTO_RESTART_COUNT")
//
//            // 現在のアニメーションを停止
//            stopAlarmBasedAnimation(context)
//
//            // 即座に再開（遅延なし）
//            android.util.Log.i("WidgetProvider", "🔄 Immediately restarting animation...")
//            startAlarmBasedAnimation(context, resetAutoRestartCounter = false)
//
//        } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Error performing auto-restart", e)
//        }
//    }
    

    
//    private fun cleanupImageCache() {
//        android.util.Log.d("WidgetProvider", "🧹 Cleaning up image cache")
//
//        try {
//            // 統計情報を出力
//            logFinalStatistics()
//
//            // 自動再開システムを停止
//            stopAutoRestartMonitoring()
//
//            optimizedImageCache.forEach { bitmap ->
//                if (!bitmap.isRecycled) {
//                    bitmap.recycle()
//                }
//            }
//            optimizedImageCache.clear()
//            isCacheReady = false
//
//            android.util.Log.d("WidgetProvider", "✅ Image cache cleaned up")
//
//                    } catch (e: Exception) {
//            android.util.Log.e("WidgetProvider", "❌ Error cleaning up image cache", e)
//        }
//    }
//}