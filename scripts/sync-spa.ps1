# ==============================================================================
# sync-spa.ps1 — Copia el build del frontend (frontend-react/dist) a
# src/main/resources/static/react-dashboard/ para que `mvn package` empaquete
# la SPA ACTUAL. Refleja el comportamiento del Dockerfile (stage 2) en local.
#
# Uso:  npm run build --prefix frontend-react; .\scripts\sync-spa.ps1
# ==============================================================================

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$dist = Join-Path $repoRoot "frontend-react\dist"
$staticTarget = Join-Path $repoRoot "src\main\resources\static\react-dashboard"

if (-not (Test-Path -LiteralPath (Join-Path $dist "index.html"))) {
    Write-Error "No existe $dist\index.html. Ejecuta primero: npm run build (en frontend-react)."
    exit 1
}

if (Test-Path -LiteralPath $staticTarget) {
    Remove-Item -LiteralPath $staticTarget -Recurse -Force
}
New-Item -ItemType Directory -Path $staticTarget -Force | Out-Null
Copy-Item -Path (Join-Path $dist "*") -Destination $staticTarget -Recurse -Force

Write-Host "[sync-spa] SPA sincronizada: frontend-react\dist -> static\react-dashboard"
Get-ChildItem -LiteralPath $staticTarget -Recurse -File | ForEach-Object {
    Write-Host ("  - " + $_.FullName.Substring($repoRoot.Length + 1))
}
