#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — Deploy Producción
# =============================================
# Uso: ./scripts/deploy-prod.sh
#
# Pull, build, up, prune, healthcheck.
# =============================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

# Check .env
if [ ! -f .env ]; then
    error ".env not found. Run: cp .env.example .env && nano .env"
fi

# Check git
if [ ! -d .git ]; then
    error "Not a git repository. Run from project root."
fi

# --- Git pull ---
info "Actualizando código..."
git pull

# --- Docker compose ---
info "Descargando imágenes..."
docker compose pull 2>/dev/null || warn "docker compose pull falló (no hay images remotas)"

info "Construyendo imágenes..."
docker compose build

info "Levantando servicios..."
docker compose up -d

# --- Prune ---
info "Limpiando imágenes no usadas..."
docker image prune -f

# --- Status ---
echo ""
info "============================================"
info "Contenedores activos:"
info "============================================"
docker ps

# --- Healthcheck ---
echo ""
info "============================================"
info "Verificando healthcheck..."
info "============================================"
sleep 5

HEALTH_URL="http://localhost/actuator/health"
for i in 1 2 3 4 5; do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
    if [ "$STATUS" = "200" ]; then
        info "Healthcheck OK (HTTP $STATUS)"
        curl -s "$HEALTH_URL" | head -c 200
        echo ""
        break
    fi
    if [ "$i" -lt 5 ]; then
        warn "Healthcheck aún no responde (intento $i/5). Esperando 5s..."
        sleep 5
    else
        warn "Healthcheck no respondió después de 5 intentos."
        warn "Verificar: docker logs monteastur-app --tail 30"
    fi
done

echo ""
info "============================================"
info " Deploy completado!"
info "============================================"
