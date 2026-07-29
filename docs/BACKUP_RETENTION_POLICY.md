# BACKUP_RETENTION_POLICY

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/backup-retention-policy
Fase: 2 — Calidad operativa
Tipo: decision documental de retencion de backups
```

---

## Proposito

Este documento define una politica inicial de retencion para backups de base de datos y uploads.

La intencion es evitar que las carpetas de backup crezcan sin limite, pero sin aplicar borrados automaticos agresivos sin una validacion previa.

---

## Contexto

Actualmente existen backups en:

```text
backup/db
backup/uploads
```

Generados por:

```text
scripts/backup-db.sh
scripts/backup-uploads.sh
```

Y restaurados por:

```text
scripts/restore-db.sh
scripts/restore-uploads.sh
```

---

## Problema

Si los backups funcionan correctamente, con el tiempo pueden acumularse muchos archivos:

```text
*.sql.gz
*.tar.gz
_pre-restore-*.sql.gz
_pre-restore-*.tar.gz
```

Riesgo:

```text
El disco del VPS puede llenarse.
Un disco lleno puede tirar abajo la app, MySQL, Docker o los logs.
```

---

## Politica inicial recomendada

```text
Conservar los ultimos 10 backups normales de DB.
Conservar los ultimos 10 backups normales de uploads.
No borrar automaticamente backups _pre-restore-* en la primera version.
```

Motivo:

```text
Los backups _pre-restore-* se generan antes de operaciones delicadas.
No deben eliminarse sin una politica mas madura.
```

---

## Variable recomendada

```text
BACKUP_RETENTION_COUNT=10
```

Valor por defecto sugerido:

```text
10
```

---

## Regla de seguridad recomendada

Antes de borrar automaticamente, implementar modo seguro:

```text
BACKUP_RETENTION_DRY_RUN=true
```

Comportamiento:

```text
Si dry-run esta activo, el script solo muestra que archivos borraria.
Si dry-run esta desactivado explicitamente, entonces borra.
```

Ejemplo:

```bash
BACKUP_RETENTION_DRY_RUN=false ./scripts/backup-db.sh
```

---

## Rutas afectadas

### Base de datos

```text
backup/db/*.sql.gz
```

Excluir:

```text
backup/db/_pre-restore-*.sql.gz
```

### Uploads

```text
backup/uploads/*.tar.gz
```

Excluir:

```text
backup/uploads/_pre-restore-*.tar.gz
```

---

## Primer cambio tecnico recomendado

Crear una implementacion con dry-run por defecto en:

```text
scripts/backup-db.sh
scripts/backup-uploads.sh
```

Criterio:

```text
Por defecto NO borra.
Muestra archivos candidatos a borrado.
Solo borra si BACKUP_RETENTION_DRY_RUN=false.
```

---

## Cambios no recomendados todavia

```text
No aplicar retencion a restore-db.sh.
No aplicar retencion a restore-uploads.sh.
No borrar _pre-restore-* automaticamente.
No borrar backups sin log visible.
No mezclar retencion con cambios de deploy.
```

---

## Checklist de validacion

```text
[ ] Ejecutar backup-db.sh con dry-run.
[ ] Ver candidatos a borrado.
[ ] Confirmar que conserva los ultimos 10.
[ ] Confirmar que no toca _pre-restore-*.
[ ] Ejecutar backup-uploads.sh con dry-run.
[ ] Confirmar comportamiento equivalente.
[ ] Solo despues probar BACKUP_RETENTION_DRY_RUN=false en entorno controlado.
```

---

## Decision actual

```text
Estado: politica de retencion documentada
Riesgo general: medio controlado
Siguiente paso: implementar retencion con dry-run por defecto
```

---

## Frase guia

Un backup que salva datos tambien puede llenar el disco.

La solucion no es borrar a ciegas, sino retener con cabeza.
