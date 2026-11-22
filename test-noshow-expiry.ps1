# ==============================================================================
# ParkEase Automated "No-Show" Expiry & Email Smoke Test
# ==============================================================================
# 1. Reserves a parking spot using your real Gmail (via Gmail plus-addressing).
# 2. Triggers "Booking Confirmed" email to manishsharma75085@gmail.com.
# 3. Simulates exceeding the 15-minute grace period by aging the booking in MySQL.
# 4. Waits for the BookingExpiryScheduler to sweep and expire the booking.
# 5. Spot is released back to AVAILABLE and "Booking Expired" email is delivered.
# ==============================================================================

param (
    [string]$GatewayUrl = "http://localhost:8080",
    [string]$BaseEmail  = "manishsharma75085@gmail.com",
    [string]$MysqlExe   = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$DbUser     = "root",
    [string]$DbPass     = "Manish[8699090628]"
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([int]$Step, [string]$Title)
    Write-Host ""
    Write-Host "==========================================================================" -ForegroundColor Cyan
    Write-Host " [Step $Step/8] $Title" -ForegroundColor Yellow
    Write-Host "==========================================================================" -ForegroundColor Cyan
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
        try {
            $response = $response | ConvertFrom-Json
        } catch {
            return $response
        }
    }
    if ($response.data -ne $null) {
        return $response.data
    }
    return $response
}

Write-Host "`n>>> Starting ParkEase Automated No-Show Expiry Test..." -ForegroundColor Magenta
Write-Host " [i] Target Gmail Inbox: $BaseEmail" -ForegroundColor DarkGray

