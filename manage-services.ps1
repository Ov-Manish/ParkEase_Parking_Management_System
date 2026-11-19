# ==============================================================================
# ParkEase Smart Parking Management System - Microservices Control Hub
# ==============================================================================
# Manages all 7 microservices in the background with ZERO popup command windows.
# Usage:
#   .\manage-services.ps1 -Action start    -> Starts all services in proper order
#   .\manage-services.ps1 -Action stop     -> Stops all services & frees ports
#   .\manage-services.ps1 -Action status   -> Displays port & health status table
#   .\manage-services.ps1 -Action logs -Service notification-service -> View live logs
# ==============================================================================

param (
    [ValidateSet("start", "stop", "status", "logs", "restart")]
    [string]$Action = "status",

    [ValidateSet("auth-service", "vehicle-service", "parkinglot-service", "spot-service", "booking-service", "payment-service", "notification-service")]
    [string]$Service = "notification-service",

    [string]$DbUser = "root",
    [string]$DbPassword = "root",
    [string]$JwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    [string]$MailUsername = $env:SPRING_MAIL_USERNAME,
    [string]$MailPassword = $env:SPRING_MAIL_PASSWORD
)

$ErrorActionPreference = "Continue"
$RootPath = $PSScriptRoot
$LogsDir = Join-Path $RootPath "logs"

# Define all 7 microservices in their optimal dependency startup sequence
$ServicesList = @(
    @{ Name = "auth-service";        Port = 8081; Order = 1; Desc = "Authentication & JWT Provider" },
    @{ Name = "vehicle-service";     Port = 8086; Order = 2; Desc = "Vehicle Registry & Management" },
    @{ Name = "parkinglot-service";  Port = 8082; Order = 3; Desc = "Parking Lot Management & Layout" },
    @{ Name = "spot-service";        Port = 8083; Order = 4; Desc = "Spot Inventory & Real-time State" },
    @{ Name = "booking-service";     Port = 8084; Order = 5; Desc = "Reservation Engine & Event Producer" },
    @{ Name = "payment-service";     Port = 8085; Order = 6; Desc = "Payment & Wallet Event Producer" },
    @{ Name = "notification-service"; Port = 8087; Order = 7; Desc = "Email & Notification Event Consumer" }
)

function Ensure-JavaHome {
    if (-not $env:JAVA_HOME) {
        if (Test-Path "C:\Program Files\Java\jdk-23") {
            $env:JAVA_HOME = "C:\Program Files\Java\jdk-23"
        } elseif (Test-Path "C:\Program Files\Java\jdk-21") {
            $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
        }
    }
    if ($env:JAVA_HOME) {
        $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
    }
}

function Show-Status {
    Write-Host ""
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host "           ParkEase Microservices Status Dashboard                       " -ForegroundColor Cyan
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host ""

    $statusReport = @()
    foreach ($svc in $ServicesList) {
        $port = $svc.Port
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        
        $statusReport += [PSCustomObject]@{
            "Order"   = $svc.Order
            "Service" = $svc.Name
            "Port"    = $port
            "Status"  = if ($conn) { "UP (Running)" } else { "DOWN (Stopped)" }
            "PID"     = if ($conn) { $conn.OwningProcess } else { "-" }
            "Swagger / Health" = "http://localhost:$port/swagger-ui.html"
        }
    }

    $statusReport | Format-Table -AutoSize
    Write-Host "Logs Location: $LogsDir\<service-name>.log`n" -ForegroundColor DarkGray
}

function Stop-AllServices {
    Write-Host ""
    Write-Host "==========================================================================" -ForegroundColor Red
    Write-Host "             Stopping All ParkEase Microservices                          " -ForegroundColor Red
    Write-Host "==========================================================================" -ForegroundColor Red
    Write-Host ""

    foreach ($svc in $ServicesList) {
        $port = $svc.Port
        $name = $svc.Name
        try {
            $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
            if ($connections) {
                foreach ($conn in $connections) {
                    $pidToKill = $conn.OwningProcess
                    Write-Host " [x] Stopping $name on Port $port (PID: $pidToKill)..." -ForegroundColor Yellow
                    Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
                    Write-Host "     [OK] Stopped PID $pidToKill." -ForegroundColor Green
                }
            } else {
                Write-Host " [-] $name (Port $port) is already stopped." -ForegroundColor DarkGray
            }
        } catch {
            Write-Host " [!] Error stopping port ${port}: $_" -ForegroundColor Red
        }
    }
    Write-Host ""
    Write-Host "[OK] All microservices stopped." -ForegroundColor Green
    Write-Host ""
}

