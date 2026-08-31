@echo off
title AasPas Backend Restart
cd /d "%~dp0"
echo Stopping AasPas backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\aaspas_backend_launcher.ps1" -Action stop
if errorlevel 1 (
    echo.
    echo Stop failed. This window will stay open so you can read the error.
    pause
    exit /b 1
)
echo Waiting briefly...
timeout /t 2 /nobreak >nul
echo Starting AasPas backend...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\aaspas_backend_launcher.ps1" -Action start
if errorlevel 1 (
    echo.
    echo Restart failed. This window will stay open so you can read the error.
    pause
)
