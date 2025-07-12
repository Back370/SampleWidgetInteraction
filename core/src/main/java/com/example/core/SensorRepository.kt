// core/src/main/java/com/example/core/SensorRepository.kt

package com.example.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs
import kotlin.math.atan2

object SensorRepository {

    fun getSensorData(context: Context): Flow<SensorUiState> = callbackFlow {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        var lastUpdate: Long = 0
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        val SHAKE_THRESHOLD = 1200

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return

                val currentTime = System.currentTimeMillis()
                var isShaking = false
                if ((currentTime - lastUpdate) > 100) {
                    val diffTime = (currentTime - lastUpdate)
                    lastUpdate = currentTime

                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val speed = abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                    isShaking = speed > SHAKE_THRESHOLD

                    val xzAngle = Math.toDegrees(atan2(x.toDouble(), z.toDouble())).toFloat()
                    val yzAngle = Math.toDegrees(atan2(y.toDouble(), z.toDouble())).toFloat()

                    val uiState = SensorUiState(
                        xzAngle = xzAngle,
                        yzAngle = yzAngle, // yzAngleをセット
                        isAngleExceeded = abs(xzAngle) > 30f,
                        isShaking = isShaking // isShakingをセット
                    )
                    trySend(uiState)

                    lastX = x
                    lastY = y
                    lastZ = z
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { /* No-op */ }
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}