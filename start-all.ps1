<#
.SYNOPSIS
    Levanta todo el entorno: Docker (MySQL + servicios), Backend Spring Boot, Frontend React.

.DESCRIPTION
    1. Docker Compose: down + up -d (sincroniza .env)
    2. Espera inteligente: MySQL aceptando conexiones
    3. Backend: mvn spring-boot:run en proceso secundario
    4. Frontend: npm run dev (Vite) en proceso secundario
    5. Abre URLs en el navegador

.USAGE
    .\start-all.ps1
    .\start-all.ps1 -SkipDocker
    .\start-all.ps1 -SkipFrontend
#>

param(
    [switch]$SkipDocker,
    [switch]$SkipFrontend,
    [switch]$SkipBrowser
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

# =========================
# Helpers
# =========================

function Write-Step($msg) { Write-Host "`n>> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "   OK: $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "   WARN: $msg" -ForegroundColor Yellow }
function Write-Fail($msg) { Write-Host "   FAIL: $msg" -ForegroundColor Red }

function Test-Port($host, $port, $timeoutMs = 2000) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $result = $tcp.BeginConnect($host, $port, $null, $null)
        $success = $result.AsyncWaitHandle.WaitOne($timeoutMs)
        if ($success) { $tcp.EndConnect($result) }
        $tcp.Close()
        return $success
    } catch { return $false }
}

# =========================
# 1. Docker Compose
# =========================

if (-not $SkipDocker) {
    Write-Step "Docker Compose: reiniciando servicios..."

    Push-Location $Root
    docker compose down 2>&1 | Out-Null
    docker compose up -d 2>&1 | ForEach-Object { Write-Host "   $_" }
    Pop-Location

    Write-Ok "Contenedores levantados."
}

# =========================
# 2. Espera MySQL (monteastur-mysql)
# =========================

Write-Step "Esperando que MySQL acepte conexiones..."

$maxWait = 60
$elapsed = 0
$interval = 2

while ($elapsed -lt $maxWait) {
    $ready = docker exec monteastur-mysql mysqladmin ping -h localhost --silent 2>$null
    if ($ready -match "alive") {
        Write-Ok "MySQL listo en ${elapsed}s."
        break
    }
    Start-Sleep -Seconds $interval
    $elapsed += $interval
    Write-Host "   esperando... (${elapsed}s)" -ForegroundColor DarkGray
}

if ($elapsed -ge $maxWait) {
    Write-Fail "MySQL no respondio en ${maxWait}s. Verifica: docker logs monteastur-mysql"
    exit 1
}

# =========================
# 3. Backend Spring Boot
# =========================

Write-Step "Iniciando Backend (Spring Boot)..."

$backendJob = Start-Job -ScriptBlock {
    param($dir)
    Set-Location -LiteralPath $dir
    & mvn spring-boot:run -q 2>&1
} -ArgumentList $Root

Write-Ok "Backend en background (Job ID: $($backendJob.Id))"

# =========================
# 4. Frontend React (Vite)
# =========================

$frontendPort = 5173

if (-not $SkipFrontend) {
    $frontendDir = Join-Path $Root "frontend-react"
    if (Test-Path (Join-Path $frontendDir "package.json")) {
        Write-Step "Iniciando Frontend (Vite)..."

        $nodeModules = Join-Path $frontendDir "node_modules"
        if (-not (Test-Path $nodeModules)) {
            Write-Host "   Instalando dependencias..." -ForegroundColor DarkGray
            Push-Location $frontendDir
            npm install 2>&1 | Out-Null
            Pop-Location
        }

        $frontendJob = Start-Job -ScriptBlock {
            param($dir)
            Set-Location -LiteralPath $dir
            & npm run dev 2>&1
        } -ArgumentList $frontendDir

        Write-Ok "Frontend en background (Job ID: $($frontendJob.Id))"
    } else {
        Write-Warn "No se encontro frontend-react/package.json. Saltando frontend."
    }
}

# =========================
# 5. Espera backend y abre navegador
# =========================

if (-not $SkipBrowser) {
    Write-Step "Esperando que el backend este disponible..."

    $backendReady = $false
    for ($i = 0; $i -lt 30; $i++) {
        if (Test-Port -host "localhost" -port 8080) {
            $backendReady = $true
            break
        }
        Start-Sleep -Seconds 2
    }

    if ($backendReady) {
        Write-Ok "Backend listo en http://localhost:8080"
        Start-Process "http://localhost:8080/actuator/health"

        if (-not $SkipFrontend -and (Test-Path (Join-Path $Root "frontend-react\package.json"))) {
            Start-Sleep -Seconds 3
            Start-Process "http://localhost:$frontendPort"
        }
    } else {
        Write-Warn "Backend no respondio en 60s. Abre manualmente: http://localhost:8080"
    }
}

# =========================
# Resumen
# =========================

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host " Entorno listo!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host " Backend:    http://localhost:8080"
Write-Host " Actuator:   http://localhost:8080/actuator/health"
Write-Host " Prometheus: http://localhost:8080/actuator/prometheus"
Write-Host " Frontend:   http://localhost:$frontendPort"
Write-Host " MySQL:      localhost:3307 (root / root)"
Write-Host ""
Write-Host " Para detener todo:" -ForegroundColor DarkGray
Write-Host "   docker compose down" -ForegroundColor DarkGray
Write-Host "   Get-Job | Stop-Job; Get-Job | Remove-Job" -ForegroundColor DarkGray
Write-Host "========================================`n" -ForegroundColor Cyan
