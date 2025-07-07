package com.seo4d696b75.android.glance_widget_demo.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService

import com.seo4d696b75.android.glance_widget_demo.theme.AppTheme
import com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val animationStateManager = remember { AnimationStateManager.getInstance(context) }
    
    // アニメーション状態の管理
    var currentAnimationType by remember { mutableStateOf(animationStateManager.getCurrentAnimationType()) }
    var currentCharacterId by remember { mutableStateOf(animationStateManager.getCurrentCharacterId()) }

    AppTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "ウィジェット アニメーション コントロール",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "現在のアニメーション:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "$currentCharacterId/State/$currentAnimationType",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                // アニメーション切り替え
                                val newAnimationType = animationStateManager.toggleAnimationType()
                                currentAnimationType = newAnimationType
                                currentCharacterId = animationStateManager.getCurrentCharacterId()
                                
                                // ウィジェットを更新
                                updateWidgetAfterToggle(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "アニメーション切り替え",
                                fontSize = 16.sp
                            )
                        }
                        
                        Text(
                            text = "次のアニメーション: ${animationStateManager.getNextAnimationDisplayText()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // 画像ダウンロードボタン
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "📥 手動ダウンロード開始")
                                ImageDownloadService.downloadCharacterImages(context, "Mao")
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Mao画像をダウンロード",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "🔧 権限チェック開始")
                                checkPermissions(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "権限とディレクトリ確認",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "📁 画像整理開始")
                                organizeExistingImages(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "既存画像を整理 (手動実行)",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    android.util.Log.d("MainScreen", "📁 Android 10+ - 直接画像整理開始")
                                    organizeExistingImages(context)
                                } else {
                                    android.util.Log.d("MainScreen", "🔐 権限要求開始")
                                    requestStoragePermission(context)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "画像を整理して準備完了" else "ストレージ権限を要求",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "🧹 強制クリーンアップ開始")
                                forceCleanupImages(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "強制クリーンアップ（手動削除用）",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "📁 外部ストレージアクセス情報表示")
                                showExternalStorageAccess(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "外部ストレージを手動で開く",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "📂 ファイルマネージャーでアプリディレクトリを開く")
                                openAppDirectoryInFileManager(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "ファイルマネージャーで開く",
                                fontSize = 14.sp
                            )
                        }
                        
                        OutlinedButton(
                            onClick = {
                                android.util.Log.d("MainScreen", "📋 パスをクリップボードにコピー")
                                copyPathToClipboard(context)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "パスをクリップボードにコピー",
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun forceWidgetUpdate(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Force Widget Update ===")
        android.util.Log.d("MainScreen", "Current time: ${System.currentTimeMillis()}")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        android.util.Log.d("MainScreen", "Found ${appWidgetIds.size} widgets")

        if (appWidgetIds.isNotEmpty()) {
            // 詳細な診断情報を出力
            android.util.Log.d("MainScreen", "--- Widget Diagnostic ---")
            android.util.Log.d("MainScreen", "Widget IDs: ${appWidgetIds.contentToString()}")
            
            // 強制的にアニメーション再開を試みる
            val intent = android.content.Intent().apply {
                component = componentName
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                // 強制更新フラグを追加
                putExtra("FORCE_ANIMATION_RESTART", true)
                putExtra("FORCE_UPDATE_TIME", System.currentTimeMillis())
            }
            
            context.sendBroadcast(intent)
            android.util.Log.d("MainScreen", "✅ Widget update broadcast sent with force restart flag")
            
            // 追加: 直接的なアニメーション再開の試行
            android.util.Log.d("MainScreen", "--- Attempting Direct Animation Restart ---")
            
            // 複数回実行して確実に動作させる
            repeat(3) { attempt ->
                android.util.Log.d("MainScreen", "Restart attempt #${attempt + 1}")
                try {
                    val delayedIntent = android.content.Intent().apply {
                        component = componentName
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                        putExtra("RESTART_ATTEMPT", attempt + 1)
                    }
                    // 少し間隔を空けて実行
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        context.sendBroadcast(delayedIntent)
                        android.util.Log.d("MainScreen", "  Delayed restart broadcast sent #${attempt + 1}")
                    }, (attempt + 1) * 500L)
                    
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "Error in restart attempt #${attempt + 1}", e)
                }
            }
            
        } else {
            android.util.Log.w("MainScreen", "No widgets found to update")
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error forcing widget update", e)
    }
}

private fun debugImagePath(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Debug Image Path ===")
        val baseDir = context.getExternalFilesDir(null)
        android.util.Log.d("MainScreen", "External files dir: ${baseDir?.absolutePath}")

        if (baseDir != null) {
            val widgetImagesDir = java.io.File(baseDir, "WidgetImages")
            android.util.Log.d("MainScreen", "Widget images dir: ${widgetImagesDir.absolutePath}")
            android.util.Log.d("MainScreen", "Directory exists: ${widgetImagesDir.exists()}")

            if (widgetImagesDir.exists()) {
                val files = widgetImagesDir.listFiles()
                android.util.Log.d("MainScreen", "Files count: ${files?.size ?: 0}")
                files?.forEach { file ->
                    android.util.Log.d("MainScreen", "File: ${file.name} (${file.length()} bytes)")
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error debugging image path", e)
    }
}

// Use reflection to avoid direct dependency issues
private fun downloadTestImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Download Test Images ===")

        // サンプル画像URLのリスト（実際のURLに置き換えてください）
//        val sampleImageUrls = listOf(
//            "https://picsum.photos/200/200?random=1",
//            "https://picsum.photos/200/200?random=2",
//            "https://picsum.photos/200/200?random=3",
//            "https://picsum.photos/200/200?random=4",
//            "https://picsum.photos/200/200?random=5"
//        )

        // ContextをActivityとして取得し、リフレクションで関数を呼び出す
        if (context is ComponentActivity) {
            try {
//                val downloadFunc = context::class.java.getMethod(
//                    "downloadImagesFromServer", List::class.java
//                )
//                downloadFunc.invoke(context, sampleImageUrls)
//                android.util.Log.d(
//                    "MainScreen",
//                    "Download started for ${sampleImageUrls.size} images using reflection"
//                )
            } catch (e: NoSuchMethodException) {
                android.util.Log.w(
                    "MainScreen",
                    "downloadImagesFromServer(List<String>) not found in Activity",
                    e
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainScreen",
                    "Failed invoking downloadImagesFromServer via reflection",
                    e
                )
            }
        } else {
            android.util.Log.w("MainScreen", "Context is not an Activity, cannot download images")
        }

        // Also trigger Firebase image downloads for demo
        downloadFirebaseImages(context)

    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error downloading test images", e)
    }
}

