@echo off
rem ============================================================================
rem  🌲 ENVIOS PARAGUAY CMS - LANZADOR MAESTRO NIVEL DIOS (.BAT)
rem  Gestión inteligente de contenedores, entorno y bypass de PowerShell.
rem ============================================================================
setlocal EnableDelayedExpansion
chcp 65001 >nul
cd /d "%~dp0"

echo ==============================================================================
echo  SUN & FOREST - ENVIOS PARAGUAY CMS (VERDE ASTURIAS x SOL PARAGUAY)
echo ==============================================================================
echo.

:: 1. Validar PowerShell
where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo [X] ERROR CRITICO: No se encuentra powershell.exe en el sistema.
    pause
    exit /b 1
)

:: 2. Validar archivo .env
if not exist ".env" (
    echo [!] ADVERTENCIA: No se encontró el archivo .env.
    if exist ".env.example" (
        echo [+] Copiando plantilla desde .env.example...
        copy .env.example .env >nul
        echo [OK] Archivo .env creado. Por favor, revísalo si es necesario.
    ) else (
        echo [X] ERROR: No existe .env ni .env.example. Crea uno antes de continuar.
        pause
        exit /b 1
    )
)

:: 3. Menú Interactivo Nivel Dios
echo Selecciona el modo de arranque:
echo   [1] Arranque Estándar (Reconstruye contenedores y abre navegador) - RECOMENDADO
echo   [2] Arranque Rápido   (Sin reconstruir imagen, solo levanta servicios)
echo   [3] Ver Logs en Vivo  (Sigue la traza de la aplicación Spring Boot)
echo   [4] Detener Todo      (Apaga contenedores de Docker Compose)
echo.
set /p "opcion=Elige una opción [1-4] (Por defecto 1): "

if "%opcion%"=="2" (
    echo.
    echo [+] Ejecutando arranque rápido (sin rebuild)...
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-app.ps1" -NoBuild
) else if "%opcion%"=="3" (
    echo.
    echo [+] Abriendo logs en tiempo real (Ctrl+C para salir)...
    docker compose logs -f app
    exit /b 0
) else if "%opcion%"=="4" (
    echo.
    echo [+] Deteniendo infraestructura Docker...
    docker compose stop
    echo [OK] Servicios detenidos correctamente.
    pause
    exit /b 0
) else (
    :: Opción por defecto (1)
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-app.ps1"
)

set "EXITCODE=%ERRORLEVEL%"
echo.
if not "%EXITCODE%"=="0" (
    echo [X] El proceso finalizó con incidencias (código %EXITCODE%).
    pause
)
exit /b %EXITCODE%