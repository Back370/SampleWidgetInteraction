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
    onWidgetSettingsClick: () -> Unit = {}
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
                        
                        // ウィジェット設定画面への遷移ボタン
                        Button(
                            onClick = onWidgetSettingsClick,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "🛠️ ウィジェット設定",
                                fontSize = 16.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
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

// ... existing helper functions from original MainScreen.kt will be here ...

private fun updateWidgetAfterToggle(context: Context) {
    try {
        android.util.Log.d("MainScreen", "🔄 Updating widget after animation toggle")
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
        if (widgetIds.isNotEmpty()) {
            val toggleIntent = Intent().apply {
                component = componentName
                action = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
                putExtra("FROM_APP", true)
                putExtra("TOGGLE_TIME", System.currentTimeMillis())
            }
            context.sendBroadcast(toggleIntent)
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
                } catch (e: Exception) {
                    android.util.Log.e("MainScreen", "❌ Error in retry toggle", e)
                }
            }, 500L)
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
        val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
        val hasWritePermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        val hasReadPermission = if (needsStoragePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
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
        }
        
        android.util.Log.d("MainScreen", "=== 権限とディレクトリ確認完了 ===")
        
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error checking permissions", e)
    }
}

private fun organizeExistingImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 既存画像の整理開始 ===")
        // Implementation would be here...
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error organizing existing images", e)
    }
}

private fun requestStoragePermission(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 権限要求処理 ===")
        // Implementation would be here...
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error requesting storage permission", e)
    }
}

private fun forceCleanupImages(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 強制クリーンアップ開始 ===")
        // Implementation would be here...
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error in force cleanup", e)
    }
}

private fun showExternalStorageAccess(context: Context) {
    try {
        android.util.Log.d("MainScreen", "=== 外部ストレージアクセス情報 ===")
        // Implementation would be here...
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error showing external storage access", e)
    }
}

private fun copyPathToClipboard(context: Context) {
    try {
        android.util.Log.d("MainScreen", "📋 パスをクリップボードにコピー")
        // Implementation would be here...
    } catch (e: Exception) {
        android.util.Log.e("MainScreen", "❌ Error copying path to clipboard", e)
    }
} 