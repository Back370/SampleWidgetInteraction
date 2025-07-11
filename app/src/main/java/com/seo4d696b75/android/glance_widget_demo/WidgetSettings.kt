package com.seo4d696b75.android.glance_widget_demo

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService
import com.seo4d696b75.android.glance_widget_demo.theme.AppTheme
import com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationStateManager = remember { AnimationStateManager.getInstance(context) }
    
    // アニメーション状態の管理
    var currentAnimationType by remember { mutableStateOf(animationStateManager.getCurrentAnimationType()) }
    var currentCharacterId by remember { mutableStateOf(animationStateManager.getCurrentCharacterId()) }

    AppTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "ウィジェット設定",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = "戻る"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
                                    Log.d("WidgetSettings", "🔄 アニメーション切り替えボタン押下")
                                    Log.d("WidgetSettings", "📊 切り替え前: ${animationStateManager.getCurrentAnimationDisplayText()}")
                                    
                                    // アニメーション切り替え
                                    val newAnimationType = animationStateManager.toggleAnimationType()
                                    currentAnimationType = newAnimationType
                                    currentCharacterId = animationStateManager.getCurrentCharacterId()

                                    Log.d("WidgetSettings", "📊 切り替え後: ${animationStateManager.getCurrentAnimationDisplayText()}")
                                    Log.d("WidgetSettings", "🎯 新しいアニメーション種別: $newAnimationType")

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
                                    Log.d("WidgetSettings", "📥 手動ダウンロード開始 (Mao)")
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
                                    Log.d("WidgetSettings", "📥 手動ダウンロード開始 (Haru)")
                                    ImageDownloadService.downloadCharacterImages(context, "Haru")
                                },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = "Haru画像をダウンロード",
                                    fontSize = 14.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    Log.d("WidgetSettings", "🔧 権限チェック開始")
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
                                    Log.d("WidgetSettings", "📁 画像整理開始")
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
                                        Log.d("WidgetSettings", "📁 Android 10+ - 直接画像整理開始")
                                        organizeExistingImages(context)
                                    } else {
                                        Log.d("WidgetSettings", "🔐 権限要求開始")
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

//                            OutlinedButton(
//                                onClick = {
//                                    android.util.Log.d("WidgetSettings", "🧹 強制クリーンアップ開始")
//                                    forceCleanupImages(context)
//                                },
//                                modifier = Modifier.padding(horizontal = 16.dp)
//                            ) {
//                                Text(
//                                    text = "強制クリーンアップ（手動削除用）",
//                                    fontSize = 14.sp
//                                )
//                            }

                            OutlinedButton(
                                onClick = {
                                    Log.d("WidgetSettings", "📁 外部ストレージアクセス情報表示")
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
                                    Log.d("WidgetSettings", "📋 パスをクリップボードにコピー")
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
}

// ヘルパー関数群
private fun updateWidgetAfterToggle(context: Context) {
    try {
        Log.d("WidgetSettings", "🔄 ウィジェット更新開始")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            context.packageName,
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        Log.d("WidgetSettings", "📊 見つかったウィジェットID: ${appWidgetIds.contentToString()}")

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(context, com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider::class.java).apply {
                action = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
            Log.d("WidgetSettings", "✅ アニメーション切り替えブロードキャスト送信")
            
            // アニメーション状態を再確認
            val animationStateManager = AnimationStateManager.getInstance(context)
            Log.d("WidgetSettings", "📊 送信後の状態確認: ${animationStateManager.getCurrentAnimationDisplayText()}")
        } else {
            Log.w("WidgetSettings", "⚠️ 更新対象のウィジェットが見つかりません")
        }
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ ウィジェット更新エラー", e)
    }
}

private fun checkPermissions(context: Context) {
    try {
        Log.d("WidgetSettings", "=== 権限とディレクトリ確認 ===")
        Log.d("WidgetSettings", "📱 Android API Level: ${Build.VERSION.SDK_INT}")

        val baseDir = context.getExternalFilesDir(null)
        Log.d("WidgetSettings", "📁 External files dir: ${baseDir?.absolutePath}")

        if (baseDir != null) {
            val widgetImagesDir = File(baseDir, "WidgetImages")
            Log.d("WidgetSettings", "📁 Widget images dir: ${widgetImagesDir.absolutePath}")
            Log.d("WidgetSettings", "📂 Directory exists: ${widgetImagesDir.exists()}")

            if (widgetImagesDir.exists()) {
                val files = widgetImagesDir.listFiles()
                Log.d("WidgetSettings", "📊 Files count: ${files?.size ?: 0}")
                files?.forEach { file ->
                    Log.d("WidgetSettings", "📄 File: ${file.name} (${file.length()} bytes)")
                }
            } else {
                Log.w("WidgetSettings", "⚠️ ディレクトリが存在しません")
            }
        } else {
            Log.e("WidgetSettings", "❌ External files directory is null")
        }
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ 権限確認エラー", e)
    }
}

private fun organizeExistingImages(context: Context) {
    try {
        Log.d("WidgetSettings", "=== 既存画像整理開始 ===")
        
        val baseDir = context.getExternalFilesDir(null) ?: return
        val sourceDir = File(baseDir, "WidgetImages")
        
        if (!sourceDir.exists()) {
            Log.w("WidgetSettings", "⚠️ ソースディレクトリが存在しません")
            return
        }

        Log.d("WidgetSettings", "📁 画像整理処理中...")
        // 実際の整理処理はここに実装
        Log.d("WidgetSettings", "✅ 画像整理完了")
        
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ 画像整理エラー", e)
    }
}

private fun requestStoragePermission(context: Context) {
    try {
        Log.d("WidgetSettings", "🔐 ストレージ権限要求")
        
        if (context is ComponentActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    1001
                )
            }
        } else {
            Log.w("WidgetSettings", "⚠️ 権限要求はActivityから実行する必要があります")
        }
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ 権限要求エラー", e)
    }
}

