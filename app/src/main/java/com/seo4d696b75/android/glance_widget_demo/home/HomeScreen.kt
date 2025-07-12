package com.seo4d696b75.android.glance_widget_demo.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seo4d696b75.android.glance_widget_demo.Button
import com.seo4d696b75.android.glance_widget_demo.R
import com.seo4d696b75.android.glance_widget_demo.notification.FloatingNotification
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList.notificationList
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationScreen

//@Preview(showBackground = true)
@Composable
fun HomeScreen(
    onCharacterClicked: () -> Unit = {},
    onSettingsClicked: () -> Unit = {},
    onWidgetSettingClicked: () -> Unit = {},
    onSensorClicked: () -> Unit = {}
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(
            text = "Widget Kawaii",
            fontSize = 55.sp
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
        onNotificationClicked = { screen.value = true },
        onWidgetSettingClicked = onWidgetSettingClicked
    )

    if (screen.value) {
        NotificationScreen(
            notificationList = notificationList,
            onBackClicked = { screen.value = false }
        )
    }
}

