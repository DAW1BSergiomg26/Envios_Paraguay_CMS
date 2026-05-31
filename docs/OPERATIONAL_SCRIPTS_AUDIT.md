# OPERATIONAL_SCRIPTS_AUDIT

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-scripts-deploy-rollback
Fase: 2 — Calidad operativa
Tipo: auditoria documental de scripts operativos
```

---

## Proposito

Este documento abre la Fase 2 de calidad operativa.

El objetivo es revisar scripts de despliegue, rollback, backup, restore, healthcheck, smoke test y preparacion de VPS antes de modificarlos.

---

## Scripts detectados

```text
backup-db.ps1
backup-db.sh
backup-uploads.ps1
backup-uploads.sh
check-ssh-connection.sh
deploy-prod.sh
predeploy-check.ps1
predeploy-check.sh
production-post-deploy-check.sh
production-smoke-test.sh
README.md
restore-db.ps1
restore-db.sh
restore-uploads.ps1
restore-uploads.sh
rollback-prod.sh
run-e2e-local.ps1
run-e2e-local.sh
server-healthcheck.sh
vps-bootstrap.sh
```

Lectura inicial:

```text
La carpeta scripts ya contiene una base operativa fuerte.
No es un proyecto sin herramientas: ya existen piezas para deploy, rollback, backup, restore, smoke test, healthcheck, E2E y VPS bootstrap.
```

---

## deploy-prod.sh — Resumen

Acciones detectadas:

```text
set -euo pipefail
comprueba .env
comprueba .git
git pull
docker compose pull
docker compose build
docker compose up -d
docker image prune -f
docker ps
healthcheck contra http://localhost/actuator/health
```

Fortalezas:

```text
Falla ante variables no definidas o errores de shell.
Valida existencia de .env.
Valida que esta dentro de repo Git.
Usa Docker Compose.
Incluye healthcheck basico.
Muestra contenedores activos.
```

Riesgos:

```text
No se observa backup automatico antes del deploy.
No se observa rollback automatico si healthcheck falla.
El healthcheck avisa, pero no parece terminar con exit 1 si falla tras todos los intentos.
No se observa tag/checkpoint automatico previo al deploy.
```

---

## rollback-prod.sh — Resumen

Acciones detectadas:

```text
set -euo pipefail
exige un TAG como argumento
comprueba .env
comprueba .git
git fetch --tags
valida existencia del tag
git checkout TAG
docker compose up -d --build
docker image prune -f
docker ps
healthcheck contra http://localhost/actuator/health
```

Fortalezas:

```text
Exige tag explicito.
Muestra tags disponibles si no se pasa argumento.
Valida existencia del tag.
Reconstruye y levanta servicios.
Incluye healthcheck basico.
```

Riesgos:

```text
Hace checkout a tag y deja el repo en detached HEAD.
El healthcheck avisa, pero no parece cortar con exit 1 si falla.
No se observa restauracion automatica de base de datos o uploads.
No se observa confirmacion explicita antes de rollback destructivo.
```

---

## Backups y restore

Scripts detectados:

```text
backup-db.sh
backup-db.ps1
backup-uploads.sh
backup-uploads.ps1
restore-db.sh
restore-db.ps1
restore-uploads.sh
restore-uploads.ps1
```

Lectura inicial:

```text
Existe estrategia de backup/restore multiplataforma.
Hay que revisar contenido antes de confiar en ella para produccion.
```

Riesgos a revisar:

```text
Ubicacion real de backups.
Nombre de contenedor MySQL usado.
Variables de entorno requeridas.
Compresion.
Retencion.
Prueba real de restauracion.
```

---

## Healthchecks y smoke tests

Scripts detectados:

```text
production-post-deploy-check.sh
production-smoke-test.sh
server-healthcheck.sh
predeploy-check.sh
predeploy-check.ps1
```

Lectura inicial:

```text
Existe una buena base para validar predeploy y postdeploy.
La siguiente auditoria debe revisar si deploy-prod.sh los llama o si quedan aislados.
```

Riesgo:

```text
Tener buenos scripts separados no garantiza seguridad operativa si no estan integrados en el flujo principal.
```

---

## E2E local

Scripts detectados:

```text
run-e2e-local.sh
run-e2e-local.ps1
```

Lectura inicial:

```text
La existencia de scripts E2E confirma madurez de pruebas frontend.
Pendiente revisar si se usan en CI/CD o solo localmente.
```

---

## Riesgos prioritarios

### P1 — Healthcheck no bloqueante

```text
Si el healthcheck falla despues de todos los intentos, deploy-prod.sh y rollback-prod.sh parecen continuar sin exit 1 final.
```

Impacto:

```text
Un deploy podria quedar marcado como completado aunque la app no este sana.
```

Recomendacion:

```text
Crear rama tecnica para hacer que el healthcheck falle con exit 1 si no responde tras N intentos.
```

---

### P1 — Deploy sin backup previo visible

```text
deploy-prod.sh no parece ejecutar backup-db.sh ni backup-uploads.sh antes del despliegue.
```

Impacto:

```text
Si el deploy cambia estado o datos y algo falla, la recuperacion puede ser mas dificil.
```

Recomendacion:

```text
Revisar scripts de backup antes de integrarlos automaticamente.
```

---

### P1 — Rollback deja detached HEAD

```text
rollback-prod.sh hace git checkout TAG.
```

Impacto:

```text
Es normal para rollback a tag, pero puede confundir operaciones posteriores si no esta documentado.
```

Recomendacion:

```text
Documentar procedimiento de vuelta a develop y validar que el script lo explica bien.
```

---

### P2 — Scripts avanzados no integrados

```text
production-smoke-test.sh y production-post-deploy-check.sh existen, pero hay que confirmar si deploy-prod.sh los ejecuta.
```

Impacto:

```text
Puede haber validaciones buenas pero no usadas en el flujo real.
```

---

## Orden recomendado de auditoria

```text
1. Revisar production-smoke-test.sh.
2. Revisar production-post-deploy-check.sh.
3. Revisar server-healthcheck.sh.
4. Revisar backup-db.sh y backup-uploads.sh.
5. Revisar restore-db.sh y restore-uploads.sh.
6. Decidir primer cambio tecnico pequeño.
```

---

## Primer cambio tecnico recomendado

Crear una rama especifica:

```text
feature/healthcheck-deploy-falla-si-no-responde
```

Objetivo:

```text
Modificar deploy-prod.sh y rollback-prod.sh para que fallen con exit 1 si el healthcheck no responde tras todos los intentos.
```

Motivo:

```text
Es un cambio pequeño, de alto valor y facil de probar.
```

No mezclar con:

```text
backups
restore
E2E
Docker Compose
Nginx
UI
React
```

---

## Siguiente documento recomendado

```text
docs/POST_DEPLOY_CHECKS_REVIEW.md
```

Objetivo:

```text
Revisar production-smoke-test.sh, production-post-deploy-check.sh y server-healthcheck.sh.
```

---

## Decision actual

```text
Estado: auditoria inicial de scripts operativos creada
Riesgo general: medio controlado
Siguiente paso: revisar scripts de postdeploy/smoke/healthcheck
```

---

## Frase guia

Un deploy no termina cuando Docker levanta contenedores.

Termina cuando la aplicacion responde, los datos estan seguros y hay camino de regreso.
