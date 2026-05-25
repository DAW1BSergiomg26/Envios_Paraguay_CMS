#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — Production Smoke Tests
# =============================================
# Uso: BASE_URL=https://monteastur.com ./scripts/production-smoke-test.sh
#
# Prueba endpoints críticos post-deploy:
#   - /actuator/health
#   - / (home)
#   - /login-react
#   - /tracking
# =============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[FAIL]${NC} $1"; }

EXIT_CODE=0

BASE_URL="${BASE_URL:-http://localhost}"
echo "============================================"
echo "  Monteastur Envios — Smoke Tests"
echo "  $(date)"
echo "  BASE_URL: $BASE_URL"
echo "============================================"
echo ""

# --- Test 1: Healthcheck ---
echo "[TEST 1/4] Healthcheck endpoint..."
HEALTH_RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
HEALTH_BODY=$(curl -s "$BASE_URL/actuator/health" 2>/dev/null || echo "{}")
if [ "$HEALTH_RESP" = "200" ] && echo "$HEALTH_BODY" | grep -q '"status":"UP"'; then
    info "Healthcheck: HTTP $HEALTH_RESP — $HEALTH_BODY"
else
    error "Healthcheck: HTTP $HEALTH_RESP (expected 200 + {\"status\":\"UP\"})"
    EXIT_CODE=1
fi
echo ""

# --- Test 2: Home page ---
echo "[TEST 2/4] Home page..."
HOME_RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/" 2>/dev/null || echo "000")
HOME_TYPE=$(curl -s -o /dev/null -w "%{content_type}" "$BASE_URL/" 2>/dev/null || echo "")
if [ "$HOME_RESP" = "200" ]; then
    info "Home page: HTTP $HOME_RESP (content-type: $HOME_TYPE)"
else
    error "Home page: HTTP $HOME_RESP (expected 200)"
    EXIT_CODE=1
fi
echo ""

# --- Test 3: Login React SPA ---
echo "[TEST 3/4] Login React SPA..."
LOGIN_RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/login-react" 2>/dev/null || echo "000")
if [ "$LOGIN_RESP" = "200" ]; then
    info "Login React: HTTP $LOGIN_RESP"
else
    warn "Login React: HTTP $LOGIN_RESP (expected 200, may vary if SPA returns index.html)"
fi
echo ""

# --- Test 4: Tracking page ---
echo "[TEST 4/4] Tracking page..."
TRACKING_RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/tracking" 2>/dev/null || echo "000")
if [ "$TRACKING_RESP" = "200" ]; then
    info "Tracking page: HTTP $TRACKING_RESP"
else
    warn "Tracking page: HTTP $TRACKING_RESP (expected 200, may be /seguimiento)"
fi

# Try alternative tracking route
if [ "$TRACKING_RESP" != "200" ]; then
    SEGUIMIENTO_RESP=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/seguimiento" 2>/dev/null || echo "000")
    if [ "$SEGUIMIENTO_RESP" = "200" ]; then
        info "Seguimiento page: HTTP $SEGUIMIENTO_RESP (alternative route)"
    else
        error "Tracking/Seguimiento page not found (HTTP $TRACKING_RESP / $SEGUIMIENTO_RESP)"
        EXIT_CODE=1
    fi
fi
echo ""

# --- Summary ---
echo "============================================"
if [ "$EXIT_CODE" -eq 0 ]; then
    info "All smoke tests passed"
else
    error "Some smoke tests failed"
fi
echo "============================================"

exit "$EXIT_CODE"
