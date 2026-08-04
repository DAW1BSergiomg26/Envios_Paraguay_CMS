@echo off
rem ============================================================================
rem  ENVIOS PARAGUAY CMS - Arranque 1-Click
rem  Lanza start-app.ps1 omitiendo las restricciones de ejecucion de
rem  PowerShell. Doble clic para iniciar la infraestructura y el navegador.
rem ============================================================================
setlocal
chcp 65001 >nul
cd /d "%~dp0"

where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo [X] No se encontro powershell.exe en el sistema.
    pause
    exit /b 1
)

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-app.ps1"
set "EXITCODE=%ERRORLEVEL%"

echo.
if not "%EXITCODE%"=="0" (
    echo [X] El arranque finalizo con errores (codigo %EXITCODE%).
    pause
)
exit /b %EXITCODE%
