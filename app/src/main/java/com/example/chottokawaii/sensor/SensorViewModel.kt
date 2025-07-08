package com.example.chottokawaii.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.abs

class SensorViewModel : ViewModel(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    private var lastUpdate: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var lastZ: Float = 0f
    // この値は端末によって調整が必要
    companion object {
        private const val SHAKE_THRESHOLD = 1200
    }

    // --- UI Stateの定義 ---
    private val _uiState = MutableStateFlow(SensorUiState())
    val uiState = _uiState.asStateFlow()

    // --- センサーの開始・停止 (変更なし) ---
    fun startListening(context: Context) {
        if (sensorManager == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        }
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    // --- センサーの値が変更されたときの処理を更新 ---
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

        val currentTime = System.currentTimeMillis()
        if ((currentTime - lastUpdate) > 100) { // 100msごとにチェック
            val diffTime = (currentTime - lastUpdate)
            lastUpdate = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000

            val isShaking = speed > SHAKE_THRESHOLD

            // --- Stateを更新 ---
            _uiState.update { currentState ->
                currentState.copy(
                    xAxis = x,
                    yAxis = y,
                    zAxis = z,
                    isShaking = isShaking // シェイク状態を更新
                )
            }

            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* No-op */ }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}