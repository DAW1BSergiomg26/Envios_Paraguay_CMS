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

Estado: ✅ Cerrado.

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

Resultado:

```text
git log --all -- .env devuelve vacio. .env esta en .gitignore y nunca fue commiteado.
```

---

## P1 — Hardening importante

### P1.1 — Perfil de produccion obligatorio

Estado: ✅ Cerrado.

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

Resultado:

```text
BootstrapPropertyEnvironmentPostProcessor hace fail-fast si el entorno no es dev y el perfil prod no esta activo.
Cubierto por BootstrapPropertyNormalizerTest y BootstrapPropertyEnvironmentPostProcessorTest.
```

---

### P1.2 — Defaults admin de desarrollo

Estado: ✅ Cerrado.

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

Resultado:

```text
En entornos no-dev, arrancar sin credenciales admin definidas lanza excepcion (mismo fail-fast de BootstrapPropertyEnvironmentPostProcessor).
```

---

### P1.3 — API cliente basada en sesion manual

Estado: ✅ Cerrado (commit 5baf904).

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

Resultado:

```text
Spring Security autentica el clienteId de la sesion en cada request de /api/v1/cliente/**.
ClienteApiControllerTest cubre acceso permitido y denegado sin sesion o contra envios ajenos.
```

---

### P1.4 — Push endpoints publicos

Estado: ✅ Cerrado (commit 5baf904).

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

Resultado:

```text
/app.push.test-enabled=false desactiva /api/v1/push/test en produccion (responde 403).
PushSubscriptionControllerTest cubre el gate habilitado y deshabilitado.
```

---

### P1.5 — Uploads de evidencias con ruta distinta

Estado: ✅ Cerrado (verificado en AdminController, subirEvidencia).

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

Resultado:

```text
AdminController.subirEvidencia() usa app.upload.dir + "evidencias/" para guardar y eliminar.
EVIDENCE_UPLOADS_AUDIT.md y KNOWN_ISSUES_PREPROD.md actualizados (Hallazgo 5 e item 10).
```

---

### P1.6 — Tracking publico y datos expuestos

Estado: ✅ Cerrado (test de contrato en PublicTrackingDtoTest).

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

Resultado:

```text
PublicTrackingDto expone exclusivamente codigoUnico, estado, origen, destino y ultimaActualizacion.
PublicTrackingDtoTest fija el contrato: serializa solo esas 5 claves y nunca destinatario, peso, contenido, cliente o id.
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

Estado: ✅ Cerrado.

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

Resultado:

```text
Los tres mensajes (subscribe, unsubscribe, testPush) usan LoggerFactory (SLF4J) con placeholders {}.
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
1. Verificar historial de secretos.                          ✅ P0.1
2. Revisar perfil prod obligatorio.                          ✅ P1.1
3. Revisar defaults admin de desarrollo.                     ✅ P1.2
4. Crear tests de API cliente.                               ✅ P1.3 (5baf904)
5. Gate de push test en produccion.                          ✅ P1.4 (5baf904)
6. Unificar ruta de evidencias/uploads.                      ✅ P1.5
7. Fijar DTO publico de tracking.                            ✅ P1.6
8. Sustituir System.out.println por SLF4J.                   ✅ P2.4
9. Documentar decision Thymeleaf + React.                    ⏳ P2.2
10. Crear indice de documentacion.                           ⏳ P3.1
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
Estado: P0 y P1 del backlog cerrados; P2.4 completado
Riesgo general: medio controlado
Pendiente: P2.1, P2.2, P2.3 (decisiones) y P3.1, P3.2 (documentacion)
Siguiente paso: documentar decision Thymeleaf + React y crear indice de documentacion
```

---

## Frase guia

Hardening no es meter miedo.

Es cerrar puertas antes de invitar al mundo a entrar.