private fun requestPermissions(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Request Permissions ===")
        if (context is ComponentActivity) {
            try {
                val requestPermFunc = context::class.java.getMethod(
                    "requestAllRequiredPermissions"
                )
                requestPermFunc.invoke(context)
                android.util.Log.d(
                    "MainScreen",
                    "Invoked requestAllRequiredPermissions() via reflection"
                )
            } catch (e: NoSuchMethodException) {
                android.util.Log.w(
                    "MainScreen",
                    "requestAllRequiredPermissions() not found in Activity",
                    e
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainScreen",
                    "Failed invoking requestAllRequiredPermissions via reflection",
                    e
                )
            }
        } else {
            android.util.Log.w(
                "MainScreen",
                "Context is not an Activity, cannot trigger permission request"
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error requesting permissions", e)
    }
}

private fun clearAllImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Clear All Images ===")
        if (context is ComponentActivity) {
            try {
                val clearFunc = context::class.java.getMethod(
                    "clearAllWidgetImages"
                )
                clearFunc.invoke(context)
                android.util.Log.d(
                    "MainScreen",
                    "Invoked clearAllWidgetImages() via reflection"
                )
            } catch (e: NoSuchMethodException) {
                android.util.Log.w(
                    "MainScreen",
                    "clearAllWidgetImages() not found in Activity",
                    e
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainScreen",
                    "Failed invoking clearAllWidgetImages via reflection",
                    e
                )
            }
        } else {
            android.util.Log.w(
                "MainScreen",
                "Context is not an Activity, cannot trigger clear images"
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error clearing all images", e)
    }
}

private fun downloadFirebaseImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Download Firebase Images ===")
        if (context is ComponentActivity) {
            try {
                val downloadFunc = context::class.java.getMethod(
                    "downloadImagesFromFirebase"
                )
                downloadFunc.invoke(context)
                android.util.Log.d(
                    "MainScreen",
                    "Invoked downloadImagesFromFirebase() via reflection"
                )
            } catch (e: NoSuchMethodException) {
                android.util.Log.w(
                    "MainScreen",
                    "downloadImagesFromFirebase() not found in Activity",
                    e
                )
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainScreen",
                    "Failed invoking downloadImagesFromFirebase via reflection",
                    e
                )
            }
        } else {
            android.util.Log.w(
                "MainScreen",
                "Context is not an Activity, cannot trigger Firebase image downloads"
            )
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error downloading Firebase images", e)
    }
}

private fun forceSystemConstraintWorkaround(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Force System Constraint Workaround ===")
        android.util.Log.d("MainScreen", "Current time: ${System.currentTimeMillis()}")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        android.util.Log.d("MainScreen", "Found ${appWidgetIds.size} widgets for constraint workaround")

        if (appWidgetIds.isNotEmpty()) {
            // システム制約回避専用の強制更新
            val constraintWorkaroundIntent = android.content.Intent().apply {
                component = componentName
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                // システム制約回避フラグを追加
                putExtra("SYSTEM_CONSTRAINT_WORKAROUND", true)
                putExtra("FORCE_MULTIPLE_EXECUTION", true)
                putExtra("WORKAROUND_TIME", System.currentTimeMillis())
            }
            
            context.sendBroadcast(constraintWorkaroundIntent)
            android.util.Log.d("MainScreen", "✅ System constraint workaround broadcast sent")
            
            // 複数回の実行を異なる間隔で試行
            val intervals = listOf(100L, 300L, 500L, 1000L, 2000L)
            intervals.forEachIndexed { index, interval ->
                android.util.Log.d("MainScreen", "Scheduling workaround attempt #${index + 1} in ${interval}ms")
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val delayedConstraintIntent = android.content.Intent().apply {
                            component = componentName
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                            putExtra("SYSTEM_CONSTRAINT_WORKAROUND", true)
                            putExtra("ATTEMPT_NUMBER", index + 1)
                            putExtra("DELAY_INTERVAL", interval)
                        }
                        context.sendBroadcast(delayedConstraintIntent)
                        android.util.Log.d("MainScreen", "  Constraint workaround attempt #${index + 1} executed")
                    } catch (e: Exception) {
                        android.util.Log.e("MainScreen", "Error in constraint workaround attempt #${index + 1}", e)
                    }
                }, interval)
            }
            
        } else {
            android.util.Log.w("MainScreen", "No widgets found for constraint workaround")
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error forcing system constraint workaround", e)
    }
}

// 強制リフレッシュ用の関数
private fun forceRefreshWidget(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== Force Refresh Widget ===")
        if (context is ComponentActivity) {
            try {
                val forceRefreshFunc = context::class.java.getMethod(
                    "forceRefreshWidget"
                )
                forceRefreshFunc.invoke(context)
                android.util.Log.d(
                    "MainScreen",
                    "Invoked forceRefreshWidget() via reflection"
                )
            } catch (e: NoSuchMethodException) {
                android.util.Log.w(
                    "MainScreen",
                    "forceRefreshWidget() not found in Activity, using fallback",
                    e
                )
                // フォールバック: 通常のウィジェット更新を実行
                forceWidgetUpdate(context)
            } catch (e: Exception) {
                android.util.Log.e(
                    "MainScreen",
                    "Failed invoking forceRefreshWidget via reflection",
                    e
                )
                // フォールバック: 通常のウィジェット更新を実行
                forceWidgetUpdate(context)
            }
        } else {
            android.util.Log.w(
                "MainScreen",
                "Context is not an Activity, using fallback widget update"
            )
            forceWidgetUpdate(context)
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error forcing widget refresh", e)
    }
}

