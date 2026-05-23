# Monteastur Envios — Backup y Restore

## Visi\u00f3n General

El sistema de backup protege dos componentes cr\u00edticos:

1. **Base de datos MySQL** — datos de env\u00edos, clientes, tracking
2. **Uploads** — im\u00e1genes de evidencias, galer\u00eda CMS, documentos

Los scripts est\u00e1n disponibles en `scripts/` en formato `.sh` (Linux) y `.ps1` (Windows).

## Backup Base de Datos

### Linux (VPS)
```bash
cd /opt/monteastur
./scripts/backup-db.sh
```

Genera: `backup/db/YYYY-MM-DD_HH-mm.sql.gz`

### Windows (desarrollo)
```powershell
.\scripts\backup-db.ps1
```

Genera: `backup\db\YYYY-MM-DD_HH-mm.sql.zip`

### Automatizaci\u00f3n diaria
```cron
0 3 * * * /opt/monteastur/scripts/backup-db.sh
0 5 * * * find /opt/monteastur/backup/db -name "*.sql.gz" -mtime +30 -delete
```

## Restore Base de Datos

### Linux
```bash
./scripts/restore-db.sh backup/db/2026-05-23_14-00.sql.gz
```

### Windows
```powershell
.\scripts\restore-db.ps1 backup\db\2026-05-23_14-00.sql.zip
```

### Verificaci\u00f3n post-restore
```bash
# Verificar que la app responde
curl http://localhost/actuator/health

# Verificar login admin
curl -X POST http://localhost/login -d "username=admin&password=..."

# Verificar datos (usando API)
curl http://localhost/api/v1/admin/envios?page=0&size=1
```

## Backup Uploads

### Linux
```bash
./scripts/backup-uploads.sh
```

Genera: `backup/uploads/YYYY-MM-DD_HH-mm.tar.gz`

### Windows
```powershell
.\scripts\backup-uploads.ps1
```

## Restore Uploads

El restore **crea autom\u00e1ticamente un backup previo** de los uploads actuales antes de sobrescribir.

### Linux
```bash
./scripts/restore-uploads.sh backup/uploads/2026-05-23_14-00.tar.gz
```

### Windows
```powershell
.\scripts\restore-uploads.ps1 backup\uploads\2026-05-23_14-00.tar.gz
```

### Verificaci\u00f3n post-restore
```bash
# Verificar archivos en el contenedor
docker exec monteastur-app ls -la /app/uploads/
docker exec monteastur-app find /app/uploads -type f | wc -l
```

## Buenas Pr\u00e1cticas

- **Probar restores peri\u00f3dicamente**: Un backup que no se prueba no es un backup
- **Copia fuera del servidor**: Sincronizar `backup/` a S3, B2 o servidor externo
- **Retenci\u00f3n m\u00ednima**: 7 d\u00edas diarios + 4 semanas mensuales
- **Encriptaci\u00f3n**: Para datos sensibles, usar `gpg` o similar:

```bash
gpg --symmetric --cipher-algo AES256 backup/db/2026-05-23.sql.gz
```

- **Alertas**: Configurar notificaci\u00f3n si backup falla:

```bash
# Ejemplo con webhook Slack
./scripts/backup-db.sh || curl -X POST -H "Content-type: application/json" \
  --data '{"text":"BACKUP DB FALLIDO"}' https://hooks.slack.com/services/...
```

## Restore Completo (disaster recovery)

En caso de p\u00e9rdida total del servidor:

```bash
# 1. Aprovisionar VPS nuevo
# 2. Instalar Docker + clonar repo
# 3. Restaurar .env desde backup externo
# 4. Iniciar servicios sin esquema
docker compose up -d db

# 5. Restaurar base de datos
./scripts/restore-db.sh backup/db/ultimo-backup.sql.gz

# 6. Restaurar uploads
./scripts/restore-uploads.sh backup/uploads/ultimo-backup.tar.gz

# 7. Iniciar app
docker compose up -d app nginx

# 8. Verificar
curl http://localhost/actuator/health
```

## Estructura de archivos

```
backup/
├── db/
│   ├── .gitkeep
│   ├── 2026-05-20_03-00.sql.gz    # Backup diario
│   └── 2026-05-21_03-00.sql.gz
└── uploads/
    ├── .gitkeep
    ├── 2026-05-20_04-00.tar.gz    # Backup diario
    └── 2026-05-21_04-00.tar.gz
```
