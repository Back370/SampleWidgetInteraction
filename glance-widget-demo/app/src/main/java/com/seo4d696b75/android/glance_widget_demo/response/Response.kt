package com.seo4d696b75.android.glance_widget_demo.response

import android.util.Log
import androidx.compose.material3.ModalBottomSheetDefaults.properties
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel

suspend fun RunGemini() {
    try {
        // モデルの準備
        val model = GenerativeModel(
            modelName = "gemini-pro",
            apiKey = BuildConfig.apiKey
        )

        // 推論の実行
        val response = model.generateContent("日本一高い山は？")
        Log.d("MyTag", response.text.toString())
    } catch (e: Exception) {
        Log.e("MyTag", "Error: ${e.message}", e)
    }
}