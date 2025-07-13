package com.example.core

import android.content.Context
import android.content.SharedPreferences

/**
 * キャラクターの表示設定を管理するクラス
 */
class CharacterDisplaySettings private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "character_display_settings"
        private const val KEY_WIDTH_SCALE = "width_scale"
        private const val KEY_HEIGHT_SCALE = "height_scale"
        private const val KEY_QUALITY_SCALE = "quality_scale"
        private const val KEY_FPS_SCALE = "fps_scale"
        
        // デフォルト値
        private const val DEFAULT_WIDTH_SCALE = 0.5f
        private const val DEFAULT_HEIGHT_SCALE = 0.5f
        private const val DEFAULT_QUALITY_SCALE = 0.5f
        private const val DEFAULT_FPS_SCALE = 0.5f
        
        @Volatile
        private var INSTANCE: CharacterDisplaySettings? = null
        
        fun getInstance(context: Context): CharacterDisplaySettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CharacterDisplaySettings(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 横幅の倍率を取得
     */
    fun getWidthScale(): Float {
        return sharedPreferences.getFloat(KEY_WIDTH_SCALE, DEFAULT_WIDTH_SCALE)
    }
    
    /**
     * 横幅の倍率を保存
     */
    fun setWidthScale(scale: Float) {
        sharedPreferences.edit().putFloat(KEY_WIDTH_SCALE, scale).apply()
    }
    
    /**
     * 縦幅の倍率を取得
     */
    fun getHeightScale(): Float {
        return sharedPreferences.getFloat(KEY_HEIGHT_SCALE, DEFAULT_HEIGHT_SCALE)
    }
    
    /**
     * 縦幅の倍率を保存
     */
    fun setHeightScale(scale: Float) {
        sharedPreferences.edit().putFloat(KEY_HEIGHT_SCALE, scale).apply()
    }
    
    /**
     * 画質の倍率を取得
     */
    fun getQualityScale(): Float {
        return sharedPreferences.getFloat(KEY_QUALITY_SCALE, DEFAULT_QUALITY_SCALE)
    }
    
    /**
     * 画質の倍率を保存
     */
    fun setQualityScale(scale: Float) {
        sharedPreferences.edit().putFloat(KEY_QUALITY_SCALE, scale).apply()
    }
    
    /**
     * FPSの倍率を取得
     */
    fun getFpsScale(): Float {
        return sharedPreferences.getFloat(KEY_FPS_SCALE, DEFAULT_FPS_SCALE)
    }
    
    /**
     * FPSの倍率を保存
     */
    fun setFpsScale(scale: Float) {
        sharedPreferences.edit().putFloat(KEY_FPS_SCALE, scale).apply()
    }
    
    /**
     * 全ての設定をデフォルト値にリセット
     */
    fun resetToDefaults() {
        sharedPreferences.edit().apply {
            putFloat(KEY_WIDTH_SCALE, DEFAULT_WIDTH_SCALE)
            putFloat(KEY_HEIGHT_SCALE, DEFAULT_HEIGHT_SCALE)
            putFloat(KEY_QUALITY_SCALE, DEFAULT_QUALITY_SCALE)
            putFloat(KEY_FPS_SCALE, DEFAULT_FPS_SCALE)
        }.apply()
    }
    
    /**
     * 現在の設定を表示用文字列で取得
     */
    fun getDisplayText(): String {
        return "横幅: ${String.format("%.1f", getWidthScale())}, " +
               "縦幅: ${String.format("%.1f", getHeightScale())}, " +
               "画質: ${String.format("%.1f", getQualityScale())}, " +
               "FPS: ${String.format("%.1f", getFpsScale())}"
    }
} 