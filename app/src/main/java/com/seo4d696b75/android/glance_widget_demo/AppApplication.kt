package com.seo4d696b75.android.glance_widget_demo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.seo4d696b75.android.glance_widget_demo.widget.AnimationStateManager

@HiltAndroidApp
class AppApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            val stateManager = AnimationStateManager.getInstance(this)

            // 一時アニメーション中なら元の状態へ復元
            if (stateManager.isInTemporaryAnimation()) {
                stateManager.restorePreviousAnimation()
            }

            // 起動時は必ず Adle にする
            if (stateManager.getCurrentAnimationType() != AnimationStateManager.ANIMATION_TYPE_ADLE) {
                stateManager.setAdleState()
            }
        } catch (e: Exception) {
            android.util.Log.e("AppApplication", "Failed to initialize animation state to Adle", e)
        }
    }
}
