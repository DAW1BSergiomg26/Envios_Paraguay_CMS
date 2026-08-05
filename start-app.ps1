# ==========================================
# Script de Arranque Maestro - Envios_Paraguay_CMS
# ==========================================

Write-Host "🚀 Iniciando entorno y sistema..." -ForegroundColor Cyan

# 1. Configurar automáticamente el JDK 25 desde su ruta real en tu equipo
$env:JAVA_HOME = "C:\Users\astur\.jdks\openjdk-25"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

Write-Host "✅ Usando Java desde: $env:JAVA_HOME" -ForegroundColor Green
java -version

# 2. Limpieza profunda y compilación limpia para que se apliquen todos los CSS y cambios visuales
Write-Host "`n🧹 Ejecutando limpieza y compilación (mvn clean compile)..." -ForegroundColor Yellow
mvn clean compile

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Error en la compilación de Maven." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 3. Arrancar la aplicación Spring Boot
Write-Host "`n🌟 Arrancando Spring Boot..." -ForegroundColor Green
mvn spring-boot:run