@echo off
REM SMS Testing Script for Android Nomad Gateway

setlocal enabledelayedexpansion

REM Set Android SDK paths for Windows
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
if exist "%ProgramFiles%\Android\Android Studio" set "ANDROID_HOME=%ProgramFiles%\Android\Android Studio\sdk"
if exist "%ProgramFiles(x86)%\Android\android-sdk" set "ANDROID_HOME=%ProgramFiles(x86)%\Android\android-sdk"

set "PATH=%PATH%;%ANDROID_HOME%\emulator;%ANDROID_HOME%\platform-tools"

REM Load configuration
set "WEBHOOK_URL=https://example.com/test_webhook"
set "TEST_PHONE=+1234567890"

echo 🔄 Testing SMS forwarding...
echo 📱 Sending test SMS to emulator...
echo 🌐 Webhook endpoint: %WEBHOOK_URL%

REM Get current timestamp for message
for /f %%a in ('powershell -Command "Get-Date -Format 'yyyy-MM-dd HH:mm:ss'"') do set "timestamp=%%a"

REM Send SMS via ADB
adb emu sms send "%TEST_PHONE%" "Automated test SMS message - %timestamp%"

if errorlevel 0 (
    echo ✅ SMS sent successfully!
    echo 📊 Check your webhook endpoint for the forwarded data.
    echo 🔍 You can also check logs with: adb logcat ^| findstr "IncomingActivityGateway"
    
    REM Wait a moment for processing
    echo ⏱️  Waiting 3 seconds for processing...
    timeout /t 3 /nobreak >nul
    
    REM Test webhook connectivity
    echo 🧪 Testing webhook connectivity...
    
    REM Get current unix timestamp
    for /f %%a in ('powershell -Command "[int][double]::Parse((Get-Date -UFormat %%s))"') do set "unix_timestamp=%%a"
    
    powershell -Command "try { $response = Invoke-RestMethod -Uri '%WEBHOOK_URL%' -Method Post -ContentType 'application/json' -Body '{\"from\":\"%TEST_PHONE%\",\"text\":\"Test connectivity check\",\"timestamp\":\"%unix_timestamp%\",\"sim\":\"sim1\",\"type\":\"sms\",\"test\":true}' -Headers @{'User-Agent'='Android-Nomad-Gateway-Test/1.0'}; Write-Host '✅ Webhook connectivity test completed' } catch { Write-Host '⚠️  Webhook connectivity issue: ' + $_.Exception.Message }"
) else (
    echo ❌ Failed to send SMS. Make sure emulator is running and ADB is connected.
)

echo.
echo Expected webhook payload:
echo {
echo   "from": "%TEST_PHONE%",
echo   "text": "Automated test SMS message - [timestamp]",
echo   "timestamp": "[unix_timestamp]",
echo   "sim": "sim1",
echo   "type": "sms"
echo }

endlocal 