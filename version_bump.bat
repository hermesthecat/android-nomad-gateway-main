@echo off
REM Version Bump Script for Android Nomad Gateway
REM Usage: version_bump.bat [major|minor|patch] [message]
REM Example: version_bump.bat minor "Add new SMS forwarding features"

setlocal enabledelayedexpansion

REM Colors for output (using prefixes since cmd doesn't support colors directly)
set "INFO_PREFIX=[INFO]"
set "SUCCESS_PREFIX=[SUCCESS]"
set "WARNING_PREFIX=[WARNING]"
set "ERROR_PREFIX=[ERROR]"

REM Configuration
set "BUILD_GRADLE_FILE=app\build.gradle"
set "CHANGELOG_FILE=CHANGELOG.md"

REM Function to print colored output
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
echo Usage: %0 [major^|minor^|patch] [commit_message]
echo.
echo Version bump types:
echo   major  - Increment major version (1.0.0 -^> 2.0.0)
echo   minor  - Increment minor version (1.0.0 -^> 1.1.0)
echo   patch  - Increment patch version (1.0.0 -^> 1.0.1)
echo.
echo Examples:
echo   %0 patch "Fix SMS forwarding bug"
echo   %0 minor "Add push notification support"
echo   %0 major "Complete UI redesign"
echo.
exit /b 1

:get_current_version
REM Get current version from build.gradle
for /f "tokens=3 delims= " %%a in ('findstr "versionCode" %BUILD_GRADLE_FILE%') do set "version_code=%%a"
for /f "tokens=3 delims= " %%a in ('findstr "versionName" %BUILD_GRADLE_FILE%') do (
    set "version_name=%%a"
    set "version_name=!version_name:"=!"
)

set "RETURN_VAL_1=!version_code!"
set "RETURN_VAL_2=!version_name!"
goto :eof

:parse_version
set "version=%~1"
set "major="
set "minor="
set "patch="

REM Parse semantic version using string operations
for /f "tokens=1,2,3 delims=." %%a in ("%version%") do (
    set "major=%%a"
    set "minor=%%b"
    set "patch=%%c"
)

REM Handle cases where patch or minor might be missing
if "%minor%"=="" set "minor=0"
if "%patch%"=="" set "patch=0"

set "RETURN_VAL_1=!major!"
set "RETURN_VAL_2=!minor!"
set "RETURN_VAL_3=!patch!"
goto :eof

:increment_version
set "bump_type=%~1"
set "current_version=%~2"

REM Parse current version
call :parse_version "%current_version%"
set "major=!RETURN_VAL_1!"
set "minor=!RETURN_VAL_2!"
set "patch=!RETURN_VAL_3!"

REM Increment based on bump type
if "%bump_type%"=="major" (
    set /a major=major+1
    set "minor=0"
    set "patch=0"
) else if "%bump_type%"=="minor" (
    set /a minor=minor+1
    set "patch=0"
) else if "%bump_type%"=="patch" (
    set /a patch=patch+1
) else (
    call :print_error "Invalid bump type: %bump_type%"
    exit /b 1
)

set "RETURN_VAL=!major!.!minor!.!patch!"
goto :eof

:update_build_gradle
set "new_version_code=%~1"
set "new_version_name=%~2"

call :print_info "Updating %BUILD_GRADLE_FILE%..."

REM Create backup
copy "%BUILD_GRADLE_FILE%" "%BUILD_GRADLE_FILE%.backup" >nul

REM Create temporary file for modifications
set "temp_file=%BUILD_GRADLE_FILE%.tmp"

REM Update versionCode and versionName
(
    for /f "delims=" %%a in (%BUILD_GRADLE_FILE%) do (
        set "line=%%a"
        if "!line:versionCode=!" neq "!line!" (
            echo         versionCode = %new_version_code%
        ) else if "!line:versionName=!" neq "!line!" (
            echo         versionName = "%new_version_name%"
        ) else (
            echo !line!
        )
    )
) > "%temp_file%"

REM Replace original file
move "%temp_file%" "%BUILD_GRADLE_FILE%" >nul

call :print_success "Updated version in %BUILD_GRADLE_FILE%"
goto :eof

:update_changelog
set "version=%~1"
set "message=%~2"

REM Get current date
for /f "tokens=2-4 delims=/ " %%a in ('date /t') do set "current_date=%%c-%%a-%%b"

call :print_info "Updating %CHANGELOG_FILE%..."

REM Create CHANGELOG.md if it doesn't exist
if not exist "%CHANGELOG_FILE%" (
    (
        echo # Changelog
        echo.
        echo All notable changes to this project will be documented in this file.
        echo.
        echo The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/^),
        echo and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html^).
        echo.
        echo ## [Unreleased]
        echo.
        echo ## [%version%] - %current_date%
        echo.
        echo ### Added
        echo - %message%
        echo.
    ) > "%CHANGELOG_FILE%"
) else (
    REM Insert new version entry after "## [Unreleased]"
    set "temp_file=%CHANGELOG_FILE%.tmp"
    set "found_unreleased=false"
    
    (
        for /f "delims=" %%a in (%CHANGELOG_FILE%) do (
            set "line=%%a"
            echo !line!
            if "!line!"=="## [Unreleased]" if "!found_unreleased!"=="false" (
                echo.
                echo ## [%version%] - %current_date%
                echo.
                echo ### Added
                echo - %message%
                echo.
                set "found_unreleased=true"
            )
        )
    ) > "!temp_file!"
    
    move "!temp_file!" "%CHANGELOG_FILE%" >nul
)

