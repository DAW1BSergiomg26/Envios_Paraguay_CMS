# POST_DEPLOY_CHECKS_REVIEW

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-scripts-deploy-rollback
Fase: 2 — Calidad operativa
Tipo: revision documental de smoke tests, post-deploy checks y healthchecks
Documento previo: docs/OPERATIONAL_SCRIPTS_AUDIT.md
```

---

## Proposito

Este documento revisa los scripts de validacion operativa que existen despues del despliegue.

El objetivo es decidir como integrarlos de forma segura en el flujo real de deploy y rollback.

---

## Scripts revisados

```text
scripts/production-smoke-test.sh
scripts/production-post-deploy-check.sh
scripts/server-healthcheck.sh
```

---

## production-smoke-test.sh

### Funcion

Ejecuta pruebas funcionales rapidas contra una URL base.

Variable principal:

```text
BASE_URL
```

Default:

```text
http://localhost
```

Endpoints revisados:

```text
/actuator/health
/
/login-react
/tracking
/seguimiento como alternativa si /tracking falla
```

### Fortalezas

```text
Usa set -euo pipefail.
Permite BASE_URL configurable.
Comprueba health con status UP.
Comprueba home publica.
Comprueba tracking.
Devuelve exit "$EXIT_CODE".
```

### Riesgos / dudas

```text
/login-react aparece como ruta testeada, pero no se confirmo aun como ruta principal en el mapa de controllers.
Algunas rutas pueden ser heredadas o SPA.
```

### Decision

```text
Mantener como smoke test funcional.
Antes de endurecerlo, confirmar si /login-react es ruta oficial, heredada o reemplazada.
```

---

## production-post-deploy-check.sh

### Funcion

Ejecuta revision operativa amplia tras despliegue.

Comprueba:

```text
contenedores Docker esperados
healthcheck de la app
espacio en disco
memoria disponible
logs recientes app/nginx
Prometheus
Grafana
Uptime Kuma
```

### Fortalezas

```text
Usa set -euo pipefail.
Controla EXIT_CODE.
Devuelve exit "$EXIT_CODE".
Detecta contenedores caidos.
Detecta healthcheck fallido.
Detecta disco critico.
Detecta RAM critica.
Revisa logs recientes.
Tiene en cuenta monitoring local.
```

### Riesgos / dudas

```text
Debe confirmarse si deploy-prod.sh lo ejecuta actualmente.
Si no se ejecuta, queda como herramienta buena pero separada.
Los contenedores esperados incluyen Prometheus, Grafana y Uptime Kuma; si en algun entorno se desactivan, podria fallar.
```

### Decision

```text
Es el mejor candidato para integrarse al final de deploy-prod.sh.
Si falla, el deploy debe fallar.
```

---

## server-healthcheck.sh

### Funcion

Revisa estado general del servidor VPS.

Comprueba:

```text
uptime
disco
RAM
contenedores Docker
app healthcheck
docker system df
```

### Fortalezas

```text
Devuelve exit "$EXIT_CODE".
Es util para diagnostico manual.
Cubre recursos basicos del VPS.
Cubre contenedores y app healthcheck.
```

### Riesgos / dudas

```text
No necesariamente debe ejecutarse en cada deploy.
Puede ser mejor como comando manual o programado.
```

### Decision

```text
Mantener como herramienta de diagnostico manual.
No integrarlo automaticamente en deploy-prod.sh por ahora.
```

---

## Hallazgo principal

El proyecto ya tiene scripts de validacion que fallan correctamente con:

```bash
exit "$EXIT_CODE"
```

El problema no es la ausencia de checks.

El problema es que `deploy-prod.sh` y `rollback-prod.sh` hacen un healthcheck propio mas debil y no parecen aprovechar todavia los scripts avanzados.

---

## Decision recomendada

### Para deploy-prod.sh

Integrar al final:

```bash
./scripts/production-post-deploy-check.sh
```

Motivo:

```text
Es mas completo que el healthcheck simple actual.
Si falla, devuelve exit 1 y el deploy queda marcado como fallido.
```

---

### Para rollback-prod.sh

No integrar todavia de golpe.

Primero conviene decidir si rollback debe:

```text
hacer solo healthcheck minimo
hacer post-deploy completo
hacer smoke test funcional
```

Motivo:

```text
Rollback debe ser rapido y predecible. No conviene hacerlo pesado sin decision previa.
```

---

## Primer cambio tecnico recomendado

Crear una rama posterior:

```text
feature/integrar-postdeploy-check-en-deploy
```

Objetivo:

```text
Modificar deploy-prod.sh para ejecutar production-post-deploy-check.sh al final.
```

Criterio de aceptacion:

```text
Si production-post-deploy-check.sh falla, deploy-prod.sh debe fallar.
```

No mezclar con:

```text
rollback-prod.sh
backups
restore
E2E
Docker Compose
Nginx
React
```

---

## Segundo cambio recomendado posterior

Crear otra rama independiente:

```text
feature/healthcheck-rollback-falla-si-no-responde
```

Objetivo:

```text
Hacer que rollback-prod.sh falle con exit 1 si su healthcheck minimo no responde.
```

---

## Riesgos pendientes

```text
[ ] Confirmar si /login-react sigue siendo ruta oficial.
[ ] Confirmar si production-post-deploy-check.sh funciona bien en VPS real.
[ ] Confirmar si todos los entornos levantan monitoring.
[ ] Decidir nivel de validacion deseado en rollback.
```

---

## Decision actual

```text
Estado: revision post-deploy creada
Riesgo general: medio controlado
Siguiente paso: integrar production-post-deploy-check.sh en deploy-prod.sh
```

---

## Frase guia

Una aplicacion no esta desplegada porque arranco.

Esta desplegada cuando responde, resiste y puede ser revisada sin miedo.
