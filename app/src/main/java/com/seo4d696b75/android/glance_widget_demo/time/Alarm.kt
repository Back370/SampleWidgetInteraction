package com.seo4d696b75.android.glance_widget_demo.time

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList.Clear
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList.IsEmpty
import java.util.Calendar
import kotlin.jvm.java

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("MyAlarmReceiver", "アラーム実行")
        Clear()
        Log.d("MyAlarmReceiver", "通知の削除" + IsEmpty().toString())
    }
}

fun setDailyAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 毎日朝6時の時刻を設定
    val calendar = Calendar.getInstance().apply{
        Log.d("date", "現在の日付：" + Calendar.DAY_OF_YEAR)
        timeInMillis = System.currentTimeMillis()
        set(Calendar.HOUR_OF_DAY, 6)  // 6時
        set(Calendar.MINUTE, 0)      // 0分
        set(Calendar.SECOND, 0)      // 0秒
        set(Calendar.DAY_OF_MONTH, 12)
        Log.d("date", "現在の日付：" + get(Calendar.DAY_OF_MONTH))
        Log.d("date", "現在の時間：" + Calendar.HOUR_OF_DAY)

        // もし設定時間が現在時刻より前なら、翌日の朝8時に設定する
        /*
        if (timeInMillis <= System.currentTimeMillis()) {
            add(Calendar.DAY_OF_YEAR, 1)
        }
        */
    }

    // 処理を実行するためのPendingIntentを作成
    val intent = Intent(context, DailyAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // アラームを設定
    // setRepeatingは、指定した間隔でアラームを繰り返す
    alarmManager.setRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY, // 1日（24時間）ごとに繰り返す
        pendingIntent
    )
}