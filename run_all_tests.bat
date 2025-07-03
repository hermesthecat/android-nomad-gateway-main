@echo off
REM Comprehensive Testing Script for Android Nomad Gateway

setlocal enabledelayedexpansion

REM Set Android SDK paths for Windows
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if exist "%ProgramFiles%\Android\Android Studio" set "ANDROID_HOME=%ProgramFiles%\Android\Android Studio\sdk"
if exist "%ProgramFiles(x86)%\Android\android-sdk" set "ANDROID_HOME=%ProgramFiles(x86)%\Android\android-sdk"

set "PATH=%PATH%;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools"

set "WEBHOOK_URL=https://example.com/test_webhook"
set "PACKAGE_NAME=tech.wdg.incomingactivitygateway"

echo 🚀 Android Nomad Gateway - Comprehensive Testing Suite
echo ==================================================
echo 🌐 Webhook endpoint: %WEBHOOK_URL%
echo.

REM Check if emulator is running
echo 🔍 Checking emulator status...
adb devices | findstr "emulator" >nul
if errorlevel 1 (
    echo ❌ No emulator detected. Please start an emulator first:
    echo    setup_test_environment.bat
    exit /b 1
)

echo ✅ Emulator detected!
echo.

REM Check if app is installed
echo 📱 Checking app installation...
adb shell pm list packages | findstr "%PACKAGE_NAME%" >nul
if errorlevel 1 (
    echo ❌ App not installed. Installing now...
    if exist "app\build\outputs\apk\debug\app-debug.apk" (
        adb install -r app\build\outputs\apk\debug\app-debug.apk
        echo ✅ App installed successfully!
    ) else (
        echo ❌ APK not found. Please build the app first:
        echo    gradlew.bat assembleDebug
        exit /b 1
    )
) else (
    echo ✅ App is installed!
)

echo.
echo 🔧 Verifying permissions...

REM Check critical permissions
set "PERMISSIONS_OK=true"

REM Check SMS permissions
adb shell dumpsys package %PACKAGE_NAME% | findstr "android.permission.RECEIVE_SMS.*granted=true" >nul
if errorlevel 0 (
    echo ✅ SMS permissions granted
) else (
    echo ⚠️  SMS permissions missing
    set "PERMISSIONS_OK=false"
)

REM Check phone permissions
adb shell dumpsys package %PACKAGE_NAME% | findstr "android.permission.READ_PHONE_STATE.*granted=true" >nul
if errorlevel 0 (
    echo ✅ Phone permissions granted
) else (
    echo ⚠️  Phone permissions missing
    set "PERMISSIONS_OK=false"
)

REM Check notification access
for /f %%a in ('adb shell settings get secure enabled_notification_listeners') do set "NOTIFICATION_ACCESS=%%a"
echo "%NOTIFICATION_ACCESS%" | findstr "%PACKAGE_NAME%" >nul
if errorlevel 0 (
    echo ✅ Notification access enabled
) else (
    echo ⚠️  Notification access not enabled
    set "PERMISSIONS_OK=false"
)

if "%PERMISSIONS_OK%"=="false" (
    echo.
    echo ❌ Some permissions are missing. Run setup script first:
    echo    setup_test_environment.bat
    echo.
    set /p "continue_choice=Continue anyway? (y/N): "
    if /i not "%continue_choice%"=="y" exit /b 1
)

echo.
echo ⚠️  IMPORTANT: Before running tests, make sure to:
echo    1. Open the app and create forwarding rules with webhook: %WEBHOOK_URL%
echo    2. Use the templates provided in setup_test_environment.bat
echo    3. Save the rules and ensure the app is running
echo.

pause

echo.
echo 🧪 Starting test sequence...
echo.

REM Test webhook connectivity first
echo 🌐 Testing webhook connectivity...
powershell -Command "try { $response = Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"test\":\"connectivity\",\"message\":\"Pre-test connectivity check\",\"timestamp\":\"'$(Get-Date -UFormat %%s)'\",\"source\":\"test_runner\"}' -Headers @{'User-Agent'='Android-Nomad-Gateway-Test/1.0'}; Write-Host '✅ Webhook is accessible' } catch { Write-Host '⚠️  Webhook connectivity issue' }"

echo.

REM Test 1: SMS
echo 1️⃣  Testing SMS forwarding...
echo ================================
call test_sms.bat
echo.
echo ⏱️  Waiting 5 seconds before next test...
timeout /t 5 /nobreak >nul

REM Test 2: Calls
echo 2️⃣  Testing call forwarding...
echo ================================
call test_calls.bat
echo.
echo ⏱️  Waiting 5 seconds before next test...
timeout /t 5 /nobreak >nul

REM Test 3: Push Notifications
echo 3️⃣  Testing push notification forwarding...
echo ==========================================
call test_push.bat
echo.

echo 🎉 All tests completed!
echo =======================
echo.
echo 📊 Results Summary:
echo    • SMS test: Check webhook for message data
echo    • Call test: Check webhook for call data
echo    • Push test: Check webhook for notification data
echo.
echo 🌐 Webhook endpoint: %WEBHOOK_URL%
echo.
echo 🔍 Debugging commands:
echo    • View logs: adb logcat ^| findstr "IncomingActivityGateway"
echo    • Check services: adb shell dumpsys activity services ^| findstr "%PACKAGE_NAME%"
echo    • Check permissions: adb shell dumpsys package %PACKAGE_NAME% ^| findstr permission
echo    • Check notification access: adb shell settings get secure enabled_notification_listeners
echo.
echo 📱 App debugging:
echo    • Open app: adb shell am start -n %PACKAGE_NAME%/.MainActivity
echo    • Check app status: adb shell am force-stop %PACKAGE_NAME% ^&^& adb shell am start -n %PACKAGE_NAME%/.MainActivity
echo.
echo 🌐 Manual webhook test:
echo    powershell -Command "Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"test\":\"manual\",\"timestamp\":\"$(Get-Date -UFormat %%s)\"}'"

endlocal 