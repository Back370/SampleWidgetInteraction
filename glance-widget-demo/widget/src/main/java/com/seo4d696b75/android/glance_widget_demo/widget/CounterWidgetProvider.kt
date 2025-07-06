package com.seo4d696b75.android.glance_widget_demo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.RemoteViews
import java.io.File

class CounterWidgetProvider : AppWidgetProvider() {
    
    // アニメーション制御
    private val animationHandler = Handler(Looper.getMainLooper())
    
    // システム制約回避用のAlarmManager
    private var alarmManager: AlarmManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 強制実行用のタイマー
    private var lastExpectedExecutionTime = 0L
    private var missedFrameCount = 0

    // アニメーション監視用のランナブル
    private val animationWatchdog = object : Runnable {
        override fun run() {
            try {
                val currentTime = System.currentTimeMillis()
                android.util.Log.d("WidgetProvider", "=== Animation Watchdog Check ===")
                android.util.Log.d("WidgetProvider", "Total widgets in animation states: ${animationStates.size}")
                android.util.Log.d("WidgetProvider", "Watchdog execution time: $currentTime")
                
                // システム情報をログ出力
                logSystemInfo()
                
                // メモリ使用量をログ出力
                logMemoryUsage()
                
                // Handler の詳細状態を確認
                logHandlerStatus()
                
                // システム制約の検出
                detectSystemConstraints(currentTime)

                animationStates.entries.forEach { (widgetId, state) ->
                    android.util.Log.d("WidgetProvider", "--- Widget $widgetId Status ---")
                    android.util.Log.d("WidgetProvider", "  isAnimating: ${state.isAnimating}")
                    android.util.Log.d("WidgetProvider", "  currentFrame: ${state.currentFrame}")
                    android.util.Log.d("WidgetProvider", "  frameDelay: ${state.frameDelay}ms")
                    android.util.Log.d("WidgetProvider", "  frameUpdateCount: ${state.frameUpdateCount}")
                    android.util.Log.d("WidgetProvider", "  lastFrameTime: ${state.lastFrameTime}")
                    android.util.Log.d("WidgetProvider", "  animationRunnable exists: ${state.animationRunnable != null}")
                    
                    // 最後のフレーム更新からの経過時間を確認
                    if (state.lastFrameTime > 0) {
                        val timeSinceLastFrame = currentTime - state.lastFrameTime
                        android.util.Log.d("WidgetProvider", "  Time since last frame: ${timeSinceLastFrame}ms")
                        
                        // 予想よりも長い間隔でフレームが更新されていない場合は警告
                        if (timeSinceLastFrame > state.frameDelay * 3 && state.isAnimating) {
                            android.util.Log.w(
                                "WidgetProvider", 
                                "  ⚠️ STALLED ANIMATION: Expected max ${state.frameDelay * 3}ms, actual ${timeSinceLastFrame}ms"
                            )
                            
                            // システム制約回避を試行
                            trySystemConstraintWorkaround(widgetId, state)
                        }
                    }
                    
                    if (state.isAnimating && imageFilePaths.size >= TOTAL_FRAMES) {
                        // アニメーション健全性チェック
                        val currentRunnable = state.animationRunnable // ローカル変数にコピー
                        if (currentRunnable != null) {
                            android.util.Log.d("WidgetProvider", "  Animation appears healthy")
                            
                            // Handler queue の状況を確認
                            val hasMessages = animationHandler.hasMessages(0)
                            val hasCallbacks = animationHandler.hasCallbacks(currentRunnable)
                            android.util.Log.d("WidgetProvider", "  Handler has pending messages: $hasMessages")
                            android.util.Log.d("WidgetProvider", "  Handler has this callback: $hasCallbacks")
                            
                        } else {
                            android.util.Log.w("WidgetProvider", "  ⚠️ Animation runnable is null - RESTARTING")
                            restartAnimationForWidget(widgetId)
                        }
                    } else if (state.isAnimating && imageFilePaths.size < TOTAL_FRAMES) {
                        android.util.Log.w("WidgetProvider", "  ⚠️ Animation marked as running but insufficient images (${imageFilePaths.size}/${TOTAL_FRAMES})")
                        state.isAnimating = false
                    }
                }
                
                // 外部ストレージの状況を確認
                logExternalStorageStatus()

                // 次回のwatchdog実行をスケジュール
                val nextCheckDelay = 8000L // 8秒間隔
                val watchdogPostResult = animationHandler.postDelayed(this, nextCheckDelay)
                android.util.Log.d("WidgetProvider", "Next watchdog check scheduled in ${nextCheckDelay}ms - Success: $watchdogPostResult")
                
                if (!watchdogPostResult) {
                    android.util.Log.e("WidgetProvider", "❌ CRITICAL: Watchdog scheduling failed - Handler may be constrained")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("WidgetProvider", "❌ Critical error in animation watchdog", e)
                // エラーが発生してもwatchdogを継続（長めの間隔で）
                val fallbackPostResult = animationHandler.postDelayed(this, 15000L)
                android.util.Log.w("WidgetProvider", "Fallback watchdog scheduled - Success: $fallbackPostResult")
            }
        }
    }
    
    // Handler の状態を詳細にログ出力
    private fun logHandlerStatus() {
        try {
            val handlerLooper = animationHandler.looper
            val handlerThread = handlerLooper.thread
            
            android.util.Log.d("WidgetProvider", "Handler Status:")
            android.util.Log.d("WidgetProvider", "  Thread alive: ${handlerThread.isAlive}")
            android.util.Log.d("WidgetProvider", "  Thread state: ${handlerThread.state}")
            android.util.Log.d("WidgetProvider", "  Thread name: ${handlerThread.name}")
            android.util.Log.d("WidgetProvider", "  Thread priority: ${handlerThread.priority}")
            android.util.Log.d("WidgetProvider", "  Looper thread: ${handlerLooper.thread}")
            android.util.Log.d("WidgetProvider", "  Is main thread: ${handlerLooper.thread == Looper.getMainLooper().thread}")
            
            // Handler に残っているメッセージ数を確認
            val hasAnyMessages = animationHandler.hasMessages(0)
            android.util.Log.d("WidgetProvider", "  Has any messages: $hasAnyMessages")
            
            // システム制約情報を追加
            android.util.Log.d("WidgetProvider", "  Last expected execution: $lastExpectedExecutionTime")
            android.util.Log.d("WidgetProvider", "  Missed frame count: $missedFrameCount")
            android.util.Log.d("WidgetProvider", "  Current system time: ${System.currentTimeMillis()}")
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Failed to log handler status", e)
        }
    }
    
    // システム制約の検出
    private fun detectSystemConstraints(currentTime: Long) {
        android.util.Log.d("WidgetProvider", "🔍 Detecting system constraints")
        
        // 最後の予想実行時刻をチェック
        if (lastExpectedExecutionTime > 0) {
            val delayFromExpected = currentTime - lastExpectedExecutionTime
            if (delayFromExpected > 1000) { // 1秒以上の遅延
                missedFrameCount++
                android.util.Log.w(
                    "WidgetProvider", 
                    "⚠️ SYSTEM CONSTRAINT DETECTED: Expected execution ${delayFromExpected}ms ago, missed frames: $missedFrameCount"
                )
            }
        }
        
        // 連続してフレームが失敗した場合は強制対策
        if (missedFrameCount >= 3) {
            android.util.Log.e("WidgetProvider", "🚨 CRITICAL: Multiple missed frames detected - applying workarounds")
            applySystemConstraintWorkarounds()
        }
    }
    
    // システム制約回避の試行
    private fun trySystemConstraintWorkaround(widgetId: Int, state: AnimationState) {
        android.util.Log.w("WidgetProvider", "🔧 Trying system constraint workaround for widget $widgetId")
        
        try {
            // 方法1: より短い間隔でのスケジューリング
            val shorterDelay = maxOf(state.frameDelay / 2, 40L) // 最短40ms
            val quickRunnable = Runnable {
                android.util.Log.d("WidgetProvider", "✅ Quick workaround execution for widget $widgetId")
                // 元のアニメーションrunnableを強制実行
                state.animationRunnable?.run()
            }
            
            val quickPostResult = animationHandler.postDelayed(quickRunnable, shorterDelay)
            android.util.Log.d("WidgetProvider", "  Quick scheduling result: $quickPostResult, delay: ${shorterDelay}ms")
            
            // 方法2: 即座実行
            val immediateRunnable = Runnable {
                android.util.Log.d("WidgetProvider", "✅ Immediate workaround execution for widget $widgetId")
                state.animationRunnable?.run()
            }
            
            val immediatePostResult = animationHandler.post(immediateRunnable)
            android.util.Log.d("WidgetProvider", "  Immediate scheduling result: $immediatePostResult")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "Failed system constraint workaround", e)
        }
    }
    
