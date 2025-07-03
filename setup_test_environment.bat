@echo off
REM Setup Test Environment for Android Nomad Gateway

setlocal enabledelayedexpansion

REM Set Android SDK paths for Windows
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if exist "%ProgramFiles%\Android\Android Studio" set "ANDROID_HOME=%ProgramFiles%\Android\Android Studio\sdk"
if exist "%ProgramFiles(x86)%\Android\android-sdk" set "ANDROID_HOME=%ProgramFiles(x86)%\Android\android-sdk"

set "PATH=%PATH%;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools"

set "WEBHOOK_URL=https://example.com/test_webhook"
set "PACKAGE_NAME=tech.wdg.incomingactivitygateway"

echo 🚀 Setting up Android Nomad Gateway Test Environment
echo ==================================================
echo.

REM Check if emulator is running
echo 🔍 Checking emulator status...
adb devices | findstr "emulator" >nul
if errorlevel 1 (
    echo ❌ No emulator detected. You need to start an emulator manually.
    echo.
    echo 📱 To start an emulator:
    echo    1. Open Android Studio
    echo    2. Go to Tools ^> AVD Manager
    echo    3. Start an existing emulator or create a new one
    echo    4. Wait for the emulator to fully boot
    echo.
    echo Alternative: Use command line:
    echo    emulator -list-avds
    echo    emulator -avd [AVD_NAME]
    echo.
    pause
    
    REM Check again after user intervention
    adb devices | findstr "emulator" >nul
    if errorlevel 1 (
        echo ❌ Still no emulator detected. Please start an emulator and run this script again.
        exit /b 1
    )
)

echo ✅ Emulator is ready!
echo.

REM Install the app
echo 📱 Installing app...
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    adb install -r app\build\outputs\apk\debug\app-debug.apk
    echo ✅ App installed successfully!
) else (
    echo ❌ APK not found. Building app...
    call gradlew.bat assembleDebug
    if errorlevel 0 (
        adb install -r app\build\outputs\apk\debug\app-debug.apk
        echo ✅ App built and installed successfully!
    ) else (
        echo ❌ Failed to build app. Please check your development environment.
        exit /b 1
    )
)

echo.
echo 🔧 Configuring permissions...

REM Grant SMS permissions
echo 📱 Granting SMS permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.RECEIVE_SMS
adb shell pm grant %PACKAGE_NAME% android.permission.READ_SMS
adb shell pm grant %PACKAGE_NAME% android.permission.SEND_SMS

REM Grant phone permissions
echo 📞 Granting phone permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.READ_PHONE_STATE
adb shell pm grant %PACKAGE_NAME% android.permission.READ_CALL_LOG
adb shell pm grant %PACKAGE_NAME% android.permission.READ_CONTACTS

REM Grant notification access
echo 🔔 Enabling notification access...
adb shell settings put secure enabled_notification_listeners "%PACKAGE_NAME%/.NotificationListener"

REM Grant other necessary permissions
echo 🔐 Granting additional permissions...
adb shell pm grant %PACKAGE_NAME% android.permission.INTERNET
adb shell pm grant %PACKAGE_NAME% android.permission.ACCESS_NETWORK_STATE
adb shell pm grant %PACKAGE_NAME% android.permission.WAKE_LOCK

echo ✅ All permissions granted!
echo.

REM Test webhook connectivity
echo 🌐 Testing webhook connectivity...

REM Get current unix timestamp
for /f %%a in ('powershell -Command "[int][double]::Parse((Get-Date -UFormat %%s))"') do set "unix_timestamp=%%a"

powershell -Command "try { $response = Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"test\":\"setup\",\"message\":\"Testing webhook connectivity from setup script\",\"timestamp\":\"%unix_timestamp%\",\"device\":\"emulator\"}' -Headers @{'User-Agent'='Android-Nomad-Gateway-Setup/1.0'}; Write-Host '✅ Webhook is accessible' } catch { Write-Host '⚠️  Webhook connectivity issue: ' + $_.Exception.Message }"

echo.
echo 📋 Configuration Summary:
echo    • Webhook URL: %WEBHOOK_URL%
echo    • Package: %PACKAGE_NAME%
echo    • Emulator: Ready
echo    • Permissions: Granted
echo.

echo 🎯 Next Steps:
echo 1. Open the app on the emulator
echo 2. Create forwarding rules with the following settings:
echo.
echo    📱 SMS Rule:
echo    - Activity Type: SMS
echo    - All sources: Enabled
echo    - Webhook URL: %WEBHOOK_URL%
echo    - Template: {"from":"%%from%%","text":"%%text%%","timestamp":"%%sentStamp%%","sim":"%%sim%%","type":"sms"}
echo.
echo    📞 Call Rule:
echo    - Activity Type: Calls
echo    - All sources: Enabled
echo    - Webhook URL: %WEBHOOK_URL%
echo    - Template: {"from":"%%from%%","contact":"%%contact%%","timestamp":"%%timestamp%%","duration":"%%duration%%","type":"call"}
echo.
echo    🔔 Push Rule:
echo    - Activity Type: Push
echo    - All sources: Enabled
echo    - Webhook URL: %WEBHOOK_URL%
echo    - Template: {"app":"%%package%%","title":"%%title%%","content":"%%content%%","message":"%%text%%","timestamp":"%%sentStamp%%","type":"push"}
echo.
echo 3. Run tests with: run_all_tests.bat
echo.
echo 🔍 Useful commands:
echo    • Check logs: adb logcat ^| findstr "IncomingActivityGateway"
echo    • Check services: adb shell dumpsys activity services ^| findstr "%PACKAGE_NAME%"
echo    • Check permissions: adb shell dumpsys package %PACKAGE_NAME% ^| findstr permission
echo.
echo 📱 Open the app now:
adb shell am start -n %PACKAGE_NAME%/.MainActivity

endlocal 