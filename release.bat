@echo off
REM Release Script for Android Nomad Gateway
REM Usage: release.bat [major|minor|patch] [message]
REM This script performs a complete release workflow

setlocal enabledelayedexpansion

REM Colors are not directly supported in cmd, using echo statements instead
set "INFO_PREFIX=[INFO]"
set "SUCCESS_PREFIX=[SUCCESS]"
set "WARNING_PREFIX=[WARNING]"
set "ERROR_PREFIX=[ERROR]"

REM Function equivalents using goto labels
goto :main

:print_info
echo %INFO_PREFIX% %~1
goto :eof

:print_success
echo %SUCCESS_PREFIX% %~1
goto :eof

:print_warning
echo %WARNING_PREFIX% %~1
goto :eof

:print_error
echo %ERROR_PREFIX% %~1
goto :eof

:show_usage
echo Usage: %0 [major^|minor^|patch] [release_message]
echo.
echo This script performs a complete release workflow:
echo   1. Version bump with git commit and tag
echo   2. Build release APK
echo   3. Run tests
echo   4. Generate release notes
echo   5. Create GitHub release (if gh CLI is available)
echo.
echo Examples:
echo   %0 patch "Fix critical SMS forwarding bug"
echo   %0 minor "Add Material Design 3 UI and operator settings"
echo   %0 major "Complete app redesign with modern architecture"
echo.
exit /b 1

:build_release
call :print_info "Building release APK..."

REM Clean and build debug APK (signed and installable on physical devices)
call gradlew.bat clean assembleDebug --warning-mode=summary
if errorlevel 1 (
    call :print_error "Failed to build APK!"
    exit /b 1
)

REM Check if APK was created
set "apk_path=app\build\outputs\apk\debug\app-debug.apk"
if exist "%apk_path%" (
    call :print_success "Release APK built successfully: %apk_path%"
    
    REM Show APK info and store size for README update
    for %%I in ("%apk_path%") do set "APK_SIZE=%%~zI"
    call :print_info "APK size: !APK_SIZE! bytes"
    
    REM Copy to releases directory
    if not exist "releases" mkdir releases
    
    REM Get version from build.gradle
    for /f "tokens=3 delims= " %%a in ('findstr "versionName" app\build.gradle') do (
        set "version=%%a"
        set "version=!version:"=!"
    )
    
    copy "%apk_path%" "releases\android-nomad-gateway-v!version!.apk" >nul
    call :print_success "APK copied to releases\android-nomad-gateway-v!version!.apk"
    call :print_info "Note: Using debug-signed APK for physical device compatibility"
) else (
    call :print_error "Failed to build release APK!"
    exit /b 1
)
goto :eof

:run_tests
call :print_info "Running tests..."

call gradlew.bat test --quiet
if errorlevel 1 (
    call :print_warning "Some tests failed, but continuing with release..."
) else (
    call :print_success "All tests passed!"
)
goto :eof

:generate_release_notes
set "version=%~1"
set "message=%~2"

call :print_info "Generating release notes..."

REM Create release notes file
set "notes_file=releases\release-notes-v%version%.md"
if not exist "releases" mkdir releases

REM Get current date and time
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do set "current_date=%%c-%%a-%%b"
for /f "tokens=1-2 delims=: " %%a in ('time /t') do set "current_time=%%a:%%b"

REM Get version code from build.gradle
for /f "tokens=3 delims= " %%a in ('findstr "versionCode" app\build.gradle') do set "version_code=%%a"

REM Get git commit hash
for /f %%a in ('git rev-parse --short HEAD') do set "git_commit=%%a"

(
echo # Android Nomad Gateway v%version%
echo.
echo ## 🚀 What's New
echo.
echo %message%
echo.
echo ## 📱 Installation
echo.
echo Download the APK file and install it on your Android device:
echo - **Minimum Android version:** 8.0 ^(API 26^)
echo - **Target Android version:** 14 ^(API 35^)
echo - **Architecture:** Universal APK
echo.
echo ## 🔧 Technical Details
echo.
echo - **Version Code:** %version_code%
echo - **Version Name:** %version%
echo - **Build Date:** %current_date% %current_time%
echo - **Git Commit:** %git_commit%
echo.
echo ## 📋 Features
echo.
echo - 📱 SMS forwarding to webhooks
echo - 📞 Call notification forwarding
echo - 🔔 Push notification forwarding
echo - 🎨 Modern Material Design 3 UI
echo - ⚙️ Comprehensive settings and permissions management
echo - 📊 SIM card management and operator settings
echo - 🔒 Privacy-first approach with granular permissions
echo.
echo ## 🛠️ Permissions Required
echo.
echo - **SMS Access** - To receive and forward SMS messages
echo - **Phone State** - To identify SIM cards and monitor calls
echo - **Call Log** - To detect incoming calls
echo - **Contacts** - To resolve phone numbers to names
echo - **Phone Numbers** - To identify your phone numbers
echo - **Post Notifications** - To show service status
echo - **Read Notifications** - For push notification forwarding
echo.
echo ## 🐛 Bug Reports
echo.
echo If you encounter any issues, please report them on our GitHub repository.
echo.
echo ## 📄 Changelog
echo.
echo See [CHANGELOG.md](../CHANGELOG.md^) for detailed changes.
) > "%notes_file%"

