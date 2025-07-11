package com.seo4d696b75.android.glance_widget_demo.sensor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.SensorRepository // coreモジュールのRepository
import com.example.core.SensorUiState // coreモジュールのUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// AndroidViewModelを継承して、安全にContextを取得する
class SensorViewModel(application: Application) : AndroidViewModel(application) {

    // 1. ViewModel内で状態を保持するためのMutableStateFlow
    private val _uiState = MutableStateFlow(SensorUiState()) // 初期値を設定

    // 2. UIには不変のStateFlowとして公開する
    val uiState: StateFlow<SensorUiState> = _uiState.asStateFlow()

    // 3. ViewModelが生成されたときに、データ収集を開始する
    init {
        viewModelScope.launch {
            SensorRepository.getSensorData(getApplication())
                .collect { newState ->
                    // Repositoryから新しいデータが流れてくるたびに、自身の状態を更新
                    _uiState.value = newState
                }
        }
    }
}