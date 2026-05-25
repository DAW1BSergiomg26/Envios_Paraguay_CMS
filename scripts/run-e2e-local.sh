#!/usr/bin/env bash
# ============================================================
# MonteAstur E2E — Local Test Runner (Linux/WSL)
# ============================================================
# Builds + starts Docker Compose, waits for health, runs Playwright.
# Usage: ./scripts/run-e2e-local.sh
# ============================================================
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

echo "╔══════════════════════════════════════════════╗"
echo "║   MonteAstur E2E — Setup & Test Runner      ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

# ── 1. Docker Compose down + up ──────────────────────────
echo "» docker compose down..."
docker compose down --remove-orphans 2>/dev/null

echo "» docker compose up -d --build..."
docker compose up -d --build
echo ""

# ── 2. Wait for app health ───────────────────────────────
echo "» Waiting for app health (http://localhost:8080/actuator/health)..."
HEALTHY=false
for i in $(seq 1 30); do
  sleep 5
  if curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    HEALTHY=true
    echo "  ✓ App health: UP (attempt $i)"
    break
  fi
  echo "  attempt $i: not ready yet"
done

if [ "$HEALTHY" = false ]; then
  echo "✖ App did not become healthy after 150s" >&2
  docker compose logs app --tail 30
  exit 1
fi

# ── 3. Wait for nginx (port 8090) ────────────────────────
echo "» Waiting for nginx (http://localhost:8090)..."
NGINX_READY=false
for i in $(seq 1 10); do
  sleep 3
  if curl -s -o /dev/null -w '%{http_code}' http://localhost:8090 | grep -q '200'; then
    NGINX_READY=true
    echo "  ✓ Nginx ready (attempt $i)"
    break
  fi
  echo "  attempt $i: nginx not ready"
done

if [ "$NGINX_READY" = false ]; then
  echo "✖ Nginx did not become ready" >&2
  docker compose logs nginx --tail 20
  exit 1
fi

echo ""

# ── 4. Run Playwright E2E tests ──────────────────────────
echo "» cd frontend-react && npm run test:e2e"
cd "$PROJECT_ROOT/frontend-react"
E2E_BASE_URL=http://localhost:8090 npm run test:e2e
EXIT_CODE=$?

cd "$PROJECT_ROOT"
echo ""

if [ "$EXIT_CODE" -eq 0 ]; then
  echo "╔══════════════════════════════════════════════╗"
  echo "║   ✅ TODOS LOS TESTS E2E PASARON            ║"
  echo "╚══════════════════════════════════════════════╝"
else
  echo "╔══════════════════════════════════════════════╗"
  echo "║   ❌ ALGUNOS TESTS E2E FALLARON             ║"
  echo "╚══════════════════════════════════════════════╝"
  echo ""
  echo "  Report: frontend-react/playwright-report/index.html"
  echo "  Traces:  frontend-react/test-results/"
  echo ""
  echo "  To inspect traces: npx playwright show-report frontend-react/playwright-report"
  echo "  To re-run headed:  cd frontend-react && npm run e2e:headed"
fi

exit "$EXIT_CODE"