# ウィジェット画像キャッシュシステム

ウィジェット用の画像を効率的にダウンロード・キャッシュ・表示するシステムです。

## 🚀 主な機能

- **画像ダウンロード**: 外部URLから画像を自動ダウンロード
- **キャッシュ管理**: 同じ画像の再ダウンロードを防止
- **安全なアクセス**: FileProvider経由での画像アクセス
- **自動クリーンアップ**: キャッシュサイズの自動管理
- **ウィジェット統合**: ウィジェットでの画像表示

## 📁 保存構造

```
/storage/emulated/0/Android/data/com.example.app/cache/widget_assets/
├── character001/
│   ├── idle/
│   │   ├── frame_00.png
│   │   ├── frame_01.png
│   │   └── ...
│   ├── tap/
│   │   ├── frame_00.png
│   │   └── ...
├── character002/
│   ├── idle/
│   └── ...
```

## 🔧 使用方法

### 1. 基本設定

#### 依存関係の追加
```kotlin
// build.gradle.kts (Module: app)
dependencies {
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.glide)
    implementation(libs.hilt.android)
    implementation(libs.coroutines)
}
```

#### 権限の設定
```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
```

### 2. アプリでの使用

#### 画像ダウンロード
```kotlin
// 特定キャラクターの画像をダウンロード
ImageDownloadService.downloadCharacterImages(
    context,
    characterId = "character001",
    animationType = "idle" // Optional: 省略すると全アニメーション
)

// 全キャラクターの画像をダウンロード
ImageDownloadService.downloadAllImages(context)
```

#### キャッシュ管理
```kotlin
// キャッシュサイズを取得
val cacheSize = imageCacheRepository.getCacheSize()

// キャッシュをクリア
imageCacheRepository.clearCache()

// 古いキャッシュを自動削除
imageCacheRepository.cleanupOldCache()
```

### 3. ウィジェットでの使用

#### ウィジェットプロバイダー
```kotlin
@AndroidEntryPoint
class MyWidgetProvider : AppWidgetProvider() {
    
    @Inject
    lateinit var widgetImageManager: WidgetImageManager
    
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        
        // キャッシュされた画像のURIを取得
        val imageUri = WidgetImageFileProvider.getCachedImageUri(
            context,
            characterId = "character001",
            animationType = "idle",
            frameIndex = 0
        )
        
        imageUri?.let {
            views.setImageViewUri(R.id.image_view, it)
        } ?: run {
            // フォールバック画像を設定
            views.setImageViewResource(R.id.image_view, R.drawable.fallback_image)
        }
        
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
```

#### 画像の事前ダウンロード
```kotlin
private fun preloadImages(context: Context) {
    scope.launch {
        val config = SampleCharacterConfigs.getCharacterById("character001")
        
        config?.animations?.forEach { animation ->
            for (frameIndex in 0 until animation.frameCount) {
                val imageUrl = ImageUrlGenerator.generateImageUrl(
                    config,
                    animation.animationType,
                    frameIndex
                )
                
                widgetImageManager.downloadAndCacheImage(
                    context,
                    imageUrl,
                    config.characterId,
                    animation.animationType,
                    frameIndex
                )
            }
        }
    }
}
```

## 🎨 キャラクター設定

### 設定データクラス
```kotlin
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
        )
    )
)
```

### URL生成
```kotlin
val imageUrl = ImageUrlGenerator.generateImageUrl(
    config = CHARACTER_001,
    animationType = "idle",
    frameIndex = 0
)
// 結果: "https://example.com/character001/idle/frame_00.png"
```

## 🔄 アニメーション

### フレームベースアニメーション
```kotlin
private fun startAnimation(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val animationRunnable = object : Runnable {
        override fun run() {
            // 次のフレームに進む
            currentFrame = (currentFrame + 1) % totalFrames
            
            // 画像を更新
            updateWidget(context, appWidgetManager, appWidgetId)
            
            // 次のフレームをスケジュール
            handler.postDelayed(this, frameDelay)
        }
    }
    
    handler.post(animationRunnable)
}
```

## 🛡️ セキュリティ

### FileProvider設定
```xml
<!-- AndroidManifest.xml -->
<provider
    android:name="com.example.app.data.WidgetImageFileProvider"
    android:authorities="com.example.app.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

### ファイルパス設定
```xml
<!-- res/xml/file_paths.xml -->
<paths>
    <external-cache-path name="widget_assets" path="widget_assets/" />
</paths>
```

## 📱 Compose UI での使用

### 管理画面
```kotlin
@Composable
fun ImageCacheManagerScreen(imageCacheRepository: ImageCacheRepository) {
    val context = LocalContext.current
    
    Column {
        Button(
            onClick = {
                ImageDownloadService.downloadAllImages(context)
            }
        ) {
            Text("全画像ダウンロード")
        }
        
        Button(
            onClick = {
                ImageDownloadService.clearCache(context)
            }
        ) {
            Text("キャッシュクリア")
        }
    }
}
```

## ⚡ パフォーマンス最適化

### キャッシュ戦略
- **メモリキャッシュ**: 頻繁に使用する画像をメモリに保持
- **ディスクキャッシュ**: 全画像をディスクに保存
- **サイズ制限**: 自動的に古いキャッシュを削除 (デフォルト100MB)

### 使用のベストプラクティス
1. **事前ダウンロード**: ウィジェット更新前に画像をダウンロード
2. **バックグラウンド処理**: UI をブロックしないよう非同期処理
3. **エラーハンドリング**: フォールバック画像を用意
4. **定期クリーンアップ**: 不要なキャッシュを定期的に削除

## 🐛 トラブルシューティング

### よくある問題

#### 1. 画像が表示されない
- インターネット接続を確認
- URL が正しいか確認
- キャッシュが存在するか確認

#### 2. キャッシュが削除される
- アプリのキャッシュクリア設定を確認
- ストレージ容量を確認
- 自動クリーンアップの設定を確認

#### 3. ウィジェットで画像が表示されない
- FileProvider の設定を確認
- URI 権限の設定を確認
- AndroidManifest.xml の設定を確認

### デバッグ方法
```kotlin
// ログでキャッシュ状態を確認
Log.d("ImageCache", "Cache size: ${imageCacheRepository.getCacheSize()}")
Log.d("ImageCache", "Cache exists: ${imageCacheRepository.isCacheExists(characterId, animationType, frameIndex)}")
```

## 📚 関連ファイル

### Core Files
- `ImageCacheRepository.kt` - キャッシュ管理のインターフェース
- `ImageCacheRepositoryImpl.kt` - キャッシュ管理の実装
- `WidgetImageManager.kt` - ウィジェット用画像管理
- `WidgetImageFileProvider.kt` - 安全なファイルアクセス

### UI Files
- `ImageCacheManagerScreen.kt` - 管理画面
- `EnhancedCounterWidgetProvider.kt` - 使用例ウィジェット

### Configuration Files
- `CharacterImageConfig.kt` - キャラクター設定
- `NetworkModule.kt` - 依存関係注入
- `ImageDownloadService.kt` - バックグラウンドダウンロード

## 🔄 更新履歴

- **v1.0.0**: 初回リリース
  - 基本的な画像キャッシュ機能
  - ウィジェット統合
  - FileProvider サポート

## 📄 ライセンス

このプロジェクトは MIT ライセンスの下で公開されています。 