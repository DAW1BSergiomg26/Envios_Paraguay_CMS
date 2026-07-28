# ==========================================
# Script de inicio robusto - Monteastur Envios
# ==========================================

Write-Host ">> Forzando apagado previo de contenedores..." -ForegroundColor Cyan
# Usamos --timeout 5 para evitar que se quede congelado si Docker tarda
docker compose down --timeout 5 2>&1 | Out-Null

Write-Host ">> Levantando servicios con Docker Compose..." -ForegroundColor Cyan
docker compose up -d

Write-Host ">> Esperando a que MySQL esté saludable..." -ForegroundColor Yellow
$maxRetries = 35
$retryCount = 0
$healthy = $false

while (-not $healthy -and $retryCount -lt $maxRetries) {
    # Comprobamos si el contenedor existe y está healthy
    $status = docker inspect --format="{{if .State.Health}}{{.State.Health.Status}}{{else}}running{{end}}" monteastur-mysql 2>&1
    if ($status -match "healthy" -or $status -match "running") {
        $healthy = $true
    } else {
        Start-Sleep -Seconds 3
        $retryCount++
        Write-Host "." -NoNewline
    }
}

Write-Host ""
if ($healthy) {
    Write-Host ">> ¡Base de datos lista y operativa!" -ForegroundColor Green
} else {
    Write-Host ">> Nota: Continuando con el arranque de la app..." -ForegroundColor Yellow
}

Write-Host ">> Abriendo pestañas en el navegador..." -ForegroundColor Cyan

Start-Process "http://localhost:8080"
Start-Process "http://localhost:3001"
Start-Process "http://localhost:3002/dashboard"

Write-Host ">> ¡Todo el sistema se ha lanzado con éxito!" -ForegroundColor Green