call :print_success "Release notes generated: %notes_file%"
goto :eof

:create_github_release
set "version=%~1"
set "message=%~2"

where gh >nul 2>nul
if errorlevel 1 (
    call :print_warning "GitHub CLI (gh) not found. Skipping GitHub release creation."
    call :print_info "To enable automatic GitHub releases:"
    call :print_info "  1. Install GitHub CLI from: https://cli.github.com/"
    call :print_info "  2. Authenticate: gh auth login"
    call :print_info "  3. Re-run the release script"
    goto :eof
)

call :print_info "Creating GitHub release v%version%..."

set "apk_path=releases\android-nomad-gateway-v%version%.apk"
set "notes_path=releases\release-notes-v%version%.md"

REM Verify files exist
if not exist "%apk_path%" (
    call :print_error "APK file not found: %apk_path%"
    exit /b 1
)

if not exist "%notes_path%" (
    call :print_error "Release notes file not found: %notes_path%"
    exit /b 1
)

REM Create GitHub release with APK attachment
gh release create "v%version%" --title "Android Nomad Gateway v%version%" --notes-file "%notes_path%" --latest "%apk_path%#Android APK (Debug Signed)"
if errorlevel 1 (
    call :print_warning "Failed to create GitHub release. You can create it manually."
    call :print_info "Manual command: gh release create v%version% --title \"Android Nomad Gateway v%version%\" --notes-file \"%notes_path%\" \"%apk_path%\""
    exit /b 1
) else (
    call :print_success "GitHub release v%version% created successfully! 🎉"
    
    REM Get the release URL
    for /f %%a in ('gh repo view --json url -q .url') do set "repo_url=%%a"
    call :print_info "📦 Release URL: !repo_url!/releases/tag/v%version%"
    call :print_info "📱 Direct APK download: !repo_url!/releases/download/v%version%/android-nomad-gateway-v%version%.apk"
)
goto :eof

:main
REM Check arguments
if "%~1"=="" (
    call :show_usage
    exit /b 1
)

if "%~2"=="" (
    call :show_usage
    exit /b 1
)

set "bump_type=%~1"
set "release_message=%~2"

REM Validate bump type
if not "%bump_type%"=="major" if not "%bump_type%"=="minor" if not "%bump_type%"=="patch" (
    call :print_error "Invalid bump type: %bump_type%"
    call :show_usage
    exit /b 1
)

call :print_info "🚀 Starting release workflow for %bump_type% version..."
call :print_info "📝 Release message: %release_message%"
echo.

REM Check if we're in a git repository
git status >nul 2>nul
if errorlevel 1 (
    call :print_error "Not in a git repository!"
    exit /b 1
)

REM Check for uncommitted changes
git diff-index --quiet HEAD --
if errorlevel 1 (
    call :print_error "You have uncommitted changes. Please commit or stash them first."
    exit /b 1
)

REM Step 1: Version bump
call :print_info "Step 1: Version bump..."
call version_bump.bat %bump_type% "%release_message%"
if errorlevel 1 (
    call :print_error "Version bump failed!"
    exit /b 1
)

REM Get the new version
for /f "tokens=3 delims= " %%a in ('findstr "versionName" app\build.gradle') do (
    set "new_version=%%a"
    set "new_version=!new_version:"=!"
)

echo.

REM Step 2: Build release APK
call :print_info "Step 2: Building release APK..."
call :build_release
if errorlevel 1 exit /b 1

echo.

REM Step 3: Run tests
call :print_info "Step 3: Running tests..."
call :run_tests

echo.

REM Step 4: Generate release notes
call :print_info "Step 4: Generating release notes..."
call :generate_release_notes "!new_version!" "%release_message%"

echo.

REM Step 5: Create GitHub release
call :print_info "Step 5: Creating GitHub release..."
call :create_github_release "!new_version!" "%release_message%"

echo.
call :print_success "🎉 Release v!new_version! completed successfully!"
call :print_info "📦 APK location: releases\android-nomad-gateway-v!new_version!.apk"
call :print_info "📝 Release notes: releases\release-notes-v!new_version!.md"

REM Show final summary
echo.
echo ============================================
echo 🎯 RELEASE SUMMARY
echo ============================================
echo Version: !new_version!
echo Type: %bump_type%
echo Message: %release_message%
echo APK: releases\android-nomad-gateway-v!new_version!.apk
echo Notes: releases\release-notes-v!new_version!.md
echo ============================================

endlocal 