# BACKUP_RESTORE_REVIEW

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/revision-backups-restore
Fase: 2 — Calidad operativa
Tipo: revision documental de backups y restores
```

---

## Proposito

Este documento revisa los scripts de backup y restore del proyecto antes de aplicar cambios tecnicos.

El objetivo es comprobar si la aplicacion no solo puede desplegarse, sino tambien recuperarse con seguridad cuando algo falla.

---

## Scripts revisados

```text
scripts/backup-db.sh
scripts/backup-uploads.sh
scripts/restore-db.sh
scripts/restore-uploads.sh
```

Tambien existen equivalentes PowerShell:

```text
scripts/backup-db.ps1
scripts/backup-uploads.ps1
scripts/restore-db.ps1
scripts/restore-uploads.ps1
```

---

## backup-db.sh

### Funcion

Crea un dump de MySQL desde el contenedor:

```text
monteastur-mysql
```

Guarda el backup en:

```text
backup/db/YYYY-MM-DD_HH-MM.sql.gz
```

### Fortalezas

```text
Comprueba existencia de .env.
Carga variables desde .env.
Valida MYSQL_ROOT_PASSWORD y MYSQL_DATABASE.
Crea directorio backup/db si no existe.
Genera dump con mysqldump.
Comprime con gzip.
Muestra tamaño final del backup.
```

### Riesgos

```text
No valida si el contenedor monteastur-mysql esta corriendo antes de ejecutar mysqldump.
No hay politica de retencion.
No hay verificacion automatica del archivo generado.
No hay prueba automatica de restore.
Usa root para el dump.
```

---

## backup-uploads.sh

### Funcion

Crea backup de uploads desde:

```text
monteastur-app:/app/uploads
```

Guarda el backup en:

```text
backup/uploads/YYYY-MM-DD_HH-MM.tar.gz
```

### Fortalezas

```text
Crea backup/uploads si no existe.
Genera tar.gz.
Muestra tamaño final.
```

### Riesgos

```text
No valida si el contenedor monteastur-app esta corriendo.
No valida si /app/uploads existe.
No hay politica de retencion.
No hay verificacion automatica del tar.gz generado.
```

---

## restore-db.sh

### Funcion

Restaura una base de datos desde archivo:

```text
.sql
.sql.gz
```

Contra el contenedor:

```text
monteastur-mysql
```

### Fortalezas

```text
Exige argumento.
Comprueba que el archivo existe.
Comprueba .env.
Carga variables desde .env.
Soporta .gz y .sql.
```

### Riesgos importantes

```text
No crea backup previo automatico antes de restaurar.
No pide confirmacion explicita antes de una accion destructiva.
No valida que MYSQL_DATABASE este definido antes de restaurar.
No valida que monteastur-mysql este corriendo.
```

### Riesgo P1

```text
Una restauracion incorrecta podria sobrescribir el estado actual sin copia previa automatica.
```

---

## restore-uploads.sh

### Funcion

Restaura uploads desde un `.tar.gz`.

### Fortalezas

```text
Exige argumento.
Comprueba que el archivo existe.
Crea backup previo automatico de uploads actuales.
Restaura despues de guardar el estado anterior.
```

### Riesgos

```text
No valida que monteastur-app este corriendo.
No valida integridad del tar.gz antes de restaurar.
No pide confirmacion explicita antes de restaurar.
```

---

## Hallazgo principal

Los backups existen y son utiles, pero la parte mas debil esta en:

```text
restore-db.sh
```

porque restaura la base de datos sin crear primero un backup automatico del estado actual.

En cambio:

```text
restore-uploads.sh
```

si crea un backup previo antes de restaurar uploads.

---

## Riesgos prioritarios

### P1 — Restore DB sin backup previo

Impacto:

```text
Alto. Una restauracion equivocada puede reemplazar datos actuales sin red de seguridad.
```

Recomendacion:

```text
Modificar restore-db.sh para crear un backup previo automatico antes de restaurar.
```

---

### P1 — Sin prueba real de restore

Impacto:

```text
Medio-alto. Un backup no probado no garantiza recuperacion real.
```

Recomendacion:

```text
Documentar una prueba periodica de restore en entorno controlado.
```

---

### P2 — Sin retencion

Impacto:

```text
Medio. Los backups pueden crecer sin control.
```

Recomendacion:

```text
Agregar politica de retencion futura, por ejemplo conservar ultimos 7/14/30 backups.
```

---

### P2 — Sin validacion de contenedores

Impacto:

```text
Medio. Los scripts fallaran igualmente, pero con mensajes menos claros.
```

Recomendacion:

```text
Agregar validacion previa de contenedores monteastur-mysql y monteastur-app.
```

---

## Primer cambio tecnico recomendado

Crear una rama:

```text
feature/restore-db-backup-previo
```

Objetivo:

```text
Modificar scripts/restore-db.sh para crear backup previo automatico antes de restaurar.
```

Criterio de aceptacion:

```text
Antes de restaurar, restore-db.sh crea un dump comprimido del estado actual en backup/db/_pre-restore-YYYY-MM-DD_HH-MM-SS.sql.gz.
Si el backup previo falla, el restore no continua.
```

No mezclar con:

```text
retencion
uploads
PowerShell
Docker Compose
deploy
rollback
```

---

## Segundo cambio recomendado posterior

```text
feature/validar-contenedores-backup-restore
```

Objetivo:

```text
Validar contenedores antes de backup/restore para mejorar mensajes de error.
```

---

## Tercer cambio recomendado posterior

```text
feature/backup-retention-policy
```

Objetivo:

```text
Agregar retencion controlada para evitar crecimiento infinito de backups.
```

---

## Checklist de recuperacion recomendado

```text
[ ] Generar backup DB.
[ ] Generar backup uploads.
[ ] Guardar backups fuera del VPS o en ruta protegida.
[ ] Probar restore DB en entorno controlado.
[ ] Probar restore uploads en entorno controlado.
[ ] Documentar fecha de ultima prueba de restore.
[ ] Revisar tamaño de backups.
[ ] Revisar politica de retencion.
```

---

## Decision actual

```text
Estado: revision backup/restore creada
Riesgo general: medio controlado
Siguiente paso: aplicar backup previo automatico en restore-db.sh
```

---

## Frase guia

Backup no es tener un archivo.

Backup es poder volver con vida cuando el sistema cae.
