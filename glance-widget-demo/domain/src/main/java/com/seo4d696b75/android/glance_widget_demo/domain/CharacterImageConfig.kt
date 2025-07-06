package com.seo4d696b75.android.glance_widget_demo.domain

/**
 * キャラクター画像設定データクラス
 */
data class CharacterImageConfig(
    val characterId: String,
    val name: String,
    val baseUrl: String,
    val animations: List<AnimationConfig>
)

/**
 * アニメーション設定データクラス
 */
data class AnimationConfig(
    val animationType: String,
    val frameCount: Int,
    val frameDelay: Long = 100L,
    val fileFormat: String = "png"
)

/**
 * 画像URL生成用のユーティリティ
 */
object ImageUrlGenerator {
    
    /**
     * 画像URLを生成
     * @param config キャラクター設定
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return 画像URL
     */
    fun generateImageUrl(
        config: CharacterImageConfig,
        animationType: String,
        frameIndex: Int
    ): String {
        return "${config.baseUrl}$animationType/frame_${frameIndex.toString().padStart(2, '0')}.${getFileFormat(config, animationType)}"
    }
    
    /**
     * アニメーション設定を取得
     * @param config キャラクター設定
     * @param animationType アニメーション種類
     * @return アニメーション設定
     */
    fun getAnimationConfig(
        config: CharacterImageConfig,
        animationType: String
    ): AnimationConfig? {
        return config.animations.find { it.animationType == animationType }
    }
    
    private fun getFileFormat(config: CharacterImageConfig, animationType: String): String {
        return getAnimationConfig(config, animationType)?.fileFormat ?: "png"
    }
}

/**
 * サンプルキャラクター設定
 */
object SampleCharacterConfigs {
    
    val CHARACTER_001 = CharacterImageConfig(
        characterId = "character001",
        name = "Sample Character 1",
        baseUrl = "https://example.com/character001/",
        animations = listOf(
            AnimationConfig(
                animationType = "idle",
                frameCount = 10,
                frameDelay = 100L
            ),
            AnimationConfig(
                animationType = "tap",
                frameCount = 5,
                frameDelay = 80L
            ),
            AnimationConfig(
                animationType = "sleep",
                frameCount = 8,
                frameDelay = 200L
            )
        )
    )
    
    val CHARACTER_002 = CharacterImageConfig(
        characterId = "character002",
        name = "Sample Character 2",
        baseUrl = "https://example.com/character002/",
        animations = listOf(
            AnimationConfig(
                animationType = "idle",
                frameCount = 12,
                frameDelay = 120L
            ),
            AnimationConfig(
                animationType = "tap",
                frameCount = 6,
                frameDelay = 70L
            )
        )
    )
    
    /**
     * 全キャラクター設定の取得
     */
    fun getAllCharacters(): List<CharacterImageConfig> {
        return listOf(CHARACTER_001, CHARACTER_002)
    }
    
    /**
     * キャラクター設定を取得
     * @param characterId キャラクターID
     * @return キャラクター設定
     */
    fun getCharacterById(characterId: String): CharacterImageConfig? {
        return getAllCharacters().find { it.characterId == characterId }
    }
} 