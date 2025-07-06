package com.seo4d696b75.android.glance_widget_demo.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager

import android.widget.RemoteViews
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class WidgetAnimationService : Service() {
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_CHANNEL_ID = "widget_animation_channel"
        private const val ANIMATION_INTERVAL_MS = 200L // 5fps (ANR防止のため)
        private const val TOTAL_FRAMES = 50
        private const val WIDGET_IMAGE_WIDTH = 400
        private const val WIDGET_IMAGE_HEIGHT = 500
        
        private var isServiceRunning = false
        private var scheduledExecutor: ScheduledExecutorService? = null
        private var wakeLock: PowerManager.WakeLock? = null
        
        // アニメーション状態
        private var currentFrame = 0
        private var actualFrameCount = 0 // 実際に利用可能な画像数
        private var frameUpdateCount = 0
        private var lastFrameTime = 0L
        private var totalFrameTime = 0L
        private var frameTimeMin = Long.MAX_VALUE
        private var frameTimeMax = 0L
        
        // 画像キャッシュ
        private val imageFilePaths = mutableListOf<String>()
        private val optimizedImageCache = mutableListOf<Bitmap>()
        private var isCacheReady = false
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("WidgetAnimationService", "🚀 Service created")
        createNotificationChannel()
        
        // 画像キャッシュの初期化をバックグラウンドで実行
        Thread {
            initializeImageCache()
        }.start()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("WidgetAnimationService", "🎬 === STARTING FOREGROUND 10FPS ANIMATION ===")
        
        when (intent?.action) {
            "START_ANIMATION" -> startAnimation()
            "STOP_ANIMATION" -> stopAnimation()
            "REBUILD_CACHE" -> {
                android.util.Log.d("WidgetAnimationService", "🔄 Force rebuilding image cache...")
                Thread {
                    initializeImageCache()
                }.start()
            }
            else -> startAnimation() // デフォルトで開始
        }
        
        return START_STICKY // 自動再起動
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "ウィジェットアニメーション",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ウィジェットの高速アニメーション用サービス"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, WidgetAnimationService::class.java).apply {
            action = "STOP_ANIMATION"
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("ウィジェットアニメーション実行中")
                .setContentText("5fps (400x500解像度) でスムーズアニメーション中")
//                .setSmallIcon(R.drawable.ic_arrow_up)
//                .setOngoing(true)
//                .addAction(
//                    Notification.Action.Builder(
//                        R.drawable.ic_arrow_down,
//                        "停止",
//                        stopPendingIntent
//                    ).build()
//                )
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("ウィジェットアニメーション実行中")
                .setContentText("5fps (400x500解像度) でスムーズアニメーション中")
//                .setSmallIcon(R.drawable.ic_arrow_up)
//                .setOngoing(true)
//                .addAction(
//                    Notification.Action.Builder(
//                        R.drawable.ic_arrow_down,
//                        "停止",
//                        stopPendingIntent
//                    ).build()
//                )
                .build()
        }
    }
    
    private fun startAnimation() {
        if (isServiceRunning) {
            android.util.Log.d("WidgetAnimationService", "⚠️ Animation already running")
            return
        }
        
        try {
            // フォアグラウンド通知を開始
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // WakeLockを取得
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "WidgetAnimationService:HighFrequencyAnimation"
            )
            wakeLock?.acquire()
            
            // 高精度タイマーを開始
            startHighFrequencyUpdates()
            
            isServiceRunning = true
            android.util.Log.d("WidgetAnimationService", "✅ Foreground 5fps animation started successfully")
            android.util.Log.d("WidgetAnimationService", "  - Frame interval: ${ANIMATION_INTERVAL_MS}ms (5fps)")
            android.util.Log.d("WidgetAnimationService", "  - Resolution: ${WIDGET_IMAGE_WIDTH}x${WIDGET_IMAGE_HEIGHT}")
            android.util.Log.d("WidgetAnimationService", "  - Total frames: $TOTAL_FRAMES")
            android.util.Log.d("WidgetAnimationService", "  - Available images: ${imageFilePaths.size}")
            android.util.Log.d("WidgetAnimationService", "  - WakeLock: ACTIVE")
            android.util.Log.d("WidgetAnimationService", "  - Foreground notification: ACTIVE")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Failed to start foreground animation", e)
            stopSelf()
        }
    }
    
    private fun startHighFrequencyUpdates() {
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
        
        scheduledExecutor?.scheduleAtFixedRate({
            try {
                updateWidget()
            } catch (e: Exception) {
                android.util.Log.e("WidgetAnimationService", "❌ Error updating widget", e)
            }
        }, 0, ANIMATION_INTERVAL_MS, TimeUnit.MILLISECONDS)
        
        android.util.Log.d("WidgetAnimationService", "⚡ High-frequency timer started (${ANIMATION_INTERVAL_MS}ms)")
    }
    
    private fun updateWidget() {
        if (!isServiceRunning) return
        
        val startTime = System.currentTimeMillis()
        
        try {
            // バックグラウンドスレッドで重い処理を実行
            Thread {
                try {
                    // 同期化された画像キャッシュアクセス
                    val bitmap = synchronized(this@WidgetAnimationService) {
                        // 画像キャッシュの確認（バックグラウンドで実行）
                        if (!isCacheReady) {
                            android.util.Log.w("WidgetAnimationService", "⚠️ Image cache not ready, rebuilding...")
                            initializeImageCache()
                            if (!isCacheReady) {
                                android.util.Log.e("WidgetAnimationService", "❌ Failed to build image cache")
                                return@Thread
                            }
                        }
                        
                        if (actualFrameCount == 0 || currentFrame >= actualFrameCount) {
                            android.util.Log.w("WidgetAnimationService", "⚠️ Frame index out of bounds: $currentFrame >= $actualFrameCount (cache size: ${optimizedImageCache.size})")
                            return@Thread
                        }
                        
                        val bitmap = optimizedImageCache[currentFrame]
                        if (bitmap.isRecycled) {
                            android.util.Log.w("WidgetAnimationService", "⚠️ Bitmap is recycled, rebuilding cache")
                            initializeImageCache()
                            return@Thread
                        }
                        
                        bitmap
                    }
                    
                    // UI更新はメインスレッドで実行
                    Handler(Looper.getMainLooper()).post {
                        try {
                            if (!isServiceRunning) return@post
                            
                            // ウィジェットの取得
                            val appWidgetManager = AppWidgetManager.getInstance(this@WidgetAnimationService)
                            val componentName = ComponentName(this@WidgetAnimationService, CounterWidgetProvider::class.java)
                            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                            
                            if (widgetIds.isEmpty()) {
                                android.util.Log.w("WidgetAnimationService", "⚠️ No widgets found, stopping service")
                                stopAnimation()
                                return@post
                            }
                            
                            // ウィジェットを更新
                            widgetIds.forEach { widgetId ->
                                val remoteViews = RemoteViews(packageName, R.layout.widget_layout)
                                remoteViews.setImageViewBitmap(R.id.widget_image, bitmap)
                                
                                // ボタンの設定
                                //setupButtons(remoteViews)
                                
                                appWidgetManager.updateAppWidget(widgetId, remoteViews)
                            }
                            
                            // フレームカウンターの更新（同期化）
                            synchronized(this@WidgetAnimationService) {
                                if (actualFrameCount > 0) {
                                    currentFrame = (currentFrame + 1) % actualFrameCount
                                }
                                frameUpdateCount++
                                lastFrameTime = System.currentTimeMillis()
                            }
                            
                            // パフォーマンス統計
                            val frameTime = System.currentTimeMillis() - startTime
                            updatePerformanceMetrics(frameTime)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("WidgetAnimationService", "❌ Error in UI update", e)
                        }
                    }
                    
                } catch (e: Exception) {
                    android.util.Log.e("WidgetAnimationService", "❌ Error in background processing", e)
                }
            }.start()
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Critical error in widget update", e)
        }
    }
    
