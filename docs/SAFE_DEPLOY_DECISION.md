# SAFE_DEPLOY_DECISION

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/hardening-secretos-perfil-prod
Fase: 1 — Hardening secretos y perfil prod
Tipo: decision documental de despliegue seguro
Documentos previos:
- docs/SECRET_HISTORY_AUDIT.md
- docs/PROD_PROFILE_HARDENING_PLAN.md
- docs/DEPLOY_SECURITY_REVIEW.md
```

---

## Proposito

Este documento toma una decision clara sobre el modelo de despliegue seguro del proyecto.

La revision previa detecto dos workflows principales:

```text
.github/workflows/deploy-prod.yml
.github/workflows/deploy.yml
```

El objetivo es evitar despliegues accidentales a produccion y definir que workflow debe usarse para cada entorno.

---

## Decision principal

```text
deploy-prod.yml debe ser el workflow oficial para produccion real.
```

Motivos:

```text
Es manual.
Requiere workflow_dispatch.
Exige confirmacion confirm=deploy.
Tiene concurrency group production-deploy.
Ejecuta validaciones previas.
Ejecuta tests backend.
Ejecuta tests frontend.
Ejecuta build frontend.
Usa GitHub Secrets.
Delega en scripts/deploy-prod.sh.
```

---

## Decision sobre deploy.yml

```text
deploy.yml no debe considerarse produccion real sin cambios.
```

Motivo:

```text
Se dispara automaticamente con push a develop.
Tiene menos validaciones que deploy-prod.yml.
No exige confirmacion manual.
No ejecuta el mismo bloque completo de tests/build.
```

---

## Politica recomendada

### Produccion real

Workflow recomendado:

```text
.github/workflows/deploy-prod.yml
```

Condiciones:

```text
Solo manual.
Solo con confirm=deploy.
Solo desde develop.
Solo despues de tests y build.
Solo con secrets configurados.
Solo con .env real en VPS.
```

---

### Staging / demo / preventa

Workflow posible:

```text
.github/workflows/deploy.yml
```

Pero requiere decision:

```text
O se limita a staging/demo.
O se convierte tambien en manual.
O se mueve a una rama staging.
O se elimina si ya no aporta valor.
```

---

## Decision recomendada para siguiente rama tecnica

Elegir esta opcion:

```text
Convertir deploy.yml en workflow manual o staging/demo, nunca produccion automatica.
```

Cambio tecnico sugerido:

```text
Eliminar trigger push a develop de deploy.yml.
Mantener workflow_dispatch.
Renombrar descripcion interna como demo/staging si aplica.
```

Motivo:

```text
Evita que un merge normal a develop dispare despliegue accidental.
```

---

## Riesgos aceptados temporalmente

### R1 — deploy.yml sigue existiendo

```text
Se acepta temporalmente mientras se decide si se transforma o desactiva.
```

### R2 — monitoring expuesto

```text
Se documenta como riesgo P1. Debe resolverse en una fase especifica.
```

### R3 — Grafana fallback admin123

```text
Se documenta como riesgo P1. Debe eliminarse o forzarse variable obligatoria.
```

---

## Cambios tecnicos recomendados, pero no aplicados aun

```text
1. Modificar deploy.yml para quitar push a develop.
2. Mantener workflow_dispatch.
3. Añadir comentario claro: staging/demo manual.
4. Revisar docker-compose.yml para eliminar fallback admin123 de Grafana.
5. Revisar exposicion de Prometheus, Grafana y Uptime Kuma.
6. Revisar scripts/deploy-prod.sh.
```

---

## Primera rama tecnica recomendada despues de esta decision

```text
feature/seguridad-workflow-deploy-manual
```

Objetivo:

```text
Evitar despliegue automatico accidental desde develop.
```

Archivos probables:

```text
.github/workflows/deploy.yml
docs/DEPLOY_SECURITY_REVIEW.md si se actualiza decision
```

No mezclar con:

```text
Grafana
Prometheus
Uptime Kuma
Spring Security
React
CSS
uploads
```

---

## Checklist de aceptacion

```text
[x] deploy-prod.yml definido como produccion real.
[x] deploy.yml no recomendado como produccion real.
[x] Riesgo de push a develop documentado.
[x] Decision de convertir deploy.yml a manual/staging documentada.
[x] Siguiente rama tecnica recomendada.
```

---

## Decision actual

```text
Estado: decision de despliegue seguro tomada
Riesgo general: medio controlado
Siguiente paso: cerrar fase documental o crear rama tecnica para deploy.yml manual
```

---

## Frase guia

Produccion no debe activarse por accidente.

Debe pedirse permiso, pasar pruebas y dejar huella.
