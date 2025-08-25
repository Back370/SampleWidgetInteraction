package com.seo4d696b75.android.glance_widget_demo.home

import android.util.Log
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seo4d696b75.android.glance_widget_demo.Button
import com.seo4d696b75.android.glance_widget_demo.R
import com.seo4d696b75.android.glance_widget_demo.data.ImageDownloadService
//import com.seo4d696b75.android.glance_widget_demo.notification.FloatingAdd
import com.seo4d696b75.android.glance_widget_demo.notification.FloatingNotification
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList.notificationList
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationScreen
import com.seo4d696b75.android.glance_widget_demo.notification.TextBox
import com.seo4d696b75.android.glance_widget_demo.response.GeminiModel

val MyCustomFontFamily = FontFamily(
    Font(R.font.futuralightbt))

@Preview(showBackground = true)
@Composable
fun HomeScreen(
    onCharacterClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onWidgetSettingClicked: () -> Unit = {},
    geminiModel: GeminiModel = viewModel(),
    input: String = "女の子のように３行以内で絵文字を使わないで可愛くて自然な挨拶をしてほしいな、あと今日の名古屋の天気もあなたがweb検索して教えて～",
    onSensorClicked: () -> Unit = {}
){
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val initialShow = remember { prefs.getBoolean("is_first_launch", true) }
    val showFirstLaunchDownload = remember { mutableStateOf(initialShow) }
    // クリックイベント内で直接Composableを呼ばないための表示フラグ
    val showDownloadDialog = remember { mutableStateOf(false) }
    // ダウンロード進捗用ステート
    val progressCurrent = remember { mutableStateOf(0) }
    val progressTotal = remember { mutableStateOf(0) }
    val progressPercent = remember { mutableStateOf(0) }
    val progressDone = remember { mutableStateOf(false) }
    // 期待総枚数（サービス側と合わせる）
    val expectedTotal = 300

    // 初回起動であれば、このセッション中は表示し続け、次回以降は表示しないようにフラグだけ落としておく
    LaunchedEffect(initialShow) {
        if (initialShow) {
            prefs.edit().putBoolean("is_first_launch", false).apply()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .paint(
                painter = painterResource(R.drawable.okumono_game40) ,
                contentScale = ContentScale.Crop
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Widget Kawaii",
            fontSize = 55.sp,
            fontFamily = MyCustomFontFamily,
            color = Color(120,50,250)
        )

        Spacer(modifier = Modifier.height(50.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ){

            Button(
                modifier = Modifier.weight(1f),
                icon = painterResource(id = R.drawable.baseline_face_24),
                text = "キャラ切替",
                onClick = onCharacterClicked
            )

            Button(
                modifier = Modifier.weight(1f),
                icon = painterResource(id = R.drawable.baseline_settings_24),
                text = "設定",
                onClick = onSettingsClicked
            )
        }

        // 初回起動時のみ表示される「画像をダウンロードする」ボタン
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ){
        Button(
                    modifier = Modifier.weight(1f),
                    icon = painterResource(id = R.drawable.baseline_notifications_24),
                    text = "画像をダウンロードする",
                    onClick = {
                        ImageDownloadService.downloadCharacterImages(context, "Mao")
                        ImageDownloadService.downloadCharacterImages(context, "Haru")
                        showFirstLaunchDownload.value = false
                        // 先に表示を出して受信準備
                        showDownloadDialog.value = true
            // 初期化
            progressCurrent.value = 0
                        progressTotal.value = expectedTotal // 分母が?にならないよう即設定
            progressPercent.value = 0
            progressDone.value = false
                    }
                )
            }


//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(10.dp),
//            horizontalArrangement = Arrangement.SpaceEvenly,
//            verticalAlignment = Alignment.CenterVertically,
//        ){
//
//            Button(
//                modifier = Modifier.weight(1f),
//                icon = painterResource(id = R.drawable.baseline_notifications_24),
//                text = "センサー",
//                onClick = onSensorClicked
//            )
//
//        }
    }

    val screen = remember { mutableStateOf(false) }

    //FloatingAdd(onAddClicked = { NotificationList.Add("test") } )
    FloatingNotification(
        onNotificationClicked = {screen.value = true},
        onWidgetSettingClicked = onWidgetSettingClicked,
        onRunGeminiClicked = { geminiModel.startGeminiProcess(input) }
    )



    if (screen.value) {
        NotificationScreen(
            notificationList = notificationList,
            onBackClicked = { screen.value = false }
        )
    }

    // ダウンロード待機ダイアログを必要に応じて表示
    if (showDownloadDialog.value) {
        // BroadcastReceiver 登録/解除
    androidx.compose.runtime.DisposableEffect(Unit) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(p0: android.content.Context?, intent: Intent?) {
                    if (intent?.action == ImageDownloadService.ACTION_PROGRESS_UPDATE) {
                        val cur = intent.getIntExtra(ImageDownloadService.EXTRA_PROGRESS_CURRENT, 0)
                        val tot = intent.getIntExtra(ImageDownloadService.EXTRA_PROGRESS_TOTAL, 0)
                        val per = intent.getIntExtra(ImageDownloadService.EXTRA_PROGRESS_PERCENT, 0)
                        val done = intent.getBooleanExtra(ImageDownloadService.EXTRA_PROGRESS_DONE, false)
                        Log.d("HomeScreen", "📥 Progress recv cur=$cur tot=$tot per=$per done=$done")
                        progressCurrent.value = cur
                        // 0 が届いた場合は期待総数で補完
                        progressTotal.value = if (tot > 0) tot else expectedTotal
                        progressPercent.value = per
                        progressDone.value = done
                        if (done) {
                            // 自動的に閉じる
                            showDownloadDialog.value = false
                        }
                    }
                }
            }
            val filter = IntentFilter(ImageDownloadService.ACTION_PROGRESS_UPDATE)
            // Android 14 (API34) 以降はRECEIVER_EXPORTED/NOT_EXPORTED指定が必須
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // アプリ内限定なので NOT_EXPORTED
                context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(receiver, filter)
            }
            onDispose {
                try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }
        WaitDownLoadScreen(
            current = progressCurrent.value,
            total = if (progressTotal.value > 0) progressTotal.value else expectedTotal,
            percent = progressPercent.value,
            done = progressDone.value,
            onBackClicked = { showDownloadDialog.value = false }
        )
    }
}

@Composable
fun WaitDownLoadScreen(
    current: Int,
    total: Int,
    percent: Int,
    done: Boolean,
    onBackClicked: () -> Unit = {},
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (done) "ダウンロード完了" else "画像をダウンロード中...",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else (percent / 100f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                    Text(
                        text = buildString {
                            append(percent.coerceIn(0, 100))
                            append("%  (")
                            append(current)
                            append(" / ")
                            append(if (total > 0) total else 300)
                            append(")")
                        },
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