    // システム制約の総合的な回避策
    private fun applySystemConstraintWorkarounds() {
        android.util.Log.w("WidgetProvider", "🔧 Applying comprehensive system constraint workarounds")
        
        try {
            // 1. 全アニメーションを一時停止して再開
            android.util.Log.d("WidgetProvider", "  Restarting all animations...")
            val activeWidgets = animationStates.filter { it.value.isAnimating }
            
            activeWidgets.forEach { (widgetId, state) ->
                android.util.Log.d("WidgetProvider", "  Restarting widget $widgetId")
                
                // 新しいRunnableを作成
                val restartRunnable = Runnable {
                    android.util.Log.d("WidgetProvider", "✅ Constraint workaround restart for widget $widgetId")
                    restartAnimationForWidget(widgetId)
                }
                
                // 複数の時間間隔で実行を試行
                animationHandler.post(restartRunnable)
                animationHandler.postDelayed(restartRunnable, 100L)
                animationHandler.postDelayed(restartRunnable, 500L)
            }
            
            // 2. カウンターリセット
            missedFrameCount = 0
            lastExpectedExecutionTime = 0L
            
            android.util.Log.d("WidgetProvider", "✅ Workarounds applied")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "Failed to apply workarounds", e)
        }
    }

    // 新しいデバッグ用のメソッド
    private fun logSystemInfo() {
        try {
            val runtime = Runtime.getRuntime()
            val availableProcessors = runtime.availableProcessors()
            android.util.Log.d("WidgetProvider", "System - Available processors: $availableProcessors")
            
            // バッテリー最適化などの情報は直接取得できないので、Handler の動作状況で判断
            val currentTime = System.currentTimeMillis()
            android.util.Log.d("WidgetProvider", "System - Current time: $currentTime")
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Failed to log system info", e)
        }
    }

    private fun logMemoryUsage() {
        try {
            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val maxMemory = runtime.maxMemory()
            
            android.util.Log.d("WidgetProvider", "Memory - Total: ${totalMemory / 1024 / 1024}MB")
            android.util.Log.d("WidgetProvider", "Memory - Used: ${usedMemory / 1024 / 1024}MB")
            android.util.Log.d("WidgetProvider", "Memory - Free: ${freeMemory / 1024 / 1024}MB")
            android.util.Log.d("WidgetProvider", "Memory - Max: ${maxMemory / 1024 / 1024}MB")
            android.util.Log.d("WidgetProvider", "Memory - Image cache size: ${imageCache.size}")
            
            // キャッシュされた画像のサイズ情報
            var totalCacheSize = 0
            imageCache.forEach { (index, bitmap) ->
                if (!bitmap.isRecycled) {
                    val bitmapSize = bitmap.allocationByteCount
                    totalCacheSize += bitmapSize
                    android.util.Log.v("WidgetProvider", "  Cache[$index]: ${bitmapSize / 1024}KB")
                } else {
                    android.util.Log.w("WidgetProvider", "  Cache[$index]: RECYCLED!")
                }
            }
            android.util.Log.d("WidgetProvider", "Memory - Total cache size: ${totalCacheSize / 1024}KB")
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Failed to log memory usage", e)
        }
    }

    private fun logExternalStorageStatus() {
        try {
            android.util.Log.d("WidgetProvider", "External Storage - Image paths count: ${imageFilePaths.size}")
            
            // いくつかの画像ファイルの存在確認
            val sampleCount = minOf(5, imageFilePaths.size)
            for (i in 0 until sampleCount) {
                val path = imageFilePaths[i]
                val file = File(path)
                android.util.Log.d("WidgetProvider", "  Sample[$i]: exists=${file.exists()}, size=${file.length()}B")
            }
            
            // 外部ストレージの状態
            val context = null // Context取得が必要な場合は後で追加
            // android.os.Environment.getExternalStorageState() などでストレージ状態を確認
            
        } catch (e: Exception) {
            android.util.Log.w("WidgetProvider", "Failed to log external storage status", e)
        }
    }

    // 画像キャッシュ
    private val imageCache = mutableMapOf<Int, Bitmap>()

    // アニメーション状態管理
    private val animationStates = mutableMapOf<Int, AnimationState>()

    // 外部ストレージの画像ファイルパスのキャッシュ
    private val imageFilePaths = mutableListOf<String>()

    companion object {
        // アニメーション設定
        private const val ANIMATION_FRAME_DELAY = 80L // 80ms間隔（より滑らかなアニメーション）
        private const val TOTAL_FRAMES = 50 // 50枚の画像
        private const val MAX_CACHE_SIZE = 20 // キャッシュする画像の最大数

        // 外部ストレージのフォルダパス
        private const val EXTERNAL_FOLDER_NAME = "WidgetImages"
    }
    
    // アニメーション状態を管理するデータクラス
    private data class AnimationState(
        var isAnimating: Boolean = false,
        var currentFrame: Int = 0,
        var animationRunnable: Runnable? = null,
        var frameDelay: Long = ANIMATION_FRAME_DELAY,
        var lastFrameTime: Long = 0L, // デバッグ用：最後にフレームが更新された時刻
        var frameUpdateCount: Long = 0L // デバッグ用：フレーム更新回数
    )

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        android.util.Log.d("WidgetProvider", "=== onReceive Called ===")
        android.util.Log.d("WidgetProvider", "Intent action: ${intent.action}")
        android.util.Log.d("WidgetProvider", "Intent extras: ${intent.extras}")
        
        // 強制更新フラグのチェック
        val forceRestart = intent.getBooleanExtra("FORCE_ANIMATION_RESTART", false)
        val forceUpdateTime = intent.getLongExtra("FORCE_UPDATE_TIME", 0L)
        val restartAttempt = intent.getIntExtra("RESTART_ATTEMPT", 0)
        
        // システム制約回避フラグのチェック
        val systemConstraintWorkaround = intent.getBooleanExtra("SYSTEM_CONSTRAINT_WORKAROUND", false)
        val forceMultipleExecution = intent.getBooleanExtra("FORCE_MULTIPLE_EXECUTION", false)
        val workaroundTime = intent.getLongExtra("WORKAROUND_TIME", 0L)
        val attemptNumber = intent.getIntExtra("ATTEMPT_NUMBER", 0)
        val delayInterval = intent.getLongExtra("DELAY_INTERVAL", 0L)
        
        // 緊急強制再開フラグのチェック
        val emergencyForceRestart = intent.getBooleanExtra("EMERGENCY_FORCE_RESTART", false)
        val emergencyTime = intent.getLongExtra("EMERGENCY_TIME", 0L)
        val emergencyAttempt = intent.getIntExtra("EMERGENCY_ATTEMPT", 0)
        val clearAllCallbacks = intent.getBooleanExtra("CLEAR_ALL_CALLBACKS", false)
        val forceMemoryCleanup = intent.getBooleanExtra("FORCE_MEMORY_CLEANUP", false)
        
        if (emergencyForceRestart) {
            android.util.Log.d("WidgetProvider", "🚨 EMERGENCY FORCE RESTART requested")
            android.util.Log.d("WidgetProvider", "  Emergency time: $emergencyTime")
            android.util.Log.d("WidgetProvider", "  Emergency attempt: $emergencyAttempt")
            android.util.Log.d("WidgetProvider", "  Clear all callbacks: $clearAllCallbacks")
            android.util.Log.d("WidgetProvider", "  Force memory cleanup: $forceMemoryCleanup")
            
            // 最も積極的な回復処理
            performEmergencyRecovery(context, intent, clearAllCallbacks, forceMemoryCleanup)
            
        } else if (systemConstraintWorkaround) {
            android.util.Log.d("WidgetProvider", "🔧 System constraint workaround requested")
            android.util.Log.d("WidgetProvider", "  Workaround time: $workaroundTime")
            android.util.Log.d("WidgetProvider", "  Attempt number: $attemptNumber")
            android.util.Log.d("WidgetProvider", "  Delay interval: $delayInterval")
            android.util.Log.d("WidgetProvider", "  Force multiple execution: $forceMultipleExecution")
            
            // システム制約回避の実行
            applySystemConstraintWorkarounds()
            
            // さらに強制的なアニメーション再開
            val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
            if (widgetIds != null) {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                widgetIds.forEach { widgetId ->
                    android.util.Log.d("WidgetProvider", "  Applying constraint workaround for widget $widgetId")
                    forceRestartAnimation(context, appWidgetManager, widgetId)
                }
            }
            
        } else if (forceRestart || restartAttempt > 0) {
            android.util.Log.d("WidgetProvider", "🔄 Force restart requested - forceRestart: $forceRestart, attempt: $restartAttempt")
            android.util.Log.d("WidgetProvider", "Force update time: $forceUpdateTime")
            
            // すべてのアニメーションを強制停止
            forceStopAllAnimations()
            
            // 少し待ってからアニメーションを再開
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                
                if (widgetIds != null) {
                    android.util.Log.d("WidgetProvider", "Force restarting animations for widgets: ${widgetIds.contentToString()}")
                    widgetIds.forEach { widgetId ->
                        forceRestartAnimation(context, appWidgetManager, widgetId)
                    }
                }
            }, 100L)
        }
    }
    
    // 強制的にすべてのアニメーションを停止
    private fun forceStopAllAnimations() {
        android.util.Log.d("WidgetProvider", "🛑 Force stopping all animations")
        
        animationStates.entries.forEach { (widgetId, state) ->
            android.util.Log.d("WidgetProvider", "  Stopping animation for widget $widgetId")
            state.isAnimating = false
            val currentRunnable = state.animationRunnable // ローカル変数にコピー
            if (currentRunnable != null) {
                animationHandler.removeCallbacks(currentRunnable)
            }
            state.animationRunnable = null
        }
        
        // Handler からすべてのコールバックを削除
        animationHandler.removeCallbacksAndMessages(null)
        android.util.Log.d("WidgetProvider", "✅ All animations force stopped")
    }
    
    // 強制的にアニメーションを再開
    private fun forceRestartAnimation(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int) {
        android.util.Log.d("WidgetProvider", "🚀 Force restarting animation for widget $widgetId")
        
        try {
            // アニメーション状態を初期化
            val newState = AnimationState()
            animationStates[widgetId] = newState
            
            // 画像パスを再初期化
            initializeImageFilePaths(context)
            
            // アニメーションを開始
            startAnimation(context, appWidgetManager, widgetId)
            
            android.util.Log.d("WidgetProvider", "✅ Force restart completed for widget $widgetId")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to force restart animation for widget $widgetId", e)
        }
    }

    //AppWidgetProviderクラスのouUpdate関数をオーバーライド
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        android.util.Log.d("WidgetProvider", "=== Widget onUpdate Called ===")
        android.util.Log.d("WidgetProvider", "Updating ${appWidgetIds.size} widgets")
        android.util.Log.d("WidgetProvider", "Current time: ${System.currentTimeMillis()}")

        // 毎回画像ファイルパスを再初期化（新しい画像を検出するため）
        val previousImageCount = imageFilePaths.size
        imageFilePaths.clear()
        initializeImageFilePaths(context)
        
        android.util.Log.d("WidgetProvider", "Image count changed: $previousImageCount -> ${imageFilePaths.size}")

        for (appWidgetId in appWidgetIds) {
            android.util.Log.d("WidgetProvider", "Updating widget ID: $appWidgetId")
            
            // 現在のアニメーション状態をチェック
            val currentState = animationStates[appWidgetId]
            android.util.Log.d("WidgetProvider", "  Current animation state: isAnimating=${currentState?.isAnimating}, updates=${currentState?.frameUpdateCount}")
            
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }

        // アニメーション監視を開始/再開
        android.util.Log.d("WidgetProvider", "Starting/restarting animation watchdog")
        animationHandler.removeCallbacks(animationWatchdog)
        animationHandler.postDelayed(animationWatchdog, 3000L) // 3秒後に最初のチェック

        android.util.Log.d("WidgetProvider", "Widget update completed")
    }

    private fun initializeImageFilePaths(context: Context) {
        // 外部ストレージのディレクトリを取得
        val baseDir = context.getExternalFilesDir(null)
        android.util.Log.d("WidgetProvider", "Base external files dir: ${baseDir?.absolutePath}")

        if (baseDir == null) {
            android.util.Log.e(
                "WidgetProvider",
                "External files directory is null - external storage not available"
            )
            return
        }

        val externalDir = File(baseDir, EXTERNAL_FOLDER_NAME)
        android.util.Log.d("WidgetProvider", "Target directory: ${externalDir.absolutePath}")
        android.util.Log.d("WidgetProvider", "Base directory exists: ${baseDir.exists()}")
        android.util.Log.d("WidgetProvider", "Base directory readable: ${baseDir.canRead()}")
        android.util.Log.d("WidgetProvider", "Base directory writable: ${baseDir.canWrite()}")

        // ディレクトリが存在しない場合は作成
        if (!externalDir.exists()) {
            android.util.Log.d(
                "WidgetProvider",
                "Directory does not exist, attempting to create..."
            )
            try {
                val created = externalDir.mkdirs()
                android.util.Log.d("WidgetProvider", "Directory creation result: $created")
                if (created) {
                    android.util.Log.i(
                        "WidgetProvider",
                        "Successfully created directory: ${externalDir.absolutePath}"
                    )
                } else {
                    android.util.Log.e(
                        "WidgetProvider",
                        "Failed to create directory: ${externalDir.absolutePath}"
                    )
                    // 親ディレクトリの状態を確認
                    android.util.Log.d(
                        "WidgetProvider",
                        "Parent directory exists: ${externalDir.parentFile?.exists()}"
                    )
                    android.util.Log.d(
                        "WidgetProvider",
                        "Parent directory writable: ${externalDir.parentFile?.canWrite()}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "WidgetProvider",
                    "Exception creating directory: ${e.message}",
                    e
                )
            }
        } else {
            android.util.Log.d("WidgetProvider", "Directory already exists")
        }

        // 作成後の状態を確認
        android.util.Log.d("WidgetProvider", "Final directory exists: ${externalDir.exists()}")
        android.util.Log.d(
            "WidgetProvider",
            "Final directory is directory: ${externalDir.isDirectory}"
        )

        if (externalDir.exists() && externalDir.isDirectory) {
            // ディレクトリ内のすべてのファイルを確認
            val allFiles = externalDir.listFiles()
            android.util.Log.d("WidgetProvider", "Total files in directory: ${allFiles?.size ?: 0}")

            allFiles?.forEach { file ->
                android.util.Log.d(
                    "WidgetProvider",
                    "Found file: ${file.name}, extension: ${file.extension.lowercase()}, isFile: ${file.isFile}"
                )
            }

            // ディレクトリ内の画像ファイルを取得（番号順にソート）
            val imageFiles = externalDir.listFiles { file ->
                val isImageFile = file.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp")
                android.util.Log.d(
                    "WidgetProvider",
                    "Checking file: ${file.name}, isImageFile: $isImageFile"
                )
                isImageFile
            }?.sortedBy { it.name } ?: emptyList()

            android.util.Log.d("WidgetProvider", "Found ${imageFiles.size} image files")

            // ファイルパスをリストに追加
            imageFiles.forEach { file ->
                imageFilePaths.add(file.absolutePath)
                android.util.Log.d(
                    "WidgetProvider",
                    "Added image: ${file.name} -> ${file.absolutePath}"
                )
            }
        } else {
            android.util.Log.w(
                "WidgetProvider",
                "Directory still does not exist or is not a directory"
            )
        }

        // 最終的な状態をログ出力
        android.util.Log.i("WidgetProvider", "imageFilePaths size: ${imageFilePaths.size}")
        imageFilePaths.forEachIndexed { index, path ->
            android.util.Log.i("WidgetProvider", "Image $index: $path")
        }

        // 画像が見つからない場合はログ出力（デバッグ用）＋ユーザー通知
        if (imageFilePaths.isEmpty()) {
            android.util.Log.w(
                "WidgetProvider",
                "No images found in external storage: ${externalDir.absolutePath}"
            )
            android.util.Log.i(
                "WidgetProvider",
                "Please add image files (jpg, png, webp) to: ${externalDir.absolutePath}"
            )
        } else {
            android.util.Log.i(
                "WidgetProvider",
                "Initialized ${imageFilePaths.size} images for animation"
            )
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        android.util.Log.v("WidgetProvider", "🔄 Updating widget $appWidgetId")
        
        val views = RemoteViews(context.packageName, R.layout.widget_layout_wall)

        // アニメーション状態を初期化
        if (!animationStates.containsKey(appWidgetId)) {
            android.util.Log.d("WidgetProvider", "  Initializing animation state for widget $appWidgetId")
            animationStates[appWidgetId] = AnimationState()
        }

        val animationState = animationStates[appWidgetId]
        android.util.Log.v("WidgetProvider", "  Animation state: isAnimating=${animationState?.isAnimating}, frame=${animationState?.currentFrame}")

        // 画像が外部ストレージに見つかった場合のみ設定、なければ予備画像表示
        if (imageFilePaths.isNotEmpty()) {
            android.util.Log.v("WidgetProvider", "  External images available: ${imageFilePaths.size}")
            
            if (animationState?.isAnimating == true) {
                // アニメーション中は現在のフレームを表示
                val currentFrame = animationState.currentFrame
                android.util.Log.v("WidgetProvider", "  Showing animation frame: $currentFrame")
                setImageFromExternalStorage(views, currentFrame)
            } else {
                // アニメーションが停止中は最初の画像を表示
                android.util.Log.v("WidgetProvider", "  Showing static image (frame 0)")
                setImageFromExternalStorage(views, 0)
                if (imageFilePaths.size < TOTAL_FRAMES) {
                    android.util.Log.d(
                        "WidgetProvider",
                        "  Showing static image - waiting for ${TOTAL_FRAMES} images (current: ${imageFilePaths.size})"
                    )
                }
            }
        } else {
            // 外部画像が見つからない場合は透明背景を設定
            android.util.Log.w("WidgetProvider", "  No external images found; using transparent background")
            views.setImageViewResource(R.id.background_image, android.R.color.transparent)
        }

        // ウィジェットを更新
        try {
            appWidgetManager.updateAppWidget(appWidgetId, views)
            android.util.Log.v("WidgetProvider", "  ✅ Widget $appWidgetId updated successfully")
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to update widget $appWidgetId", e)
        }

        // アニメーションがまだ開始されていない場合は開始を試行
        if (!(animationStates[appWidgetId]?.isAnimating ?: false)) {
            android.util.Log.v("WidgetProvider", "  Animation not running, attempting to start...")
            startAnimation(context, appWidgetManager, appWidgetId)
        } else {
            android.util.Log.v("WidgetProvider", "  Animation already running")
        }
    }
    
    private fun startAnimation(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val animationState = animationStates[appWidgetId] ?: AnimationState()

        android.util.Log.d("WidgetProvider", "=== Starting Animation for Widget $appWidgetId ===")
        android.util.Log.d("WidgetProvider", "Current animation state: isAnimating=${animationState.isAnimating}")
        android.util.Log.d("WidgetProvider", "Available images: ${imageFilePaths.size}")
        android.util.Log.d("WidgetProvider", "Required images: $TOTAL_FRAMES")

        // システム制約情報をリセット
        missedFrameCount = 0
        lastExpectedExecutionTime = 0L
        android.util.Log.d("WidgetProvider", "System constraint counters reset")

        // 外部画像が50枚未満の場合はアニメーションを開始しない
        if (imageFilePaths.size < TOTAL_FRAMES) {
            android.util.Log.w(
                "WidgetProvider",
                "❌ Animation not started - need $TOTAL_FRAMES images, found ${imageFilePaths.size}"
            )
            animationState.isAnimating = false
            animationStates[appWidgetId] = animationState
            return
        }

        android.util.Log.d("WidgetProvider", "✅ Sufficient images found, starting animation")
        animationState.isAnimating = true
        animationState.frameUpdateCount = 0
        animationState.lastFrameTime = System.currentTimeMillis()

        // 外部画像がある場合は外部画像数を、ない場合は固定フレーム数を使用
        val totalFrames = if (imageFilePaths.isNotEmpty()) {
            imageFilePaths.size
        } else {
            TOTAL_FRAMES // フォールバック用の固定フレーム数
        }

        android.util.Log.d("WidgetProvider", "Images available: ${imageFilePaths.size}")
        android.util.Log.d("WidgetProvider", "Total frames for animation: $totalFrames")

        val animationRunnable = object : Runnable {
            override fun run() {
                try {
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastFrame = currentTime - animationState.lastFrameTime
                    
                    android.util.Log.v(
                        "WidgetProvider",
                        "🎬 Animation frame update - Widget: $appWidgetId, Frame: ${animationState.currentFrame}, Time since last: ${timeSinceLastFrame}ms"
                    )
                    
                    // 遅延の異常を検出
                    if (timeSinceLastFrame > animationState.frameDelay * 2) {
                        android.util.Log.w(
                            "WidgetProvider",
                            "⚠️ DELAY DETECTED: Expected ${animationState.frameDelay}ms, actual ${timeSinceLastFrame}ms"
                        )
                    }
                    
                    // アニメーション状態の詳細チェック
                    android.util.Log.v("WidgetProvider", "  isAnimating: ${animationState.isAnimating}")
                    android.util.Log.v("WidgetProvider", "  currentFrame: ${animationState.currentFrame}")
                    android.util.Log.v("WidgetProvider", "  frameUpdateCount: ${animationState.frameUpdateCount}")
                    android.util.Log.v("WidgetProvider", "  frameDelay: ${animationState.frameDelay}ms")

                    if (animationState.isAnimating) {
                        // 実行時に画像が削除された場合の安全チェック
                        if (imageFilePaths.size < TOTAL_FRAMES) {
                            android.util.Log.e(
                                "WidgetProvider",
                                "❌ Animation stopped - images reduced from $totalFrames to ${imageFilePaths.size}"
                            )
                            animationState.isAnimating = false
                            return
                        }

                        // フレームを進める（ループ処理）
                        animationState.currentFrame = (animationState.currentFrame + 1) % totalFrames
                        animationState.frameUpdateCount++
                        animationState.lastFrameTime = currentTime

                        android.util.Log.v(
                            "WidgetProvider",
                            "  ➡️ Advanced to frame: ${animationState.currentFrame + 1}/$totalFrames (Update #${animationState.frameUpdateCount})"
                        )

                        // ウィジェットを更新（エラーが発生してもアニメーションは継続）
                        try {
                            android.util.Log.v("WidgetProvider", "  🔄 Updating widget display...")
                            updateAppWidget(context, appWidgetManager, appWidgetId)
                            android.util.Log.v("WidgetProvider", "  ✅ Widget display updated successfully")
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "WidgetProvider",
                                "❌ Error updating widget frame ${animationState.currentFrame}, but continuing animation",
                                e
                            )
                        }

                        // 次のフレームをスケジュール（動的な遅延時間を使用）
                        if (animationState.isAnimating) {
                            // Handler の現在の状態を確認
                            val handlerLooper = animationHandler.looper
                            val handlerThread = handlerLooper.thread
                            
                            android.util.Log.v("WidgetProvider", "  Handler thread alive: ${handlerThread.isAlive}")
                            android.util.Log.v("WidgetProvider", "  Handler thread state: ${handlerThread.state}")
                            
                            // thisへの参照を保存（スマートキャストエラーを避けるため）
                            val currentRunnable = this
                            android.util.Log.v("WidgetProvider", "  Handler has callbacks: ${animationHandler.hasCallbacks(currentRunnable)}")
                            
                            // 次のフレームをスケジュール
                            val postResult = animationHandler.postDelayed(this, animationState.frameDelay)
                            android.util.Log.v(
                                "WidgetProvider",
                                "  ⏰ Next frame scheduled in ${animationState.frameDelay}ms - Success: $postResult"
                            )
                            
                            // 追加の健全性チェック: 実際にスケジュールされたかを確認
                            android.util.Log.v("WidgetProvider", "  Handler callbacks after post: ${animationHandler.hasCallbacks(currentRunnable)}")
                            
                            // システム時刻をログに記録（デバッグ用）
                            android.util.Log.v("WidgetProvider", "  Current system time: $currentTime")
                            android.util.Log.v("WidgetProvider", "  Next expected execution: ${currentTime + animationState.frameDelay}")
                            
                            // 次回の予想実行時刻を記録（システム制約検出用）
                            lastExpectedExecutionTime = currentTime + animationState.frameDelay
                            
                            // 積極的制約検出: 予想実行時刻から200ms後にチェック
                            val constraintCheckDelay = animationState.frameDelay + 200L
                            val constraintCheckRunnable = Runnable {
                                val checkTime = System.currentTimeMillis()
                                if (checkTime > lastExpectedExecutionTime + 150L) {
                                    android.util.Log.w(
                                        "WidgetProvider",
                                        "🚨 IMMEDIATE CONSTRAINT DETECTED: Frame should have executed ${checkTime - lastExpectedExecutionTime}ms ago"
                                    )
                                    
                                    // 即座にシステム制約回避を実行
                                    trySystemConstraintWorkaround(appWidgetId, animationState)
                                }
                            }
                            animationHandler.postDelayed(constraintCheckRunnable, constraintCheckDelay)
                            
                        } else {
                            android.util.Log.w(
                                "WidgetProvider",
                                "  ⚠️ Animation stopped - not scheduling next frame"
                            )
                        }
                    } else {
                        android.util.Log.w(
                            "WidgetProvider",
                            "❌ Animation runnable called but isAnimating is false - stopping execution"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "WidgetProvider",
                        "❌ Critical error in animation runnable for widget $appWidgetId - attempting to continue",
                        e
                    )
                    // エラーが発生してもアニメーションを継続しようと試みる
                    if (animationState.isAnimating) {
                        val recoveryDelay = animationState.frameDelay * 2
                        val recoveryResult = animationHandler.postDelayed(this, recoveryDelay)
                        android.util.Log.w(
                            "WidgetProvider",
                            "🔄 Attempting recovery in ${recoveryDelay}ms - Success: $recoveryResult"
                        )
                    }
                }
            }
        }

        //現在の状態をアニメーションステイトに格納
        animationState.animationRunnable = animationRunnable
        animationStates[appWidgetId] = animationState

        // アニメーション開始
        android.util.Log.d("WidgetProvider", "🚀 Posting animation runnable to Handler")
        animationHandler.post(animationRunnable)
        android.util.Log.d("WidgetProvider", "✅ Animation started for widget $appWidgetId")
    }

    //letは{}の処理をanimationStateを引数にとりながら処理できる
    //isAnimatingがtrueの時だけrunを実行
    private fun stopAnimation(appWidgetId: Int) {
        android.util.Log.d("WidgetProvider", "🛑 Stopping animation for widget $appWidgetId")
        
        val animationState = animationStates[appWidgetId]
        animationState?.let { state ->
            android.util.Log.d("WidgetProvider", "  Animation was running: ${state.isAnimating}")
            android.util.Log.d("WidgetProvider", "  Total frame updates: ${state.frameUpdateCount}")
            android.util.Log.d("WidgetProvider", "  Last frame time: ${state.lastFrameTime}")
            
            state.isAnimating = false
            val currentRunnable = state.animationRunnable // ローカル変数にコピー
            if (currentRunnable != null) {
                android.util.Log.d("WidgetProvider", "  Removing runnable from Handler")
                animationHandler.removeCallbacks(currentRunnable)
            }
            state.animationRunnable = null
            
            android.util.Log.d("WidgetProvider", "✅ Animation stopped for widget $appWidgetId")
        } ?: run {
            android.util.Log.w("WidgetProvider", "⚠️ No animation state found for widget $appWidgetId")
        }
    }

    private fun restartAnimationForWidget(widgetId: Int) {
        try {
            android.util.Log.d("WidgetProvider", "🔄 Restarting animation for widget $widgetId")
            val oldState = animationStates[widgetId]
            android.util.Log.d("WidgetProvider", "  Previous state: isAnimating=${oldState?.isAnimating}, updates=${oldState?.frameUpdateCount}")
            
            stopAnimation(widgetId)
            
            // animationStateをリセット
            val resetState = AnimationState()
            animationStates[widgetId] = resetState
            
            android.util.Log.d("WidgetProvider", "  Animation state reset for widget $widgetId")
            // AnimationRunnableがnull状態で、onUpdate時か次のuserイベント時に再駆動される
            
        } catch (e: Exception) {
            android.util.Log.e(
                "WidgetProvider",
                "❌ Failed to restart animation for widget $widgetId",
                e
            )
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
            android.util.Log.d("WidgetProvider", "🧹 Optimizing memory usage - Cache size: ${imageCache.size}")
            
            // 保存できる量を超えた古いキャッシュエントリを削除
            val entriesToRemove = imageCache.entries.take(imageCache.size - MAX_CACHE_SIZE)
            entriesToRemove.forEach { (key, bitmap) ->
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
                imageCache.remove(key)
                android.util.Log.v("WidgetProvider", "  Removed cache entry for frame $key")
            }
            
            android.util.Log.d("WidgetProvider", "  ✅ Memory optimization completed - New cache size: ${imageCache.size}")
        }
    }

    private fun setImageFromExternalStorage(views: RemoteViews, frame: Int) {
        android.util.Log.v("WidgetProvider", "📸 Setting image for frame $frame")
        
        if (imageFilePaths.isEmpty()) {
            android.util.Log.w("WidgetProvider", "❌ No images available for frame $frame")
            views.setImageViewResource(R.id.background_image, android.R.color.transparent)
            return
        }

        // 利用可能な画像数でフレームインデックスを適切に循環させる
        val index = frame % imageFilePaths.size

        android.util.Log.v(
            "WidgetProvider",
            "  Frame $frame -> Index $index (total: ${imageFilePaths.size})"
        )

        try {
            val imagePath = imageFilePaths[index]
            android.util.Log.v("WidgetProvider", "  Image path: $imagePath")

            // ファイルの存在確認
            val imageFile = java.io.File(imagePath)
            val fileExists = imageFile.exists()
            val fileSize = if (fileExists) imageFile.length() else 0L
            
            android.util.Log.v("WidgetProvider", "  File exists: $fileExists, Size: ${fileSize}B")
            
            if (!fileExists || fileSize == 0L) {
                android.util.Log.w(
                    "WidgetProvider",
                    "❌ Image file does not exist or is empty: $imagePath"
                )
                // 前のフレームを試す
                useFallbackImage(views, index)
                return
            }

            // キャッシュから画像を取得、なければファイルから読み込み
            val bitmap = if (imageCache.containsKey(index)) {
                android.util.Log.v("WidgetProvider", "  ✅ Using cached image for index $index")
                imageCache[index]
            } else {
                android.util.Log.v("WidgetProvider", "  📁 Loading image from file for index $index")
                loadImageFromFile(imagePath, index)
            }

            bitmap?.let {
                if (!it.isRecycled) {
                    views.setImageViewBitmap(R.id.background_image, it)
                    android.util.Log.v(
                        "WidgetProvider",
                        "  ✅ Successfully set image for frame $frame (index $index) - Size: ${it.width}x${it.height}"
                    )
                } else {
                    android.util.Log.w(
                        "WidgetProvider",
                        "  ❌ Bitmap is recycled for frame $frame (index $index)"
                    )
                    // キャッシュから削除して再読み込み
                    imageCache.remove(index)
                    useFallbackImage(views, index)
                }
            } ?: run {
                android.util.Log.e(
                    "WidgetProvider",
                    "❌ Failed to load bitmap for frame $frame (index $index)"
                )
                useFallbackImage(views, index)
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "WidgetProvider",
                "❌ Exception loading image for frame $frame (index $index)",
                e
            )
            useFallbackImage(views, index)
        }
    }

    private fun loadImageFromFile(imagePath: String, frame: Int): Bitmap? {
        android.util.Log.v("WidgetProvider", "📁 Loading image from file: $imagePath")
        
        return try {
            val startTime = System.currentTimeMillis()
            val file = java.io.File(imagePath)
            
            android.util.Log.v("WidgetProvider", "  File size: ${file.length()}B")
            
            // ファイル存在の再確認
            if (!file.exists()) {
                android.util.Log.w("WidgetProvider", "  ❌ File disappeared during loading: $imagePath")
                return null
            }
            
            // タイムアウト機能付きの画像読み込み
            val bitmap = loadImageWithTimeout(imagePath, 2000L) // 2秒タイムアウト
            val loadTime = System.currentTimeMillis() - startTime
            
            if (bitmap != null) {
                android.util.Log.v(
                    "WidgetProvider",
                    "  ✅ Image loaded successfully in ${loadTime}ms - Size: ${bitmap.width}x${bitmap.height}, Bytes: ${bitmap.allocationByteCount}"
                )
                
                // キャッシュに保存（メモリ最適化後）
                optimizeMemoryUsage()
                imageCache[frame] = bitmap
                android.util.Log.v("WidgetProvider", "  📦 Image cached for frame $frame")
                
            } else {
                android.util.Log.w(
                    "WidgetProvider",
                    "  ❌ BitmapFactory.decodeFile returned null or timed out for: $imagePath (${loadTime}ms)"
                )
            }

            bitmap
        } catch (e: OutOfMemoryError) {
            android.util.Log.e("WidgetProvider", "❌ Out of memory loading image: $imagePath", e)
            // メモリ不足の場合は緊急キャッシュクリア
            emergencyMemoryCleanup()
            null
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "❌ Failed to load image: $imagePath", e)
            null
        }
    }
    
    // タイムアウト機能付き画像読み込み
    private fun loadImageWithTimeout(imagePath: String, timeoutMs: Long): Bitmap? {
        android.util.Log.v("WidgetProvider", "  🕐 Loading with ${timeoutMs}ms timeout...")
        
        return try {
            val startTime = System.currentTimeMillis()
            
            // バックグラウンド制限の検出
            val currentTime = System.currentTimeMillis()
            android.util.Log.v("WidgetProvider", "  Background check - Current time: $currentTime")
            
            val options = BitmapFactory.Options().apply {
                // メモリ使用量を抑えるため、適度にサンプリング
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.RGB_565
                // プリサイズを有効にしてメモリ効率を向上
                inJustDecodeBounds = false
                inMutable = false
            }
            
            // 実際の画像読み込み開始時刻をログ
            val decodeStartTime = System.currentTimeMillis()
            android.util.Log.v("WidgetProvider", "  🎯 Starting BitmapFactory.decodeFile at: $decodeStartTime")
            
            val bitmap = BitmapFactory.decodeFile(imagePath, options)
            
            val decodeEndTime = System.currentTimeMillis()
            val decodeTime = decodeEndTime - decodeStartTime
            android.util.Log.v("WidgetProvider", "  🎯 BitmapFactory.decodeFile completed in: ${decodeTime}ms")
            
            // タイムアウトチェック
            val totalTime = decodeEndTime - startTime
            if (totalTime > timeoutMs) {
                android.util.Log.w("WidgetProvider", "  ⏰ Timeout detected: ${totalTime}ms > ${timeoutMs}ms")
                bitmap?.recycle()
                return null
            }
            
            // バックグラウンド制限の検出
            if (decodeTime > 500L) {
                android.util.Log.w("WidgetProvider", "  ⚠️ Slow decode detected (${decodeTime}ms) - possible background restriction")
                detectBackgroundRestriction()
            }
            
            bitmap
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "  ❌ Error in timeout loading", e)
            null
        }
    }
    
    // バックグラウンド制限の検出
    private fun detectBackgroundRestriction() {
        android.util.Log.w("WidgetProvider", "🔍 Detecting background restrictions...")
        
        try {
            val currentTime = System.currentTimeMillis()
            android.util.Log.w("WidgetProvider", "  Detection time: $currentTime")
            
            // テスト用の軽量処理を実行して応答時間を測定
            val testStartTime = System.currentTimeMillis()
            val testEndTime = System.currentTimeMillis()
            val testTime = testEndTime - testStartTime
            
            android.util.Log.w("WidgetProvider", "  Test processing time: ${testTime}ms")
            
            if (testTime > 100L) {
                android.util.Log.e("WidgetProvider", "🚨 BACKGROUND RESTRICTION DETECTED: System is throttling our process")
                
                // バックグラウンド制限対策
                applyBackgroundRestrictionWorkarounds()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "Error detecting background restriction", e)
        }
    }
    
    // バックグラウンド制限の回避策
    private fun applyBackgroundRestrictionWorkarounds() {
        android.util.Log.w("WidgetProvider", "🔧 Applying background restriction workarounds...")
        
        try {
            // 1. より小さなサンプリングサイズを使用
            android.util.Log.d("WidgetProvider", "  Switching to lighter image processing...")
            
            // 2. キャッシュ済み画像の積極的活用
            android.util.Log.d("WidgetProvider", "  Current cache size: ${imageCache.size}")
            
            // 3. アニメーション速度を調整
            animationStates.values.forEach { state ->
                if (state.isAnimating) {
                    val oldDelay = state.frameDelay
                    state.frameDelay = maxOf(state.frameDelay * 2, 200L) // 最低200ms間隔
                    android.util.Log.d("WidgetProvider", "  Adjusted frame delay: ${oldDelay}ms -> ${state.frameDelay}ms")
                }
            }
            
            // 4. フォールバック画像の使用を促進
            android.util.Log.d("WidgetProvider", "  Enabling aggressive fallback mode")
            
        } catch (e: Exception) {
            android.util.Log.e("WidgetProvider", "Failed to apply background restriction workarounds", e)
        }
    }

    // 緊急メモリクリーンアップ
    private fun emergencyMemoryCleanup() {
        android.util.Log.w("WidgetProvider", "🚨 Emergency memory cleanup triggered")
        
        val beforeCacheSize = imageCache.size
        imageCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        imageCache.clear()
        
        android.util.Log.w("WidgetProvider", "  Cleared $beforeCacheSize cached images")
        
        // システム制約情報もリセット
        missedFrameCount = 0
        lastExpectedExecutionTime = 0L
        android.util.Log.w("WidgetProvider", "  Reset system constraint counters")
        
        // システムGCを要求
        System.gc()
        
        // メモリ状況を再度ログ出力
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            logMemoryUsage()
        }, 100L)
    }

    // フォールバック画像用（外部画像が見つからない場合のデフォルト設定）
    private fun useFallbackImage(views: RemoteViews, currentIndex: Int) {
        android.util.Log.v("WidgetProvider", "🔄 Using fallback image for index $currentIndex")
        
        // 1つ前のフレームを試す
        if (imageFilePaths.isNotEmpty()) {
            val previousIndex = if (currentIndex > 0) currentIndex - 1 else imageFilePaths.size - 1
            val previousPath = imageFilePaths[previousIndex]
            val previousFile = java.io.File(previousPath)

            android.util.Log.v("WidgetProvider", "  Trying previous index: $previousIndex")
            android.util.Log.v("WidgetProvider", "  Previous file exists: ${previousFile.exists()}")

            if (previousFile.exists() && previousFile.length() > 0L) {
                val previousBitmap = if (imageCache.containsKey(previousIndex)) {
                    android.util.Log.v("WidgetProvider", "  Using cached previous image")
                    imageCache[previousIndex]
                } else {
                    android.util.Log.v("WidgetProvider", "  Loading previous image from file")
                    loadImageFromFile(previousPath, previousIndex)
                }
                
                previousBitmap?.let {
                    if (!it.isRecycled) {
                        views.setImageViewBitmap(R.id.background_image, it)
                        android.util.Log.v(
                            "WidgetProvider",
                            "  ✅ Using fallback image at index $previousIndex"
                        )
                        return
                    } else {
                        android.util.Log.w("WidgetProvider", "  ❌ Previous bitmap is recycled")
                    }
                }
            }
        }

        // 最後の手段として透明背景を設定
        views.setImageViewResource(R.id.background_image, android.R.color.transparent)
        android.util.Log.w("WidgetProvider", "  🚫 Using transparent background as final fallback")
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        android.util.Log.d("WidgetProvider", "🗑️ Widgets deleted: ${appWidgetIds.contentToString()}")

        // ウィジェットが削除されたときにアニメーションを停止
        for (appWidgetId in appWidgetIds) {
            android.util.Log.d("WidgetProvider", "  Cleaning up widget $appWidgetId")
            val animationState = animationStates[appWidgetId]
            if (animationState != null) {
                android.util.Log.d("WidgetProvider", "    Animation was running: ${animationState.isAnimating}")
                android.util.Log.d("WidgetProvider", "    Total updates: ${animationState.frameUpdateCount}")
            }
            
            stopAnimation(appWidgetId)
            animationStates.remove(appWidgetId)
        }
        
        android.util.Log.d("WidgetProvider", "  Remaining widgets: ${animationStates.size}")
        
        // メモリ最適化
        optimizeMemoryUsage()
        
        android.util.Log.d("WidgetProvider", "✅ Widget deletion cleanup completed")
    }
    
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        
        android.util.Log.d("WidgetProvider", "💀 Widget provider disabled - cleaning up all resources")
        android.util.Log.d("WidgetProvider", "  Active widgets: ${animationStates.size}")
        android.util.Log.d("WidgetProvider", "  Cached images: ${imageCache.size}")
        android.util.Log.d("WidgetProvider", "  Image file paths: ${imageFilePaths.size}")
        
        // すべてのアニメーションを停止
        var stoppedAnimations = 0
        animationStates.values.forEach { state ->
            if (state.isAnimating) {
                stoppedAnimations++
                android.util.Log.d("WidgetProvider", "  Stopping animation (${state.frameUpdateCount} updates)")
            }
            state.isAnimating = false
            val currentRunnable = state.animationRunnable // ローカル変数にコピー
            if (currentRunnable != null) {
                animationHandler.removeCallbacks(currentRunnable)
            }
        }
        animationStates.clear()
        
        android.util.Log.d("WidgetProvider", "  Stopped $stoppedAnimations animations")

        // アニメーション監視を停止
        android.util.Log.d("WidgetProvider", "  Removing animation watchdog")
        animationHandler.removeCallbacks(animationWatchdog)

        // 画像キャッシュをクリア
        android.util.Log.d("WidgetProvider", "  Clearing image cache")
        var recycledBitmaps = 0
        imageCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
                recycledBitmaps++
            }
        }
        imageCache.clear()
        imageFilePaths.clear()
        
        android.util.Log.d("WidgetProvider", "  Recycled $recycledBitmaps bitmaps")
        
        // 最終的なメモリ状況をログ出力
        logMemoryUsage()
        
        android.util.Log.d("WidgetProvider", "✅ Widget provider cleanup completed")
    }

    // 緊急強制再開のためのメソッド
    private fun performEmergencyRecovery(context: Context, intent: Intent, clearAllCallbacks: Boolean, forceMemoryCleanup: Boolean) {
        android.util.Log.d("WidgetProvider", "🚨 Performing emergency recovery...")
        
        if (clearAllCallbacks) {
            android.util.Log.d("WidgetProvider", "  Clearing all pending callbacks from Handler")
            animationHandler.removeCallbacksAndMessages(null)
            android.util.Log.d("WidgetProvider", "  All pending callbacks cleared.")
        }

        if (forceMemoryCleanup) {
            android.util.Log.d("WidgetProvider", "  Forcing memory cleanup...")
            emergencyMemoryCleanup()
            android.util.Log.d("WidgetProvider", "  Memory cleanup completed.")
        }

        // アニメーション状態をリセット
        animationStates.clear()
        android.util.Log.d("WidgetProvider", "  Animation states cleared.")

        // 画像ファイルパスを再初期化
        initializeImageFilePaths(context)
        android.util.Log.d("WidgetProvider", "  Image file paths re-initialized.")

        // アニメーションを再開
        val widgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        if (widgetIds != null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            widgetIds.forEach { widgetId ->
                android.util.Log.d("WidgetProvider", "  🚀 Emergency restart for widget $widgetId")
                
                // 新しいアニメーション状態を作成
                val newState = AnimationState()
                animationStates[widgetId] = newState
                
                // アニメーションを開始
                startAnimation(context, appWidgetManager, widgetId)
            }
        }
        android.util.Log.d("WidgetProvider", "✅ Emergency recovery completed.")
    }
}