package com.seo4d696b75.android.glance_widget_demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService
import com.seo4d696b75.android.glance_widget_demo.domain.ImageCacheRepository
import com.seo4d696b75.android.glance_widget_demo.domain.SampleCharacterConfigs
import kotlinx.coroutines.delay

/**
 * 画像キャッシュ管理画面
 * アプリ内でキャッシュ管理機能を提供
 */
@Composable
fun ImageCacheManagerScreen(
    imageCacheRepository: ImageCacheRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var cacheSize by remember { mutableStateOf(0L) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // キャッシュサイズを定期的に更新
    LaunchedEffect(Unit) {
        while (true) {
            cacheSize = imageCacheRepository.getCacheSize()
            delay(2000) // 2秒ごとに更新
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ヘッダー
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "画像キャッシュ管理",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ウィジェット用画像の管理",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // キャッシュ情報
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "キャッシュ情報",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("キャッシュサイズ:")
                        Text(
                            text = formatBytes(cacheSize),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("保存場所:")
                        Text(
                            text = "外部キャッシュ",
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // キャラクター画像ダウンロード
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "キャラクター画像ダウンロード",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 各キャラクターのダウンロードボタン
                    SampleCharacterConfigs.getAllCharacters().forEach { character ->
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = character.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        ImageDownloadService.downloadCharacterImages(
                                            context,
                                            character.characterId
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("全アニメーション")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        ImageDownloadService.downloadCharacterImages(
                                            context,
                                            character.characterId,
                                            "idle"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("アイドルのみ")
                                }
                            }
                        }
                    }
                }
            }
            
            // 全体操作
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "全体操作",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                ImageDownloadService.downloadAllImages(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("全画像ダウンロード")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                ImageDownloadService.clearCache(context)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("キャッシュクリア")
                        }
                    }
                }
            }
            
            // 使用方法
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "使用方法",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = """
                        1. キャラクター画像をダウンロードしてキャッシュに保存
                        2. ウィジェットでキャッシュされた画像を表示
                        3. 同じ画像は再DLせず、キャッシュから読み込み
                        4. FileProvider経由で安全にアクセス
                        
                        保存構造:
                        /Android/data/[package]/cache/widget_assets/
                        ├── character001/
                        │   ├── idle/
                        │   │   ├── frame_00.png
                        │   │   ├── frame_01.png
                        │   ├── tap/
                        ├── character002/
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * バイト数を読みやすい形式に変換
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
} 