#!/bin/bash
set -euo pipefail

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
BACKUP_DIR="$PROJECT_DIR/backup/db"

if [ ! -f "$ENV_FILE" ]; then
    echo "Error: .env not found at $ENV_FILE"
    exit 1
fi

source "$ENV_FILE"

if [ -z "${MYSQL_ROOT_PASSWORD:-}" ] || [ -z "${MYSQL_DATABASE:-}" ]; then
    echo "Error: MYSQL_ROOT_PASSWORD or MYSQL_DATABASE not set"
    exit 1
fi

mkdir -p "$BACKUP_DIR"

PRE_RESTORE_TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
PRE_RESTORE_FILE="$BACKUP_DIR/_pre-restore-${PRE_RESTORE_TIMESTAMP}.sql"


echo "Creating pre-restore database backup for '$MYSQL_DATABASE'..."
docker exec monteastur-mysql mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > "$PRE_RESTORE_FILE"
gzip "$PRE_RESTORE_FILE"
echo "Pre-restore backup created: ${PRE_RESTORE_FILE}.gz ($(du -h "${PRE_RESTORE_FILE}.gz" | cut -f1))"

echo "Restoring database '$MYSQL_DATABASE' from $(basename "$BACKUP_FILE")..."

if [[ "$BACKUP_FILE" == *.gz ]]; then
    gunzip -c "$BACKUP_FILE" | docker exec -i monteastur-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"
else
    docker exec -i monteastur-mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$BACKUP_FILE"
fi

echo "Restore completed successfully."
