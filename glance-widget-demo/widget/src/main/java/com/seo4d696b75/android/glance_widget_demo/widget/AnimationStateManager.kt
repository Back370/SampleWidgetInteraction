package com.seo4d696b75.android.glance_widget_demo.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * ウィジェットのアニメーション状態を管理するクラス
 */
class AnimationStateManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "widget_animation_state"
        private const val KEY_ANIMATION_TYPE = "animation_type"
        private const val KEY_CHARACTER_ID = "character_id"
        private const val KEY_PREVIOUS_ANIMATION_TYPE = "previous_animation_type"
        private const val KEY_IS_TEMPORARY_ANIMATION = "is_temporary_animation"
        
        // アニメーション種別の定数
        const val ANIMATION_TYPE_ADLE = "Adle"
        const val ANIMATION_TYPE_FLOW = "Flow"
        const val ANIMATION_TYPE_SPECIAL = "Special"
        
        // キャラクターID
        const val CHARACTER_ID_MAO = "Mao"
        const val CHARACTER_ID_HARU = "Haru"
        
        @Volatile
        private var INSTANCE: AnimationStateManager? = null
        
        fun getInstance(context: Context): AnimationStateManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AnimationStateManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 現在のアニメーション種別を取得
     */
    fun getCurrentAnimationType(): String {
        return sharedPreferences.getString(KEY_ANIMATION_TYPE, ANIMATION_TYPE_ADLE) ?: ANIMATION_TYPE_ADLE
    }
    
    /**
     * 現在のキャラクターIDを取得
     */
    fun getCurrentCharacterId(): String {
        return sharedPreferences.getString(KEY_CHARACTER_ID, CHARACTER_ID_MAO) ?: CHARACTER_ID_MAO
    }
    
    /**
     * アニメーション種別を設定
     */
    fun setAnimationType(animationType: String) {
        sharedPreferences.edit()
            .putString(KEY_ANIMATION_TYPE, animationType)
            .apply()
        
        android.util.Log.d("AnimationStateManager", "Animation type set to: $animationType")
    }
    
    /**
     * キャラクターIDを設定
     */
    fun setCharacterId(characterId: String) {
        sharedPreferences.edit()
            .putString(KEY_CHARACTER_ID, characterId)
            .apply()

        android.util.Log.d("AnimationStateManager", "Character ID set to: $characterId")
    }

    //アイドル状態にする関数
    fun setAdleState(): String {
        setAnimationType(ANIMATION_TYPE_ADLE)
        return ANIMATION_TYPE_ADLE
    }

    //ふらふら状態にする関数
    fun setFlowState(): String {
        setAnimationType(ANIMATION_TYPE_FLOW)
        return ANIMATION_TYPE_FLOW
    }

    //スペシャル状態にする関数
    fun setSpecialState(): String {
        setAnimationType(ANIMATION_TYPE_SPECIAL)
        return ANIMATION_TYPE_SPECIAL
    }

    
    /**
     * アニメーション種別を切り替え（Adle → Flow → Special → Adle...）
     */
    fun toggleAnimationType(): String {
        val currentType = getCurrentAnimationType()
        val newType = when (currentType) {
            ANIMATION_TYPE_ADLE -> ANIMATION_TYPE_FLOW
            ANIMATION_TYPE_FLOW -> ANIMATION_TYPE_SPECIAL
            ANIMATION_TYPE_SPECIAL -> ANIMATION_TYPE_ADLE
            else -> ANIMATION_TYPE_ADLE // デフォルト
        }
        
        setAnimationType(newType)
        android.util.Log.d("AnimationStateManager", "Animation switched: $currentType → $newType")
        return newType
    }
    
    /**
     * 現在のアニメーション状態の表示用テキストを取得
     */
    fun getCurrentAnimationDisplayText(): String {
        return "${getCurrentCharacterId()}/State/${getCurrentAnimationType()}"
    }
    
    /**
     * 次のアニメーション状態の表示用テキストを取得
     */
    fun getNextAnimationDisplayText(): String {
        val currentType = getCurrentAnimationType()
        val nextType = when (currentType) {
            ANIMATION_TYPE_ADLE -> ANIMATION_TYPE_FLOW
            ANIMATION_TYPE_FLOW -> ANIMATION_TYPE_SPECIAL
            ANIMATION_TYPE_SPECIAL -> ANIMATION_TYPE_ADLE
            else -> ANIMATION_TYPE_ADLE // デフォルト
        }
        return "${getCurrentCharacterId()}/State/${nextType}"
    }
    
    /**
     * ウィジェットタップ時にSpecialアニメーションに一時切り替え
     * 現在のアニメーション状態を保存してからSpecialに切り替える
     */
    fun switchToSpecialTemporarily(): String {
        return switchToTemporarily(ANIMATION_TYPE_SPECIAL)
    }
    
    /**
     * 一時的に指定されたアニメーション種別に切り替え
     * 現在のアニメーション状態を保存してから指定されたアニメーションに切り替える
     */
    private fun switchToTemporarily(targetAnimationType: String): String {
        val currentType = getCurrentAnimationType()
        
        // 現在の状態が既に指定されたアニメーションの場合は何もしない
        if (currentType == targetAnimationType) {
            android.util.Log.d("AnimationStateManager", "Already in $targetAnimationType state, no change needed")
            return currentType
        }
        
        // 現在のアニメーション種別を前の状態として保存
        sharedPreferences.edit()
            .putString(KEY_PREVIOUS_ANIMATION_TYPE, currentType)
            .putBoolean(KEY_IS_TEMPORARY_ANIMATION, true)
            .apply()
        
        // 指定されたアニメーションに切り替え
        setAnimationType(targetAnimationType)
        
        android.util.Log.d("AnimationStateManager", "Switched to $targetAnimationType temporarily, saved previous: $currentType")
        return targetAnimationType
    }
    
    /**
     * Specialアニメーションから元のアニメーション状態に復元
     */
    fun restorePreviousAnimation(): String {
        val previousType = sharedPreferences.getString(KEY_PREVIOUS_ANIMATION_TYPE, ANIMATION_TYPE_ADLE) ?: ANIMATION_TYPE_ADLE
        
        // 前の状態に復元
        setAnimationType(previousType)
        
        // 保存された前の状態をクリア
        sharedPreferences.edit()
            .remove(KEY_PREVIOUS_ANIMATION_TYPE)
            .remove(KEY_IS_TEMPORARY_ANIMATION)
            .apply()
        
        android.util.Log.d("AnimationStateManager", "Restored previous animation: $previousType")
        return previousType
    }
    
    /**
     * 現在Specialアニメーション中かどうかを確認
     */
    fun isInTemporarySpecial(): Boolean {
        return isInTemporaryAnimation() && getCurrentAnimationType() == ANIMATION_TYPE_SPECIAL
    }
    
    /**
     * 現在一時的なアニメーション中かどうかを確認
     */
    fun isInTemporaryAnimation(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_TEMPORARY_ANIMATION, false)
    }
    
    //１アニメーションだけアイドル状態にする関数
    fun RapidAdleState(): String {
        return switchToTemporarily(ANIMATION_TYPE_ADLE)
    }

    //１アニメーションだけふらふら状態にする関数
    fun RapidFlowState(): String {
        return switchToTemporarily(ANIMATION_TYPE_FLOW)
    }

    //１アニメーションだけスペシャル状態にする関数
    fun RapidSpecialState(): String {
        return switchToTemporarily(ANIMATION_TYPE_SPECIAL)
    }
} 