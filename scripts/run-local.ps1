# Runs the whole platform on Windows WITHOUT Docker (needs JDK 17+ and Node 18+).
#   .\scripts\run-local.ps1          start everything (stops any previous run first)
#   .\scripts\run-local.ps1 stop     stop everything
# NOTE: it frees ports 8761, 8080-8083 and 5173 - anything listening there is stopped.
param([string]$Action = "start")
Set-Location (Join-Path $PSScriptRoot "..")
New-Item -ItemType Directory -Force logs | Out-Null
$Ports = 8761, 8080, 8081, 8082, 8083, 5173

function Stop-All {
    if (Test-Path logs\pids) {
        Get-Content logs\pids | Where-Object { $_ -match '^\d+$' } |
            ForEach-Object { Stop-Process -Id ([int]$_) -Force -ErrorAction SilentlyContinue }
        Remove-Item logs\pids -ErrorAction SilentlyContinue
    }
    # Catch strays (e.g. the dashboard's node child process, or a run started by hand)
    foreach ($port in $Ports) {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue }
    }
    Start-Sleep -Seconds 2   # let Windows release the jar file locks
}

if ($Action -eq "stop") { Stop-All; Write-Host "Stopped."; exit 0 }

function Wait-For($name, $url) {
    Write-Host -NoNewline "Waiting for $name "
    for ($i = 0; $i -lt 90; $i++) {
        try { Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host " up"; return }
        catch { Write-Host -NoNewline "."; Start-Sleep 2 }
    }
    Write-Host " TIMED OUT - see logs\$name.log and logs\$name.err"; Stop-All; exit 1
}

function Start-Module($module) {
    $p = Start-Process java -ArgumentList "-jar", "$module\target\$module-1.0.0.jar" `
        -RedirectStandardOutput "logs\$module.log" -RedirectStandardError "logs\$module.err" -PassThru -WindowStyle Hidden
    Add-Content logs\pids $p.Id
}

Write-Host "Stopping any previous run..."
Stop-All

Write-Host "Building (first run downloads dependencies and takes a few minutes)..."
.\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { Write-Host "Build failed - see the error above."; exit 1 }

if (-not $env:TRACING_ENABLED) { $env:TRACING_ENABLED = "false" }   # no Zipkin locally

Start-Module eureka-server
Wait-For eureka-server http://localhost:8761/actuator/health
foreach ($m in "product-service","inventory-service","recommendation-service","gateway-service") { Start-Module $m }
Wait-For gateway-service http://localhost:8080/actuator/health

Write-Host -NoNewline "Waiting for all services to register "
for ($i = 0; $i -lt 60; $i++) {
    try {
        $r = Invoke-WebRequest http://localhost:8080/resilience/status -UseBasicParsing -TimeoutSec 3
        if (([regex]::Matches($r.Content, '"status":"UP"')).Count -ge 3) { break }
    } catch { }
    Write-Host -NoNewline "."; Start-Sleep 2
}
Write-Host " ok"
Write-Host "Warming up the gateway (first requests are slow while the JVM and load balancer initialise)..."
foreach ($round in 1..6) {
    foreach ($route in "products", "inventory", "recommendations") {
        try { Invoke-WebRequest "http://localhost:8080/$route" -UseBasicParsing -TimeoutSec 15 | Out-Null } catch { }
    }
}

Push-Location dashboard
if (-not (Test-Path node_modules)) { npm ci --no-audit --no-fund }
$d = Start-Process npm.cmd -ArgumentList "run","dev" -RedirectStandardOutput "..\logs\dashboard.log" -RedirectStandardError "..\logs\dashboard.err" -PassThru -WindowStyle Hidden
Add-Content ..\logs\pids $d.Id
Pop-Location

Write-Host ""
Write-Host "Everything is up and warmed. Open:"
Write-Host "  Dashboard : http://localhost:5173"
Write-Host "  Gateway   : http://localhost:8080"
Write-Host "  Eureka    : http://localhost:8761"
Write-Host "Verify everything with:  .\scripts\smoke-test.ps1"
Write-Host "Stop with              :  .\scripts\run-local.ps1 stop"
