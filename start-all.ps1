param(
    [switch]$NoBrowser,
    [switch]$NoBuild
)

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  MONTEASTUR ENVIOS - ORQUESTADOR DOCKER" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Apagar contenedores previos
Write-Host ">> Deteniendo contenedores anteriores..." -ForegroundColor Yellow
docker compose down

# 2. Build (opcional)
if (-not $NoBuild) {
    Write-Host ">> Construyendo contenedores (multi-stage)..." -ForegroundColor Yellow
    docker compose build
    if ($LASTEXITCODE -ne 0) {
        Write-Host ">> Error en el build de Docker." -ForegroundColor Red
        exit 1
    }
}

# 3. Levantar servicios
Write-Host ">> Arrancando servicios con Docker Compose..." -ForegroundColor Yellow
docker compose up -d

# 4. Esperar a que MySQL esté healthy
Write-Host ">> Esperando a que MySQL esté listo..." -ForegroundColor Cyan
$maxRetries = 30
$retryCount = 0
$dbReady = $false

while (-not $dbReady -and $retryCount -lt $maxRetries) {
    $status = docker inspect --format="{{json .State.Health.Status}}" monteastur-mysql 2>$null
    if ($status -eq '"healthy"') {
        $dbReady = $true
        Write-Host ">> ¡MySQL está operativo!" -ForegroundColor Green
    } else {
        Start-Sleep -Seconds 2
        $retryCount++
        Write-Host "   Esperando base de datos... ($retryCount/$maxRetries)" -ForegroundColor Gray
    }
}

if (-not $dbReady) {
    Write-Host ">> Advertencia: MySQL tardó demasiado en responder, continuando de todos modos..." -ForegroundColor Yellow
}

# 5. Esperar a que la App devuelva UP en el healthcheck
Write-Host ">> Esperando a que Spring Boot esté listo (/actuator/health)..." -ForegroundColor Cyan
$appReady = $false
$retryCount = 0
$maxAppRetries = 40

while (-not $appReady -and $retryCount -lt $maxAppRetries) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop
        if ($response.status -eq "UP") {
            $appReady = $true
            Write-Host ">> ¡Spring Boot está UP y funcionando!" -ForegroundColor Green
        }
    } catch {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "   Esperando aplicación web... ($retryCount/$maxAppRetries)" -ForegroundColor Gray
    }
}

# 6. Abrir navegador en Nginx (:8090)
if (-not $NoBrowser) {
    Write-Host ">> Abriendo plataforma en el navegador (Nginx :8090)..." -ForegroundColor Cyan
    Start-Process "http://localhost:8090"
}

Write-Host "==========================================" -ForegroundColor Green
Write-Host "  ¡SISTEMA LEVANTADO CON ÉXITO!" -ForegroundColor Green
Write-Host "  - Nginx (Web): http://localhost:8090" -ForegroundColor White
Write-Host "  - Uptime Kuma: http://localhost:3002" -ForegroundColor White
Write-Host "  - Grafana:     http://localhost:3001" -ForegroundColor White
Write-Host "  Usa 'docker compose logs -f' para ver logs en tiempo real." -ForegroundColor Gray
Write-Host "==========================================" -ForegroundColor Green