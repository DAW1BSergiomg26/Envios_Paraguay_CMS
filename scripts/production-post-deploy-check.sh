#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — Production Post-Deploy Check
# =============================================
# Uso: ./scripts/production-post-deploy-check.sh
#
# Comprueba:
#   - docker ps (contenedores esperados)
#   - Healthcheck de la app
#   - Espacio en disco
#   - Memoria disponible
#   - Logs recientes (app + nginx)
#   - Prometheus / Grafana / Uptime Kuma (si puertos disponibles)
# =============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[FAIL]${NC} $1"; }

EXIT_CODE=0
APP_PORT="${PORT:-80}"

echo "============================================"
echo "  Monteastur Envios — Post-Deploy Check"
echo "  $(date)"
echo "============================================"
echo ""

# --- 1. Docker Containers ---
echo "[1/7] Docker containers..."
if command -v docker &>/dev/null; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || warn "Docker not accessible"
    echo ""

    EXPECTED_CONTAINERS="monteastur-app monteastur-nginx monteastur-mysql monteastur-prometheus monteastur-grafana monteastur-uptime-kuma"
    for c in $EXPECTED_CONTAINERS; do
        STATUS=$(docker ps --filter "name=$c" --format "{{.Status}}" 2>/dev/null || true)
        if [ -z "$STATUS" ]; then
            error "Container $c not running"
            EXIT_CODE=1
        else
            info "Container $c: $STATUS"
        fi
    done
else
    warn "Docker not installed"
fi
echo ""

# --- 2. App Healthcheck ---
echo "[2/7] App healthcheck..."
HEALTH_URL="http://localhost:$APP_PORT/actuator/health"
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
if [ "$HEALTH_STATUS" = "200" ]; then
    HEALTH_BODY=$(curl -s "$HEALTH_URL" 2>/dev/null)
    info "Healthcheck: HTTP $HEALTH_STATUS — $HEALTH_BODY"
else
    error "Healthcheck: HTTP $HEALTH_STATUS (expected 200)"
    EXIT_CODE=1
fi
echo ""

# --- 3. Disk space ---
echo "[3/7] Disk space..."
df -h / | awk 'NR==2 {print "  Size: "$2, "Used: "$3, "Avail: "$4, "Use%: "$5}'
DISK_PCT=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$DISK_PCT" -gt 90 ]; then
    error "Disk critical: ${DISK_PCT}%"
    EXIT_CODE=1
elif [ "$DISK_PCT" -gt 80 ]; then
    warn "Disk warning: ${DISK_PCT}%"
else
    info "Disk: ${DISK_PCT}% used"
fi
echo ""

# --- 4. Memory ---
echo "[4/7] Memory..."
free -h | awk 'NR==2 {print "  Total: "$2, "Used: "$3, "Free: "$4, "Avail: "$7}'
TOTAL_MEM=$(free | awk 'NR==2 {print $2}')
AVAIL_MEM=$(free | awk 'NR==2 {print $7}')
if [ "$TOTAL_MEM" -gt 0 ]; then
    AVAIL_PCT=$((AVAIL_MEM * 100 / TOTAL_MEM))
    if [ "$AVAIL_PCT" -lt 10 ]; then
        error "Available RAM critical: ${AVAIL_PCT}%"
        EXIT_CODE=1
    elif [ "$AVAIL_PCT" -lt 20 ]; then
        warn "Available RAM low: ${AVAIL_PCT}%"
    else
        info "Available RAM: ${AVAIL_PCT}%"
    fi
fi
echo ""

# --- 5. Recent logs (app + nginx) ---
echo "[5/7] Recent logs..."
if command -v docker &>/dev/null; then
    # App logs (last 10 lines)
    APP_LOG=$(docker logs monteastur-app --tail 10 2>&1 || true)
    APP_ERRORS=$(echo "$APP_LOG" | grep -ci "error" 2>/dev/null || echo "0")
    echo "  App logs (last 10): $APP_ERRORS error(s) found"
    echo "  ---"
    echo "$APP_LOG" | tail -5
    echo ""

    # Nginx logs (last 10 lines)
    NGINX_LOG=$(docker logs monteastur-nginx --tail 10 2>&1 || true)
    NGINX_5XX=$(echo "$NGINX_LOG" | grep -cE '" 5[0-9][0-9] ' 2>/dev/null || echo "0")
    echo "  Nginx logs (last 10): $NGINX_5XX 5xx error(s) found"
    echo "  ---"
    echo "$NGINX_LOG" | tail -5
    echo ""
else
    warn "Docker not installed, skipping logs"
fi

# --- 6. Prometheus (if port reachable) ---
echo "[6/7] Prometheus..."
PROM_PORT="${PROMETHEUS_PORT:-9090}"
if curl -s -o /dev/null --connect-timeout 3 "http://localhost:$PROM_PORT/-/ready" 2>/dev/null; then
    PROM_TARGETS=$(curl -s "http://localhost:$PROM_PORT/api/v1/targets" 2>/dev/null | grep -o '"health":"[^"]*"' | head -3 || true)
    info "Prometheus reachable on port $PROM_PORT"
    echo "  Targets health: $PROM_TARGETS"
else
    warn "Prometheus not reachable on port $PROM_PORT (expected if port not exposed)"
fi
echo ""

# --- 7. Grafana / Uptime Kuma (if ports reachable) ---
echo "[7/7] Grafana & Uptime Kuma..."
GRAFANA_PORT="${GRAFANA_PORT:-3000}"
if curl -s -o /dev/null --connect-timeout 3 "http://localhost:$GRAFANA_PORT/login" 2>/dev/null; then
    info "Grafana reachable on port $GRAFANA_PORT"
else
    warn "Grafana not reachable on port $GRAFANA_PORT (expected if port not exposed)"
fi

KUMA_PORT="${UPTIME_KUMA_PORT:-3001}"
if curl -s -o /dev/null --connect-timeout 3 "http://localhost:$KUMA_PORT" 2>/dev/null; then
    info "Uptime Kuma reachable on port $KUMA_PORT"
else
    warn "Uptime Kuma not reachable on port $KUMA_PORT (expected if port not exposed)"
fi
echo ""

# --- Summary ---
echo "============================================"
if [ "$EXIT_CODE" -eq 0 ]; then
    info "All post-deploy checks passed"
else
    error "Some post-deploy checks failed"
fi
echo "============================================"

exit "$EXIT_CODE"