private fun emergencyForceRestart(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== EMERGENCY FORCE RESTART ===")
        android.util.Log.d("MainScreen", "Current time: ${System.currentTimeMillis()}")
        android.util.Log.d("MainScreen", "This is the most aggressive recovery method")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        android.util.Log.d("MainScreen", "Found ${appWidgetIds.size} widgets for emergency restart")

        if (appWidgetIds.isNotEmpty()) {
            // 緊急強制再開専用のIntent
            val emergencyIntent = android.content.Intent().apply {
                component = componentName
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                // 緊急再開フラグを追加
                putExtra("EMERGENCY_FORCE_RESTART", true)
                putExtra("EMERGENCY_TIME", System.currentTimeMillis())
                putExtra("CLEAR_ALL_CALLBACKS", true)
                putExtra("FORCE_MEMORY_CLEANUP", true)
            }
            
            context.sendBroadcast(emergencyIntent)
            android.util.Log.d("MainScreen", "✅ Emergency force restart broadcast sent")
            
            // 複数回の緊急実行（より短い間隔で）
            val emergencyIntervals = listOf(50L, 100L, 200L, 400L, 800L, 1500L, 3000L)
            emergencyIntervals.forEachIndexed { index, interval ->
                android.util.Log.d("MainScreen", "Scheduling emergency attempt #${index + 1} in ${interval}ms")
                
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        val delayedEmergencyIntent = android.content.Intent().apply {
                            component = componentName
                            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                            putExtra("EMERGENCY_FORCE_RESTART", true)
                            putExtra("EMERGENCY_ATTEMPT", index + 1)
                            putExtra("EMERGENCY_INTERVAL", interval)
                            putExtra("CLEAR_ALL_CALLBACKS", true)
                        }
                        context.sendBroadcast(delayedEmergencyIntent)
                        android.util.Log.d("MainScreen", "  🚨 Emergency attempt #${index + 1} executed")
                    } catch (e: Exception) {
                        android.util.Log.e("MainScreen", "Error in emergency attempt #${index + 1}", e)
                    }
                }, interval)
            }
            
            android.util.Log.d("MainScreen", "Scheduled ${emergencyIntervals.size} emergency recovery attempts")
            
        } else {
            android.util.Log.w("MainScreen", "No widgets found for emergency restart")
        }
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "Error in emergency force restart", e)
    }
}

private fun updateWidgetAfterToggle(context: Context) {
    try {
        android.util.Log.d("MainScreen", "🔄 Updating widget after animation toggle")
        
        // ウィジェットプロバイダーにアニメーション切り替えを通知
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        android.util.Log.d("MainScreen", "Found ${widgetIds.size} widgets to update")
        
        if (widgetIds.isNotEmpty()) {
            // ウィジェットプロバイダーに切り替え通知を送信
            val toggleIntent = Intent().apply {
                component = componentName
                action = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
                putExtra("FROM_APP", true)
                putExtra("TOGGLE_TIME", System.currentTimeMillis())
            }
            context.sendBroadcast(toggleIntent)
            
            android.util.Log.d("MainScreen", "✅ Widget toggle broadcast sent to ${widgetIds.size} widgets")
            
            // 追加の確実性のため、少し遅延してもう一度送信
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    val retryIntent = Intent().apply {
                        component = componentName
                        action = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
                        putExtra("FROM_APP", true)
                        putExtra("RETRY_TOGGLE", true)
                        putExtra("TOGGLE_TIME", System.currentTimeMillis())
                    }
                    context.sendBroadcast(retryIntent)
                    android.util.Log.d("MainScreen", "🔄 Retry toggle broadcast sent")
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "❌ Error in retry toggle", e)
                }
            }, 500L)
            
        } else {
            android.util.Log.w("MainScreen", "⚠️ No widgets found to update")
            
            // ウィジェットが見つからない場合は、強制的にサービス経由で更新を試行
            try {
                val serviceIntent = Intent().apply {
                    component = ComponentName(
                        "com.seo4d696b75.android.glance_widget_demo",
                        "com.seo4d696b75.android.glance_widget_demo.widget.WidgetAnimationService"
                    )
                    action = "TOGGLE_ANIMATION"
                    putExtra("FROM_APP", true)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                
                android.util.Log.d("MainScreen", "📞 Fallback: Service toggle requested")
                
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "❌ Error in service fallback", e)
            }
        }
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error updating widget after toggle", e)
    }
}