function Start-AllServices {
    Ensure-JavaHome

    if (-not (Test-Path $LogsDir)) {
        New-Item -ItemType Directory -Path $LogsDir -Force | Out-Null
    }

    Write-Host ""
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host "     Starting All ParkEase Microservices (Quiet Background Mode)          " -ForegroundColor Cyan
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host " [i] JAVA_HOME : $env:JAVA_HOME" -ForegroundColor DarkGray
    Write-Host " [i] Logs Dir  : $LogsDir" -ForegroundColor DarkGray
    Write-Host " [i] Note: No popup windows will appear. Everything runs cleanly in background." -ForegroundColor DarkGray
    Write-Host ""

    # Check MySQL (port 3309)
    $mysqlCheck = Get-NetTCPConnection -LocalPort 3309 -State Listen -ErrorAction SilentlyContinue
    if (-not $mysqlCheck) {
        Write-Host " [!] WARNING: MySQL is not detected on port 3309. Ensure MySQL is running." -ForegroundColor Yellow
    }

    # Check RabbitMQ (port 5672)
    $rabbitCheck = Get-NetTCPConnection -LocalPort 5672 -State Listen -ErrorAction SilentlyContinue
    if (-not $rabbitCheck) {
        Write-Host " [!] WARNING: RabbitMQ is not detected on port 5672. Ensure RabbitMQ is running." -ForegroundColor Yellow
    }

    foreach ($svc in $ServicesList) {
        $name = $svc.Name
        $port = $svc.Port
        $dir = Join-Path $RootPath $name
        $logFile = Join-Path $LogsDir "$name.log"

        # Check if already running
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conn) {
            Write-Host " [=] $name is already RUNNING on Port $port (PID: $($conn.OwningProcess)). Skipping." -ForegroundColor Yellow
            continue
        }

        Write-Host " [>] Launching $($svc.Order)/7: $name on Port $port..." -ForegroundColor Cyan

        # Prepare background process using Maven wrapper / installed maven
        $mailUserArg = if ($MailUsername) { "set SPRING_MAIL_USERNAME=$MailUsername&& " } else { "" }
        $mailPassArg = if ($MailPassword) { "set SPRING_MAIL_PASSWORD=$MailPassword&& " } else { "" }

        $cmdCommand = "/c set JAVA_HOME=$($env:JAVA_HOME)&& set PATH=$($env:JAVA_HOME)\bin;%PATH%&& set DB_USERNAME=$DbUser&& set DB_PASSWORD=$DbPassword&& set JWT_SECRET_KEY=$JwtSecret&& $mailUserArg$mailPassArg cd /d `"$dir`"&& mvn spring-boot:run"

        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = "cmd.exe"
        $psi.Arguments = $cmdCommand
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $psi.UseShellExecute = $false
        $psi.CreateNoWindow = $true

        $process = New-Object System.Diagnostics.Process
        $process.StartInfo = $psi

        # Clean previous log
        if (Test-Path $logFile) { Clear-Content -Path $logFile -ErrorAction SilentlyContinue }

        # Asynchronously capture output to log file
        $outEvent = Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
            if ($EventArgs.Data) { Add-Content -Path $Event.MessageData -Value $EventArgs.Data -ErrorAction SilentlyContinue }
        } -MessageData $logFile

        $errEvent = Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
            if ($EventArgs.Data) { Add-Content -Path $Event.MessageData -Value $EventArgs.Data -ErrorAction SilentlyContinue }
        } -MessageData $logFile

        [void]$process.Start()
        $process.BeginOutputReadLine()
        $process.BeginErrorReadLine()

        Write-Host "     Started in background. Logs: logs\$name.log" -ForegroundColor DarkGray
        Start-Sleep -Seconds 3
    }

    Write-Host ""
    Write-Host "[OK] All 7 services initiated. Run '.\manage-services.ps1 -Action status' in ~30s to view UP status." -ForegroundColor Green
    Write-Host ""
}

function Show-Logs {
    $logFile = Join-Path $LogsDir "$Service.log"
    if (-not (Test-Path $logFile)) {
        Write-Host " [!] Log file does not exist yet for $Service ($logFile)." -ForegroundColor Yellow
        return
    }

    Write-Host "Streaming logs for $Service (Press Ctrl+C to exit)..." -ForegroundColor Cyan
    Get-Content -Path $logFile -Tail 50 -Wait
}

# --- Action Switcher ---
switch ($Action.ToLower()) {
    "start"   { Start-AllServices }
    "stop"    { Stop-AllServices }
    "status"  { Show-Status }
    "logs"    { Show-Logs }
    "restart" { Stop-AllServices; Start-Sleep -Seconds 2; Start-AllServices }
    default   { Show-Status }
}
