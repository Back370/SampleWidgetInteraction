package com.seo4d696b75.android.glance_widget_demo.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
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
//import com.seo4d696b75.android.glance_widget_demo.notification.FloatingAdd
import com.seo4d696b75.android.glance_widget_demo.notification.FloatingNotification
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList.notificationList
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationScreen
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ){

            Button(
                modifier = Modifier.weight(1f),
                icon = painterResource(id = R.drawable.baseline_notifications_24),
                text = "センサー",
                onClick = onSensorClicked
            )

        }
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
}

