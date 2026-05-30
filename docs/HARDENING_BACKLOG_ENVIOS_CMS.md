# HARDENING_BACKLOG_ENVIOS_CMS

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-inicial-envios-cms
Tipo: backlog de hardening sin cambios de codigo
Documentos previos:
- docs/AUDIT_INITIAL_ENVIOS_CMS.md
- docs/PROJECT_MAP_ENVIOS_CMS.md
- docs/ROUTE_AND_FLOW_MAP_ENVIOS_CMS.md
```

---

## Proposito

Este documento convierte los riesgos encontrados durante la auditoria inicial en un backlog priorizado de hardening.

No modifica codigo. Sirve para decidir que mejorar primero, que dejar para despues y que no tocar sin pruebas.

---

## Criterios de prioridad

```text
P0 → bloqueo critico o riesgo grave inmediato
P1 → riesgo importante antes de produccion real
P2 → mejora tecnica necesaria, pero no urgente
P3 → mejora de calidad, documentacion o refinamiento futuro
```

---

## P0 — Bloqueos criticos

### P0.1 — Verificar historial de secretos

Descripcion:

```text
.env esta ignorado, pero conviene comprobar que nunca haya sido subido al historial.
```

Accion recomendada:

```text
Revisar historial Git y GitHub para confirmar que .env no fue commiteado.
```

Comandos sugeridos:

```powershell
git log --all -- .env
git log --all --name-only | Select-String ".env"
```

Criterio de cierre:

```text
Confirmado que no hay secretos reales en historial.
```

---

## P1 — Hardening importante

### P1.1 — Perfil de produccion obligatorio

Descripcion:

```text
application.properties tiene defaults comodos para desarrollo.
application-prod.properties es mas seguro, pero produccion debe arrancar siempre con SPRING_PROFILES_ACTIVE=prod.
```

Riesgo:

```text
Si produccion arranca sin perfil prod, podria usar valores inseguros como ddl-auto update o credenciales admin por defecto.
```

Accion recomendada:

```text
Documentar y validar en runbook que SPRING_PROFILES_ACTIVE=prod es obligatorio.
```

Criterio de cierre:

```text
Runbook de deploy exige perfil prod y smoke test lo comprueba.
```

---

### P1.2 — Defaults admin de desarrollo

Descripcion:

```text
application.properties permite fallback de ADMIN_USERNAME=admin y ADMIN_PASSWORD=admin123.
```

Riesgo:

```text
Si se usa fuera de desarrollo, la seguridad queda debil.
```

Accion recomendada:

```text
Mantener solo para dev o eliminar fallback en una rama especifica de hardening tras probar.
```

Criterio de cierre:

```text
Produccion falla si no se definen credenciales admin seguras.
```

---

### P1.3 — API cliente basada en sesion manual

Descripcion:

```text
/api/v1/cliente/** no esta protegida por Spring Security, pero los endpoints revisan clienteId en HttpSession.
```

Riesgo:

```text
La proteccion depende de que todos los endpoints mantengan validacion manual correcta.
```

Accion recomendada:

```text
Crear tests para verificar 403 sin session y 403 si un cliente intenta acceder a envio ajeno.
```

Criterio de cierre:

```text
Tests automatizados cubren acceso cliente permitido y denegado.
```

---

### P1.4 — Push endpoints publicos

Descripcion:

```text
/api/v1/push/** queda publico por la regla anyRequest().permitAll().
El endpoint /api/v1/push/test parece de demo.
```

Riesgo:

```text
En produccion podria usarse para abuso, ruido o exposicion innecesaria.
```

Accion recomendada:

```text
Decidir si /test se elimina, se protege o se desactiva en prod.
```

Criterio de cierre:

```text
/test no esta abierto en produccion real.
```

---

### P1.5 — Uploads de evidencias con ruta distinta

Descripcion:

```text
Imagenes usa app.upload.dir, pero evidencias usa System.getProperty("user.dir") + /uploads/evidencias/.
```

Riesgo:

```text
Puede fallar en Docker, VPS o producir rutas inconsistentes.
```

Accion recomendada:

```text
Unificar evidencias con app.upload.dir o crear propiedad especifica app.evidencias.dir.
```

Criterio de cierre:

```text
Imagenes y evidencias usan configuracion externa coherente.
```

---

### P1.6 — Tracking publico y datos expuestos

Descripcion:

```text
/api/v1/tracking/{codigo} devuelve datos basicos del envio.
```

Riesgo:

```text
Puede exponer destinatario, origen, destino, peso o contenido si se considera sensible.
```

Accion recomendada:

```text
Decidir que campos son seguros para consulta publica y crear DTO publico especifico si hace falta.
```

Criterio de cierre:

```text
Tracking publico solo expone informacion aprobada para cliente final.
```

---

## P2 — Mejoras tecnicas necesarias

### P2.1 — Naming heredado Casa Rural / Monteastur

Descripcion:

```text
Conviven entidades y rutas de Casa Rural con el producto Monteastur Envios.
```

Riesgo:

```text
Confusion tecnica, comercial y documental.
```

Accion recomendada:

```text
Crear decision log: conservar, renombrar o separar modulos.
```

---

### P2.2 — Thymeleaf + React dashboard

Descripcion:

```text
La app tiene templates Thymeleaf, CMS, zona cliente y React dashboard compilado en static.
```

Riesgo:

```text
Duplicidad de interfaz o confusion sobre flujo oficial.
```

Accion recomendada:

```text
Documentar que pantallas son oficiales, legacy o complementarias.
```

---

### P2.3 — Build React dentro de static

Descripcion:

```text
static/react-dashboard contiene assets compilados.
```

Riesgo:

```text
Puede quedar desactualizado respecto a frontend-react/src.
```

Accion recomendada:

```text
Definir proceso oficial de build y copia.
```

---

### P2.4 — System.out.println en PushSubscriptionController

Descripcion:

```text
PushSubscriptionController usa System.out.println.
```

Riesgo:

```text
Logging poco profesional y dificil de gestionar en produccion.
```

Accion recomendada:

```text
Cambiar a logger en fase pequena de limpieza.
```

---

## P3 — Mejoras futuras

### P3.1 — Reducir duplicidad documental

Descripcion:

```text
El proyecto tiene mucha documentacion de deploy, VPS, demo, preventa y produccion.
```

Accion recomendada:

```text
Crear indice docs/README_DOCS.md o DOCS_INDEX.md.
```

---

### P3.2 — Mejorar mapa de flujos visual

Descripcion:

```text
Los flujos ya estan documentados en texto, pero podrian tener diagrama.
```

Accion recomendada:

```text
Crear diagrama Mermaid o PlantUML.
```

---

## Orden recomendado de ejecucion

```text
1. Verificar historial de secretos.
2. Revisar perfil prod obligatorio.
3. Revisar endpoints push demo.
4. Crear tests de API cliente.
5. Unificar ruta de evidencias/uploads.
6. Revisar DTO publico de tracking.
7. Documentar decision Thymeleaf + React.
8. Crear indice de documentacion.
```

---

## Primera rama tecnica recomendada despues de esta auditoria

```text
feature/hardening-secretos-perfil-prod
```

Objetivo:

```text
Verificar secretos, perfil prod, defaults admin y checklist de arranque seguro.
```

No mezclar con:

```text
React
UX/UI
uploads
testing E2E
refactor naming
```

---

## Decision actual

```text
Estado: backlog de hardening creado
Riesgo general: medio controlado
Siguiente paso: cerrar PR de auditoria inicial o crear docs/SECURITY_AUDIT_NOTES.md si se quiere mas detalle
```

---

## Frase guia

Hardening no es meter miedo.

Es cerrar puertas antes de invitar al mundo a entrar.