private fun checkPermissions(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 権限とディレクトリ確認 ===")
        
        // Android バージョン確認
        android.util.Log.d("MainScreen", "📱 Android API Level: ${Build.VERSION.SDK_INT}")
        
        // Scoped Storage対応の権限チェック
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q // Android 10未満のみ権限必要
        val hasWritePermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10+ では scoped storage により権限不要
        }
        
        val hasReadPermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10+ では scoped storage により権限不要
        }
        
        android.util.Log.d("MainScreen", "📄 権限状態:")
        android.util.Log.d("MainScreen", "  - Android バージョン: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        android.util.Log.d("MainScreen", "  - Scoped Storage: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "✅ 有効 (権限不要)" else "❌ 無効 (権限必要)"}")
        android.util.Log.d("MainScreen", "  - WRITE_EXTERNAL_STORAGE: ${if (hasWritePermission) "✅" else "❌"}")
        android.util.Log.d("MainScreen", "  - READ_EXTERNAL_STORAGE: ${if (hasReadPermission) "✅" else "❌"}")
        
        // ディレクトリチェック
        val baseDir = context.getExternalFilesDir(null)
        android.util.Log.d("MainScreen", "📁 ディレクトリ情報:")
        android.util.Log.d("MainScreen", "  - External files dir: ${baseDir?.absolutePath}")
        
        if (baseDir != null) {
            val widgetImagesDir = java.io.File(baseDir, "WidgetImages")
            android.util.Log.d("MainScreen", "  - Widget images dir: ${widgetImagesDir.absolutePath}")
            android.util.Log.d("MainScreen", "  - Widget images存在: ${if (widgetImagesDir.exists()) "✅" else "❌"}")
            
            // Mao/State/ ディレクトリ確認
            val maoStateDir = java.io.File(widgetImagesDir, "Mao/State")
            android.util.Log.d("MainScreen", "  - Mao/State dir: ${maoStateDir.absolutePath}")
            android.util.Log.d("MainScreen", "  - Mao/State存在: ${if (maoStateDir.exists()) "✅" else "❌"}")
            
            // 各アニメーションディレクトリ確認
            val maoAdleDir = java.io.File(maoStateDir, "Adle")
            val maoFlowDir = java.io.File(maoStateDir, "Flow")
            android.util.Log.d("MainScreen", "  - Mao/State/Adle dir: ${maoAdleDir.absolutePath}")
            android.util.Log.d("MainScreen", "  - Mao/State/Adle存在: ${if (maoAdleDir.exists()) "✅" else "❌"}")
            android.util.Log.d("MainScreen", "  - Mao/State/Flow dir: ${maoFlowDir.absolutePath}")
            android.util.Log.d("MainScreen", "  - Mao/State/Flow存在: ${if (maoFlowDir.exists()) "✅" else "❌"}")
            
            // 各ディレクトリ内のファイル数を確認
            if (maoAdleDir.exists()) {
                val adleFiles = maoAdleDir.listFiles()
                android.util.Log.d("MainScreen", "  - Adle画像数: ${adleFiles?.size ?: 0}")
                adleFiles?.take(5)?.forEach { file ->
                    android.util.Log.d("MainScreen", "    - ${file.name} (${file.length()} bytes)")
                }
            }
            
            if (maoFlowDir.exists()) {
                val flowFiles = maoFlowDir.listFiles()
                android.util.Log.d("MainScreen", "  - Flow画像数: ${flowFiles?.size ?: 0}")
                flowFiles?.take(5)?.forEach { file ->
                    android.util.Log.d("MainScreen", "    - ${file.name} (${file.length()} bytes)")
                }
            }
            
            // 全体のWidgetImagesディレクトリ内容
            if (widgetImagesDir.exists()) {
                android.util.Log.d("MainScreen", "📂 WidgetImagesディレクトリ内容:")
                widgetImagesDir.listFiles()?.forEach { file ->
                    if (file.isDirectory) {
                        android.util.Log.d("MainScreen", "  📁 ${file.name}/")
                        file.listFiles()?.forEach { subFile ->
                            if (subFile.isDirectory) {
                                android.util.Log.d("MainScreen", "    📁 ${subFile.name}/")
                            } else {
                                android.util.Log.d("MainScreen", "    📄 ${subFile.name}")
                            }
                        }
                    } else {
                        android.util.Log.d("MainScreen", "  📄 ${file.name}")
                    }
                }
            }
        }
        
        // 推奨アクション
        android.util.Log.d("MainScreen", "💡 推奨アクション:")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.util.Log.d("MainScreen", "  ✅ Android 10+ Scoped Storage により権限は不要です")
            if (baseDir != null) {
                val maoStateDir = java.io.File(baseDir, "WidgetImages/Mao/State")
                if (!maoStateDir.exists()) {
                    android.util.Log.d("MainScreen", "  📁 「既存画像を整理」ボタンを押してディレクトリ構造を作成してください")
                } else {
                    android.util.Log.d("MainScreen", "  ✅ ディレクトリ構造は準備済みです")
                }
            }
        } else {
            // Android 9以下の場合のみ権限が必要
            if (!hasWritePermission || !hasReadPermission) {
                android.util.Log.d("MainScreen", "  1. 設定 → アプリ → glance_widget_demo → 権限 → ストレージ権限を許可")
                android.util.Log.d("MainScreen", "  2. 権限許可後「既存画像を整理」ボタンを押してください")
            } else {
                android.util.Log.d("MainScreen", "  📁 「既存画像を整理」ボタンを押してディレクトリ構造を作成してください")
            }
        }
        
        android.util.Log.d("MainScreen", "=== 権限とディレクトリ確認完了 ===")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error checking permissions", e)
    }
}

