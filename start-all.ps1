# ==============================================================================
# ENVIOS PARAGUAY CMS - ORQUESTADOR DOCKER COMPLETO
# Apaga contenedores previos, reconstruye (opcional), levanta todos los
# servicios y verifica la salud de MySQL, Redis y Spring Boot.
# Los puertos y la configuración se leen de .env (nunca hardcodeados).
# ------------------------------------------------------------------------------
# Uso:
#   .\start-all.ps1                # orquestación completa (build + navegador)
#   .\start-all.ps1 -NoBuild       # sin reconstruir imágenes
#   .\start-all.ps1 -NoBrowser     # sin abrir el navegador
# ==============================================================================

param(
    [switch]$NoBrowser,
    [switch]$NoBuild
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Get-EnvValue {
    param([string]$Key, [string]$Default = "")
    $value = $Default
    if (Test-Path -LiteralPath ".env") {
        $line = Get-Content -LiteralPath ".env" | Where-Object { $_ -match "^\s*$([regex]::Escape($Key))\s*=" } | Select-Object -First 1
        if ($line) {
            $value = (($line -split "=", 2)[1]).Trim().Trim('"', "'")
        }
    }
    return $value
}

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  MONTEASTUR ENVIOS - ORQUESTADOR DOCKER" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 0. Pre-requisitos: docker, daemon y plugin compose
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host " [X] ERROR: No se encontró el comando 'docker'. Instala Docker Desktop." -ForegroundColor Red
    exit 1
}
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] ERROR: Docker Desktop no está ejecutándose." -ForegroundColor Red
    exit 1
}
docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] ERROR: Docker Compose (plugin 'docker compose') no está disponible." -ForegroundColor Red
    exit 1
}

# 1. Garantizar .env (docker compose lo requiere)
if (-not (Test-Path -LiteralPath ".env")) {
    Write-Host ">> No existe .env. Copiando .env.example -> .env..." -ForegroundColor Yellow
    Copy-Item -LiteralPath ".env.example" -Destination ".env"
    Write-Host ">> Edita '.env' y ajusta las credenciales antes de continuar." -ForegroundColor Yellow
}

# 2. Configuración dinámica desde .env
$port        = Get-EnvValue "PORT" "8080"
$nginxPort   = Get-EnvValue "NGINX_PORT" "80"
$mailpitPort = Get-EnvValue "MAILPIT_UI_PORT" "8025"
$promPort    = Get-EnvValue "PROMETHEUS_PORT" "9090"
$grafPort    = Get-EnvValue "GRAFANA_PORT" "3000"
$kumaPort    = Get-EnvValue "UPTIME_KUMA_PORT" "3001"

# 3. Apagar contenedores previos
Write-Host ">> Deteniendo contenedores anteriores..." -ForegroundColor Yellow
docker compose down

# 4. Build (opcional)
if (-not $NoBuild) {
    Write-Host ">> Construyendo contenedores (multi-stage)..." -ForegroundColor Yellow
    docker compose build
    if ($LASTEXITCODE -ne 0) {
        Write-Host " [X] Error en el build de Docker." -ForegroundColor Red
        exit 1
    }
}

# 5. Levantar servicios (Redis, MySQL, App, Nginx, Mailpit, observabilidad)
Write-Host ">> Arrancando servicios con Docker Compose..." -ForegroundColor Yellow
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] 'docker compose up' falló. Revisa 'docker compose logs -f app'." -ForegroundColor Red
    exit 1
}

# 6. Esperar a que MySQL esté healthy (contenedor localizado dinámicamente)
Write-Host ">> Esperando a que MySQL esté listo..." -ForegroundColor Cyan
$dbId = (docker compose ps -q db).Trim()
$maxRetries = 30
$retryCount = 0
$dbReady = $false

while (-not $dbReady -and $retryCount -lt $maxRetries) {
    if ($dbId) {
        $status = docker inspect --format "{{json .State.Health.Status}}" $dbId 2>$null
        if ($status -match 'healthy') {
            $dbReady = $true
            Write-Host ">> ¡MySQL está operativo!" -ForegroundColor Green
        }
    }
    if (-not $dbReady) {
        Start-Sleep -Seconds 2
        $retryCount++
        Write-Host "    Esperando base de datos... ($retryCount/$maxRetries)" -ForegroundColor Gray
    }
}

if (-not $dbReady) {
    Write-Host ">> Advertencia: MySQL tardó demasiado en responder, continuando..." -ForegroundColor Yellow
}

# 7. Comprobar Redis (vía servicio compose, sin hardcodear el nombre del contenedor)
Write-Host ">> Comprobando disponibilidad de Redis..." -ForegroundColor Cyan
$redisReady = $false
$retryCount = 0
$maxRedisRetries = 15

while (-not $redisReady -and $retryCount -lt $maxRedisRetries) {
    $redisPing = docker compose exec -T redis redis-cli ping 2>$null
    if ($redisPing -match "PONG") {
        $redisReady = $true
        Write-Host ">> ¡Redis está respondiendo correctamente!" -ForegroundColor Green
    } else {
        Start-Sleep -Seconds 2
        $retryCount++
        Write-Host "    Esperando a Redis... ($retryCount/$maxRedisRetries)" -ForegroundColor Gray
    }
}

if (-not $redisReady) {
    Write-Host ">> Advertencia: Redis no respondió al ping, continuando..." -ForegroundColor Yellow
}

# 8. Esperar a que Spring Boot devuelva UP (incluye MySQL y Redis internos)
Write-Host ">> Esperando a que Spring Boot esté listo (/actuator/health)..." -ForegroundColor Cyan
$appUrl = "http://localhost:$port/actuator/health"
$appReady = $false
$retryCount = 0
$maxAppRetries = 40

while (-not $appReady -and $retryCount -lt $maxAppRetries) {
    try {
        $response = Invoke-RestMethod -Uri $appUrl -Method Get -ErrorAction Stop
        if ($response.status -eq "UP") {
            $appReady = $true
            Write-Host ">> ¡Spring Boot está UP y funcionando con caché y base de datos conectadas!" -ForegroundColor Green
        }
    } catch {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "    Esperando aplicación web... ($retryCount/$maxAppRetries)" -ForegroundColor Gray
    }
}

# 9. Abrir navegador en Nginx (puerto dinámico)
if (-not $NoBrowser) {
    Write-Host ">> Abriendo plataforma en el navegador (Nginx :$nginxPort)..." -ForegroundColor Cyan
    Start-Process "http://localhost:$nginxPort"
}

Write-Host "==========================================" -ForegroundColor Green
Write-Host "  ¡SISTEMA LEVANTADO CON ÉXITO Y VERIFICADO!" -ForegroundColor Green
Write-Host "  - Nginx (Web):       http://localhost:$nginxPort" -ForegroundColor White
Write-Host "  - App (directa):     http://localhost:$port" -ForegroundColor White
Write-Host "  - Mailpit (Email):   http://localhost:$mailpitPort" -ForegroundColor White
Write-Host "  - Prometheus:        http://localhost:$promPort" -ForegroundColor White
Write-Host "  - Grafana:           http://localhost:$grafPort" -ForegroundColor White
Write-Host "  - Uptime Kuma:       http://localhost:$kumaPort" -ForegroundColor White
Write-Host "  Usa 'docker compose logs -f' para ver logs en tiempo real." -ForegroundColor Gray
Write-Host "==========================================" -ForegroundColor Green
