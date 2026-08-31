@echo off
title Check AasPas Device Versions
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\check_aaspas_devices.ps1"
echo.
pause
