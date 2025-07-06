package com.seo4d696b75.android.glance_widget_demo.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.glance_widget_demo.theme.AppTheme

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    AppTheme {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.main_hello_message),
                    )

                    Button(
                        onClick = {
                            forceWidgetUpdate(context)
                        }
                    ) {
                        Text("Force Widget Update")
                    }
                    
                    Button(
                        onClick = {
                            forceSystemConstraintWorkaround(context)
                        }
                    ) {
                        Text("Force System Workaround")
                    }

                    Button(
                        onClick = {
                            debugImagePath(context)
                        }
                    ) {
                        Text("Debug Path")
                    }

                    Button(
                        onClick = {
                            downloadTestImages(context)
                        }
                    ) {
                        Text("Download Images (HTTP)")
                    }

                    Button(
                        onClick = {
                            downloadFirebaseImages(context)
                        }
                    ) {
                        Text("Download Firebase Images")
                    }

                    Button(
                        onClick = {
                            requestPermissions(context)
                        }
                    ) {
                        Text("Request Permissions")
                    }

                    Button(
                        onClick = {
                            clearAllImages(context)
                        }
                    ) {
                        Text("Clear All Images")
                    }
                    
                    Button(
                        onClick = {
                            emergencyForceRestart(context)
                        }
                    ) {
                        Text("Emergency Force Restart")
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