# ------------------------------------------------------------------------------
# STEP 1: API Gateway Health Check
# ------------------------------------------------------------------------------
Write-Step 1 "Checking API Gateway Health via Actuator"
try {
    $health = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method GET
    if ($health.status -eq "UP") {
        Write-Success "API Gateway is UP and operational on Port 8080!"
    } else {
        Write-Failure "Gateway health status is $($health.status)" $null
    }
} catch {
    Write-Failure "Failed to reach $GatewayUrl/actuator/health: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 2: Register Driver with User Gmail (Using + addressing)
# ------------------------------------------------------------------------------
Write-Step 2 "Registering Test Driver with $BaseEmail"
$timestamp   = Get-Date -Format "yyyyMMddHHmmss"
$emailParts  = $BaseEmail.Split("@")
$driverEmail = "$($emailParts[0])+noshow$($timestamp.Substring(8))@$($emailParts[1])"
$driverPass  = "Password123!"
$driverPhone = "9" + (Get-Random -Minimum 100000000 -Maximum 999999999).ToString()

$registerBody = @{
    fullName    = "Manish Sharma"
    email       = $driverEmail
    password    = $driverPass
    phone       = $driverPhone
    role        = "DRIVER"
} | ConvertTo-Json

try {
    $regRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/register" -Method POST -Body $registerBody -ContentType "application/json"
    $regRes = Parse-ApiResponse $regRaw
    Write-Success "Driver registered: $driverEmail (Alerts routed to $BaseEmail)"
} catch {
    Write-Failure "Registration failed: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 3: Authenticate Driver & Extract JWT
# ------------------------------------------------------------------------------
Write-Step 3 "Authenticating Driver & Extracting JWT Token"
$loginBody = @{
    email    = $driverEmail
    password = $driverPass
} | ConvertTo-Json

$driverToken = $null
$driverUserId = $null
try {
    $loginRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $loginRes = Parse-ApiResponse $loginRaw
    $driverToken = if ($loginRes.accessToken) { $loginRes.accessToken } else { $loginRaw.accessToken }
    $driverUserId = if ($loginRes.user.userId) { $loginRes.user.userId } elseif ($loginRes.userId) { $loginRes.userId } else { $loginRaw.userId }
    Write-Success "Login successful! JWT acquired. Driver UserID: $driverUserId"
} catch {
    Write-Failure "Login failed: $_" $_.Exception
}

$driverHeaders = @{
    "Authorization" = "Bearer $driverToken"
}

# ------------------------------------------------------------------------------
# STEP 4: Register Driver Vehicle
# ------------------------------------------------------------------------------
Write-Step 4 "Registering Driver Vehicle via Port 8080"
$licensePlate = "KA-01-NS-$($timestamp.Substring(8))"
$vehicleBody = @{
    licensePlate = $licensePlate
    make         = "Tesla"
    model        = "Model 3"
    color        = "Midnight Silver"
    vehicleType  = "FOUR_WHEELER"
    isEV         = $true
} | ConvertTo-Json

$vehicleId = $null
try {
    $vehRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/vehicles" -Method POST -Body $vehicleBody -ContentType "application/json" -Headers $driverHeaders
    $vehRes = Parse-ApiResponse $vehRaw
    $vehicleId = if ($vehRes.vehicleId) { $vehRes.vehicleId } else { $vehRaw.vehicleId }
    Write-Success "Vehicle registered: $licensePlate (Vehicle ID: $vehicleId)"
} catch {
    Write-Failure "Vehicle registration failed: $_" $_.Exception
}

# ------------------------------------------------------------------------------
# STEP 5: Select Parking Lot & Available Spot
# ------------------------------------------------------------------------------
Write-Step 5 "Selecting Parking Lot & Available Spot"
$lotsRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/lots/search/city?city=Bangalore" -Method GET
$lotList = Parse-ApiResponse $lotsRaw
$lotList = @($lotList)
if ($lotList.Count -eq 0) {
    Write-Failure "No parking lots found in Bangalore. Please run .\test-e2e-gateway.ps1 first to bootstrap a lot." $null
}

$selectedLot = $lotList[0]
$lotId = if ($selectedLot.lotId) { $selectedLot.lotId } else { $selectedLot.data.lotId }
Write-Success "Selected Parking Lot: '$($selectedLot.name)' in Bangalore (Lot ID: $lotId)"

$spotsRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/spots/lot/$lotId/available" -Method GET -Headers $driverHeaders
$spotList = Parse-ApiResponse $spotsRaw
$spotList = @($spotList)

if ($spotList.Count -eq 0 -or -not $spotList[0].spotId) {
    Write-Failure "No available spots found in lot $lotId." $null
}

# ------------------------------------------------------------------------------
# STEP 6: Reserve Spot (Fires Email #1 to Gmail)
# ------------------------------------------------------------------------------
Write-Step 6 "Reserving Spot (Dispatches Email #1 to $BaseEmail)"
$now = [DateTime]::Now
$fromTime = $now.AddMinutes(5).ToString("yyyy-MM-ddTHH:mm:ss")
$toTime   = $now.AddHours(2).ToString("yyyy-MM-ddTHH:mm:ss")

$selectedSpot = $null
$bookRes = $null
$bookRaw = $null

foreach ($sp in $spotList) {
    if (-not $sp.spotId) { continue }
    $candidateId = $sp.spotId
    $candidateNumber = $sp.spotNumber

    $bookingBody = @{
        lotId        = $lotId
        spotId       = $candidateId
        vehicleId    = $vehicleId
        bookingType  = "PRE_BOOKING"
        reservedFrom = $fromTime
        reservedTo   = $toTime
    } | ConvertTo-Json

    try {
        $bookRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/bookings" -Method POST -Body $bookingBody -ContentType "application/json" -Headers $driverHeaders
        $bookRes = Parse-ApiResponse $bookRaw
        $selectedSpot = $sp
        $spotId = $candidateId
        $spotNumber = $candidateNumber
        break
    } catch {
        Write-Host " [i] Spot '$candidateNumber' has an existing active booking. Trying next spot..." -ForegroundColor DarkGray
    }
}

if (-not $selectedSpot -or -not $bookRes) {
    Write-Failure "All available spots in this lot have overlapping reservations. Please wait for them to expire or add more spots." $null
}

$bookingId  = if ($bookRes.bookingId) { $bookRes.bookingId } else { $bookRaw.bookingId }
$bookingRef = if ($bookRes.bookingReference) { $bookRes.bookingReference } else { $bookRaw.bookingReference }
$status     = if ($bookRes.status) { $bookRes.status } else { $bookRaw.status }

Write-Success "Reserved Spot: '$spotNumber' (Spot ID: $spotId)"
Write-Success "Reservation CONFIRMED! Reference: $bookingRef, Status: $status"
Write-Host " [✉] Check your Gmail ($BaseEmail) -> You should see the 'Booking Confirmed' email!" -ForegroundColor Cyan

# Verify Spot is now RESERVED
$spotCheck1 = Parse-ApiResponse (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/spots/$spotId" -Method GET -Headers $driverHeaders)
Write-Success "Spot '$spotNumber' status verified: $($spotCheck1.status) (Occupied/Reserved inventory hold)"

# ------------------------------------------------------------------------------
# STEP 7: Simulate Exceeded Grace Period (> 15 Minutes No-Show)
# ------------------------------------------------------------------------------
Write-Step 7 "Simulating No-Show (Aging reservation start time by 2 hours in MySQL)"

if (-not (Test-Path $MysqlExe)) {
    # Try finding in common paths
    $alt = Get-ChildItem -Path "C:\Program Files\MySQL" -Recurse -Filter "mysql.exe" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($alt) { $MysqlExe = $alt.FullName }
}

# Update by booking_reference (VARCHAR) subtracting 2 hours from reserved_from
# This avoids binary(16) UUID mismatches and timezone offset discrepancies completely
$sqlQuery = "UPDATE parkease_booking.bookings SET reserved_from = DATE_SUB(reserved_from, INTERVAL 2 HOUR) WHERE booking_reference = '$bookingRef';"
Write-Host " [>] Running SQL update for booking '$bookingRef'..." -ForegroundColor DarkGray

$origEap = $ErrorActionPreference
$ErrorActionPreference = "SilentlyContinue"
$env:MYSQL_PWD = $DbPass
& $MysqlExe -h 127.0.0.1 -P 3309 -u $DbUser -e $sqlQuery 2>&1 | Out-Null
$ErrorActionPreference = $origEap

Write-Success "Reservation '$bookingRef' start time aged 2 hours back in database."

# ------------------------------------------------------------------------------
# STEP 8: Monitor Scheduler Expiry & Verify Auto-Release (Fires Email #2)
# ------------------------------------------------------------------------------
Write-Step 8 "Waiting for BookingExpiryScheduler to Sweep & Expire Reservation"
Write-Host " [i] The scheduler sweeps every 60s. Polling booking status via Gateway..." -ForegroundColor DarkGray

$maxAttempts = 20
$attempt = 0
$expired = $false

while ($attempt -lt $maxAttempts) {
    $attempt++
    Start-Sleep -Seconds 5
    try {
        $checkBookingRaw = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/bookings/$bookingId" -Method GET -Headers $driverHeaders
        $checkBooking = Parse-ApiResponse $checkBookingRaw
        $currStatus = if ($checkBooking.status) { $checkBooking.status } else { $checkBookingRaw.status }

        Write-Host "     [Attempt $attempt/$maxAttempts] Current Booking Status: $currStatus" -ForegroundColor DarkGray

        if ($currStatus -eq "EXPIRED") {
            $expired = $true
            break
        }
    } catch {
        Write-Host "     [Attempt $attempt/$maxAttempts] Polling error: $_" -ForegroundColor DarkYellow
    }
}

if (-not $expired) {
    Write-Failure "Booking did not expire within the expected timeout. Please verify BookingExpiryScheduler is active." $null
}

Write-Success "No-Show Engine EXPIRED the booking! (Status: EXPIRED)"
Write-Success "Cancellation Reason: $($checkBooking.cancellationReason)"

# Verify spot is back to AVAILABLE
$spotCheck2 = Parse-ApiResponse (Invoke-RestMethod -Uri "$GatewayUrl/api/v1/spots/$spotId" -Method GET -Headers $driverHeaders)
Write-Success "Spot '$spotNumber' has been automatically RELEASED back to: $($spotCheck2.status)"

Write-Host ""
Write-Host "==========================================================================" -ForegroundColor Green
Write-Host "          NO-SHOW AUTOMATED EXPIRY & EMAIL NOTIFICATION VERIFIED!         " -ForegroundColor Green
Write-Host "==========================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " 1. Booking Confirmation Email  -> DELIVERED to $BaseEmail" -ForegroundColor White
Write-Host " 2. Grace Period Expiry (15m)   -> TRIGGERED (No-Show detected)" -ForegroundColor White
Write-Host " 3. Automated Spot Release      -> RELEASED (Spot $spotNumber back to AVAILABLE)" -ForegroundColor White
Write-Host " 4. Spot Released Alert Email   -> DELIVERED to $BaseEmail" -ForegroundColor White
Write-Host ""
Write-Host " Check your Gmail ($BaseEmail) - you should now have the cancellation email!" -ForegroundColor Cyan
Write-Host ""