//    private fun setupButtons(remoteViews: RemoteViews) {
//        // 停止ボタン
//        val stopIntent = Intent(this, WidgetAnimationService::class.java).apply {
//            action = "STOP_ANIMATION"
//        }
//        val stopPendingIntent = PendingIntent.getService(
//            this, 2, stopIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        remoteViews.setOnClickPendingIntent(R.id.button_stop, stopPendingIntent)
//
//        // 開始ボタン
//        val startIntent = Intent(this, WidgetAnimationService::class.java).apply {
//            action = "START_ANIMATION"
//        }
//        val startPendingIntent = PendingIntent.getService(
//            this, 3, startIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//        remoteViews.setOnClickPendingIntent(R.id.button_start, startPendingIntent)
//    }
    
    private fun stopAnimation() {
        android.util.Log.d("WidgetAnimationService", "⏹️ Stopping foreground animation")
        
        try {
            isServiceRunning = false
            
            // タイマーを停止
            scheduledExecutor?.shutdown()
            scheduledExecutor = null
            
            // WakeLockを解放
            wakeLock?.release()
            wakeLock = null
            
            // フォアグラウンドを停止
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            
            // 統計情報を出力
            logFinalStatistics()
            
            android.util.Log.d("WidgetAnimationService", "✅ Foreground animation stopped")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Error stopping animation", e)
        }
    }
    
    private fun initializeImageCache() {
        try {
            android.util.Log.d("WidgetAnimationService", "🖼️ Initializing image cache...")
            
            // 同期化された操作
            synchronized(this) {
                imageFilePaths.clear()
                optimizedImageCache.clear()
            }
            
            // 画像ファイルパスを取得（外部ファイルディレクトリを使用）
            val baseDir = getExternalFilesDir(null)
            val imagesDir = if (baseDir != null) File(baseDir, "Mao/State/Adle") else null
            
            android.util.Log.d("WidgetAnimationService", "🔍 Checking directories:")
            android.util.Log.d("WidgetAnimationService", "  - Base dir: ${baseDir?.absolutePath}")
            android.util.Log.d("WidgetAnimationService", "  - Images dir: ${imagesDir?.absolutePath}")
            android.util.Log.d("WidgetAnimationService", "  - Images dir exists: ${imagesDir?.exists()}")
            
            // 親ディレクトリの確認
            if (baseDir != null) {
                android.util.Log.d("WidgetAnimationService", "  - Base dir contents:")
                baseDir.listFiles()?.forEach { file ->
                    android.util.Log.d("WidgetAnimationService", "    - ${file.name} (${if (file.isDirectory) "DIR" else "FILE"})")
                }
                
                // Maoディレクトリの確認
                val maoDir = File(baseDir, "Mao")
                if (maoDir.exists()) {
                    android.util.Log.d("WidgetAnimationService", "  - Mao dir contents:")
                    maoDir.listFiles()?.forEach { file ->
                        android.util.Log.d("WidgetAnimationService", "    - ${file.name} (${if (file.isDirectory) "DIR" else "FILE"})")
                    }
                    
                    // Mao/Stateディレクトリの確認
                    val stateDir = File(maoDir, "State")
                    if (stateDir.exists()) {
                        android.util.Log.d("WidgetAnimationService", "  - State dir contents:")
                        stateDir.listFiles()?.forEach { file ->
                            android.util.Log.d("WidgetAnimationService", "    - ${file.name} (${if (file.isDirectory) "DIR" else "FILE"})")
                        }
                    }
                }
            }
            
            if (imagesDir == null || !imagesDir.exists()) {
                android.util.Log.e("WidgetAnimationService", "❌ Images directory not found: ${imagesDir?.absolutePath}")
                
                // 代替パス（MainActivity等で使用される内部ファイルディレクトリ）をチェック
                val alternativeDir = java.io.File(filesDir, "WidgetImages")
                android.util.Log.d("WidgetAnimationService", "🔍 Checking alternative directory: ${alternativeDir.absolutePath}")
                android.util.Log.d("WidgetAnimationService", "  - Alternative dir exists: ${alternativeDir.exists()}")
                
                if (alternativeDir.exists()) {
                    android.util.Log.d("WidgetAnimationService", "📁 Using alternative images directory: ${alternativeDir.absolutePath}")
                    scanImagesDirectory(alternativeDir)
                } else {
                    android.util.Log.e("WidgetAnimationService", "❌ Alternative images directory also not found: ${alternativeDir.absolutePath}")
                    
                    // 手動でディレクトリを作成してみる
                    android.util.Log.d("WidgetAnimationService", "🔧 Attempting to create images directory...")
                    if (imagesDir != null) {
                        try {
                            imagesDir.mkdirs()
                            android.util.Log.d("WidgetAnimationService", "✅ Directory created: ${imagesDir.absolutePath}")
                            android.util.Log.d("WidgetAnimationService", "  - Directory exists now: ${imagesDir.exists()}")
                        } catch (e: Exception) {
                            android.util.Log.e("WidgetAnimationService", "❌ Failed to create directory", e)
                        }
                    }
                }
                return
            }
            
            android.util.Log.d("WidgetAnimationService", "📁 Using images directory: ${imagesDir.absolutePath}")
            scanImagesDirectory(imagesDir)
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Error initializing image cache", e)
        }
    }
    
    private fun scanImagesDirectory(imagesDir: java.io.File) {
        try {
            android.util.Log.d("WidgetAnimationService", "🔍 Scanning directory: ${imagesDir.absolutePath}")
            android.util.Log.d("WidgetAnimationService", "  - Directory exists: ${imagesDir.exists()}")
            android.util.Log.d("WidgetAnimationService", "  - Directory readable: ${imagesDir.canRead()}")
            android.util.Log.d("WidgetAnimationService", "  - Directory is directory: ${imagesDir.isDirectory}")
            
            val allFiles = imagesDir.listFiles()
            android.util.Log.d("WidgetAnimationService", "  - Total files in directory: ${allFiles?.size ?: 0}")
            
            val imageFiles = imagesDir.listFiles { file ->
                file.isFile && file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp")
            }?.sortedBy { it.name } ?: emptyList()
            
            android.util.Log.d("WidgetAnimationService", "  - Image files found: ${imageFiles.size}")
            
            if (imageFiles.isEmpty()) {
                android.util.Log.e("WidgetAnimationService", "❌ No image files found in directory!")
                android.util.Log.d("WidgetAnimationService", "  - All files in directory:")
                allFiles?.forEach { file ->
                    android.util.Log.d("WidgetAnimationService", "    - ${file.name} (${file.length()} bytes, ext: ${file.extension})")
                }
                return
            } else {
                android.util.Log.d("WidgetAnimationService", "  - Found image files:")
                imageFiles.take(10).forEach { file ->
                    android.util.Log.d("WidgetAnimationService", "    - ${file.name} (${file.length()} bytes, ext: ${file.extension})")
                }
            }
            
            if (imageFiles.size < TOTAL_FRAMES) {
                android.util.Log.w("WidgetAnimationService", "⚠️ Not enough images: ${imageFiles.size} < $TOTAL_FRAMES")
            }
            
            // 同期化された操作でパスを追加
            synchronized(this) {
                imageFiles.take(TOTAL_FRAMES).forEach { file ->
                    imageFilePaths.add(file.absolutePath)
                    android.util.Log.d("WidgetAnimationService", "  - Added image: ${file.name} (${file.length()} bytes)")
                }
            }
            
            android.util.Log.d("WidgetAnimationService", "📁 Found ${imageFilePaths.size} image files")
            
            // 画像を最適化してキャッシュ
            buildImageCache()
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Error scanning images directory", e)
        }
    }
    
    private fun buildImageCache() {
        // 重い処理はバックグラウンドスレッドで実行
        Thread {
            try {
                android.util.Log.d("WidgetAnimationService", "🔄 Building optimized image cache...")
                
                // メモリ使用量の予測計算
                val bytesPerPixel = 2 // RGB_565
                val pixelsPerImage = WIDGET_IMAGE_WIDTH * WIDGET_IMAGE_HEIGHT
                val bytesPerImage = pixelsPerImage * bytesPerPixel
                val totalCacheSize = bytesPerImage * TOTAL_FRAMES
                android.util.Log.d("WidgetAnimationService", "  - Estimated cache size: ${totalCacheSize / 1024 / 1024}MB (${TOTAL_FRAMES} images × ${bytesPerImage / 1024}KB)")
                
                // 同期化された操作
                synchronized(this@WidgetAnimationService) {
                    optimizedImageCache.clear()
                    
                    // imageFilePathsをコピーして安全に反復処理
                    val pathsCopy = imageFilePaths.toList()
                    android.util.Log.d("WidgetAnimationService", "📋 Processing ${pathsCopy.size} image paths")
                    
                    pathsCopy.forEachIndexed { index, imagePath ->
                    try {
                        android.util.Log.d("WidgetAnimationService", "📸 Processing image ${index + 1}/${pathsCopy.size}: ${java.io.File(imagePath).name}")
                        
                        // ファイルの存在確認
                        val imageFile = java.io.File(imagePath)
                        if (!imageFile.exists()) {
                            android.util.Log.e("WidgetAnimationService", "❌ Image file not found: $imagePath")
                            return@forEachIndexed
                        }
                        
                        android.util.Log.d("WidgetAnimationService", "  - File size: ${imageFile.length()} bytes")
                        
                        // メモリ効率的なデコード処理
                        val originalBitmap = decodeImageSafely(imagePath)
                        if (originalBitmap != null) {
                            android.util.Log.d("WidgetAnimationService", "  - Original size: ${originalBitmap.width}x${originalBitmap.height}")
                            android.util.Log.d("WidgetAnimationService", "  - Original config: ${originalBitmap.config}")
                            
                            val optimizedBitmap = Bitmap.createScaledBitmap(
                                originalBitmap,
                                WIDGET_IMAGE_WIDTH,
                                WIDGET_IMAGE_HEIGHT,
                                true
                            )
                            
                            val compressedBitmap = optimizedBitmap.copy(Bitmap.Config.RGB_565, false)
                            optimizedImageCache.add(compressedBitmap)
                            
                            android.util.Log.d("WidgetAnimationService", "  - Optimized size: ${compressedBitmap.width}x${compressedBitmap.height}")
                            android.util.Log.d("WidgetAnimationService", "  - Optimized config: ${compressedBitmap.config}")
                            
                            originalBitmap.recycle()
                            if (optimizedBitmap != compressedBitmap) {
                                optimizedBitmap.recycle()
                            }
                            
                        } else {
                            android.util.Log.e("WidgetAnimationService", "❌ Failed to decode image: $imagePath")
                        }
                    } catch (e: OutOfMemoryError) {
                        android.util.Log.e("WidgetAnimationService", "❌ OutOfMemoryError processing image: $imagePath", e)
                        // メモリをクリーンアップ
                        System.gc()
                    } catch (e: Exception) {
                        android.util.Log.e("WidgetAnimationService", "❌ Error processing image: $imagePath", e)
                    }
                    }
                    
                    isCacheReady = optimizedImageCache.isNotEmpty()
                    actualFrameCount = optimizedImageCache.size
                    
                    android.util.Log.d("WidgetAnimationService", "✅ Image cache built: ${optimizedImageCache.size} images")
                    android.util.Log.d("WidgetAnimationService", "  - Image size: ${WIDGET_IMAGE_WIDTH}x${WIDGET_IMAGE_HEIGHT}")
                    android.util.Log.d("WidgetAnimationService", "  - Format: RGB_565 (50% memory reduction)")
                    android.util.Log.d("WidgetAnimationService", "  - Cache ready: $isCacheReady")
                    android.util.Log.d("WidgetAnimationService", "  - Actual frame count: $actualFrameCount (vs TOTAL_FRAMES: $TOTAL_FRAMES)")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("WidgetAnimationService", "❌ Error building image cache", e)
            }
        }.start()
    }
    
    private fun decodeImageSafely(imagePath: String): Bitmap? {
        return try {
            // メモリ使用量をログ出力
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
            
            android.util.Log.d("WidgetAnimationService", "  - Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB (${String.format("%.1f", memoryUsagePercent)}%)")
            
            // 高解像度のためメモリ使用量が高い場合は警告
            if (memoryUsagePercent > 75) {
                android.util.Log.w("WidgetAnimationService", "⚠️ High memory usage detected! ${String.format("%.1f", memoryUsagePercent)}% - consider reducing cache size")
            }
            
            // まず画像の情報だけを取得
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(imagePath, options)
            
            android.util.Log.d("WidgetAnimationService", "  - Image dimensions: ${options.outWidth}x${options.outHeight}")
            android.util.Log.d("WidgetAnimationService", "  - Image format: ${options.outMimeType}")
            
            // サンプルサイズを計算してメモリ使用量を削減
            val targetSize = maxOf(WIDGET_IMAGE_WIDTH, WIDGET_IMAGE_HEIGHT) * 2
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, targetSize)
            android.util.Log.d("WidgetAnimationService", "  - Sample size: $sampleSize")
            
            // 実際の画像を読み込み
            val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565  // メモリ効率
                inPurgeable = true
                inInputShareable = true
            }
            
            val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath, decodeOptions)
            android.util.Log.d("WidgetAnimationService", "  - Decoded size: ${bitmap?.width}x${bitmap?.height}")
            
            bitmap
            
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("WidgetAnimationService", "❌ OutOfMemoryError decoding image: $imagePath", e)
            System.gc()
            null
        } catch (e: Exception) {
            android.util.Log.e("WidgetAnimationService", "❌ Error decoding image: $imagePath", e)
            null
        }
    }
    
    private fun calculateSampleSize(width: Int, height: Int, targetSize: Int): Int {
        var sampleSize = 1
        val maxDimension = maxOf(width, height)
        
        if (maxDimension > targetSize) {
            sampleSize = 2
            while (maxDimension / sampleSize > targetSize) {
                sampleSize *= 2
            }
        }
        
        return sampleSize
    }
    
    private fun updatePerformanceMetrics(frameTime: Long) {
        totalFrameTime += frameTime
        frameTimeMin = minOf(frameTimeMin, frameTime)
        frameTimeMax = maxOf(frameTimeMax, frameTime)
        
        // 100フレームごとに統計を出力
        if (frameUpdateCount % 100 == 0) {
            val avgFrameTime = totalFrameTime / frameUpdateCount
            
            android.util.Log.i("WidgetAnimationService", "📊 Foreground 5fps Performance Stats:")
            android.util.Log.i("WidgetAnimationService", "  - Frame time: Avg ${avgFrameTime}ms, Max ${frameTimeMax}ms, Min ${frameTimeMin}ms")
            android.util.Log.i("WidgetAnimationService", "  - Total frames: $frameUpdateCount")
            android.util.Log.i("WidgetAnimationService", "  - Target interval: ${ANIMATION_INTERVAL_MS}ms (5fps)")
        }
    }
    
    private fun logFinalStatistics() {
        if (frameUpdateCount > 0) {
            val avgFrameTime = totalFrameTime / frameUpdateCount
            
            android.util.Log.i("WidgetAnimationService", "📊 Final Foreground Animation Statistics:")
            android.util.Log.i("WidgetAnimationService", "  - Total frames: $frameUpdateCount")
            android.util.Log.i("WidgetAnimationService", "  - Average frame time: ${avgFrameTime}ms")
            android.util.Log.i("WidgetAnimationService", "  - Frame time range: ${frameTimeMin}ms - ${frameTimeMax}ms")
            android.util.Log.i("WidgetAnimationService", "  - Target interval: ${ANIMATION_INTERVAL_MS}ms (5fps)")
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("WidgetAnimationService", "🛑 Service destroyed")
        stopAnimation()
        
        // キャッシュをクリーンアップ
        optimizedImageCache.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        optimizedImageCache.clear()
        isCacheReady = false
    }
} 