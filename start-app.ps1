# ==========================================
# Script de inicio rápido de la App - Monteastur Envios
# ==========================================

Write-Host ">> Verificando compilación del proyecto (Maven)..." -ForegroundColor Cyan
./mvnw clean compile -q

if ($LASTEXITCODE -eq 0) {
    Write-Host ">> ¡Compilación exitosa! Arrancando Spring Boot..." -ForegroundColor Green
    
    # Abrir la web principal automáticamente tras unos segundos
    Start-Job -ScriptBlock {
        Start-Sleep -Seconds 6
        Start-Process "http://localhost:8080"
    } | Out-Null

    # Arrancar la aplicación en primer plano para ver los logs en tiempo real
    ./mvnw spring-boot:run
} else {
    Write-Host ">> Error en la compilación. Revisa el código antes de arrancar." -ForegroundColor Red
}