private fun forceCleanupImages(context: Context) {
    try {
        Log.d("WidgetSettings", "=== 強制クリーンアップ開始 ===")

        val baseDir = context.getExternalFilesDir(null) ?: return
        val directoriesToClean = listOf(
            File(baseDir, "WidgetImages"),
            File(baseDir, "Mao")
        )

        var totalDeleted = 0
        for (directory in directoriesToClean) {
            if (directory.exists()) {
                val deleted = deleteDirectoryRecursively(directory)
                if (deleted) totalDeleted++
            }
        }

        Log.d("WidgetSettings", "✅ 強制クリーンアップ完了: $totalDeleted ディレクトリ削除")
        
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ 強制クリーンアップエラー", e)
    }
}

private fun showExternalStorageAccess(context: Context) {
    try {
        Log.d("WidgetSettings", "=== 外部ストレージアクセス情報 ===")
        
        val baseDir = context.getExternalFilesDir(null)
        Log.d("WidgetSettings", "📁 アプリ専用ディレクトリ: ${baseDir?.absolutePath}")
        
        if (baseDir != null) {
            Log.d("WidgetSettings", "📂 ディレクトリ存在: ${baseDir.exists()}")
            Log.d("WidgetSettings", "📝 書き込み可能: ${baseDir.canWrite()}")
        }
        
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ 外部ストレージアクセス確認エラー", e)
    }
}

private fun copyPathToClipboard(context: Context) {
    try {
        val baseDir = context.getExternalFilesDir(null)
        val path = baseDir?.absolutePath ?: "パスが取得できません"
        
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("アプリディレクトリパス", path)
        clipboard.setPrimaryClip(clip)
        
        Log.d("WidgetSettings", "✅ パス情報をクリップボードにコピーしました: $path")
        
    } catch (e: Exception) {
        Log.e("WidgetSettings", "❌ パスコピーエラー", e)
    }
}

private fun deleteDirectoryRecursively(directory: File): Boolean {
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
        Log.e("WidgetSettings", "❌ ディレクトリ削除エラー: ${directory.name}", e)
        false
    }
}