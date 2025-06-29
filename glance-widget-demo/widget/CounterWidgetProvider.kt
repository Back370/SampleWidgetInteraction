package com.seo4d696b75.android.glance_widget_demo.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews
import com.seo4d696b75.android.glance_widget_demo.domain.CountRepository
import com.seo4d696b75.android.glance_widget_demo.data.CountRepositoryImpl

class CounterWidgetProvider : AppWidgetProvider() {

    private val repository: CountRepository = CountRepositoryImpl()

    companion object {
        private const val ACTION_INCREMENT = "com.seo4d696b75.android.glance_widget_demo.widget.ACTION_INCREMENT"
        private const val ACTION_DECREMENT = "com.seo4d696b75.android.glance_widget_demo.widget.ACTION_DECREMENT"
        private const val PREFS_NAME = "WidgetPrefs"
        private const val KEY_COUNT_PREFIX = "count_"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, repository)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, CounterWidgetProvider::class.java)
        )

        val repo = repository

        when (intent.action) {
            ACTION_INCREMENT -> {
                for (appWidgetId in appWidgetIds) {
                    val currentCount = getCount(context, appWidgetId)
                    val newCount = repo.increment(currentCount)
                    saveCount(context, appWidgetId, newCount)
                    updateAppWidget(context, appWidgetManager, appWidgetId, repo)
                }
            }
            ACTION_DECREMENT -> {
                for (appWidgetId in appWidgetIds) {
                    val currentCount = getCount(context, appWidgetId)
                    val newCount = repo.decrement(currentCount)
                    saveCount(context, appWidgetId, newCount)
                    updateAppWidget(context, appWidgetManager, appWidgetId, repo)
                }
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        @Suppress("UNUSED_PARAMETER") repository: CountRepository
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        // カウント値を取得して表示
        val count = getCount(context, appWidgetId)
        views.setTextViewText(R.id.text_count, count.toString())

        // インクリメントボタンのPendingIntent
        val incrementIntent = Intent(context, CounterWidgetProvider::class.java).apply {
            action = ACTION_INCREMENT
        }
        val incrementPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            incrementIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.button_increment, incrementPendingIntent)

        // デクリメントボタンのPendingIntent
        val decrementIntent = Intent(context, CounterWidgetProvider::class.java).apply {
            action = ACTION_DECREMENT
        }
        val decrementPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            decrementIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.button_decrement, decrementPendingIntent)

        // ウィジェットを更新
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getCount(context: Context, appWidgetId: Int): Int {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_COUNT_PREFIX + appWidgetId, 0)
    }

    private fun saveCount(context: Context, appWidgetId: Int, count: Int) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COUNT_PREFIX + appWidgetId, count).apply()
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        // ウィジェットが削除されたときに対応するデータを削除
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(KEY_COUNT_PREFIX + appWidgetId)
        }
        editor.apply()
    }
}