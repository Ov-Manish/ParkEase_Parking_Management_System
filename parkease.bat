@echo off
setlocal
cd /d "%~dp0"

:: Simple double-clickable Batch Wrapper around manage-services.ps1
:: If no argument given, show status dashboard
if "%1"=="" (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0manage-services.ps1" -Action status
    pause
    goto end
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0manage-services.ps1" -Action %1
if not "%1"=="logs" pause

:end
