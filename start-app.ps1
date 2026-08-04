# ==============================================================================
# ENVIOS PARAGUAY CMS - SCRIPT MAESTRO DE ARRANQUE 1-CLICK NIVEL DIOS
# ==============================================================================
# Requisitos: Docker Desktop en marcha y `.env` en la raiz (cp .env.example .env).
# Uso:
#   - Doble clic en start-app.bat (recomendado en Windows)
#   - o:  powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\start-app.ps1
#   - o:  pwsh -NoProfile -ExecutionPolicy Bypass -File ./start-app.ps1
# Opcional:  -NoBrowser  (no abrir pestanas en el navegador)
# ==============================================================================

[CmdletBinding()]
param(
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
    param([string]$Key)
    $envFile = Join-Path $PSScriptRoot ".env"
    if (Test-Path -LiteralPath $envFile) {
        $line = Get-Content -LiteralPath $envFile | Where-Object { $_ -match "^$Key=" } | Select-Object -First 1
        if ($line) {
            return ($line -split "=", 2)[1].Trim().Trim('"').Trim("'")
        }
    }
    return $null
}

# --- Configuracion leida de .env (con fallbacks) ------------------------------
$AppPort     = Get-EnvValue "PORT";             if (-not $AppPort)     { $AppPort = "8080" }
$MailpitPort = Get-EnvValue "MAILPIT_UI_PORT";  if (-not $MailpitPort) { $MailpitPort = "8025" }
$AdminUser   = Get-EnvValue "ADMIN_USERNAME";   if (-not $AdminUser)   { $AdminUser = "admin" }
$AdminPass   = Get-EnvValue "ADMIN_PASSWORD";   if (-not $AdminPass)   { $AdminPass = "admin123" }

$baseUrl    = "http://localhost:$AppPort"
$mailpitUrl = "http://localhost:$MailpitPort"
$healthUrl  = "$baseUrl/actuator/health"

Write-Header
Write-Host " [1/3] 🐳 Verificando y levantando la infraestructura Docker..." -ForegroundColor Cyan

# 1. Comprobar el daemon de Docker
docker info > $null 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host " [X] ERROR: Docker Desktop no está ejecutándose. Por favor, inícialo y vuelve a intentarlo." -ForegroundColor Red
    Write-Host ""
    $null = Read-Host " Pulsa Enter para cerrar..."
    exit 1
}

# 2. Levantar la stack (la primera vez construye la imagen automaticamente)
docker compose up -d
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host " [X] ERROR: 'docker compose up -d' falló. Revisa .env y los logs de los servicios." -ForegroundColor Red
    Write-Host ""
    $null = Read-Host " Pulsa Enter para cerrar..."
    exit 1
}

Write-Host ""
Write-Host " [2/3] ⏳ Verificando salud del sistema Spring Boot y servicios..." -ForegroundColor Cyan

# 3. Esperar al healthcheck del backend (arranque en frio puede tardar 1-3 min)
$maxRetries = 60
$count = 0
$isHealthy = $false

while ($count -lt $maxRetries) {
    try {
        $response = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            $isHealthy = $true
            break
        }
    } catch {
        # Aun no esta listo; reintentar
    }
    $count++
    Start-Sleep -Seconds 3
    Write-Host "." -NoNewline -ForegroundColor Green
}

Write-Host ""

if ($isHealthy) {
    Write-Host " [✔] ¡El backend y los servicios están 100% ONLINE y respondiendo! ($healthUrl)" -ForegroundColor Green
} else {
    Write-Host " [!] El backend aún no responde tras $($maxRetries * 3) s; abriendo el navegador igualmente..." -ForegroundColor Yellow
}

Write-Host ""
Write-Host " [3/3] 🌐 Lanzando pestañas en tu navegador web..." -ForegroundColor Cyan

# 4. Abrir pestanas automaticamente (omitir con -NoBrowser)
if (-not $NoBrowser) {
    Start-Process $baseUrl
    Start-Sleep -Milliseconds 500
    Start-Process "$baseUrl/login"
    Start-Sleep -Milliseconds 500
    Start-Process $mailpitUrl
}

Write-Header
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host " 🎉 ¡SISTEMA ENVIOS PARAGUAY CMS OPERATIVO EN TU NAVEGADOR! " -ForegroundColor Yellow
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " 🌐 ACCESOS DIRECTOS:" -ForegroundColor White
Write-Host "  - 🌲 Web Principal / Landing:     $baseUrl" -ForegroundColor Green
Write-Host "  - 🔐 Login Administrativo:        $baseUrl/login" -ForegroundColor Green
Write-Host "  - 🏡 Sección La Casa (Fix):       $baseUrl/casa" -ForegroundColor Green
Write-Host "  - 📦 Tracking Público (Redis):   $baseUrl/tracking" -ForegroundColor Green
Write-Host "  - 📄 Ingesta Masiva CSV:          $baseUrl/admin/imports" -ForegroundColor Green
Write-Host "  - 🖨️ Documentos & Etiquetas PDF:  $baseUrl/admin/documentos" -ForegroundColor Green
Write-Host "  - 📬 Buzón de Email (Mailpit):    $mailpitUrl" -ForegroundColor Green
Write-Host ""
Write-Host " 🔑 CREDENCIALES DE ACCESO ADMIN (leídas de .env):" -ForegroundColor White
Write-Host "  - Usuario:     $AdminUser" -ForegroundColor Yellow
Write-Host "  - Contraseña:  $AdminPass" -ForegroundColor Yellow
Write-Host ""
Write-Host " 💡 COMANDOS ÚTILES:" -ForegroundColor White
Write-Host "  - Detener la aplicación:   docker compose stop" -ForegroundColor Gray
Write-Host "  - Ver logs en vivo:        docker compose logs -f app" -ForegroundColor Gray
Write-Host "==============================================================================" -ForegroundColor Green