call :print_success "Updated %CHANGELOG_FILE%"
goto :eof

:commit_and_tag
set "version=%~1"
set "message=%~2"

call :print_info "Committing changes and creating git tag..."

REM Add files to git
git add "%BUILD_GRADLE_FILE%" "%CHANGELOG_FILE%"
if errorlevel 1 (
    call :print_error "Failed to add files to git!"
    exit /b 1
)

REM Commit changes
git commit -m "chore: bump version to %version%" -m "%message%" -m "- Updated versionCode and versionName in build.gradle" -m "- Updated CHANGELOG.md with release notes"
if errorlevel 1 (
    call :print_error "Failed to commit changes!"
    exit /b 1
)

REM Create and push tag
git tag -a "v%version%" -m "Release v%version%: %message%"
if errorlevel 1 (
    call :print_error "Failed to create tag!"
    exit /b 1
)

call :print_success "Created git tag v%version%"

REM Ask if user wants to push changes
set /p "push_choice=Push changes to remote repository? (y/N): "
if /i "%push_choice%"=="y" (
    call :print_info "Pushing changes to remote..."
    git push origin main
    git push origin "v%version%"
    if errorlevel 1 (
        call :print_warning "Failed to push to remote. You may need to push manually:"
        call :print_info "  git push origin main"
        call :print_info "  git push origin v%version%"
    ) else (
        call :print_success "Changes pushed to remote repository!"
    )
) else (
    call :print_info "Skipped pushing to remote. You can push manually later:"
    call :print_info "  git push origin main"
    call :print_info "  git push origin v%version%"
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
set "commit_message=%~2"

REM Validate bump type
if not "%bump_type%"=="major" if not "%bump_type%"=="minor" if not "%bump_type%"=="patch" (
    call :print_error "Invalid bump type: %bump_type%"
    call :show_usage
    exit /b 1
)

call :print_info "🚀 Starting version bump for %bump_type% version..."
call :print_info "📝 Commit message: %commit_message%"
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
    call :print_info "Use 'git status' to see uncommitted changes."
    exit /b 1
)

REM Check if build.gradle exists
if not exist "%BUILD_GRADLE_FILE%" (
    call :print_error "Build gradle file not found: %BUILD_GRADLE_FILE%"
    exit /b 1
)

REM Get current version
call :get_current_version
set "current_version_code=!RETURN_VAL_1!"
set "current_version_name=!RETURN_VAL_2!"

call :print_info "Current version: !current_version_name! (code: !current_version_code!)"

REM Calculate new version
call :increment_version "!bump_type!" "!current_version_name!"
set "new_version_name=!RETURN_VAL!"
set /a new_version_code=!current_version_code!+1

call :print_info "New version: !new_version_name! (code: !new_version_code!)"
echo.

REM Confirm with user
set /p "confirm=Continue with version bump? (Y/n): "
if /i "%confirm%"=="n" (
    call :print_info "Version bump cancelled."
    exit /b 0
)

REM Update build.gradle
call :update_build_gradle "%new_version_code%" "%new_version_name%"
if errorlevel 1 exit /b 1

echo.

REM Update CHANGELOG.md
call :update_changelog "%new_version_name%" "%commit_message%"
if errorlevel 1 exit /b 1

echo.

REM Commit and tag
call :commit_and_tag "%new_version_name%" "%commit_message%"
if errorlevel 1 exit /b 1

echo.
call :print_success "🎉 Version bump completed successfully!"
call :print_info "📦 New version: !new_version_name! (code: !new_version_code!)"
call :print_info "🏷️  Git tag: v!new_version_name!"

REM Show next steps
echo.
echo ============================================
echo 🎯 NEXT STEPS
echo ============================================
echo 1. Build and test the new version:
echo    gradlew.bat assembleDebug
echo.
echo 2. Create a release (optional):
echo    release.bat %bump_type% "%commit_message%"
echo.
echo 3. Verify the changes:
echo    git log --oneline -3
echo    git show v!new_version_name!
echo ============================================

endlocal 