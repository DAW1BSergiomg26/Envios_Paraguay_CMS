# ==============================================================================
# ENVIOS PARAGUAY CMS - SCRIPT MAESTRO DE ARRANQUE 1-CLICK NIVEL DIOS
# Infraestructura Docker completa + espera de salud +
# pestañas automáticas en el navegador (Público + Admin BI + Mailpit + Actuator).
# ==============================================================================

param(
    [switch]$NoBuild,
    [switch]$NoBrowser
)

$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Write-Header {
    Clear-Host
    Write-Host "==============================================================================" -ForegroundColor DarkGreen
    Write-Host " 🌲 SUN & FOREST - ENVIOS PARAGUAY CMS (VERDE ASTURIAS x SOL PARAGUAY) ☀️ " -ForegroundColor Yellow
    Write-Host "==============================================================================" -ForegroundColor DarkGreen
    Write-Host ""
}

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

Write-Header
Write-Host " [1/3] 🐳 Levantando la infraestructura Docker completa..." -ForegroundColor Cyan

# 1. Comprobar que el comando docker exista
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host " [X] ERROR: No se encontró el comando 'docker'. Instala Docker Desktop y vuelve a intentarlo." -ForegroundColor Red
    pause
    exit 1
}

# 2. Comprobar que el daemon esté ejecutándose
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] ERROR: Docker Desktop no está ejecutándose. Por favor inícialo y espera a que esté listo." -ForegroundColor Red
    pause
    exit 1
}

# 3. Comprobar el plugin docker compose
docker compose version *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] ERROR: Docker Compose (plugin 'docker compose') no está disponible." -ForegroundColor Red
    pause
    exit 1
}

# 4. Garantizar que existe .env (docker compose lo requiere)
if (-not (Test-Path -LiteralPath ".env")) {
    Write-Host " [!] No existe .env. Creando copia desde .env.example..." -ForegroundColor Yellow
    Copy-Item -LiteralPath ".env.example" -Destination ".env"
    Write-Host " [⚠] IMPORTANTE: edita '.env' y ajusta las credenciales antes de continuar." -ForegroundColor Yellow
    Read-Host "Pulsa ENTER cuando hayas revisado .env..."
}

# 5. Leer la configuración real desde .env
$port        = Get-EnvValue "PORT" "8080"
$nginxPort   = Get-EnvValue "NGINX_PORT" "80"
$mailpitPort = Get-EnvValue "MAILPIT_UI_PORT" "8025"
$adminUser   = Get-EnvValue "ADMIN_USERNAME" "admin"
$adminPass   = Get-EnvValue "ADMIN_PASSWORD" "(definida en .env)"

# 6. Reconstruir e Iniciar Contenedores
Write-Host ""
if ($NoBuild) {
    Write-Host " 🚀 Arrancando contenedores (sin reconstruir la imagen)..." -ForegroundColor Gray
    docker compose up -d
} else {
    Write-Host " 🏗️ Reconstruyendo la imagen (aplica estáticos, seguridad y código nuevo) y arrancando..." -ForegroundColor Gray
    docker compose up -d --build
}
if ($LASTEXITCODE -ne 0) {
    Write-Host " [X] ERROR: 'docker compose up' falló. Revisa los logs con 'docker compose logs -f app'." -ForegroundColor Red
    pause
    exit 1
}

Write-Host ""
Write-Host " [2/3] ⏳ Verificando salud del sistema Spring Boot y servicios..." -ForegroundColor Cyan

$healthUrl  = "http://localhost:$port/actuator/health"
$maxRetries = 45
$count      = 0
$isHealthy  = $false

while ($count -lt $maxRetries) {
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 2 -ErrorAction Stop
        if ($response.StatusCode -eq 200 -and $response.Content -match '"status"\s*:\s*"UP"') {
            $isHealthy = $true
            break
        }
    } catch {
        # El backend aún está arrancando (Flyway, pool de conexiones, etc.) -> reintentar
    }
    $count++
    Start-Sleep -Seconds 2
    Write-Host "." -NoNewline -ForegroundColor Green
}

Write-Host ""

if ($isHealthy) {
    Write-Host " [✔] ¡El backend y los servicios están 100% ONLINE y respondiendo! ($healthUrl)" -ForegroundColor Green
} else {
    Write-Host " [!] El backend tardó en responder, pero abriremos los accesos para revisión." -ForegroundColor Yellow
}

Write-Host ""
Write-Host " [3/3] 🌐 Lanzando pestañas en tu navegador web..." -ForegroundColor Cyan

if (-not $NoBrowser) {
    $urls = @(
        "http://localhost:$port",
        "http://localhost:$port/casa",
        "http://localhost:$port/tracking",
        "http://localhost:$port/login",
        "http://localhost:$port/admin/dashboard",
        "http://localhost:$port/actuator/health/infraestructura",
        "http://localhost:$mailpitPort"
    )
    foreach ($url in $urls) {
        Start-Process $url
        Start-Sleep -Milliseconds 400
    }
}

Write-Header
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host " 🎉 ¡SISTEMA ENVIOS PARAGUAY CMS OPERATIVO EN TU NAVEGADOR! " -ForegroundColor Yellow
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " 🌐 ACCESOS DIRECTOS PRINCIPALES:" -ForegroundColor White
Write-Host "  - 🌲 Web Principal / Landing:      http://localhost:$port" -ForegroundColor Green
Write-Host "  - 🏡 Sección La Casa:              http://localhost:$port/casa" -ForegroundColor Green
Write-Host "  - 📦 Tracking Público:             http://localhost:$port/tracking" -ForegroundColor Green
Write-Host "  - 🔐 Login Administrativo:         http://localhost:$port/login" -ForegroundColor Green
Write-Host "  - 📊 BI Dashboard & Analítica:     http://localhost:$port/admin/dashboard" -ForegroundColor Green
Write-Host "  - 📄 Ingesta Masiva CSV:           http://localhost:$port/admin/imports" -ForegroundColor Green
Write-Host "  - 🖨️ Documentos & Etiquetas:       http://localhost:$port/admin/documentos" -ForegroundColor Green
Write-Host ""
Write-Host " 🔭 OBSERVABILIDAD Y HERRAMIENTAS TÉCNICAS (FASE 6):" -ForegroundColor White
Write-Host "  - 🩺 Health Check Infraestructura: http://localhost:$port/actuator/health/infraestructura" -ForegroundColor Cyan
Write-Host "  - 📈 Métricas Prometheus:          http://localhost:$port/actuator/prometheus" -ForegroundColor Cyan
Write-Host "  - 📬 Buzón de Email (Mailpit):     http://localhost:$mailpitPort" -ForegroundColor Cyan
Write-Host ""
Write-Host " 🔑 CREDENCIALES DE ACCESO ADMIN (leídas de .env):" -ForegroundColor White
Write-Host "  - Usuario:     $adminUser" -ForegroundColor Yellow
Write-Host "  - Contraseña:  $adminPass" -ForegroundColor Yellow
Write-Host ""
Write-Host " 💡 COMANDOS ÚTILES:" -ForegroundColor White
Write-Host "  - Detener la aplicación:     docker compose stop" -ForegroundColor Gray
Write-Host "  - Ver logs en vivo:          docker compose logs -f app" -ForegroundColor Gray
Write-Host "  - Arranque sin rebuild:      .\start-app.ps1 -NoBuild" -ForegroundColor Gray
Write-Host "==============================================================================" -ForegroundColor Green