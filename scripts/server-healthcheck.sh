#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — Server Healthcheck
# =============================================
# Uso: ./scripts/server-healthcheck.sh
#
# Muestra estado del servidor VPS:
#   - Uptime
#   - Disco
#   - RAM
#   - Docker containers
#   - App healthcheck
#   - Docker disk usage
# =============================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[OK]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[FAIL]${NC} $1"; }

EXIT_CODE=0

echo "============================================"
echo "  Monteastur Envios — Server Healthcheck"
echo "  $(date)"
echo "============================================"
echo ""

# --- Uptime ---
echo "[UPTIME]"
uptime
echo ""

# --- Disk ---
echo "[DISK]"
df -h / | awk 'NR==2 {print "  Filesystem: "$1, "Size: "$2, "Used: "$3, "Avail: "$4, "Use%: "$5}'
USED_PCT=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ "$USED_PCT" -gt 90 ]; then
    error "Disk usage critical: ${USED_PCT}%"
    EXIT_CODE=1
elif [ "$USED_PCT" -gt 80 ]; then
    warn "Disk usage warning: ${USED_PCT}%"
else
    info "Disk usage: ${USED_PCT}%"
fi
echo ""

# --- RAM ---
echo "[RAM]"
free -h | awk 'NR==2 {print "  Total: "$2, "Used: "$3, "Free: "$4, "Available: "$7}'
TOTAL_MEM=$(free | awk 'NR==2 {print $2}')
AVAIL_MEM=$(free | awk 'NR==2 {print $7}')
AVAIL_PCT=$((AVAIL_MEM * 100 / TOTAL_MEM))
if [ "$AVAIL_PCT" -lt 10 ]; then
    error "Available RAM critical: ${AVAIL_PCT}%"
    EXIT_CODE=1
elif [ "$AVAIL_PCT" -lt 20 ]; then
    warn "Available RAM low: ${AVAIL_PCT}%"
else
    info "Available RAM: ${AVAIL_PCT}%"
fi
echo ""

# --- Docker Containers ---
echo "[DOCKER CONTAINERS]"
if command -v docker &>/dev/null; then
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || warn "Docker not accessible (run as deploy or root)"
    echo ""

    # Check all expected containers
    EXPECTED_CONTAINERS="monteastur-app monteastur-nginx monteastur-mysql monteastur-prometheus monteastur-grafana monteastur-uptime-kuma"
    for c in $EXPECTED_CONTAINERS; do
        STATUS=$(docker ps --filter "name=$c" --format "{{.Status}}" 2>/dev/null)
        if [ -z "$STATUS" ]; then
            error "Container $c is not running"
            EXIT_CODE=1
        else
            info "Container $c: $STATUS"
        fi
    done
else
    warn "Docker not installed"
fi
echo ""

# --- App Healthcheck ---
echo "[APP HEALTHCHECK]"
HEALTH_URL="http://localhost/actuator/health"
HEALTH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
if [ "$HEALTH_STATUS" = "200" ]; then
    INFO=$(curl -s "$HEALTH_URL" 2>/dev/null)
    info "App healthcheck: HTTP $HEALTH_STATUS — $INFO"
else
    error "App healthcheck: HTTP $HEALTH_STATUS (expected 200)"
    EXIT_CODE=1
fi
echo ""

# --- Docker Disk Usage ---
echo "[DOCKER DISK USAGE]"
if command -v docker &>/dev/null; then
    docker system df 2>/dev/null || warn "Docker not accessible"
else
    warn "Docker not installed"
fi
echo ""

echo "============================================"
if [ "$EXIT_CODE" -eq 0 ]; then
    info "All checks passed"
else
    error "Some checks failed"
fi
echo "============================================"

exit "$EXIT_CODE"
