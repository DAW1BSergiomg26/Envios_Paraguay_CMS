#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$PROJECT_DIR/backup/db"
ENV_FILE="$PROJECT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env not found at $ENV_FILE"
    exit 1
fi

source "$ENV_FILE"

if [ -z "$MYSQL_ROOT_PASSWORD" ] || [ -z "$MYSQL_DATABASE" ]; then
    echo "Error: MYSQL_ROOT_PASSWORD or MYSQL_DATABASE not set"
    exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date +"%Y-%m-%d_%H-%M")
BACKUP_FILE="$BACKUP_DIR/$TIMESTAMP.sql"

echo "Backing up database '$MYSQL_DATABASE'..."
docker exec monteastur-mysql mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > "$BACKUP_FILE"

gzip "$BACKUP_FILE"
echo "Backup created: ${BACKUP_FILE}.gz ($(du -h "${BACKUP_FILE}.gz" | cut -f1))"
