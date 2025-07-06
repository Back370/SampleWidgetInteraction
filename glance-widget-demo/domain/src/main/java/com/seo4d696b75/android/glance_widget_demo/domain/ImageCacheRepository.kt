package com.seo4d696b75.android.glance_widget_demo.domain

import java.io.File

/**
 * 画像キャッシュ管理用のリポジトリインターフェース
 */
interface ImageCacheRepository {
    /**
     * 画像をダウンロードしてキャッシュに保存
     * @param imageUrl 画像URL
     * @param characterId キャラクターID
     * @param animationType アニメーション種類 (idle, tap, etc.)
     * @param frameIndex フレーム番号
     * @return 保存されたファイルパス
     */
    suspend fun downloadAndCacheImage(
        imageUrl: String,
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Result<File>

    /**
     * キャッシュされた画像ファイルを取得
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return キャッシュされたファイル (存在しない場合はnull)
     */
    fun getCachedImageFile(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): File?

    /**
     * キャッシュされた画像が存在するかチェック
     * @param characterId キャラクターID
     * @param animationType アニメーション種類
     * @param frameIndex フレーム番号
     * @return キャッシュが存在する場合はtrue
     */
    fun isCacheExists(
        characterId: String,
        animationType: String,
        frameIndex: Int
    ): Boolean

    /**
     * キャッシュサイズを取得
     * @return キャッシュサイズ (bytes)
     */
    fun getCacheSize(): Long

    /**
     * キャッシュをクリア
     */
    suspend fun clearCache()

    /**
     * 古いキャッシュを削除 (サイズ制限に基づく)
     */
    suspend fun cleanupOldCache()
} 