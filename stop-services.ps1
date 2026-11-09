# ==============================================================================
# ParkEase Smart Parking Management System - Microservices Shutdown Script
# ==============================================================================

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Red
Write-Host "   🛑 Stopping All ParkEase Microservices                  " -ForegroundColor Red
Write-Host "==========================================================" -ForegroundColor Red
Write-Host ""

$ports = @(8081, 8082, 8083, 8086)

foreach ($port in $ports) {
    try {
        $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($connections) {
            foreach ($conn in $connections) {
                $processId = $conn.OwningProcess
                $proc = Get-Process -Id $processId -ErrorAction SilentlyContinue
                Write-Host " [x] Stopping service on Port $port (PID: $processId, Name: $($proc.ProcessName))..." -ForegroundColor Yellow
                Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                Write-Host "     [✓] Successfully terminated PID $processId." -ForegroundColor Green
            }
        } else {
            Write-Host " [-] No active service listening on Port $port." -ForegroundColor DarkGray
        }
    } catch {
        Write-Host " [!] Error checking port $port: $_" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "   ✅ All ParkEase services have been stopped.            " -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host ""
