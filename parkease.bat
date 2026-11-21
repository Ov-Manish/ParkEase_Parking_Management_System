@echo off
setlocal
cd /d "%~dp0"

:: Default double-click action starts all 9 services
if "%1"=="" (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0manage-services.ps1" -Action start
    pause
    goto end
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0manage-services.ps1" -Action %1
if not "%1"=="logs" pause

:end
