@echo off
title ParkEase - Microservices Stopper
color 0c

echo.
echo ==========================================================
echo    Stopping All ParkEase Microservices (8081, 8082, 8083, 8084, 8086)
echo ==========================================================
echo.

for %%P in (8081 8082 8083 8084 8086) do (
    echo Stopping process on port %%P...
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr /r /c:":%%P *LISTENING"') do (
        taskkill /f /pid %%a >nul 2>&1
        if not errorlevel 1 (
            echo   [x] Killed PID %%a on port %%P
        )
    )
)

echo.
echo ==========================================================
echo    All ParkEase services have been stopped.
echo ==========================================================
pause
