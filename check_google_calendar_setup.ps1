# Google Calendar API 設定確認スクリプト
# PowerShell script to check Google Calendar API setup

Write-Host "🔍 Google Calendar API 設定確認スクリプト" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan

# 1. SHA-1証明書フィンガープリントの確認
Write-Host "`n1. SHA-1証明書フィンガープリントの確認" -ForegroundColor Yellow
try {
    $sha1Output = & keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android 2>&1
    $sha1Line = $sha1Output | Select-String "SHA1:"
    if ($sha1Line) {
        Write-Host "✅ SHA-1: $($sha1Line.Line.Trim())" -ForegroundColor Green
        $sha1Value = ($sha1Line.Line -split "SHA1: ")[1].Trim()
        Write-Host "   この値をGoogle Cloud ConsoleのOAuth 2.0クライアントIDに設定してください" -ForegroundColor Gray
    } else {
        Write-Host "❌ SHA-1証明書フィンガープリントが見つかりません" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ SHA-1証明書フィンガープリントの取得に失敗しました" -ForegroundColor Red
    Write-Host "   エラー: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. パッケージ名の確認
Write-Host "`n2. パッケージ名の確認" -ForegroundColor Yellow
$expectedPackage = "com.seo4d696b75.android.glance_widget_demo"
Write-Host "✅ 期待されるパッケージ名: $expectedPackage" -ForegroundColor Green
Write-Host "   このパッケージ名がGoogle Cloud Consoleで正しく設定されているか確認してください" -ForegroundColor Gray

# 3. google-services.jsonの確認
Write-Host "`n3. google-services.jsonの確認" -ForegroundColor Yellow
$googleServicesPath = "app\google-services.json"
if (Test-Path $googleServicesPath) {
    Write-Host "✅ google-services.jsonファイルが存在します" -ForegroundColor Green
    try {
        $jsonContent = Get-Content $googleServicesPath -Raw | ConvertFrom-Json
        $packageName = $jsonContent.client[0].client_info.android_client_info.package_name
        Write-Host "   パッケージ名: $packageName" -ForegroundColor Gray
        
        if ($packageName -eq $expectedPackage) {
            Write-Host "   ✅ パッケージ名が一致しています" -ForegroundColor Green
        } else {
            Write-Host "   ❌ パッケージ名が一致しません" -ForegroundColor Red
        }
        
        $oauthClients = $jsonContent.client[0].oauth_client
        Write-Host "   OAuthクライアント数: $($oauthClients.Count)" -ForegroundColor Gray
        
        foreach ($client in $oauthClients) {
            $clientType = switch ($client.client_type) {
                1 { "Web" }
                2 { "iOS" }
                3 { "Android" }
                default { "Unknown" }
            }
            Write-Host "   - タイプ: $clientType, ID: $($client.client_id)" -ForegroundColor Gray
        }
    } catch {
        Write-Host "❌ google-services.jsonの解析に失敗しました" -ForegroundColor Red
        Write-Host "   エラー: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "❌ google-services.jsonファイルが見つかりません" -ForegroundColor Red
}

# 4. 必要な設定の確認
Write-Host "`n4. Google Cloud Consoleでの設定確認" -ForegroundColor Yellow
Write-Host "以下の設定をGoogle Cloud Consoleで確認してください:" -ForegroundColor White
Write-Host "  🔗 https://console.cloud.google.com/apis/credentials?project=backproject-c19a9" -ForegroundColor Cyan
Write-Host "" -ForegroundColor White
Write-Host "  1. Google Calendar APIが有効化されているか" -ForegroundColor White
Write-Host "  2. OAuth同意画面で以下が設定されているか:" -ForegroundColor White
Write-Host "     - アプリ名: Glance Widget Demo" -ForegroundColor Gray
Write-Host "     - スコープ: https://www.googleapis.com/auth/calendar" -ForegroundColor Gray
Write-Host "     - テストユーザー: あなたのGoogleアカウント" -ForegroundColor Gray
Write-Host "  3. OAuth 2.0クライアントIDで以下が設定されているか:" -ForegroundColor White
Write-Host "     - パッケージ名: $expectedPackage" -ForegroundColor Gray
Write-Host "     - SHA-1証明書フィンガープリント: 上記で取得した値" -ForegroundColor Gray

# 5. トラブルシューティング
Write-Host "`n5. トラブルシューティング" -ForegroundColor Yellow
Write-Host "よくある問題と解決策:" -ForegroundColor White
Write-Host "  ❌ DEVELOPER_ERROR: SHA-1またはパッケージ名の不一致" -ForegroundColor Red
Write-Host "  ❌ SIGN_IN_REQUIRED: OAuth同意画面でテストユーザーが追加されていない" -ForegroundColor Red
Write-Host "  ❌ NETWORK_ERROR: インターネット接続またはファイアウォールの問題" -ForegroundColor Red

# 6. ログの確認方法
Write-Host "`n6. ログの確認方法" -ForegroundColor Yellow
Write-Host "アプリ実行時に以下のコマンドでログを確認してください:" -ForegroundColor White
Write-Host "  adb logcat | findstr /i \"GoogleCalendarManager\|VoiceInputActivity\|CalendarConfigChecker\"" -ForegroundColor Cyan

Write-Host "`n✅ 設定確認完了" -ForegroundColor Green
Write-Host "問題が解決しない場合は、上記のログを確認して詳細なエラー情報を取得してください。" -ForegroundColor Gray
