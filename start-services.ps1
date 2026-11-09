# ==============================================================================
# ParkEase Smart Parking Management System - Microservices Startup Script
# ==============================================================================
# Correct Startup Order:
#   1. auth-service       (Port 8081) - Generates & validates JWT credentials
#   2. vehicle-service    (Port 8086) - Vehicle fleet & license plate management
#   3. parkinglot-service (Port 8082) - Lot catalog, facilities, & admin approval
#   4. spot-service       (Port 8083) - Spot inventory & capacity sync with 8082
# ==============================================================================

param (
    [string]$DbUser = "root",
    [string]$DbPassword = "root",
    [string]$JwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   🚀 Starting ParkEase Microservices in Order            " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Resolve JDK / Java Home
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\Java\jdk-23") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    } elseif (Test-Path "C:\Program Files\Java\jdk-21") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}
Write-Host " [i] Using JAVA_HOME: $env:JAVA_HOME" -ForegroundColor DarkGray

# 2. Check MySQL Connectivity on Port 3309
Write-Host " [1/5] Checking MySQL on port 3309..." -ForegroundColor Yellow
$tcpClient = New-Object System.Net.Sockets.TcpClient
$asyncResult = $tcpClient.BeginConnect("127.0.0.1", 3309, $null, $null)
$success = $asyncResult.AsyncWaitHandle.WaitOne(2000, $false)

if (-not $success -or -not $tcpClient.Connected) {
    Write-Host " [!] WARNING: MySQL is not reachable on localhost:3309!" -ForegroundColor Red
    Write-Host "     Please ensure your MySQL container / service is running before services start." -ForegroundColor Red
} else {
    $tcpClient.Close()
    Write-Host " [✓] MySQL is UP on port 3309." -ForegroundColor Green
}
Write-Host ""

$RootPath = (Get-Location).Path

# Helper function to launch a microservice in a dedicated terminal window
function Start-ParkEaseService {
    param (
        [string]$ServiceName,
        [int]$Port,
        [string]$Folder,
        [int]$WaitSeconds
    )

    Write-Host " [>] Launching $ServiceName on Port $Port..." -ForegroundColor Cyan

    $cmd = "title $ServiceName (Port $Port) && " +
           "set JAVA_HOME=$($env:JAVA_HOME) && " +
           "set PATH=$($env:JAVA_HOME)\bin;%PATH% && " +
           "set DB_USERNAME=$DbUser && " +
           "set DB_PASSWORD=$DbPassword && " +
           "set JWT_SECRET_KEY=$JwtSecret && " +
           "cd /d `"$RootPath\$Folder`" && " +
           "mvn spring-boot:run"

    Start-Process "cmd.exe" -ArgumentList "/k `"$cmd`""

    Write-Host "     Window opened. Waiting $WaitSeconds seconds for initialization..." -ForegroundColor DarkGray
    Start-Sleep -Seconds $WaitSeconds
}

# --- Service 1: Auth Service (Port 8081) ---
Start-ParkEaseService -ServiceName "ParkEase - Auth Service" `
                      -Port 8081 `
                      -Folder "auth-service" `
                      -WaitSeconds 10

# --- Service 2: Vehicle Service (Port 8086) ---
Start-ParkEaseService -ServiceName "ParkEase - Vehicle Service" `
                      -Port 8086 `
                      -Folder "vehicle-service" `
                      -WaitSeconds 8

# --- Service 3: ParkingLot Service (Port 8082) ---
Start-ParkEaseService -ServiceName "ParkEase - ParkingLot Service" `
                      -Port 8082 `
                      -Folder "parkinglot-service" `
                      -WaitSeconds 10

# --- Service 4: Spot Service (Port 8083) ---
Start-ParkEaseService -ServiceName "ParkEase - Spot Service" `
                      -Port 8083 `
                      -Folder "spot-service" `
                      -WaitSeconds 5

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "   ✅ All 4 Services Have Been Launched!                 " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " 1. auth-service       : http://localhost:8081/swagger-ui.html" -ForegroundColor White
Write-Host " 2. vehicle-service    : http://localhost:8086/swagger-ui.html" -ForegroundColor White
Write-Host " 3. parkinglot-service : http://localhost:8082/swagger-ui.html" -ForegroundColor White
Write-Host " 4. spot-service       : http://localhost:8083/swagger-ui.html" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
