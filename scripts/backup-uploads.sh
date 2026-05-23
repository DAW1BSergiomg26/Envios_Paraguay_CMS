#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$PROJECT_DIR/backup/uploads"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M")
BACKUP_FILE="$BACKUP_DIR/$TIMESTAMP.tar.gz"

echo "Backing up uploads from monteastur-app:/app/uploads..."
docker exec monteastur-app tar czf - -C /app/uploads . > "$BACKUP_FILE"
echo "Backup created: $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"