private fun organizeExistingImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 既存画像の整理開始 ===")
        android.util.Log.d("MainScreen", "📱 Android API Level: ${Build.VERSION.SDK_INT}")
        android.util.Log.d("MainScreen", "📁 Scoped Storage: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "有効 (権限不要)" else "無効"}")
        
        val baseDir = context.getExternalFilesDir(null)
        if (baseDir == null) {
            android.util.Log.e("MainScreen", "❌ External files directory is null")
            return
        }
        
        android.util.Log.d("MainScreen", "📂 Base directory: ${baseDir.absolutePath}")
        android.util.Log.d("MainScreen", "📝 Note: Android 10+ では scoped storage により、このディレクトリへの書き込みに権限は不要です")
        
        val widgetImagesDir = java.io.File(baseDir, "WidgetImages")
        if (!widgetImagesDir.exists()) {
            android.util.Log.w("MainScreen", "❌ WidgetImages directory does not exist")
            return
        }
        
        // 既存画像の確認
        val existingImages = widgetImagesDir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{3}\\.png"))
        }?.sortedBy { it.name }
        
        if (existingImages.isNullOrEmpty()) {
            android.util.Log.w("MainScreen", "❌ No existing images found")
            return
        }
        
        android.util.Log.d("MainScreen", "📋 Found ${existingImages.size} existing images")
        
        // ディレクトリ構造を作成
        val maoDir = java.io.File(baseDir, "Mao")
        val stateDir = java.io.File(maoDir, "State")
        val adleDir = java.io.File(stateDir, "Adle")
        val flowDir = java.io.File(stateDir, "Flow")
        
        // 既存のディレクトリをクリア
        if (adleDir.exists()) {
            android.util.Log.d("MainScreen", "🧹 Clearing existing Adle directory")
            adleDir.listFiles()?.forEach { it.delete() }
        }
        if (flowDir.exists()) {
            android.util.Log.d("MainScreen", "🧹 Clearing existing Flow directory")
            flowDir.listFiles()?.forEach { it.delete() }
        }
        
        adleDir.mkdirs()
        flowDir.mkdirs()
        
        android.util.Log.d("MainScreen", "📁 Created clean directory structure:")
        android.util.Log.d("MainScreen", "  - Adle: ${adleDir.absolutePath}")
        android.util.Log.d("MainScreen", "  - Flow: ${flowDir.absolutePath}")
        
        // 既存画像を一時的にAdle用として移動（FirebaseからダウンロードされたものがAdleと仮定）
        android.util.Log.d("MainScreen", "📊 既存画像をAdleアニメーションに移動:")
        android.util.Log.d("MainScreen", "  - Adle: ${existingImages.size} images")
        
        // 既存の50枚をAdleに移動
        existingImages.forEachIndexed { index, sourceFile ->
            val targetFile = java.io.File(adleDir, "${(index + 1).toString().padStart(3, '0')}.png")
            try {
                sourceFile.copyTo(targetFile, overwrite = true)
                android.util.Log.d("MainScreen", "✅ Copied ${sourceFile.name} → Adle/${targetFile.name}")
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "❌ Failed to copy ${sourceFile.name} to Adle", e)
            }
        }
        
        // 元の場所の画像を削除（混在を防ぐため）
        android.util.Log.d("MainScreen", "🧹 Cleaning up original images to prevent mixing")
        
        // 1. まずウィジェットアニメーションサービスを停止
        android.util.Log.d("MainScreen", "⏹️ Stopping widget animation service before cleanup")
        try {
            val stopIntent = android.content.Intent(context, com.seo4d696b75.android.glance_widget_demo.widget.WidgetAnimationService::class.java)
            context.stopService(stopIntent)
            android.util.Log.d("MainScreen", "✅ Widget animation service stopped")
        } catch (e: Exception) {
            android.util.Log.w("MainScreen", "⚠️ Failed to stop widget animation service", e)
        }
        
        // 2. 少し待ってからファイル削除
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.util.Log.d("MainScreen", "🗑️ Starting file deletion after service stop")
            
            var deletedCount = 0
            var failedCount = 0
            
            existingImages.forEach { file ->
                try {
                    if (file.delete()) {
                        android.util.Log.d("MainScreen", "🗑️ Deleted original: ${file.name}")
                        deletedCount++
                    } else {
                        android.util.Log.w("MainScreen", "⚠️ Failed to delete original: ${file.name}")
                        failedCount++
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "❌ Error deleting original: ${file.name}", e)
                    failedCount++
                }
            }
            
            android.util.Log.d("MainScreen", "📊 Cleanup result: $deletedCount deleted, $failedCount failed")
            
            // 3. クリーンアップ後、必要に応じてガベージコレクションを実行
            System.gc()
            
        }, 1000L) // 1秒後に実行
        
        // Flowアニメーションの画像はFirebaseから別途ダウンロード
        android.util.Log.d("MainScreen", "📥 Flowアニメーションの画像をダウンロード開始...")
        try {
            // Flowアニメーションのダウンロードを開始
            ImageDownloadService.downloadCharacterImages(context, "Mao", "Flow")
            android.util.Log.d("MainScreen", "✅ Flowアニメーションダウンロード開始")
        } catch (e: Exception) {
            android.util.Log.e("MainScreen", "❌ Failed to start Flow animation download", e)
        }
        
        // 結果確認
        val adleCount = adleDir.listFiles()?.size ?: 0
        val flowCount = flowDir.listFiles()?.size ?: 0
        
        android.util.Log.d("MainScreen", "🎯 整理完了:")
        android.util.Log.d("MainScreen", "  - Adle: $adleCount 個の画像")
        android.util.Log.d("MainScreen", "  - Flow: $flowCount 個の画像 (ダウンロード中)")
        
        // 実際のフレーム数をSharedPreferencesに保存
        val prefs = context.getSharedPreferences("actual_frame_counts", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("Mao_Adle", adleCount)
            putInt("Mao_Flow", 50) // Flowは50フレーム予定
            apply()
        }
        
        android.util.Log.d("MainScreen", "💾 実際のフレーム数を保存: Adle=$adleCount, Flow=50 (ダウンロード予定)")
        
        // 追加：画像整理後にFlowアニメーションを再ダウンロード
        android.util.Log.d("MainScreen", "🔄 既存のSharedPreferencesをリセットして正しいフレーム数を設定")
        prefs.edit().apply {
            putInt("Mao_Flow", 50) // 正しいフレーム数を設定
            apply()
        }
        android.util.Log.d("MainScreen", "=== 既存画像の整理完了 ===")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error organizing existing images", e)
    }
}

private fun requestStoragePermission(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 権限要求処理 ===")
        
        // Android 9以下の場合のみ権限が必要
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            android.util.Log.d("MainScreen", "📋 Android 9以下 - 手動で権限を許可してください")
            android.util.Log.d("MainScreen", "設定 → アプリ → glance_widget_demo → 権限 → ストレージ")
            
            // 権限確認後、画像整理を実行
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                android.util.Log.d("MainScreen", "🔄 権限確認後の処理を開始")
                checkPermissionsAndOrganize(context)
            }, 3000L) // 3秒後に実行
        } else {
            android.util.Log.d("MainScreen", "✅ Android 10+ - 権限不要、直接画像整理を実行")
            organizeExistingImages(context)
        }
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error requesting storage permission", e)
    }
}

