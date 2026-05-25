<#
.SYNOPSIS
  Run E2E tests locally with Docker Compose + Playwright.
.DESCRIPTION
  Builds and starts Docker Compose, waits for health, then runs Playwright E2E tests.
  Reports clear pass/fail at the end.
.EXAMPLE
  .\scripts\run-e2e-local.ps1
#>

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSCommandPath)
Set-Location -LiteralPath $ProjectRoot

Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║   MonteAstur E2E — Setup & Test Runner      ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ── 1. Docker Compose down + up ──────────────────────────────────
Write-Host "» docker compose down..." -ForegroundColor Yellow
docker compose down --remove-orphans 2>&1 | Out-Null

Write-Host "» docker compose up -d --build..." -ForegroundColor Yellow
docker compose up -d --build 2>&1
if ($LASTEXITCODE -ne 0) {
  Write-Host "✖ docker compose build/up failed" -ForegroundColor Red
  exit 1
}

# ── 2. Wait for app health ───────────────────────────────────────
Write-Host "» Waiting for app health (http://localhost:8080/actuator/health)..." -ForegroundColor Yellow
$healthy = $false
for ($i = 1; $i -le 30; $i++) {
  Start-Sleep -Seconds 5
  try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -UseBasicParsing -TimeoutSec 3
    if ($resp.Content -match '"status"\s*:\s*"UP"') {
      $healthy = $true
      Write-Host "  ✓ App health: UP (attempt $i)" -ForegroundColor Green
      break
    }
  } catch {
    Write-Host "  attempt $i: not ready yet" -ForegroundColor DarkGray
  }
}
if (-not $healthy) {
  Write-Host "✖ App did not become healthy after 150s" -ForegroundColor Red
  docker compose logs app --tail 30
  exit 1
}

# ── 3. Wait for nginx (port 8090) ────────────────────────────────
Write-Host "» Waiting for nginx (http://localhost:8090)..." -ForegroundColor Yellow
$nginxReady = $false
for ($i = 1; $i -le 10; $i++) {
  Start-Sleep -Seconds 3
  try {
    $resp = Invoke-WebRequest -Uri "http://localhost:8090" -UseBasicParsing -TimeoutSec 3
    if ($resp.StatusCode -eq 200) {
      $nginxReady = $true
      Write-Host "  ✓ Nginx ready (attempt $i)" -ForegroundColor Green
      break
    }
  } catch {
    Write-Host "  attempt $i: nginx not ready" -ForegroundColor DarkGray
  }
}
if (-not $nginxReady) {
  Write-Host "✖ Nginx did not become ready" -ForegroundColor Red
  docker compose logs nginx --tail 20
  exit 1
}

Write-Host ""

# ── 4. Run Playwright E2E tests ──────────────────────────────────
Write-Host "» cd frontend-react && npm run test:e2e" -ForegroundColor Yellow
Set-Location -LiteralPath "$ProjectRoot/frontend-react"
$env:E2E_BASE_URL = "http://localhost:8090"
npm run test:e2e
$exitCode = $LASTEXITCODE

Set-Location -LiteralPath $ProjectRoot

Write-Host ""
if ($exitCode -eq 0) {
  Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Green
  Write-Host "║   ✅ TODOS LOS TESTS E2E PASARON            ║" -ForegroundColor Green
  Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Green
} else {
  Write-Host "╔══════════════════════════════════════════════╗" -ForegroundColor Red
  Write-Host "║   ❌ ALGUNOS TESTS E2E FALLARON             ║" -ForegroundColor Red
  Write-Host "╚══════════════════════════════════════════════╝" -ForegroundColor Red
  Write-Host ""
  Write-Host "  Report: frontend-react/playwright-report/index.html" -ForegroundColor Cyan
  Write-Host "  Traces:  frontend-react/test-results/" -ForegroundColor Cyan
  Write-Host ""
  Write-Host "  To inspect traces: npx playwright show-report frontend-react/playwright-report" -ForegroundColor Cyan
  Write-Host "  To re-run headed:  cd frontend-react && npm run e2e:headed" -ForegroundColor Cyan
}

exit $exitCode