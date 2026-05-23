# Monteastur Envios — Scripts de Backup y Restore

## Requisitos
- Docker y Docker Compose instalados
- Contenedores en ejecución: `monteastur-mysql` y `monteastur-app`
- Archivo `.env` en la raíz del proyecto

## Uso

### Backup Base de Datos
```bash
# Linux/Mac
./scripts/backup-db.sh
# Genera: backup/db/YYYY-MM-DD_HH-mm.sql.gz

# Windows PowerShell
.\scripts\backup-db.ps1
# Genera: backup/db/YYYY-MM-DD_HH-mm.sql.zip
```

### Restore Base de Datos
```bash
# Linux/Mac
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz

# Windows PowerShell
.\scripts\restore-db.ps1 backup\db\2026-05-23_14-00.sql.zip
```

### Backup Uploads
```bash
# Linux/Mac
./scripts/backup-uploads.sh
# Genera: backup/uploads/YYYY-MM-DD_HH-mm.tar.gz

# Windows PowerShell
.\scripts\backup-uploads.ps1
```

### Restore Uploads
```bash
# Linux/Mac
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz

# Windows PowerShell
.\scripts\restore-uploads.ps1 backup\uploads\2026-05-23_14-00.tar.gz
```

## Automatización (Linux VPS)

Añadir al crontab para backup diario automático:

```bash
# Backup diario a las 3:00 AM
0 3 * * * /path/to/project/scripts/backup-db.sh
0 4 * * * /path/to/project/scripts/backup-uploads.sh

# Rotación: eliminar backups > 30 días
0 5 * * * find /path/to/project/backup/db -name "*.sql.gz" -mtime +30 -delete
0 5 * * * find /path/to/project/backup/uploads -name "*.tar.gz" -mtime +30 -delete
```

## Notas
- Los backups se almacenan localmente en `backup/`
- Para producción, copiar backups a almacenamiento externo (S3, B2, SCP)
- El restore de uploads crea un backup previo automático antes de sobrescribir
