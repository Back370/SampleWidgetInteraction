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
//    fun setCharacterId(characterId: String) {
//        sharedPreferences.edit()
//            .putString(KEY_CHARACTER_ID, characterId)
//            .apply()
//
//        android.util.Log.d("AnimationStateManager", "Character ID set to: $characterId")
//    }

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

    //１アニメーションだけアイドル状態にする関数
    fun RapidAdleState(): String {
        setAnimationType(ANIMATION_TYPE_ADLE)
        return ANIMATION_TYPE_ADLE
    }

    //１アニメーションだけふらふら状態にする関数
    fun RapidFlowState(): String {
        setAnimationType(ANIMATION_TYPE_FLOW)
        return ANIMATION_TYPE_FLOW
    }

    //１アニメーションだけスペシャル状態にする関数
    fun RapidSpecialState(): String {
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
} 