private fun showExternalStorageAccess(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 外部ストレージアクセス情報 ===")
        
        val baseDir = context.getExternalFilesDir(null)
        if (baseDir == null) {
            android.util.Log.e("MainScreen", "❌ External files directory is null")
            return
        }
        
        android.util.Log.d("MainScreen", "📂 アプリの外部ストレージディレクトリ:")
        android.util.Log.d("MainScreen", "   ${baseDir.absolutePath}")
        
        android.util.Log.d("MainScreen", "📁 画像ディレクトリ:")
        android.util.Log.d("MainScreen", "   ${baseDir.absolutePath}/WidgetImages")
        android.util.Log.d("MainScreen", "   ${baseDir.absolutePath}/Mao/State/Adle")
        android.util.Log.d("MainScreen", "   ${baseDir.absolutePath}/Mao/State/Flow")
        
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "💡 手動アクセス方法:")
        android.util.Log.d("MainScreen", "1. ファイルマネージャーアプリを開く")
        android.util.Log.d("MainScreen", "2. 内部ストレージ → Android → data を開く")
        android.util.Log.d("MainScreen", "3. com.seo4d696b75.android.glance_widget_demo を開く")
        android.util.Log.d("MainScreen", "4. files フォルダを開く")
        android.util.Log.d("MainScreen", "5. WidgetImages や Mao フォルダが見えます")
        
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "📱 端末のファイルマネージャーが必要:")
        android.util.Log.d("MainScreen", "- ES File Explorer")
        android.util.Log.d("MainScreen", "- Solid Explorer")
        android.util.Log.d("MainScreen", "- FX File Explorer")
        android.util.Log.d("MainScreen", "- 端末内蔵のファイルマネージャー")
        
        // 現在のディレクトリ状況も表示
        showCurrentDirectoryStatus(context)
        
        // ダウンロードされたファイルの詳細情報も表示
        showDetailedFileList(context)
        
        android.util.Log.d("MainScreen", "=== 外部ストレージアクセス情報完了 ===")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error showing external storage access", e)
    }
}

private fun openAppDirectoryInFileManager(context: Context) {
    try {
        android.util.Log.d("MainScreen", "📂 ファイルマネージャーでアプリディレクトリを開く")
        
        val baseDir = context.getExternalFilesDir(null)
        if (baseDir == null) {
            android.util.Log.e("MainScreen", "❌ External files directory is null")
            return
        }
        
        // 方法1: ファイルマネージャーで直接開く（Android標準）
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.fromFile(baseDir), "resource/folder")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d("MainScreen", "✅ ファイルマネージャーで開きました")
            return
        } catch (e: Exception) {
            android.util.Log.w("MainScreen", "⚠️ 標準方法で開けませんでした: ${e.message}")
        }
        
        // 方法2: DocumentsUIで開く
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d("MainScreen", "✅ DocumentsUIで開きました")
            android.util.Log.d("MainScreen", "📍 パスを手動で移動してください:")
            android.util.Log.d("MainScreen", "   Android → data → com.seo4d696b75.android.glance_widget_demo → files")
            return
        } catch (e: Exception) {
            android.util.Log.w("MainScreen", "⚠️ DocumentsUIで開けませんでした: ${e.message}")
        }
        
        // 方法3: 一般的なファイルマネージャーアプリを試す
        val fileManagerPackages = listOf(
            "com.estrongs.android.pop",           // ES File Explorer
            "pl.solidexplorer2",                  // Solid Explorer
            "nextapp.fx",                         // FX File Explorer
            "com.android.documentsui",            // Android Documents
            "com.google.android.documentsui",     // Google Documents
            "com.mi.android.globalFileexplorer",  // Mi File Manager
            "com.samsung.android.app.files"      // Samsung Files
        )
        
        for (packageName in fileManagerPackages) {
            try {
                val pm = context.packageManager
                val launchIntent = pm.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                    android.util.Log.d("MainScreen", "✅ ${packageName} で開きました")
                    android.util.Log.d("MainScreen", "📍 パスを手動で移動してください:")
                    android.util.Log.d("MainScreen", "   ${baseDir.absolutePath}")
                    return
                }
            } catch (e: Exception) {
                android.util.Log.d("MainScreen", "⚠️ ${packageName} は利用できません")
            }
        }
        
        // 全て失敗した場合
        android.util.Log.w("MainScreen", "❌ ファイルマネージャーを開けませんでした")
        android.util.Log.w("MainScreen", "💡 手動でファイルマネージャーアプリを開いて以下のパスに移動してください:")
        android.util.Log.w("MainScreen", "   ${baseDir.absolutePath}")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error opening app directory in file manager", e)
    }
}

private fun showCurrentDirectoryStatus(context: Context) {
    try {
        val baseDir = context.getExternalFilesDir(null) ?: return
        
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "📊 現在のディレクトリ状況:")
        
        // WidgetImagesディレクトリ
        val widgetImagesDir = java.io.File(baseDir, "WidgetImages")
        if (widgetImagesDir.exists()) {
            val files = widgetImagesDir.listFiles()?.size ?: 0
            android.util.Log.d("MainScreen", "   WidgetImages: $files files")
        } else {
            android.util.Log.d("MainScreen", "   WidgetImages: 存在しません")
        }
        
        // Adleディレクトリ
        val adleDir = java.io.File(baseDir, "Mao/State/Adle")
        if (adleDir.exists()) {
            val files = adleDir.listFiles()?.size ?: 0
            android.util.Log.d("MainScreen", "   Mao/State/Adle: $files files")
        } else {
            android.util.Log.d("MainScreen", "   Mao/State/Adle: 存在しません")
        }
        
        // Flowディレクトリ
        val flowDir = java.io.File(baseDir, "Mao/State/Flow")
        if (flowDir.exists()) {
            val files = flowDir.listFiles()?.size ?: 0
            android.util.Log.d("MainScreen", "   Mao/State/Flow: $files files")
        } else {
            android.util.Log.d("MainScreen", "   Mao/State/Flow: 存在しません")
        }
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error showing directory status", e)
    }
}

