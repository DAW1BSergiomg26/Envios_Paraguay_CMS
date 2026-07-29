#!/bin/bash
set -euo pipefail

# =============================================
# Monteastur Envios — Check SSH Connection
# =============================================
# Uso: ./scripts/check-ssh-connection.sh
#
# Verifica conexión SSH al VPS de producción
# usando los mismos parámetros que GitHub Actions.
# =============================================

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()    { echo -e "${GREEN}[OK]${NC} $1"; }
warn()    { echo -e "${YELLOW}[WARN]${NC} $1"; }
error()   { echo -e "${RED}[FAIL]${NC} $1"; }
header()  { echo -e "${CYAN}$1${NC}"; }

EXIT_CODE=0

# Default values (overridable via env vars)
SSH_HOST="${VPS_HOST:-}"
SSH_USER="${VPS_USER:-deploy}"
SSH_PORT="${VPS_PORT:-22}"
SSH_KEY="${VPS_SSH_KEY_PATH:-$HOME/.ssh/github-actions-monteastur}"

echo "============================================"
echo "  Monteastur Envios — SSH Connection Check"
echo "============================================"
echo ""

# --- Check required vars ---
if [ -z "$SSH_HOST" ]; then
    echo "  Host:    (not set — use VPS_HOST env var or edit script)"
    echo "  User:    $SSH_USER"
    echo "  Port:    $SSH_PORT"
    echo "  Key:     $SSH_KEY"
    echo ""
    error "VPS_HOST is required."
    echo ""
    echo "Usage:"
    echo "  VPS_HOST=203.0.113.10 ./scripts/check-ssh-connection.sh"
    echo "  VPS_HOST=monteastur.com ./scripts/check-ssh-connection.sh"
    exit 1
fi

echo "  Host:    $SSH_HOST"
echo "  User:    $SSH_USER"
echo "  Port:    $SSH_PORT"
echo "  Key:     $SSH_KEY"
echo ""

# --- Check SSH key exists ---
header "[CHECK] SSH key exists..."
if [ -f "$SSH_KEY" ]; then
    info "Key found: $SSH_KEY"
else
    warn "Key not found at $SSH_KEY"
    warn "Generate it first: ssh-keygen -t ed25519 -f $SSH_KEY"
    EXIT_CODE=1
fi
echo ""

# --- Check pub key exists ---
header "[CHECK] Public key exists..."
PUB_KEY="${SSH_KEY}.pub"
if [ -f "$PUB_KEY" ]; then
    info "Public key found: $PUB_KEY"
else
    warn "Public key not found at $PUB_KEY"
    EXIT_CODE=1
fi
echo ""

# --- Test SSH connection ---
header "[CHECK] Testing SSH connection..."
SSH_OPTS="-o ConnectTimeout=10 -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o BatchMode=yes"
if SSH_OUTPUT=$(ssh -i "$SSH_KEY" $SSH_OPTS -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}" "echo CONNECTED && whoami && uname -a" 2>&1); then
    info "Connection successful!"
    echo ""
    echo "  Server info:"
    echo "$SSH_OUTPUT" | while IFS= read -r line; do
        echo "    $line"
    done
else
    error "Connection failed."
    echo ""
    echo "  Error details:"
    echo "$SSH_OUTPUT" | while IFS= read -r line; do
        echo "    $line"
    done
    echo ""
    warn "Troubleshooting tips:"
    warn "  1. Verify the key is added to VPS authorized_keys"
    warn "  2. ssh-copy-id -i ${SSH_KEY}.pub ${SSH_USER}@${SSH_HOST}"
    warn "  3. Verify VPS: systemctl status sshd"
    warn "  4. Verify UFW: sudo ufw status | grep 22"
    warn "  5. Check fail2ban: sudo fail2ban-client status sshd"
    EXIT_CODE=1
fi
echo ""

# --- Check Docker access (if SSH worked) ---
if [ "$EXIT_CODE" -eq 0 ]; then
    header "[CHECK] Docker access on VPS..."
    if DOCKER_OUTPUT=$(ssh -i "$SSH_KEY" $SSH_OPTS -p "$SSH_PORT" "${SSH_USER}@${SSH_HOST}" "docker ps --format 'table {{.Names}}\t{{.Status}}'" 2>&1); then
        info "Docker accessible"
        echo ""
        echo "$DOCKER_OUTPUT" | while IFS= read -r line; do
            echo "    $line"
        done
    else
        warn "Docker check failed:"
        echo "$DOCKER_OUTPUT" | while IFS= read -r line; do
            echo "    $line"
        done
    fi
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
