package com.seo4d696b75.android.glance_widget_demo.character

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seo4d696b75.android.glance_widget_demo.Button
import com.seo4d696b75.android.glance_widget_demo.R
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager


//@Preview(showBackground = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "キャラ切替"
                    )
                        },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                colors = topAppBarColors(
                    containerColor = Color.Cyan.copy(alpha = 0.1f)
                ),
                windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
            )
        }
    ) {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_face_24),
                contentDescription = null,
                modifier = Modifier.size(300.dp)
            )

            Spacer(modifier = Modifier.height(50.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    icon = painterResource(id = R.drawable.baseline_face_24),
                    text = "Mao",
                    onClick = {
                        android.util.Log.d("CharacterScreen", "📥 Maoキャラクター選択")
                        val animationStateManager = AnimationStateManager.getInstance(context)
                        animationStateManager.setCharacterId(AnimationStateManager.CHARACTER_ID_MAO)
                        updateWidgetAfterCharacterChange(context)
                    }
                )

                Button(
                    modifier = Modifier.weight(1f),
                    icon = painterResource(id = R.drawable.baseline_face_24),
                    text = "Haru",
                    onClick = {
                        android.util.Log.d("CharacterScreen", "📥 Haruキャラクター選択")
                        val animationStateManager = AnimationStateManager.getInstance(context)
                        animationStateManager.setCharacterId(AnimationStateManager.CHARACTER_ID_HARU)
                        updateWidgetAfterCharacterChange(context)
                    }
                )

                Button(
                    modifier = Modifier.weight(1f),
                    icon = painterResource(id = R.drawable.baseline_face_24),
                    text = "キャラC",
                    onClick = {
                        android.util.Log.d("CharacterScreen", "📥 キャラC選択（未実装）")
                        // キャラCは未実装のためログのみ
                    }
                )

            }
        }
    }
}

// ヘルパー関数: キャラクター変更後のウィジェット更新
private fun updateWidgetAfterCharacterChange(context: android.content.Context) {
    try {
        android.util.Log.d("CharacterScreen", "🔄 キャラクター変更後ウィジェット更新開始")
        
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            "com.seo4d696b75.android.glance_widget_demo",
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        android.util.Log.d("CharacterScreen", "📊 見つかったウィジェットID: ${appWidgetIds.contentToString()}")

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent().apply {
                component = componentName
                action = "com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                putExtra("CHARACTER_CHANGED", true)
            }
            context.sendBroadcast(intent)
            android.util.Log.d("CharacterScreen", "✅ キャラクター変更ブロードキャスト送信")
            
            // アニメーション状態を再確認
            val animationStateManager = AnimationStateManager.getInstance(context)
            android.util.Log.d("CharacterScreen", "📊 送信後の状態確認: ${animationStateManager.getCurrentAnimationDisplayText()}")
        } else {
            android.util.Log.w("CharacterScreen", "⚠️ 更新対象のウィジェットが見つかりません")
        }
    } catch (e: Exception) {
        android.util.Log.e("CharacterScreen", "❌ ウィジェット更新エラー", e)
    }
}