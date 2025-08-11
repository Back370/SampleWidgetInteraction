package com.seo4d696b75.android.glance_widget_demo.time

import android.content.Context
import android.util.Log
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.seo4d696b75.android.glance_widget_demo.response.GeminiModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class GeminiData(val geminiModel: GeminiModel)

class UploadWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {
    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        uploadText()

        return Result.success()
    }

    fun uploadText() {
        val currentDateTime =  LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedDateTime = currentDateTime.format(formatter)
        val GeminiJsonData = inputData.getString("gemini_json_data")

        val gson = Gson()
        val Gemini_Data = try {
            gson.fromJson(GeminiJsonData, GeminiData::class.java)
        } catch (e: Exception) {
            Log.e("MyDataProcessingWorker", "ユーザーデータのデシリアライズに失敗しました。", e)
            null
        }

        val random = (0..10).random()

        if (Gemini_Data != null) {
                Gemini_Data.geminiModel.GreetingByGemini(formattedDateTime)
        }

    }


} 