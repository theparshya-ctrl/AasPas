@echo off
title Build AasPas BETA APK
setlocal
cd /d "%~dp0"

if not exist "android\beta.properties" (
    echo ERROR: android\beta.properties not found.
    echo Copy android\beta.properties.example and set BETA_API_BASE_URL to your deployed HTTPS Beta URL.
    exit /b 1
)

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
call gradlew.bat assembleBeta testDebugUnitTest lintBeta
if errorlevel 1 (
    echo.
    echo FAIL: Beta build/tests/lint failed. BETA APK was NOT published.
    cd ..
    exit /b 1
)
cd ..

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\archive_and_publish_apk.ps1" -BuildType Beta
if errorlevel 1 (
    echo.
    echo FAIL: BETA APK publish/archive failed.
    exit /b 1
)

echo.
echo PASS: AasPas BETA APK built and published to releases\current\
echo NOTE: Beta backend must be reachable at the URL in android\beta.properties before sharing with friends.
exit /b 0