private fun showDetailedFileList(context: Context) {
    try {
        val baseDir = context.getExternalFilesDir(null) ?: return
        
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "📄 ダウンロードされたファイルの詳細:")
        
        // WidgetImagesディレクトリの詳細
        val widgetImagesDir = java.io.File(baseDir, "WidgetImages")
        showDirectoryDetails(widgetImagesDir, "WidgetImages")
        
        // Adleディレクトリの詳細
        val adleDir = java.io.File(baseDir, "Mao/State/Adle")
        showDirectoryDetails(adleDir, "Mao/State/Adle")
        
        // Flowディレクトリの詳細
        val flowDir = java.io.File(baseDir, "Mao/State/Flow")
        showDirectoryDetails(flowDir, "Mao/State/Flow")
        
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "📝 ファイル名形式の説明:")
        android.util.Log.d("MainScreen", "  WidgetImages: 001.png, 002.png, ... 050.png")
        android.util.Log.d("MainScreen", "  Adle: 001.png, 002.png, ... 050.png")
        android.util.Log.d("MainScreen", "  Flow: 001.png, 002.png, ... 050.png")
        android.util.Log.d("MainScreen", "  (注: Firebase元画像は 001_reduced_16colors.png 形式)")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error showing detailed file list", e)
    }
}

private fun showDirectoryDetails(directory: java.io.File, displayName: String) {
    try {
        android.util.Log.d("MainScreen", "")
        android.util.Log.d("MainScreen", "📁 $displayName:")
        android.util.Log.d("MainScreen", "   パス: ${directory.absolutePath}")
        
        if (!directory.exists()) {
            android.util.Log.d("MainScreen", "   ❌ ディレクトリが存在しません")
            return
        }
        
        val files = directory.listFiles { file -> 
            file.isFile && file.name.matches(Regex("\\d{3}\\.png"))
        }?.sortedBy { it.name }
        
        if (files.isNullOrEmpty()) {
            android.util.Log.d("MainScreen", "   ❌ PNG画像ファイルが見つかりません")
            
            // 他のファイルがあるかチェック
            val allFiles = directory.listFiles()
            if (allFiles != null && allFiles.isNotEmpty()) {
                android.util.Log.d("MainScreen", "   📂 他のファイル:")
                allFiles.take(5).forEach { file ->
                    val size = if (file.isFile) " (${file.length() / 1024}KB)" else ""
                    android.util.Log.d("MainScreen", "     - ${file.name}$size")
                }
                if (allFiles.size > 5) {
                    android.util.Log.d("MainScreen", "     ... and ${allFiles.size - 5} more files")
                }
            }
            return
        }
        
        android.util.Log.d("MainScreen", "   ✅ ${files.size}個のPNG画像ファイル:")
        
        // 最初の5個と最後の5個を表示
        val displayFiles = if (files.size <= 10) {
            files
        } else {
            files.take(5) + files.takeLast(5)
        }
        
        displayFiles.forEachIndexed { index, file ->
            try {
                val sizeKB = file.length() / 1024
                val lastModified = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(file.lastModified()))
                
                if (files.size > 10 && index == 5) {
                    android.util.Log.d("MainScreen", "     ... (${files.size - 10}個のファイルを省略)")
                }
                
                android.util.Log.d("MainScreen", "     - ${file.name} (${sizeKB}KB, $lastModified)")
                
            } catch (e: Exception) {
                android.util.Log.d("MainScreen", "     - ${file.name} (情報取得エラー)")
            }
        }
        
        // 連続性をチェック
        val expectedFiles = (1..files.size).map { "${it.toString().padStart(3, '0')}.png" }
        val actualFiles = files.map { it.name }
        val missingFiles = expectedFiles - actualFiles.toSet()
        
        if (missingFiles.isNotEmpty()) {
            android.util.Log.d("MainScreen", "   ⚠️ 不足しているファイル: ${missingFiles.take(5).joinToString(", ")}")
            if (missingFiles.size > 5) {
                android.util.Log.d("MainScreen", "     ... and ${missingFiles.size - 5} more missing")
            }
        } else {
            android.util.Log.d("MainScreen", "   ✅ ファイル連続性: OK (001.png ~ ${files.last().name})")
        }
        
        // 総サイズ計算
        val totalSizeKB = files.sumOf { it.length() } / 1024
        android.util.Log.d("MainScreen", "   📊 総サイズ: ${totalSizeKB}KB (${totalSizeKB / 1024}MB)")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error showing directory details for $displayName", e)
    }
}

private fun copyPathToClipboard(context: Context) {
    try {
        android.util.Log.d("MainScreen", "📋 パスをクリップボードにコピー")
        
        val baseDir = context.getExternalFilesDir(null)
        if (baseDir == null) {
            android.util.Log.e("MainScreen", "❌ External files directory is null")
            return
        }
        
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        
        val pathInfo = """
アプリディレクトリ: ${baseDir.absolutePath}

画像ディレクトリ:
- WidgetImages: ${baseDir.absolutePath}/WidgetImages
- Adle: ${baseDir.absolutePath}/Mao/State/Adle  
- Flow: ${baseDir.absolutePath}/Mao/State/Flow

手動アクセス手順:
1. ファイルマネージャーを開く
2. 内部ストレージ → Android → data
3. com.seo4d696b75.android.glance_widget_demo
4. files フォルダ
        """.trimIndent()
        
        val clip = android.content.ClipData.newPlainText("アプリディレクトリパス", pathInfo)
        clipboardManager.setPrimaryClip(clip)
        
        android.util.Log.d("MainScreen", "✅ パス情報をクリップボードにコピーしました")
        android.util.Log.d("MainScreen", "📱 他のアプリでペーストできます")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error copying path to clipboard", e)
    }
}

