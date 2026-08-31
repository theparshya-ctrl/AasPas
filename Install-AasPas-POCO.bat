@echo off
title Install AasPas on POCO F1
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install_aaspas_apk.ps1" -DeviceProfile POCO
if errorlevel 1 (
    echo.
    echo Install failed.
    pause
    exit /b 1
)
pause
