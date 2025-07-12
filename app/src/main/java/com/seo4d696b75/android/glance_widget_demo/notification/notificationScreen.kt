package com.seo4d696b75.android.glance_widget_demo.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel

object NotificationList : ViewModel() {
    val notificationList = mutableStateListOf<String>()

    fun Add(text: String) {
        notificationList.add(text)
    }

    fun Remove(text: String) {
        notificationList.remove(text)
    }


}

@Composable
fun NotificationScreen(
    onBackClicked: () -> Unit = {},
    notificationList: List<String> = listOf<String>(),
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box {
            FloatingActionButton(
                onClick = onBackClicked,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
            ) {
                Icon(
                    Icons.Default.Close,
                    "close",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(24.dp)
                )

                Text(
                    text = "通知",
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .padding(horizontal = 8.dp)
                        .padding(top = 48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = notificationList
                    ) { item ->
                        TextBox(text = item)
                    }
                }
            }
        }
    }
}

//
//@Composable
//fun FloatingAdd(
//    onAddClicked: (String) -> Unit = { text -> NotificationList.Add(text) },
//    text: String = "test",
//) {
//    Box(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        FloatingActionButton(
//            onClick = { onAddClicked(text) },
//            modifier = Modifier
//                .align(Alignment.TopStart)
//        ) {
//            Icon(Icons.Default.Add, "Add")
//        }
//    }
//}

//通知ボタン
@Composable
fun FloatingNotification(
    //引数であるonNotificationClicked関数は引数をとらず戻り値を返さない
    onNotificationClicked: () -> Unit = {},
    onWidgetSettingClicked: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
           modifier = Modifier
                .align(Alignment.TopEnd)

        ) {
            FloatingActionButton(
                onClick = onNotificationClicked,
                modifier = Modifier
                ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notification"
                )
            }
            FloatingActionButton(
                onClick = onWidgetSettingClicked,
                modifier = Modifier
            ) {
                Icon(
                    Icons.Default.Tab,
                    contentDescription = "GoToWidgetSetting"
                )
            }
        }
    }
}


@Composable
fun TextBox(
    text: String = "",
) {
    Box(
        modifier = Modifier
            .background(color = Color.Gray.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp)
    ) {
        Text(text = text)
    }
}



//
//@Preview(showBackground = true)
//@Composable
//fun ScreenPreview(
//    modifier: Modifier = Modifier
//        .fillMaxSize(),
//) {
//    val screen = remember { mutableStateOf(false) }
//
//    FloatingAdd(onAddClicked = {
//        NotificationList.Add("test")
//    }
//    )
//
//    FloatingNotification(onNotificationClicked = {screen.value = true})
//
//    if (screen.value) {
//        NotificationScreen(
//            notificationList = notificationList,
//            onBackClicked = { screen.value = false }
//        )
//    }
//
//}

