@echo off
REM Call Testing Script for Android Nomad Gateway

setlocal enabledelayedexpansion

REM Set Android SDK paths for Windows
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if exist "%ProgramFiles%\Android\Android Studio" set "ANDROID_HOME=%ProgramFiles%\Android\Android Studio\sdk"
if exist "%ProgramFiles(x86)%\Android\android-sdk" set "ANDROID_HOME=%ProgramFiles(x86)%\Android\android-sdk"

set "PATH=%PATH%;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools"

REM Load configuration
set "WEBHOOK_URL=https://example.com/test_webhook"
set "TEST_PHONE=+1987654321"

echo 🔄 Testing call forwarding...
echo 📞 Simulating incoming call to emulator...
echo 🌐 Webhook endpoint: %WEBHOOK_URL%

REM Simulate incoming call
echo 📱 Initiating call from %TEST_PHONE%...
adb emu gsm call "%TEST_PHONE%"

if errorlevel 0 (
    echo ✅ Incoming call initiated!
    echo ⏱️  Letting call ring for 8 seconds to trigger call detection...
    timeout /t 8 /nobreak >nul
    
    echo 📞 Ending call...
    adb emu gsm cancel "%TEST_PHONE%"
    
    echo ✅ Call simulation complete!
    echo 📊 Check your webhook endpoint for the forwarded data.
    echo 🔍 You can also check logs with: adb logcat ^| findstr "IncomingActivityGateway"
    
    REM Wait for processing
    echo ⏱️  Waiting 3 seconds for processing...
    timeout /t 3 /nobreak >nul
    
    REM Test webhook connectivity
    echo 🧪 Testing webhook connectivity...
    
    REM Get current unix timestamp
    for /f %%a in ('powershell -Command "[int][double]::Parse((Get-Date -UFormat %%s))"') do set "unix_timestamp=%%a"
    
    powershell -Command "try { $response = Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"from\":\"%TEST_PHONE%\",\"contact\":\"Unknown\",\"timestamp\":\"%unix_timestamp%\",\"duration\":\"8\",\"type\":\"call\",\"test\":true}' -Headers @{'User-Agent'='Android-Nomad-Gateway-Test/1.0'}; Write-Host '✅ Webhook connectivity test completed' } catch { Write-Host '⚠️  Webhook connectivity issue: ' + $_.Exception.Message }"
        
    REM Additional call state verification
    echo 🔍 Checking call state in logs...
    adb logcat -d | findstr /i "phone call telephony" | powershell -Command "$input | Select-Object -Last 5"
) else (
    echo ❌ Failed to simulate call. Make sure emulator is running and ADB is connected.
)

echo.
echo Expected webhook payload:
echo {
echo   "from": "%TEST_PHONE%",
echo   "contact": "Unknown",
echo   "timestamp": "[unix_timestamp]",
echo   "duration": "[call_duration]",
echo   "type": "call"
echo }

echo.
echo 💡 Note: Call detection requires:
echo    1. Phone permissions granted to the app
echo    2. CallBroadcastReceiver properly registered
echo    3. Phone state changes to be monitored

endlocal 