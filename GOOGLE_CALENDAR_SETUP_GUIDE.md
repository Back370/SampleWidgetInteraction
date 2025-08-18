# Google Calendar API 設定ガイド

## 現在の問題
Google Calendar APIへのアクセス時に設定エラーが発生しています。

## 解決手順

### 1. Google Cloud Consoleでの設定

#### 1.1 Google Cloud Consoleにアクセス
- https://console.cloud.google.com/ にアクセス
- プロジェクト `backproject-c19a9` を選択

#### 1.2 Google Calendar APIを有効化
1. 「APIとサービス」→「ライブラリ」を選択
2. 「Google Calendar API」を検索
3. 「有効にする」をクリック

#### 1.3 OAuth同意画面の設定
1. 「APIとサービス」→「OAuth同意画面」を選択
2. ユーザータイプを「外部」に設定
3. 必要な情報を入力：
   - アプリ名: Glance Widget Demo
   - ユーザーサポートメール: あなたのメールアドレス
   - 開発者の連絡先情報: あなたのメールアドレス

#### 1.4 スコープの追加
1. 「スコープ」セクションで「スコープを追加または削除」をクリック
2. 以下のスコープを追加：
   - `https://www.googleapis.com/auth/calendar`
   - `https://www.googleapis.com/auth/calendar.events`

#### 1.5 テストユーザーの追加
1. 「テストユーザー」セクションで「テストユーザーを追加」をクリック
2. アプリをテストするGoogleアカウントのメールアドレスを追加

### 2. OAuth 2.0クライアントIDの設定

#### 2.1 Webアプリケーションクライアントの追加
1. 「APIとサービス」→「認証情報」を選択
2. 「認証情報を作成」→「OAuth 2.0クライアントID」を選択
3. アプリケーションの種類で「Webアプリケーション」を選択
4. 名前: "Glance Widget Demo Web Client"
5. 承認済みのリダイレクトURIを追加：
   - `https://backproject-c19a9.firebaseapp.com/__/auth/handler`
   - `http://localhost:8080`

#### 2.2 Androidクライアントの確認
現在の設定を確認：
- パッケージ名: `com.seo4d696b75.android.glance_widget_demo`
- SHA-1証明書フィンガープリント: デバッグ用のSHA-1を確認

### 3. SHA-1証明書フィンガープリントの取得

#### 3.1 デバッグ用SHA-1の取得
```bash
# Windowsの場合
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

# macOS/Linuxの場合
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### 3.2 リリース用SHA-1の取得（必要に応じて）
```bash
keytool -list -v -keystore your-release-key.keystore -alias your-key-alias
```

### 4. google-services.jsonの更新

#### 4.1 新しいgoogle-services.jsonのダウンロード
1. Firebase Consoleでプロジェクトを開く
2. プロジェクト設定→「全般」タブ
3. 「google-services.json」をダウンロード
4. 既存のファイルを置き換え

#### 4.2 期待される設定内容
```json
{
  "project_info": {
    "project_number": "594804857009",
    "project_id": "backproject-c19a9",
    "storage_bucket": "backproject-c19a9.firebasestorage.app"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:594804857009:android:557f7a93210fbd612caa90",
        "android_client_info": {
          "package_name": "com.seo4d696b75.android.glance_widget_demo"
        }
      },
      "oauth_client": [
        {
          "client_id": "594804857009-l5pqeagkt49u7afq6u996ph03n8l2gfo.apps.googleusercontent.com",
          "client_type": 3
        },
        {
          "client_id": "594804857009-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com",
          "client_type": 1
        }
      ],
      "api_key": [
        {
          "current_key": "AIzaSyB8i6KjZyrmvpMwwP2In4oGSTH6RPhDkLo"
        }
      ],
      "services": {
        "appinvite_service": {
          "other_platform_oauth_client": [
            {
              "client_id": "594804857009-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com",
              "client_type": 1
            }
          ]
        }
      }
    }
  ],
  "configuration_version": "1"
}
```

### 5. アプリの再ビルドとテスト

1. プロジェクトをクリーンビルド
```bash
./gradlew clean
./gradlew build
```

2. アプリを再インストール
3. Googleサインインをテスト
4. カレンダー機能をテスト

### 6. トラブルシューティング

#### 6.1 よくあるエラーと解決策

**エラー: "DEVELOPER_ERROR"**
- SHA-1証明書フィンガープリントが正しく設定されているか確認
- パッケージ名が一致しているか確認

**エラー: "SIGN_IN_REQUIRED"**
- OAuth同意画面でテストユーザーが追加されているか確認
- スコープが正しく設定されているか確認

**エラー: "NETWORK_ERROR"**
- インターネット接続を確認
- ファイアウォール設定を確認

#### 6.2 ログの確認
```bash
adb logcat | grep -E "(GoogleCalendarManager|VoiceInputActivity|FA)"
```

### 7. セキュリティのベストプラクティス

1. **APIキーの保護**: APIキーを公開リポジトリにコミットしない
2. **OAuth同意画面の設定**: 本番環境では適切なドメインを設定
3. **スコープの最小化**: 必要最小限のスコープのみを要求
4. **テストユーザーの管理**: 本番環境では適切なユーザー管理を行う

## 参考リンク

- [Google Calendar API Documentation](https://developers.google.com/calendar/api)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Firebase Authentication](https://firebase.google.com/docs/auth)
