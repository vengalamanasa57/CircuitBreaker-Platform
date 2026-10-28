# End-to-end check of the running platform. Takes ~1 minute (includes a full circuit-breaker cycle).
#   .\scripts\smoke-test.ps1            full test
#   .\scripts\smoke-test.ps1 -SkipChaos skip the ~50 s chaos cycle
param([string]$Gateway = "http://localhost:8080", [switch]$SkipChaos)
$script:failed = 0

function Check($name, [scriptblock]$test) {
    try {
        if (& $test) { Write-Host "  PASS  $name" -ForegroundColor Green }
        else { Write-Host "  FAIL  $name" -ForegroundColor Red; $script:failed++ }
    } catch {
        Write-Host "  FAIL  $name  ($($_.Exception.Message))" -ForegroundColor Red; $script:failed++
    }
}

# Returns the HTTP status code, including for 4xx/5xx responses.
function StatusOf($method, $url, $body) {
    try {
        $p = @{ Method = $method; Uri = $url; UseBasicParsing = $true }
        if ($body) { $p.Body = $body; $p.ContentType = "application/json" }
        return [int](Invoke-WebRequest @p).StatusCode
    } catch {
        if ($_.Exception.Response) { return [int]$_.Exception.Response.StatusCode }
        return 0
    }
}

function BreakerState {
    $s = Invoke-RestMethod "$Gateway/resilience/status"
    ($s.circuitBreakers | Where-Object { $_.name -like "*recommendation*" }).state
}

function Chaos($mode) {
    Invoke-RestMethod -Method Post -Uri "$Gateway/admin/recommendation/chaos" -ContentType "application/json" -Body "{`"mode`":`"$mode`"}" | Out-Null
}

Write-Host "`n[1] Routing through the gateway" -ForegroundColor Cyan
Check "GET /  describes the gateway"          { (Invoke-RestMethod "$Gateway/").service -eq "api-gateway" }
# Counts "id" fields in the raw text, so it does not depend on how PowerShell unrolls JSON arrays (differs between 5.1 and 7)
Check "GET /products returns 8 products"      { $raw = (Invoke-WebRequest "$Gateway/products" -UseBasicParsing).Content; ([regex]::Matches($raw, '"id"\s*:')).Count -ge 8 }
Check "GET /products/1 is the Laptop"         { (Invoke-RestMethod "$Gateway/products/1").name -eq "Laptop" }
Check "GET /inventory/1 is in stock"          { (Invoke-RestMethod "$Gateway/inventory/1").inStock -eq $true }
Check "GET /inventory/8 is out of stock"      { (Invoke-RestMethod "$Gateway/inventory/8").inStock -eq $false }
Check "GET /recommendations is live data"     { $r = Invoke-RestMethod "$Gateway/recommendations"; ($r.degraded -eq $false) -and ($r.source -eq "recommendation-service") }

Write-Host "`n[2] Business rules and validation" -ForegroundColor Cyan
Check "POST /products creates (201)"          { (StatusOf Post "$Gateway/products" '{"name":"Smoke Webcam","price":59.9,"category":"Accessories"}') -eq 201 }
Check "POST invalid product is rejected (400)" { (StatusOf Post "$Gateway/products" '{"name":"","price":-1,"category":""}') -eq 400 }
Check "GET unknown product is 404"            { (StatusOf Get "$Gateway/products/99999" $null) -eq 404 }
Check "Reserve stock decrements quantity"     {
    $before = (Invoke-RestMethod "$Gateway/inventory/2").quantity
    $after  = (Invoke-RestMethod -Method Post -Uri "$Gateway/inventory/2/reserve?quantity=1").quantity
    $after -eq ($before - 1)
}
Check "Over-reserving is a conflict (409)"    { (StatusOf Post "$Gateway/inventory/5/reserve?quantity=99999" $null) -eq 409 }

Write-Host "`n[3] Gateway status feed" -ForegroundColor Cyan
Check "3 services reported UP"                { @((Invoke-RestMethod "$Gateway/resilience/status").services | Where-Object { $_.status -eq "UP" }).Count -eq 3 }
Check "3 circuit breakers, all CLOSED"        { $b = (Invoke-RestMethod "$Gateway/resilience/status").circuitBreakers; ($b.Count -eq 3) -and (@($b | Where-Object { $_.state -ne "CLOSED" }).Count -eq 0) }
Check "Exactly 2 bulkheads reported"          { @((Invoke-RestMethod "$Gateway/resilience/status").bulkheads).Count -eq 2 }

Write-Host "`n[4] Rate limiter" -ForegroundColor Cyan
Check "Burst of 80 requests gets some 429s"   {
    $codes = 1..80 | ForEach-Object { StatusOf Get "$Gateway/recommendations" $null }
    @($codes | Where-Object { $_ -eq 429 }).Count -gt 0
}
Start-Sleep -Seconds 3   # let the token bucket refill

if (-not $SkipChaos) {
    Write-Host "`n[5] Circuit breaker lifecycle (about 50 seconds)" -ForegroundColor Cyan
    Chaos "LATENCY"
    $opened = $false; $sawFallback = $false
    for ($i = 0; $i -lt 8 -and -not $opened; $i++) {
        $resp = Invoke-WebRequest "$Gateway/recommendations" -UseBasicParsing
        if ($resp.Headers["X-Fallback"] -eq "true") { $sawFallback = $true }
        $opened = (BreakerState) -eq "OPEN"
    }
    Check "Slow service serves fallback data"   { $sawFallback }
    Check "Breaker opens after repeated timeouts" { $opened }
    Check "While OPEN, fallback is fast (<500 ms)" {
        $t = Measure-Command { Invoke-WebRequest "$Gateway/recommendations" -UseBasicParsing | Out-Null }
        $t.TotalMilliseconds -lt 500
    }
    Check "Product service unaffected while OPEN" { (StatusOf Get "$Gateway/products" $null) -eq 200 }

    Chaos "NONE"
    Write-Host "  ...healed; waiting up to 40 s for HALF_OPEN -> CLOSED"
    $closed = $false
    for ($i = 0; $i -lt 40 -and -not $closed; $i++) {
        Start-Sleep -Seconds 1
        Invoke-WebRequest "$Gateway/recommendations" -UseBasicParsing | Out-Null
        $closed = (BreakerState) -eq "CLOSED"
    }
    Check "Breaker recovers to CLOSED by itself" { $closed }
    Check "Timeline recorded the full cycle"    {
        $e = (Invoke-RestMethod "$Gateway/resilience/status").events
        (@($e | Where-Object { $_.to -eq "OPEN" }).Count -ge 1) -and (@($e | Where-Object { $_.to -eq "HALF_OPEN" }).Count -ge 1) -and (@($e | Where-Object { $_.to -eq "CLOSED" }).Count -ge 1)
    }
}

Write-Host ""
if ($script:failed -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green; exit 0 }
Write-Host "$($script:failed) CHECK(S) FAILED" -ForegroundColor Red; exit 1
