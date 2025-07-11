package com.example.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SensorService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "SensorServiceChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        scope.launch {
            SensorRepository.getSensorData(applicationContext).collect { state ->
                // 揺れを検知したら、専用の合言葉を送信
                if (state.isShaking) {
                    sendBroadcastFor(Constants.ACTION_SHAKE_DETECTED)
                }
                // 角度のしきい値を超えたら、専用の合言葉を送信
                if (state.isAngleExceeded) {
                    sendBroadcastFor(Constants.ACTION_ANGLE_EXCEEDED)
                }
            }
        }
        return START_STICKY
    }

    private fun sendBroadcastFor(action: String) {
        val intent = Intent(action).apply {
            setPackage(applicationContext.packageName)
        }
        applicationContext.sendBroadcast(intent)
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_LOW // 通知の重要度を低めに設定
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("センサー監視中")
            .setContentText("デバイスの傾きを監視しています。")
            .setSmallIcon(R.drawable.baseline_notifications_24)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}