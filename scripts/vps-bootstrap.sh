#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — VPS Bootstrap
# =============================================
# Uso: sudo ./scripts/vps-bootstrap.sh
#
# Instala Docker, Docker Compose plugin,
# crea directorios, configura UFW.
# =============================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# Check root
if [ "$(id -u)" -ne 0 ]; then
    error "Ejecutar como root: sudo ./scripts/vps-bootstrap.sh"
fi

# --- System update ---
info "Actualizando sistema..."
apt update && apt upgrade -y

# --- Install packages ---
info "Instalando Docker, Docker Compose, Git, curl..."
apt install -y \
    docker.io \
    docker-compose-v2 \
    git \
    curl \
    wget \
    unzip

# --- Enable Docker ---
info "Habilitando Docker..."
systemctl enable --now docker

# --- Verify ---
info "Verificando instalación..."
docker --version
docker compose version

# --- Create directories ---
info "Creando directorios en /opt/monteastur..."
mkdir -p /opt/monteastur
mkdir -p /opt/monteastur/backups
mkdir -p /opt/monteastur/logs

# --- Permissions ---
info "Ajustando permisos..."
chmod 755 /opt/monteastur
chmod 755 /opt/monteastur/backups
chmod 755 /opt/monteastur/logs

# --- UFW ---
info "Configurando UFW..."
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH'
ufw allow 80/tcp comment 'HTTP'
ufw allow 443/tcp comment 'HTTPS'
ufw --force enable

info "UFW status:"
ufw status verbose

# --- Summary ---
echo ""
info "============================================"
info " Bootstrap completado!"
info "============================================"
info "Próximos pasos:"
info "  1. Crear usuario deploy:"
info "     adduser deploy"
info "     usermod -aG docker deploy"
info "  2. Configurar SSH: nano /etc/ssh/sshd_config"
info "     - PermitRootLogin no"
info "     - PasswordAuthentication no"
info "  3. Clonar repositorio:"
info "     cd /opt && git clone <repo> monteastur"
info "  4. Configurar .env: cp .env.example .env && nano .env"
info "  5. Levantar stack: docker compose up -d --build"
info "============================================"
