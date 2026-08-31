@echo off
title Install AasPas on Samsung S23
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install_aaspas_apk.ps1" -DeviceProfile Samsung
if errorlevel 1 (
    echo.
    echo Install failed.
    pause
    exit /b 1
)
pause
