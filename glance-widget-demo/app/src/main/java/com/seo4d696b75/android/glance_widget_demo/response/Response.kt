package com.seo4d696b75.android.glance_widget_demo.response

import android.util.Log
import androidx.compose.material3.ModalBottomSheetDefaults.properties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.google.ai.client.generativeai.BuildConfig
import com.seo4d696b75.android.glance_widget_demo.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.seo4d696b75.android.glance_widget_demo.notification.NotificationList
import kotlinx.coroutines.launch

class GeminiModel : ViewModel() {

    fun startGeminiProcess(input: String = "絵文字を使わないで可愛くて自然な挨拶をして！"){

        Log.d("MyTag", "startGeminiProcess called with input: $input")
        // viewModelScope は ViewModel のライフサイクルに紐づくコルーチンスコープ

        var response : String = ""
        viewModelScope.launch { // launch は新しいコルーチンを開始する
            response = RunGemini(input) // suspend 関数をコルーチン内で呼び出す

            NotificationList.Add(response)
        }
    }

    suspend fun RunGemini(
        input: String = "絵文字を使わないで可愛くて自然な挨拶をして！"
    ) : String {
        var response : String = ""
        try {
            // モデルの準備
            val model = GenerativeModel(
                modelName = "gemini-1.5-pro",
                apiKey = BuildConfig.apiKey
            )

            // 推論の実行
            Log.d("MyTag", response)
            response = (model.generateContent(prompt = input)).text.toString()
        } catch (e: Exception) {
            Log.e("MyTag", "Error: ${e.message}", e)
        }

        return response
    }

    fun GreetingByGemini(input: String) {
        startGeminiProcess("現在の時刻は${input}です。絵文字を使わないで可愛くて自然な挨拶をして！")
    }

    fun WeatherByGemini(input: String) {
        startGeminiProcess("絵文字を使わないで可愛く自然に今日と明日の名古屋の天気予報をして！")
    }
}