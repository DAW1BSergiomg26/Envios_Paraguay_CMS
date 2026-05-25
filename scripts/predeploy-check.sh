#!/usr/bin/env bash
# ============================================================
# MonteAstur — Pre-deploy Check
# ============================================================
# Runs git status, Docker config, Maven tests, frontend tests,
# and health checks. Reports clear pass/fail at the end.
#
# Usage:
#   ./scripts/predeploy-check.sh
#   ./scripts/predeploy-check.sh --e2e   # also run E2E tests
# ============================================================
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT"

RUN_E2E=false
if [ "${1:-}" = "--e2e" ]; then
  RUN_E2E=true
fi

EXIT_CODE=0

echo "╔══════════════════════════════════════════════╗"
echo "║   MonteAstur — Pre-deploy Check             ║"
echo "╚══════════════════════════════════════════════╝"
echo ""

step() {
  local label="$1"
  shift
  echo "» $label..."
  if "$@"; then
    echo "  ✓ $label"
  else
    echo "  ✖ $label (exit code $?)" >&2
    EXIT_CODE=1
  fi
}

# ── 1. Git status ───────────────────────────────────
echo "» git status..."
GIT_STATUS=$(git status --short 2>&1 || true)
if [ -z "$GIT_STATUS" ]; then
  echo "  ✓ Working tree clean"
else
  echo "  ⚠ Uncommitted changes:"
  echo "$GIT_STATUS" | sed 's/^/    /'
fi

# ── 2. Docker compose config ────────────────────────
step "docker compose config" docker compose config >/dev/null

# ── 3. Maven tests ──────────────────────────────────
step "mvn test" mvn test -q

# ── 4. Frontend unit tests ──────────────────────────
step "npm run test:unit" \
  bash -c "cd '$PROJECT_ROOT/frontend-react' && npm run test:unit"

# ── 5. Frontend build ───────────────────────────────
step "npm run build" \
  bash -c "cd '$PROJECT_ROOT/frontend-react' && npm run build"

# ── 6. E2E tests (optional) ─────────────────────────
if [ "$RUN_E2E" = true ]; then
  echo "» npm run test:e2e (requires Docker stack UP)"
  cd "$PROJECT_ROOT/frontend-react"
  E2E_BASE_URL=http://localhost:8090 npm run test:e2e && \
    echo "  ✓ E2E tests" || EXIT_CODE=1
  cd "$PROJECT_ROOT"
else
  echo "» npm run test:e2e (skipped, use --e2e to enable)"
fi

# ── 7. Docker compose ps ────────────────────────────
echo "» docker compose ps..."
docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | \
  sed 's/^/    /'

# ── 8. Healthcheck ──────────────────────────────────
echo "» curl healthcheck..."
if curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
  echo "  ✓ Health: UP"
else
  echo "  ✖ Healthcheck failed" >&2
  EXIT_CODE=1
fi

echo ""
echo "╔══════════════════════════════════════════════╗"
if [ "$EXIT_CODE" -eq 0 ]; then
  echo "║   ✅ PREDEPLOY CHECK PASSED                  ║"
  echo "╚══════════════════════════════════════════════╝"
else
  echo "║   ❌ PREDEPLOY CHECK FAILED                  ║"
  echo "╚══════════════════════════════════════════════╝"
fi

exit "$EXIT_CODE"