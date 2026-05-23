#!/bin/bash
set -e

if [ $# -ne 1 ]; then
    echo "Usage: $0 <backup-file>"
    echo "Example: $0 ../backup/db/2026-05-23_14-00.sql.gz"
    exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
    echo "Error: file not found: $BACKUP_FILE"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
ENV_FILE="$PROJECT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env not found at $ENV_FILE"
    exit 1
fi

source "$ENV_FILE"

echo "Restoring database '$MYSQL_DATABASE' from $(basename "$BACKUP_FILE")..."

if [[ "$BACKUP_FILE" == *.gz ]]; then
    gunzip -c "$BACKUP_FILE" | docker exec -i monteastur-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"
else
    docker exec -i monteastur-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$BACKUP_FILE"
fi

echo "Restore completed successfully."
