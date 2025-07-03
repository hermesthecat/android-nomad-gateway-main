@echo off
REM Push Notification Testing Script for Android Nomad Gateway

setlocal enabledelayedexpansion

REM Set Android SDK paths for Windows
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if exist "%ProgramFiles%\Android\Android Studio" set "ANDROID_HOME=%ProgramFiles%\Android\Android Studio\sdk"
if exist "%ProgramFiles(x86)%\Android\android-sdk" set "ANDROID_HOME=%ProgramFiles(x86)%\Android\android-sdk"

set "PATH=%PATH%;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools"

REM Load configuration
set "WEBHOOK_URL=https://example.com/test_webhook"
set "TEST_PACKAGE=com.android.chrome"

echo 🔄 Testing push notification forwarding...
echo 🔔 Creating test notification...
echo 🌐 Webhook endpoint: %WEBHOOK_URL%

REM Check if notification access is enabled
echo 🔍 Checking notification access...
for /f %%a in ('adb shell settings get secure enabled_notification_listeners') do set "NOTIFICATION_ACCESS=%%a"
echo "%NOTIFICATION_ACCESS%" | findstr "tech.wdg.incomingactivitygateway" >nul
if errorlevel 0 (
    echo ✅ Notification access is enabled
) else (
    echo ⚠️  Notification access may not be enabled. Please enable it in Settings.
)

REM Get current timestamp for notification
for /f %%a in ('powershell -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss'"') do set "timestamp=%%a"

REM Method 1: Using notification command (Android 7+)
echo 📱 Method 1: Using notification command...
adb shell cmd notification post -S bigtext -t "Test Notification" "TestTag" "This is a test notification from automated script - %timestamp%"

if errorlevel 0 (
    echo ✅ Notification posted successfully!
) else (
    echo ⚠️  Direct notification failed, trying alternative methods...
)

REM Method 2: Install and use a test app to generate notifications
echo 📱 Method 2: Triggering Chrome notification...
adb shell am start -n com.android.chrome/com.google.android.apps.chrome.Main
timeout /t 2 /nobreak >nul

REM Method 3: Using am broadcast to simulate notification
echo 📱 Method 3: Broadcasting notification intent...
adb shell am broadcast -a android.intent.action.MAIN --es title "Test Push Notification" --es text "Test notification from script - %timestamp%" --es package "%TEST_PACKAGE%"

REM Method 4: Create a test notification using service call
echo 📱 Method 4: Using service call...
adb shell service call notification 1 s16 "%TEST_PACKAGE%" i32 12345 s16 "Test" s16 "Test notification content" i32 0

echo 📊 Check your webhook endpoint for the forwarded data.
echo 🔍 You can also check logs with: adb logcat ^| findstr "IncomingActivityGateway"

REM Wait for processing
echo ⏱️  Waiting 5 seconds for processing...
timeout /t 5 /nobreak >nul

REM Test webhook connectivity
echo 🧪 Testing webhook connectivity...

REM Get current unix timestamp
for /f %%a in ('powershell -Command "[int][double]::Parse((Get-Date -UFormat %%s))"') do set "unix_timestamp=%%a"

powershell -Command "try { $response = Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"app\":\"%TEST_PACKAGE%\",\"title\":\"Test Push Notification\",\"content\":\"Test notification from script\",\"message\":\"Test notification content\",\"timestamp\":\"%unix_timestamp%\",\"type\":\"push\",\"test\":true}' -Headers @{'User-Agent'='Android-Nomad-Gateway-Test/1.0'}; Write-Host '✅ Webhook connectivity test completed' } catch { Write-Host '⚠️  Webhook connectivity issue: ' + $_.Exception.Message }"

REM Check notification listener service
echo 🔍 Checking notification listener service...
adb shell dumpsys notification | findstr /i "tech.wdg.incomingactivitygateway"

echo.
echo 💡 Note: For push notifications to work properly:
echo    1. Enable notification access for the app in Settings ^> Apps ^> Special access ^> Notification access
echo    2. Install apps that send notifications (Chrome, Gmail, etc.)
echo    3. Make sure NotificationListenerService is running
echo    4. Grant notification permissions to the app

echo.
echo 🔧 To enable notification access manually:
echo    adb shell settings put secure enabled_notification_listeners tech.wdg.incomingactivitygateway/.NotificationListener

echo.
echo Expected webhook payload:
echo {
echo   "app": "%TEST_PACKAGE%",
echo   "title": "Test Push Notification",
echo   "content": "Test notification from script",
echo   "message": "[notification_text]",
echo   "timestamp": "[unix_timestamp]",
echo   "type": "push"
echo }

endlocal 