# ==========================================
# Monteastur Envios — Docker Compose Launcher
# ==========================================
# Uso: .\start-all.ps1 [-NoBrowser] [-NoBuild]
# ==========================================

param(
    [switch]$NoBrowser,
    [switch]$NoBuild
)

$NginxUrl  = "http://localhost:8090"
$AppUrl    = "http://localhost:8080"
$HealthUrl = "http://localhost:8080/actuator/health"

Write-Host ">> Forzando apagado previo de contenedores..." -ForegroundColor Cyan
docker compose down --timeout 5 2>&1 | Out-Null

if (-not $NoBuild) {
    Write-Host ">> Construyendo imágenes Docker..." -ForegroundColor Cyan
    docker compose build --quiet 2>&1 | Out-Null
}

Write-Host ">> Levantando servicios..." -ForegroundColor Cyan
docker compose up -d

Write-Host ">> Esperando a que MySQL esté saludable..." -ForegroundColor Yellow
$maxRetries = 30
$retryCount = 0
$healthy = $false
while (-not $healthy -and $retryCount -lt $maxRetries) {
    $status = docker inspect --format="{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}" monteastur-mysql 2>&1
    if ($status -match "healthy") {
        $healthy = $true
    } else {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "." -NoNewline
    }
}
Write-Host ""
if ($healthy) { Write-Host ">> MySQL lista" -ForegroundColor Green }

Write-Host ">> Esperando a que la App esté saludable..." -ForegroundColor Yellow
$retryCount = 0
$healthy = $false
while (-not $healthy -and $retryCount -lt 40) {
    try {
        $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 3
        if ($response.Content -match '"status":"UP"') {
            $healthy = $true
        }
    } catch {}
    if (-not $healthy) {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "." -NoNewline
    }
}
Write-Host ""
if ($healthy) { Write-Host ">> App lista en $AppUrl (Nginx: $NginxUrl)" -ForegroundColor Green }

if (-not $NoBrowser) {
    Write-Host ">> Abriendo navegador..." -ForegroundColor Cyan
    Start-Process $NginxUrl
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  MONTEASTUR ENVIOS — Sistema operativo   ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  Web:       $NginxUrl  ║" -ForegroundColor Green
Write-Host "║  App:       $AppUrl  ║" -ForegroundColor Green
Write-Host "║  Health:    $HealthUrl ║" -ForegroundColor Green
Write-Host "║  Grafana:   http://localhost:3001        ║" -ForegroundColor Green
Write-Host "║  Uptime:    http://localhost:3002        ║" -ForegroundColor Green
Write-Host "║  Prometeus: http://localhost:9090        ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════╝" -ForegroundColor Green
Write-Host ""
Write-Host ">> docker compose logs -f para ver logs en tiempo real" -ForegroundColor Gray