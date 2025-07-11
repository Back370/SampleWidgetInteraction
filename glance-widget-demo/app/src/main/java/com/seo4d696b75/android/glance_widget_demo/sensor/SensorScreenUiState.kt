package com.seo4d696b75.android.glance_widget_demo.sensor

import com.example.core.SensorUiState

data class SensorScreenUiState(
    // 実際にUIに表示されるセンサーの値
    val currentSensorValue: SensorUiState = SensorUiState(),

    // デバッグ機能の状態
    val isDebugMode: Boolean = false,
    val debugXzAngle: Float = 0f,
    val debugYzAngle: Float = 0f,
)