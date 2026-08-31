@echo off
title AasPas Backend Stop
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\aaspas_backend_launcher.ps1" -Action stop
if /I not "%~1"=="/nopause" (
    echo.
    pause
)
