@echo off
title AasPas Backend
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\aaspas_backend_launcher.ps1" -Action start
if errorlevel 1 (
    echo.
    echo Start failed. This window will stay open so you can read the error.
    pause
)
