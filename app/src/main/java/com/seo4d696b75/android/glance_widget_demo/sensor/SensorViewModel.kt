package com.seo4d696b75.android.glance_widget_demo.sensor

// app/src/main/java/com/seo4d696b75/android/glance_widget_demo/sensor/SensorViewModel.kt

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SensorRepository
import com.example.core.SensorUiState
import com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SensorScreenUiState())
    val uiState: StateFlow<SensorScreenUiState> = _uiState.asStateFlow()

    private var realSensorJob: Job? = null
    
    // 60度を超えたかどうかのフラグ（一度だけ実行するため）
    private var hasTriggeredRapidFlow = false

    init {
        // 最初は本物のセンサーを起動
        startRealSensorListening()
    }

    // --- UIからのイベントを受け取る関数 ---

    fun onDebugModeChanged(isDebugMode: Boolean) {
        _uiState.update { it.copy(isDebugMode = isDebugMode) }
        if (isDebugMode) {
            // デバッグモードON: 本物センサーを止め、現在のスライダー値でUIを更新
            stopRealSensorListening()
            updateUiWithDebugValues()
        } else {
            // デバッグモードOFF: 本物センサーを再開
            startRealSensorListening()
        }
    }

    fun onDebugXzAngleChanged(angle: Float) {
        _uiState.update { it.copy(debugXzAngle = angle) }
        updateUiWithDebugValues()
        
        // デバッグモードで60度を超えた場合の処理
        if (angle > 60f && !hasTriggeredRapidFlow) {
            triggerRapidFlowState()
        } else if (angle <= 60f) {
            // 60度以下に戻ったらフラグをリセット
            hasTriggeredRapidFlow = false
        }
    }

    fun onDebugYzAngleChanged(angle: Float) {
        _uiState.update { it.copy(debugYzAngle = angle) }
        updateUiWithDebugValues()
    }

    fun onDebugShakeTriggered() {
        // デバッグモード中のみ、一時的にisShakingをtrueにしてUIを更新
        if (_uiState.value.isDebugMode) {
            viewModelScope.launch {
                val debugShakeState = _uiState.value.currentSensorValue.copy(isShaking = true)
                _uiState.update { it.copy(currentSensorValue = debugShakeState) }
            }
        }
    }

    // --- 内部ロジック ---

    private fun startRealSensorListening() {
        // 既に動いていたら何もしない
        if (realSensorJob?.isActive == true) return

        realSensorJob = viewModelScope.launch {
            SensorRepository.getSensorData(getApplication())
                .collect { realSensorValue ->
                    // デバッグモードがOFFの時だけ、本物の値でUIを更新
                    if (!_uiState.value.isDebugMode) {
                        _uiState.update { it.copy(currentSensorValue = realSensorValue) }
                        
                        // 60度を超えた場合の処理
                        if (realSensorValue.xzAngle > 60f && !hasTriggeredRapidFlow) {
                            triggerRapidFlowState()
                        } else if (realSensorValue.xzAngle <= 60f) {
                            // 60度以下に戻ったらフラグをリセット
                            hasTriggeredRapidFlow = false
                        }
                    }
                }
        }
    }

    private fun stopRealSensorListening() {
        realSensorJob?.cancel()
    }

    private fun updateUiWithDebugValues() {
        // デバッグモードがONの時だけ、スライダーの値でUIを更新
        if (_uiState.value.isDebugMode) {
            val currentState = _uiState.value
            val debugSensorValue = SensorUiState(
                xzAngle = currentState.debugXzAngle,
                yzAngle = currentState.debugYzAngle,
                isShaking = false, // シェイクはボタンでトリガーするため、通常はfalse
                isAngleExceeded = kotlin.math.abs(currentState.debugXzAngle) > 30f
            )
            _uiState.update { it.copy(currentSensorValue = debugSensorValue) }
        }
    }
    
    /**
     * RapidFlowStateを実行する
     */
    private fun triggerRapidFlowState() {
        try {
            val animationStateManager = AnimationStateManager.getInstance(getApplication())
            val newAnimationType = animationStateManager.RapidFlowState()
            
            android.util.Log.d("SensorViewModel", "🎯 XZ Angle exceeded 60°, triggered RapidFlowState: $newAnimationType")
            
            // フラグを設定して一度だけ実行されるようにする
            hasTriggeredRapidFlow = true
            
            // ウィジェットに通知を送信
            sendWidgetUpdateBroadcast()
            
        } catch (e: Exception) {
            android.util.Log.e("SensorViewModel", "❌ Error triggering RapidFlowState", e)
        }
    }
    
    /**
     * ウィジェットに更新通知を送信
     */
    private fun sendWidgetUpdateBroadcast() {
        try {
            val intent = android.content.Intent("com.seo4d696b75.android.glance_widget_demo.TOGGLE_ANIMATION").apply {
                setPackage(getApplication<Application>().packageName)
            }
            getApplication<Application>().sendBroadcast(intent)
            android.util.Log.d("SensorViewModel", "📡 Widget update broadcast sent")
        } catch (e: Exception) {
            android.util.Log.e("SensorViewModel", "❌ Error sending widget update broadcast", e)
        }
    }
}