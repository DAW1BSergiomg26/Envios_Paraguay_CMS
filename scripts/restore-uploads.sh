#!/bin/bash
set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: $0 <backup-file.tar.gz>"
    echo "Example: $0 ../backup/uploads/2026-05-23_14-00.tar.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: file not found: $BACKUP_FILE"
    exit 1
fi

if ! docker ps --format '{{.Names}}' | grep -qx 'monteastur-app'; then
    echo "Error: container monteastur-app is not running"
    exit 1
fi

BACKUP_DIR="$(dirname "$BACKUP_FILE")"
PREVIOUS_BACKUP="$BACKUP_DIR/_pre-restore-$(date +"%Y-%m-%d_%H-%M-%S").tar.gz"

echo "Creating backup of current uploads before restore..."
docker exec monteastur-app tar czf - -C /app/uploads . > "$PREVIOUS_BACKUP"
echo "Current uploads saved to: $PREVIOUS_BACKUP"

echo "Restoring uploads from $(basename "$BACKUP_FILE")..."
cat "$BACKUP_FILE" | docker exec -i monteastur-app tar xzf - -C /app/uploads

echo "Restore completed successfully."
