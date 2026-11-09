@echo off
title ParkEase - Microservices Launcher
color 0b

echo.
echo ==========================================================
echo    Launching ParkEase Microservices in Proper Order
echo ==========================================================
echo.

:: 1. Setup Java Environment
if defined JAVA_HOME goto check_java
if exist "C:\Program Files\Java\jdk-23" set "JAVA_HOME=C:\Program Files\Java\jdk-23"
if exist "C:\Program Files\Java\jdk-21" set "JAVA_HOME=C:\Program Files\Java\jdk-21"

:check_java
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [i] Using JAVA_HOME: %JAVA_HOME%

:: 2. Global Environment Variables
set "DB_USERNAME=root"
set "DB_PASSWORD=root"
set "JWT_SECRET_KEY=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
set "ROOT_DIR=%~dp0"

echo.
echo ==========================================================
echo  1. Starting Auth-Service (Port 8081)...
echo ==========================================================
start "ParkEase - Auth Service (8081)" cmd /k "cd /d "%ROOT_DIR%auth-service" && title Auth-Service (8081) && mvn spring-boot:run"
timeout /t 10 /nobreak >nul

echo.
echo ==========================================================
echo  2. Starting Vehicle-Service (Port 8086)...
echo ==========================================================
start "ParkEase - Vehicle Service (8086)" cmd /k "cd /d "%ROOT_DIR%vehicle-service" && title Vehicle-Service (8086) && mvn spring-boot:run"
timeout /t 8 /nobreak >nul

echo.
echo ==========================================================
echo  3. Starting ParkingLot-Service (Port 8082)...
echo ==========================================================
start "ParkEase - ParkingLot Service (8082)" cmd /k "cd /d "%ROOT_DIR%parkinglot-service" && title ParkingLot-Service (8082) && mvn spring-boot:run"
timeout /t 10 /nobreak >nul

echo.
echo ==========================================================
echo  4. Starting Spot-Service (Port 8083)...
echo ==========================================================
start "ParkEase - Spot Service (8083)" cmd /k "cd /d "%ROOT_DIR%spot-service" && title Spot-Service (8083) && mvn spring-boot:run"
timeout /t 8 /nobreak >nul

echo.
echo ==========================================================
echo  5. Starting Booking-Service (Port 8084)...
echo ==========================================================
start "ParkEase - Booking Service (8084)" cmd /k "cd /d "%ROOT_DIR%booking-service" && title Booking-Service (8084) && mvn spring-boot:run"
timeout /t 5 /nobreak >nul

echo.
echo ==========================================================
echo   All 5 ParkEase Microservices Started Successfully!
echo ==========================================================
echo.
echo  Swagger UI Dashboards:
echo   - Auth-Service       : http://localhost:8081/swagger-ui.html
echo   - Vehicle-Service    : http://localhost:8086/swagger-ui.html
echo   - ParkingLot-Service : http://localhost:8082/swagger-ui.html
echo   - Spot-Service       : http://localhost:8083/swagger-ui.html
echo   - Booking-Service    : http://localhost:8084/swagger-ui.html
echo.
pause
