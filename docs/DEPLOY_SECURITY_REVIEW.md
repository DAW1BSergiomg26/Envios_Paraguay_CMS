# DEPLOY_SECURITY_REVIEW

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/hardening-secretos-perfil-prod
Fase: 1 — Hardening secretos y perfil prod
Tipo: revision documental de despliegue y CI/CD
Documentos previos:
- docs/SECRET_HISTORY_AUDIT.md
- docs/PROD_PROFILE_HARDENING_PLAN.md
```

---

## Proposito

Este documento revisa la seguridad inicial del despliegue de `Envios_Paraguay_CMS`.

Se centra en:

```text
docker-compose.yml
.github/workflows/deploy-prod.yml
.github/workflows/deploy.yml
```

No modifica codigo ni configuracion. Solo documenta riesgos y decisiones recomendadas.

---

## Resumen ejecutivo

El proyecto ya tiene una base de despliegue avanzada:

```text
Docker Compose
MySQL
Spring Boot app
Nginx
Certbot
Prometheus
Grafana
Uptime Kuma
GitHub Actions
Deploy por SSH
```

La base es buena, pero hay riesgos importantes antes de produccion real:

```text
1. deploy.yml despliega automaticamente al hacer push a develop.
2. Grafana tiene fallback admin123 si falta variable.
3. Prometheus, Grafana y Uptime Kuma publican puertos directamente.
4. deploy-prod.yml es mas seguro que deploy.yml y deberia ser el flujo recomendado para produccion.
```

---

## docker-compose.yml — Hallazgos positivos

```text
La app fuerza SPRING_PROFILES_ACTIVE=prod.
Usa env_file: .env.
MySQL tiene volumen persistente.
La app tiene healthcheck contra /actuator/health.
Nginx separa configuracion y SSL.
Hay Certbot para SSL.
Hay Prometheus, Grafana y Uptime Kuma.
Usa restart: unless-stopped.
Usa red bridge interna backend.
Define volumenes persistentes para mysql, uploads, logs, certbot, prometheus, grafana y uptime kuma.
```

---

## docker-compose.yml — Riesgos detectados

### P1 — Grafana con fallback inseguro

Fragmento revisado:

```text
GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER:-admin}
GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD:-admin123}
```

Riesgo:

```text
Si GRAFANA_ADMIN_PASSWORD no esta definido, Grafana podria arrancar con admin123.
```

Decision recomendada:

```text
Eliminar fallback admin123 o exigir variable obligatoria en entorno de produccion.
```

Criterio de cierre:

```text
Grafana no arranca con password por defecto en produccion.
```

---

### P1 — Puertos de monitoring expuestos

Puertos detectados:

```text
Prometheus: 9090
Grafana: 3000
Uptime Kuma: 3001
```

Riesgo:

```text
Si el VPS no tiene firewall o reverse proxy protegido, estos paneles podrian quedar expuestos publicamente.
```

Decision recomendada:

```text
Proteger por firewall, VPN, basic auth, reverse proxy seguro o limitar a localhost si aplica.
```

Criterio de cierre:

```text
Monitoring no queda abierto al publico sin proteccion.
```

---

### P2 — Mem limits bajos o ajustados

Valores observados:

```text
mysql: 256m
app: 512m
nginx: 64m
prometheus: 256m
grafana: 128m
uptime-kuma: 256m
```

Riesgo:

```text
Puede ser correcto para VPS pequeño, pero conviene monitorizar memoria real.
```

Decision recomendada:

```text
Mantener por ahora y revisar con metricas reales.
```

---

## deploy-prod.yml — Hallazgos positivos

```text
Es manual con workflow_dispatch.
Exige confirmacion confirm=deploy.
Usa concurrency group production-deploy.
No cancela deploys en progreso.
Valida docker compose config.
Ejecuta mvn test.
Ejecuta npm test.
Ejecuta npm run build.
Usa GitHub Secrets para VPS_HOST, VPS_USER, VPS_SSH_KEY y VPS_PORT.
Hace checkout develop en el VPS.
Ejecuta scripts/deploy-prod.sh.
```

Decision:

```text
deploy-prod.yml debe ser el workflow recomendado para produccion real.
```

---

## deploy-prod.yml — Riesgos detectados

### P1 — docker compose config puede depender de .env

Riesgo:

```text
En GitHub Actions puede fallar si docker compose config necesita variables no disponibles.
```

Decision recomendada:

```text
Crear .env temporal de validacion con valores dummy seguros o usar variables de entorno de CI.
```

Criterio de cierre:

```text
La validacion docker compose config pasa sin secretos reales.
```

---

### P2 — No hay smoke test remoto visible despues del deploy

Riesgo:

```text
El workflow delega en scripts/deploy-prod.sh. Hay que confirmar si ese script hace smoke tests.
```

Decision recomendada:

```text
Revisar scripts/deploy-prod.sh antes de modificar workflow.
```

---

## deploy.yml — Hallazgos

Workflow:

```text
.github/workflows/deploy.yml
```

Disparadores:

```text
push a develop
workflow_dispatch
```

Comportamiento:

```text
Valida existencia de Dockerfile, docker-compose.yml, .env.example y nginx/conf.d.
Luego despliega por SSH y ejecuta docker compose up -d --build.
```

---

## deploy.yml — Riesgo principal

### P1/P0 — Deploy automatico al hacer push a develop

Riesgo:

```text
Cualquier merge o push a develop puede lanzar despliegue al VPS.
```

Impacto:

```text
Alto si develop apunta a entorno real de produccion.
Medio si apunta a staging/demo controlado.
```

Decision recomendada:

```text
No usar deploy.yml como despliegue de produccion real.
Convertirlo en staging/demo o desactivar trigger push a develop.
```

Opciones:

```text
Opcion A: dejar deploy.yml solo con workflow_dispatch.
Opcion B: mover deploy automatico a rama staging.
Opcion C: conservarlo solo para demo/preproduccion documentada.
Opcion D: eliminarlo si deploy-prod.yml lo reemplaza.
```

Criterio de cierre:

```text
Produccion no se despliega automaticamente por accidente desde develop.
```

---

## Comparacion de workflows

| Workflow | Seguridad | Uso recomendado |
|---|---|---|
| deploy-prod.yml | Alta/media | Produccion manual controlada |
| deploy.yml | Media/baja | Staging, demo o revisar/desactivar |

---

## Recomendaciones priorizadas

### P1.1 — Marcar deploy-prod.yml como flujo oficial de produccion

```text
Documentar que produccion real debe usar deploy-prod.yml.
```

### P1.2 — Revisar deploy.yml

```text
Decidir si se convierte en staging/demo o si se elimina el trigger push a develop.
```

### P1.3 — Eliminar fallback admin123 de Grafana

```text
Exigir GRAFANA_ADMIN_PASSWORD en .env real.
```

### P1.4 — Proteger puertos de monitoring

```text
Prometheus, Grafana y Uptime Kuma no deberian quedar expuestos sin proteccion.
```

### P1.5 — Revisar scripts/deploy-prod.sh

```text
Confirmar que hace backup, build, restart, healthcheck, smoke test y rollback o al menos deja rollback preparado.
```

---

## Proximo archivo recomendado

```text
docs/SAFE_DEPLOY_DECISION.md
```

Objetivo:

```text
Tomar una decision clara sobre deploy.yml vs deploy-prod.yml.
```

Debe responder:

```text
Que workflow es produccion.
Que workflow es staging/demo.
Si develop despliega automaticamente o no.
Que cambios tecnicos se haran en una rama posterior.
```

---

## Decision actual

```text
Estado: revision de despliegue creada
Riesgo general: medio-alto controlable
Siguiente paso: documentar decision segura de deploy
```

---

## Frase guia

Un deploy no falla solo cuando se rompe.

Tambien falla cuando llega demasiado facil a produccion.
