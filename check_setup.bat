@echo off
echo 🔍 Google Calendar API Setup Check
echo =================================

echo.
echo 1. SHA-1 Certificate Fingerprint:
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android | findstr "SHA1:"

echo.
echo 2. Package Name Check:
echo Expected: com.seo4d696b75.android.glance_widget_demo

echo.
echo 3. google-services.json exists:
if exist "app\google-services.json" (
    echo ✅ google-services.json found
) else (
    echo ❌ google-services.json not found
)

echo.
echo 4. Next Steps:
echo - Go to: https://console.cloud.google.com/apis/credentials?project=backproject-c19a9
echo - Enable Google Calendar API
echo - Configure OAuth consent screen
echo - Add SHA-1 fingerprint to OAuth 2.0 client ID
echo - Add test users to OAuth consent screen

echo.
echo 5. Log Check Command:
echo adb logcat ^| findstr /i "GoogleCalendarManager VoiceInputActivity CalendarConfigChecker"

pause
