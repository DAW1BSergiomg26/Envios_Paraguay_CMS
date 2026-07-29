#!/bin/bash
# Monteastur Envios — MySQL Backup Script
# Usage: ./scripts/backup-mysql.sh
# Scheduled via host cron or Docker cron container

set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-./backup}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-app_user}"
DB_PASSWORD="${DB_PASSWORD:-changeme_app}"
DB_NAME="${DB_NAME:-envios_paraguay_cms}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
FILENAME="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql.gz"

mkdir -p "${BACKUP_DIR}"

mysqldump -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}" \
  -p"${DB_PASSWORD}" "${DB_NAME}" \
  --single-transaction --routines --triggers --events \
  | gzip > "${FILENAME}"

echo "Backup created: ${FILENAME} ($(du -h "${FILENAME}" | cut -f1))"

# Rotate old backups
find "${BACKUP_DIR}" -name "${DB_NAME}_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete
echo "Cleaned backups older than ${RETENTION_DAYS} days"
