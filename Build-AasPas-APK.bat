@echo off
title Build AasPas APK
setlocal
cd /d "%~dp0"

for %%D in ("%ProgramFiles%\Android\Android Studio\jbr" "%LOCALAPPDATA%\Programs\Android Studio\jbr") do (
    if exist "%%~D\bin\java.exe" (
        set "JAVA_HOME=%%~D"
        goto :java_found
    )
)
echo ERROR: JAVA_HOME not found. Install Android Studio or set JAVA_HOME.
exit /b 1

:java_found
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo JAVA_HOME=%JAVA_HOME%
echo.

cd android
call gradlew.bat assembleDebug testDebugUnitTest lintDebug
if errorlevel 1 (
    echo.
    echo FAIL: Build/tests/lint failed. APK was NOT published.
    cd ..
    exit /b 1
)
cd ..

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\archive_and_publish_apk.ps1" -BuildType Debug
if errorlevel 1 (
    echo.
    echo FAIL: APK publish/archive failed.
    exit /b 1
)

echo.
echo PASS: AasPas APK built, tested, linted, and published to releases\current\
exit /b 0
