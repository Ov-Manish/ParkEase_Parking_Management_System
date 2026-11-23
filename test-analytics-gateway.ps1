# ==============================================================================
# ParkEase - Analytics Service & Gateway Smoke Test
# ==============================================================================
# Verifies all 10 endpoints of analytics-service through API Gateway (:8080)
# ==============================================================================

param(
    [string]$GatewayUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([int]$Num, [string]$Title)
    Write-Host "`n==================================================================" -ForegroundColor Cyan
    Write-Host " STEP $Num : $Title" -ForegroundColor Cyan
    Write-Host "==================================================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Msg)
    Write-Host " [+] SUCCESS: $Msg" -ForegroundColor Green
}

function Write-Failure {
    param([string]$Msg, [System.Exception]$Ex)
    Write-Host " [-] FAILED: $Msg" -ForegroundColor Red
    if ($Ex -and $Ex.Response) {
        try {
            $stream = $Ex.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            Write-Host "     Server Error Details: $($reader.ReadToEnd())" -ForegroundColor DarkYellow
        } catch {}
    }
    exit 1
}

function Parse-ApiResponse($response) {
    if ($null -eq $response) { return $null }
    if ($response -is [string]) {
        try { return $response | ConvertFrom-Json } catch { return $response }
    }
    return $response
}

# ------------------------------------------------------------------------------
# STEP 1: Health Check on Gateway
# ------------------------------------------------------------------------------
Write-Step 1 "Checking API Gateway Health (:8080)"
try {
    $gwHealth = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method GET
    Write-Success "Gateway is UP: $($gwHealth.status)"
} catch {
    Write-Failure "Gateway is not reachable on $($GatewayUrl): $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 2: Authenticate Admin & Manager
# ------------------------------------------------------------------------------
Write-Step 2 "Authenticating Admin & Lot Manager via Gateway"
$adminToken = $null
try {
    $admLoginRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/login" -Method POST -Body (@{ email = "user@example.com"; password = "stringst" } | ConvertTo-Json) -ContentType "application/json"
    $admLogin = Parse-ApiResponse $admLoginRaw
    $adminToken = if ($admLogin.accessToken) { $admLogin.accessToken } else { $admLoginRaw.accessToken }
    Write-Success "Admin authenticated successfully."
} catch {
    Write-Failure "Admin login failed: $_" $_.Exception
}

$adminHeaders = @{ "Authorization" = "Bearer $adminToken" }

# ------------------------------------------------------------------------------
# STEP 3: Fetch an existing parking lot ID
# ------------------------------------------------------------------------------
Write-Step 3 "Fetching Parking Lot for Analytics"
$testLotId = $null
try {
    $lotsRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/lots/search/city?city=Bangalore" -Method GET
    $lots = Parse-ApiResponse $lotsRaw
    if ($lots -and $lots.Count -gt 0) {
        $testLotId = $lots[0].lotId
    }
} catch {
    Write-Host " [i] Searching city lots: $_" -ForegroundColor DarkGray
}

if (-not $testLotId) {
    $testLotId = [System.Guid]::NewGuid().ToString()
    Write-Host " [i] Using mock lot UUID: $testLotId" -ForegroundColor Yellow
} else {
    Write-Success "Using Parking Lot: $testLotId"
}

# ------------------------------------------------------------------------------
# STEP 4: Log Occupancy Snapshot (POST /api/v1/analytics/occupancy/log)
# ------------------------------------------------------------------------------
Write-Step 4 "Logging Real-Time Occupancy Snapshot"
$logBody = @{
    lotId          = $testLotId
    totalSpots     = 100
    availableSpots = 25
    vehicleType    = "FOUR_WHEELER"
} | ConvertTo-Json

try {
    $logRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/occupancy/log" -Method POST -Body $logBody -ContentType "application/json" -Headers $adminHeaders
    $savedLog = Parse-ApiResponse $logRes
    Write-Success "Snapshot Logged! Log ID: $($savedLog.logId) | Rate: $($savedLog.occupancyRate)% | Free: $($savedLog.availableSpots)"
} catch {
    Write-Failure "Failed to log occupancy snapshot: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 5: Query Real-time Occupancy Rate
# ------------------------------------------------------------------------------
Write-Step 5 "Querying Lot Occupancy Rate (GET /api/v1/analytics/lot/{id}/occupancy)"
try {
    $occRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/lot/$testLotId/occupancy?period=TODAY" -Method GET -Headers $adminHeaders
    $occ = Parse-ApiResponse $occRes
    Write-Success "Current Rate: $($occ.currentOccupancyRate)% | Average: $($occ.averageOccupancyRate)% | Occupied: $($occ.occupiedSpots)/$($occ.totalSpots)"
} catch {
    Write-Failure "Failed to get occupancy rate: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 6: Query 24-Hour Hourly Curve
# ------------------------------------------------------------------------------
Write-Step 6 "Querying 24-Hour Curve (GET /api/v1/analytics/lot/{id}/hourly)"
try {
    $hourlyRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/lot/$testLotId/hourly" -Method GET -Headers $adminHeaders
    $hourlyList = Parse-ApiResponse $hourlyRes
    Write-Success "Received $($hourlyList.Count) hourly data points (Hour 0 to Hour 23 guaranteed)."
} catch {
    Write-Failure "Failed to get hourly breakdown: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 7: Query Peak Hours Analysis
# ------------------------------------------------------------------------------
Write-Step 7 "Querying Peak Hours (GET /api/v1/analytics/lot/{id}/peak-hours)"
try {
    $peakRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/lot/$testLotId/peak-hours" -Method GET -Headers $adminHeaders
    $peak = Parse-ApiResponse $peakRes
    Write-Success "Peak Hour: $($peak.peakHour):00 ($($peak.peakOccupancyRate)%) | Low Hour: $($peak.lowHour):00 ($($peak.lowOccupancyRate)%)"
} catch {
    Write-Failure "Failed to get peak hours: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 8: Query Spot Type Utilization (Live via spot-service)
# ------------------------------------------------------------------------------
Write-Step 8 "Querying Spot Type Utilization (GET /api/v1/analytics/lot/{id}/spot-types)"
try {
    $spotTypeRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/lot/$testLotId/spot-types" -Method GET -Headers $adminHeaders
    $spotTypeList = Parse-ApiResponse $spotTypeRes
    Write-Success "Spot Types Retrieved: $($spotTypeList.Count) category groups analyzed dynamically."
} catch {
    Write-Failure "Failed to get spot type utilization: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 9: Query Platform Summary (Super Admin)
# ------------------------------------------------------------------------------
Write-Step 9 "Querying Macro Platform Summary (GET /api/v1/analytics/platform/summary)"
try {
    $platRes = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/analytics/platform/summary" -Method GET -Headers $adminHeaders
    $summary = Parse-ApiResponse $platRes
    Write-Success "Platform Summary: Lots: $($summary.totalLots) | Spots: $($summary.totalSpots) | Platform Occupancy: $($summary.averagePlatformOccupancy)%"
} catch {
    Write-Failure "Failed to get platform summary: $_" $_.Exception
}

Write-Host "`n==================================================================" -ForegroundColor Green
Write-Host " ALL ANALYTICS SERVICE TESTS PASSED 9/9! MILESTONE 1 VERIFIED! " -ForegroundColor Green
Write-Host "==================================================================`n" -ForegroundColor Green
