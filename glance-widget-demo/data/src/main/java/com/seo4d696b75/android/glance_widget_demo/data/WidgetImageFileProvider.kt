package com.seo4d696b75.android.glance_widget_demo.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * ウィジェット画像用のFileProvider
 * 安全にファイルアクセスを提供
 */
class WidgetImageFileProvider : FileProvider() {
    
    companion object {
        private const val AUTHORITY = "com.seo4d696b75.android.glance_widget_demo.fileprovider"
        
        /**
         * ファイルのUriを取得
         * @param context Context
         * @param file ファイル
         * @return 安全なUri
         */
        fun getUriForFile(context: Context, file: File): Uri {
            return getUriForFile(context, AUTHORITY, file)
        }
        
        /**
         * キャッシュファイルのUriを取得
         * @param context Context
         * @param characterId キャラクターID
         * @param animationType アニメーション種類
         * @param frameIndex フレーム番号
         * @return 安全なUri (ファイルが存在する場合)
         */
        fun getCachedImageUri(
            context: Context,
            characterId: String,
            animationType: String,
            frameIndex: Int
        ): Uri? {
            val cacheDir = File(context.externalCacheDir, "widget_assets")
            val subDir = File(cacheDir, "$characterId/$animationType")
            val filename = "frame_${frameIndex.toString().padStart(2, '0')}.png"
            val file = File(subDir, filename)
            
            return if (file.exists()) {
                getUriForFile(context, AUTHORITY, file)
            } else {
                null
            }
        }
    }
} 