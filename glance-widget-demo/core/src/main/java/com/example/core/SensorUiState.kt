package com.example.core

data class SensorUiState(
    val xzAngle: Float = 0f,
    val yzAngle: Float = 0f,              // 復活
    val isAngleExceeded: Boolean = false,
    val isShaking: Boolean = false        // 復活
)