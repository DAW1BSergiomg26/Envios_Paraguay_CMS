# ==========================================
# Script de inicio rápido de la App - Monteastur Envios
# ==========================================

param(
    [switch]$NoBrowser,
    [switch]$SkipCompile
)

if (-not $SkipCompile) {
    Write-Host ">> Verificando compilación del proyecto (Maven)..." -ForegroundColor Cyan
    mvn clean compile -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host ">> Error en la compilación. Revisa el código antes de arrancar." -ForegroundColor Red
        exit 1
    }
    Write-Host ">> ¡Compilación exitosa!" -ForegroundColor Green
}

Write-Host ">> Arrancando Spring Boot en http://localhost:8080 ..." -ForegroundColor Cyan

if (-not $NoBrowser) {
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 8
        Start-Process "http://localhost:8080"
        Start-Sleep -Seconds 2
        Start-Process "http://localhost:8080/actuator/health"
    } | Out-Null
}

mvn spring-boot:run
