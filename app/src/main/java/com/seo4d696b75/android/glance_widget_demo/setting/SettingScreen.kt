package com.seo4d696b75.android.glance_widget_demo

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.CharacterDisplaySettings

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun settingScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "設定"
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .padding(innerPadding)
        ) {
            WidgetDisplaySetting()         //音量設定
            notificationSet()   //通知設定
        }
    }
}

/**
 * ウィジェットの更新を通知する
 */
private fun notifyWidgetUpdate(context: android.content.Context) {
    try {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(
            context.packageName,
            "com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider"
        )
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            val intent = Intent(context, 
                Class.forName("com.seo4d696b75.android.glance_widget_demo.widget.CounterWidgetProvider")
            ).apply {
                action = "com.seo4d696b75.android.glance_widget_demo.SETTINGS_CHANGED"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(intent)
        }
    } catch (e: Exception) {
        // エラーログ
        android.util.Log.e("SettingScreen", "Failed to notify widget update", e)
    }
}

@Composable
fun WidgetDisplaySetting(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displaySettings = remember { CharacterDisplaySettings.getInstance(context) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
    ){
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "横幅"
            )
            SliderUI(
                initialValue = displaySettings.getWidthScale(),
                onValueChange = { value -> 
                    displaySettings.setWidthScale(value)
                    notifyWidgetUpdate(context)
                },
                valueRange = 0.1f..1.0f
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "縦幅"
            )
            SliderUI(
                initialValue = displaySettings.getHeightScale(),
                onValueChange = { value -> 
                    displaySettings.setHeightScale(value)
                    notifyWidgetUpdate(context)
                },
                valueRange = 0.1f..1.0f
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "画質"
            )
            SliderUI(
                initialValue = displaySettings.getQualityScale(),
                onValueChange = { value -> 
                    displaySettings.setQualityScale(value)
                    notifyWidgetUpdate(context)
                },
                valueRange = 0.1f..1.0f
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "FPS"
            )
            SliderUI(
                initialValue = displaySettings.getFpsScale(),
                onValueChange = { value ->
                    displaySettings.setFpsScale(value)
                    notifyWidgetUpdate(context)
                },
                valueRange = 0.1f..1.0f
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderUI(
    initialValue: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f
) {
    // 初期値が範囲内に収まるように調整
    val clampedInitialValue = initialValue.coerceIn(valueRange.start, valueRange.endInclusive)
    var sliderPosition by remember { mutableFloatStateOf(clampedInitialValue) }
    val interactionSource = remember { MutableInteractionSource() }

    Row (
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                // 値が範囲内に収まるように調整
                val clampedValue = newValue.coerceIn(valueRange.start, valueRange.endInclusive)
                sliderPosition = clampedValue
                onValueChange(clampedValue)
            },
            modifier = Modifier.weight(1f),
            valueRange = valueRange,
            interactionSource = interactionSource,
            thumb = {
                Box (
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            },
            track = { sliderState ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(2.dp)
                        )
                ) {
                    // 値の範囲を0.0-1.0に正規化してプログレスバーを表示
                    val rangeSize = valueRange.endInclusive - valueRange.start
                    val progress = if (rangeSize > 0f) {
                        ((sliderState.value - valueRange.start) / rangeSize).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        )
        
        // 現在の値を表示
        Text(
            text = String.format("%.1f", sliderPosition),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun notificationSet(
    modifier: Modifier = Modifier
) {
    var checked by remember { mutableStateOf(true) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(all = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "通知"
        )
        Switch(
            checked = checked,
            onCheckedChange = {
                checked = !checked
            }
        )
    }
}
