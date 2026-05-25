<#
.SYNOPSIS
  Pre-deploy health check for MonteAstur Envios.
.DESCRIPTION
  Runs git status, Docker config, Maven tests, frontend tests, and health checks.
  Reports clear pass/fail at the end.
.EXAMPLE
  .\scripts\predeploy-check.ps1
  .\scripts\predeploy-check.ps1 -RunE2E
.PARAMETER RunE2E
  Also run Playwright E2E tests (requires Docker stack to be UP).
#>

param(
  [switch]$RunE2E
)

$ErrorActionPreference = "Stop"
$exitCode = 0
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSCommandPath)
Set-Location -LiteralPath $ProjectRoot

Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   MonteAstur — Pre-deploy Check             ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

function Step {
  param([string]$Label, [ScriptBlock]$Block)
  Write-Host "» $Label..." -ForegroundColor Yellow
  try {
    & $Block
    Write-Host "  ✓ $Label" -ForegroundColor Green
    return $true
  } catch {
    Write-Host "  ✖ $Label : $_" -ForegroundColor Red
    $script:exitCode = 1
    return $false
  }
}

# ── 1. Git status ───────────────────────────────────────
Step "git status" {
  $status = git status --short 2>&1
  if ($status) {
    Write-Host "    Uncommitted changes:" -ForegroundColor DarkYellow
    $status | ForEach-Object { Write-Host "    $_" }
  } else {
    Write-Host "    Working tree clean" -ForegroundColor Green
  }
}

# ── 2. Docker compose config ────────────────────────────
Step "docker compose config" {
  $result = docker compose config 2>&1
  if ($LASTEXITCODE -ne 0) { throw "docker compose config failed" }
}

# ── 3. Maven tests ──────────────────────────────────────
Step "mvn test" {
  $result = mvn test -q 2>&1
  if ($LASTEXITCODE -ne 0) { throw "Maven tests failed" }
}

# ── 4. Frontend unit tests ──────────────────────────────
Step "npm run test:unit" {
  Push-Location "$ProjectRoot/frontend-react"
  try {
    $result = npm run test:unit 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Unit tests failed" }
  } finally {
    Pop-Location
  }
}

# ── 5. Frontend build ───────────────────────────────────
Step "npm run build" {
  Push-Location "$ProjectRoot/frontend-react"
  try {
    $result = npm run build 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Build failed" }
  } finally {
    Pop-Location
  }
}

# ── 6. E2E tests (optional) ─────────────────────────────
if ($RunE2E) {
  Step "npm run test:e2e (requires Docker stack UP)" {
    Push-Location "$ProjectRoot/frontend-react"
    try {
      $env:E2E_BASE_URL = "http://localhost:8090"
      $result = npm run test:e2e 2>&1
      if ($LASTEXITCODE -ne 0) { throw "E2E tests failed" }
    } finally {
      Pop-Location
    }
  }
} else {
  Write-Host "» npm run test:e2e (skipped, use -RunE2E to enable)" -ForegroundColor DarkGray
}

# ── 7. Docker compose ps ────────────────────────────────
Step "docker compose ps" {
  $ps = docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>&1
  Write-Host "" -NoNewline
  $ps | ForEach-Object { Write-Host "    $_" }
}

# ── 8. Healthcheck ──────────────────────────────────────
Step "curl healthcheck" {
  try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 5
    if ($resp.Content -notmatch '"status"\s*:\s*"UP"') { throw "Health is not UP: $($resp.Content)" }
  } catch {
    throw "Healthcheck failed: $_"
  }
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Cyan
if ($exitCode -eq 0) {
  Write-Host "║   ✅ PREDEPLOY CHECK PASSED                  ║" -ForegroundColor Green
  Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Green
} else {
  Write-Host "║   ❌ PREDEPLOY CHECK FAILED                  ║" -ForegroundColor Red
  Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Red
}

exit $exitCode