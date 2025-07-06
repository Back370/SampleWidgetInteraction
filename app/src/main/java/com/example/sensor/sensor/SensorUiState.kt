package com.example.sensor.sensor

data class SensorUiState(
    val xAxis: Float = 0f,
    val yAxis: Float = 0f,
    val zAxis: Float = 0f,
    val isShaking: Boolean = false
)