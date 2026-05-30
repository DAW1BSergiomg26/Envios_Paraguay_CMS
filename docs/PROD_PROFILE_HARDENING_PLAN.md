# PROD_PROFILE_HARDENING_PLAN

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/hardening-secretos-perfil-prod
Fase: 1 — Hardening secretos y perfil prod
Tipo: plan documental sin cambios de codigo
Documento previo: docs/SECRET_HISTORY_AUDIT.md
```

---

## Proposito

Este documento define el plan para asegurar que `Envios_Paraguay_CMS` arranque en produccion con el perfil correcto, variables obligatorias y configuracion segura.

No modifica codigo. Sirve como guia antes de aplicar cambios tecnicos.

---

## Problema principal

El proyecto tiene dos capas de configuracion:

```text
src/main/resources/application.properties       → desarrollo/local
src/main/resources/application-prod.properties  → produccion
```

Riesgo:

```text
Si produccion arranca sin SPRING_PROFILES_ACTIVE=prod, podria usar valores pensados para desarrollo.
```

---

## Objetivo de hardening

Garantizar que en produccion:

```text
SPRING_PROFILES_ACTIVE=prod este definido.
DB_DDL_AUTO use validate o none.
JPA_SHOW_SQL este desactivado.
ADMIN_USERNAME este definido por variable de entorno.
ADMIN_PASSWORD este definido por variable de entorno segura.
DB_USERNAME y DB_PASSWORD esten definidos.
SPRING_DATASOURCE_URL apunte al servicio correcto.
.env no se suba a Git.
.env.production.example siga siendo plantilla, no secreto real.
```

---

## Variables obligatorias en produccion

```text
SPRING_PROFILES_ACTIVE=prod
PORT=8080
SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&zeroDateTimeBehavior=convertToNull
DB_USERNAME=casarural_user
DB_PASSWORD=CHANGE_ME_DB_PASSWORD
ADMIN_USERNAME=CHANGE_ME_ADMIN
ADMIN_PASSWORD=CHANGE_ME_ADMIN_PASSWORD
DB_DDL_AUTO=validate
UPLOAD_DIR=/app/uploads
LOG_DIR=/app/logs
```

Opcionales segun despliegue:

```text
SMTP_HOST
SMTP_PORT
SMTP_USERNAME
SMTP_PASSWORD
SMTP_FROM
SMTP_TO
GRAFANA_ADMIN_USER
GRAFANA_ADMIN_PASSWORD
PROMETHEUS_PORT
GRAFANA_PORT
UPTIME_KUMA_PORT
SSL_EMAIL
BACKUP_PATH
TZ
```

---

## Reglas de produccion

### Regla 1 — Perfil prod obligatorio

```text
Toda ejecucion real debe usar SPRING_PROFILES_ACTIVE=prod.
```

Criterio:

```text
Si no esta activo el perfil prod, no se considera despliegue valido.
```

---

### Regla 2 — Sin credenciales por defecto

```text
No usar admin/admin123, changeme, CHANGE_ME o admin123 como valores reales.
```

Criterio:

```text
Los placeholders solo viven en archivos example.
```

---

### Regla 3 — Hibernate seguro

```text
DB_DDL_AUTO=validate
```

No usar en produccion estable:

```text
update
create
create-drop
```

---

### Regla 4 — Logs SQL desactivados

```text
spring.jpa.show-sql=false
```

Criterio:

```text
No exponer consultas ni ruido innecesario en logs de produccion.
```

---

### Regla 5 — Secrets fuera del repo

```text
.env nunca debe commitearse.
GitHub Secrets debe guardar claves de despliegue.
Los ejemplos deben contener placeholders, no valores reales.
```

---

## Smoke tests recomendados

Despues de levantar produccion:

```powershell
# Health
curl http://localhost:8080/actuator/health

# Home publica
curl http://localhost:8080/

# Login admin
curl -I http://localhost:8080/login

# Tracking publico con codigo inexistente
curl -I http://localhost:8080/api/v1/tracking/NO_EXISTE
```

Checklist esperado:

```text
/actuator/health responde UP o estado controlado.
/ responde sin error 500.
/login responde 200 o redirect controlado.
/api/v1/tracking/NO_EXISTE responde 404 controlado.
```

---

## Validacion recomendada en Docker

Antes de despliegue real:

```powershell
docker compose config
```

Objetivo:

```text
Verificar que docker-compose resuelve variables sin mostrar secretos en commits.
```

---

## Riesgos pendientes

### P1 — No hay comprobacion automatica visible del perfil activo

Recomendacion:

```text
Documentar en runbook como verificar que prod esta activo.
```

### P1 — Defaults dev existen en application.properties

Recomendacion:

```text
Mantenerlos para local o endurecerlos en una rama tecnica posterior.
```

### P1 — .env.production.example debe ser plantilla limpia

Recomendacion:

```text
Revisar que todos los valores reales esten sustituidos por CHANGE_ME o placeholders seguros.
```

---

## Primer cambio tecnico recomendado despues de esta documentacion

Crear una verificacion documental o script ligero:

```text
scripts/check-prod-env.ps1
```

Objetivo:

```text
Comprobar que variables criticas existen antes de deploy.
```

Variables minimas:

```text
SPRING_PROFILES_ACTIVE
DB_USERNAME
DB_PASSWORD
ADMIN_USERNAME
ADMIN_PASSWORD
DB_DDL_AUTO
```

No hacerlo todavia hasta cerrar esta fase documental o decidir abrir subrama tecnica.

---

## Checklist de cierre

```text
[ ] SECRET_HISTORY_AUDIT creado.
[x] PROD_PROFILE_HARDENING_PLAN creado.
[ ] Confirmar resultado local de busqueda de secretos.
[ ] Revisar docker-compose.yml.
[ ] Revisar deploy-prod.yml.
[ ] Crear checklist de arranque seguro.
[ ] Decidir si se crea script check-prod-env.ps1.
```

---

## Decision actual

```text
Estado: plan de perfil prod creado
Riesgo general: medio controlado
Siguiente paso: revisar docker-compose.yml y workflow deploy-prod.yml
```

---

## Frase guia

Produccion no debe depender de la suerte.

Debe arrancar con perfil claro, secretos fuera del repo y fallos visibles antes del desastre.
