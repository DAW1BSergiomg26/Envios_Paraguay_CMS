# ==============================================================================
# smoke-test.ps1 — Smoke test de producción sobre el JAR empaquetado.
# Levanta docker compose (db+redis+app+nginx), espera /actuator/health y
# verifica: salud, cabeceras de seguridad, SPA, login y prometheus.
#
# Uso: .\scripts\smoke-test.ps1            (usa .env + docker compose)
#      .\scripts\smoke-test.ps1 -NoBuild    (sin rebuild de la imagen)
# Exit: 0 = todo PASS | 1 = algún FAIL
# ==============================================================================

param(
    [switch]$NoBuild,
    [int]$TimeoutSec = 120
)

$ErrorActionPreference = "Stop"
$PORT = 8080
if (Test-Path -LiteralPath ".env") {
    $line = Get-Content -LiteralPath ".env" | Where-Object { $_ -match "^\s*PORT\s*=" } | Select-Object -First 1
    if ($line) { $PORT = [int]((($line -split "=", 2)[1]).Trim().Trim('"', "'")) }
}
$baseUrl = "http://localhost:$PORT"

$failed = $false
function Check-Smoke([string]$name, [scriptblock]$script) {
    try {
        & $script
        Write-Host "  [PASS] $name" -ForegroundColor Green
    } catch {
        Write-Host "  [FAIL] $name -> $($_.Exception.Message)" -ForegroundColor Red
        $script:failed = $true
    }
}

Write-Host "[1/6] Levantando stack con docker compose..."
if ($NoBuild) { docker compose up -d } else { docker compose up -d --build }
if ($LASTEXITCODE -ne 0) { throw "docker compose up falló" }

Write-Host "[2/6] Esperando /actuator/health (timeout ${TimeoutSec}s)..."
$healthy = $false
for ($i = 0; $i -lt $TimeoutSec; $i++) {
    try {
        $r = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 2
        if ($r.status -eq "UP") { $healthy = $true; break }
    } catch { }
    Start-Sleep -Seconds 1
}
if (-not $healthy) { Write-Host "[FAIL] health timeout"; exit 1 }

Check-Smoke "Health endpoint devuelve UP" {
    $r = Invoke-RestMethod -Uri "$baseUrl/actuator/health" -TimeoutSec 5
    if ($r.status -ne "UP") { throw "status=$($r.status)" }
}

Check-Smoke "Cabeceras de seguridad en la home" {
    $resp = Invoke-WebRequest -Uri "$baseUrl/" -UseBasicParsing -TimeoutSec 10
    if ($resp.StatusCode -ne 200) { throw "HTTP $($resp.StatusCode)" }
    foreach ($h in @("X-Frame-Options", "X-Content-Type-Options", "Content-Security-Policy")) {
        if (-not $resp.Headers.ContainsKey($h)) { throw "falta cabecera $h" }
    }
    if ($resp.Headers["Content-Security-Policy"] -notmatch "frame-ancestors 'none'") { throw "CSP sin frame-ancestors 'none'" }
}

Check-Smoke "SPA /react-dashboard/ servida" {
    $resp = Invoke-WebRequest -Uri "$baseUrl/react-dashboard/" -UseBasicParsing -TimeoutSec 10
    if ($resp.StatusCode -ne 200) { throw "HTTP $($resp.StatusCode)" }
    if ($resp.Content -notmatch "react-dashboard/assets/index-") { throw "no referencia assets del bundle" }
}

Check-Smoke "Login admin responde 200" {
    $resp = Invoke-WebRequest -Uri "$baseUrl/login" -UseBasicParsing -TimeoutSec 10
    if ($resp.StatusCode -ne 200) { throw "HTTP $($resp.StatusCode)" }
}

Check-Smoke "Prometheus /actuator/prometheus responde" {
    $resp = Invoke-WebRequest -Uri "$baseUrl/actuator/prometheus" -UseBasicParsing -TimeoutSec 10
    if ($resp.StatusCode -ne 200) { throw "HTTP $($resp.StatusCode)" }
}

if ($failed) {
    Write-Host "`nRESULTADO: FALLOS EN SMOKE TEST" -ForegroundColor Red
    exit 1
}
Write-Host "`nRESULTADO: SMOKE TEST COMPLETO (6/6 PASS)" -ForegroundColor Green
exit 0