private fun deleteDirectoryRecursively(directory: java.io.File): Boolean {
    return try {
        if (directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDirectoryRecursively(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        directory.delete()
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error deleting directory recursively: ${directory.name}", e)
        false
    }
}

private fun forceCleanupImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 強制クリーンアップ開始 ===")
        
        // 1. ウィジェットアニメーションサービスを強制停止
        try {
            val stopIntent = android.content.Intent(context, com.seo4d696b75.android.glance_widget_demo.widget.WidgetAnimationService::class.java)
            context.stopService(stopIntent)
            android.util.Log.d("MainScreen", "⏹️ Widget animation service stopped")
        } catch (e: Exception) {
            android.util.Log.w("MainScreen", "⚠️ Failed to stop widget animation service", e)
        }
        
        // 2. アプリの画像キャッシュをクリア
        System.gc()
        
        // 3. 少し待ってからファイル削除
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val baseDir = context.getExternalFilesDir(null)
            if (baseDir == null) {
                android.util.Log.e("MainScreen", "❌ External files directory is null")
                return@postDelayed
            }
            
            // 全ての画像ディレクトリを削除
            val directoriesToClean = listOf(
                java.io.File(baseDir, "WidgetImages"),
                java.io.File(baseDir, "Mao/State/Adle"),
                java.io.File(baseDir, "Mao/State/Flow"),
                java.io.File(baseDir, "Mao/State"),
                java.io.File(baseDir, "Mao")
            )
            
            var totalDeleted = 0
            var totalFailed = 0
            
            for (directory in directoriesToClean) {
                if (directory.exists()) {
                    android.util.Log.d("MainScreen", "🗑️ Cleaning directory: ${directory.name}")
                    
                    if (directory.isDirectory) {
                        val files = directory.listFiles()
                        if (files != null) {
                            for (file in files) {
                                try {
                                    if (file.isDirectory) {
                                        // サブディレクトリは再帰的に削除
                                        deleteDirectoryRecursively(file)
                                        android.util.Log.d("MainScreen", "🗑️ Deleted directory: ${file.name}")
                                        totalDeleted++
                                    } else {
                                        if (file.delete()) {
                                            android.util.Log.d("MainScreen", "🗑️ Deleted file: ${file.name}")
                                            totalDeleted++
                                        } else {
                                            android.util.Log.w("MainScreen", "⚠️ Failed to delete: ${file.name}")
                                            totalFailed++
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("MainScreen", "❌ Error deleting: ${file.name}", e)
                                    totalFailed++
                                }
                            }
                        }
                        
                        // 空になったディレクトリ自体も削除
                        try {
                            if (directory.delete()) {
                                android.util.Log.d("MainScreen", "🗑️ Deleted empty directory: ${directory.name}")
                                totalDeleted++
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("MainScreen", "⚠️ Failed to delete directory: ${directory.name}", e)
                            totalFailed++
                        }
                    }
                } else {
                    android.util.Log.d("MainScreen", "📂 Directory does not exist: ${directory.name}")
                }
            }
            
            android.util.Log.d("MainScreen", "📊 強制クリーンアップ結果: $totalDeleted deleted, $totalFailed failed")
            
            if (totalFailed > 0) {
                android.util.Log.w("MainScreen", "⚠️ 一部のファイルが削除できませんでした")
                android.util.Log.w("MainScreen", "💡 解決方法:")
                android.util.Log.w("MainScreen", "  1. アプリを完全終了してから再試行")
                android.util.Log.w("MainScreen", "  2. 端末を再起動してから再試行")
                android.util.Log.w("MainScreen", "  3. 設定 → アプリ → このアプリ → ストレージ → キャッシュクリア")
            } else {
                android.util.Log.d("MainScreen", "✅ 全ての画像ファイルが正常に削除されました")
            }
            
            // 4. SharedPreferencesもクリア
            try {
                val prefs = context.getSharedPreferences("actual_frame_counts", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
                android.util.Log.d("MainScreen", "🧹 SharedPreferences (actual_frame_counts) もクリアしました")
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "❌ Error clearing SharedPreferences", e)
            }
            
            // 5. アニメーション状態もリセット
            try {
                val animationStateManager = com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager.getInstance(context)
                // デフォルト状態にリセット（必要に応じて）
                android.util.Log.d("MainScreen", "🔄 アニメーション状態管理も初期化可能")
            } catch (e: Exception) {
                android.util.Log.e("MainScreen", "❌ Error resetting animation state", e)
            }
            
            // 6. 再度ガベージコレクション
            System.gc()
            
            // 7. 最終状態確認
            android.util.Log.d("MainScreen", "")
            android.util.Log.d("MainScreen", "📊 クリーンアップ後の状態確認:")
            showCurrentDirectoryStatus(context)
            
            android.util.Log.d("MainScreen", "=== 強制クリーンアップ完了 ===")
            android.util.Log.d("MainScreen", "✅ すべての画像ファイルとデータがクリアされました")
            android.util.Log.d("MainScreen", "🔄 新しい画像をダウンロードする準備が整いました")
            
        }, 2000L) // 2秒後に実行
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error in force cleanup", e)
    }
}

private fun checkPermissionsAndOrganize(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 権限確認と画像整理 ===")
        android.util.Log.d("MainScreen", "📱 Android API Level: ${Build.VERSION.SDK_INT}")
        
        // Scoped Storage対応の権限チェック
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q // Android 10未満のみ権限必要
        val hasWritePermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10+ では scoped storage により権限不要
        }
        
        val hasReadPermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 10+ では scoped storage により権限不要
        }
        
        android.util.Log.d("MainScreen", "📄 権限確認結果:")
        android.util.Log.d("MainScreen", "  - Scoped Storage: ${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "✅ 有効 (権限不要)" else "❌ 無効 (権限必要)"}")
        android.util.Log.d("MainScreen", "  - WRITE_EXTERNAL_STORAGE: ${if (hasWritePermission) "✅" else "❌"}")
        android.util.Log.d("MainScreen", "  - READ_EXTERNAL_STORAGE: ${if (hasReadPermission) "✅" else "❌"}")
        
        if (hasWritePermission && hasReadPermission) {
            android.util.Log.d("MainScreen", "✅ 権限OK - 自動で画像整理を開始")
            organizeExistingImages(context)
        } else {
            android.util.Log.w("MainScreen", "⚠️ 権限がまだ許可されていません (Android 9以下)")
            android.util.Log.w("MainScreen", "手動で設定から権限を許可してください：")
            android.util.Log.w("MainScreen", "設定 → アプリ → glance_widget_demo → 権限 → ストレージ")
        }
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error checking permissions and organizing", e)
    }
}