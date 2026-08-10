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

Estado: ✅ Cerrado (2026-08-10, commit `669a185`).

Resultado:

```text
Eliminados los emails heredados de Casa Rural (`info@casarrural.com`, `admin@casarrural.com`)
en `DataInitializer` (Aviso Legal) y `OpenApiConfig` (contacto OpenAPI), reemplazados por
`info@monteastur.com` y `admin@monteastur.com`. Tests TDD nuevos (`DataInitializerTest`,
`OpenApiConfigTest`) verifican el cambio y la ausencia de la marca heredada. Sin más
referencias a `casarrural` en el codigo.
```

Criterio de aceptacion:

```text
[✅] Sin referencias a casarrural.com en codigo fuente.
[✅] Tests verdes: DataInitializerTest y OpenApiConfigTest (4 tests).


---

### P2.2 — Thymeleaf + React dashboard

Estado: ✅ Cerrado.

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

Resultado:

```text
- Matriz oficial/legacy/complementaria documentada en docs/ARQUITECTURA_INTERFACES.md.
- Login admin (GET /login) y POST /login correcto -> 302 /react-dashboard/.
- GET /admin/dashboard con sesion -> 302 /react-dashboard/; anonimo -> /login.
- Resto de /admin/** sigue sirviendo cms/*.html con banner "Interfaz heredada"
  (.legacy-banner autocontenido en admin-sidebar.html + design-system.css).
- Suite sin infraestructura en verde (307 tests, 0 failures; 37 errores ambientales
  de *IntegrationTest requieren Docker).
- Observacion (hallazgo H8): design-system.css no contiene .sidebar/.nav-links/.main-content
  (regresion previa de e72def6). No reparado en P2.2; pendiente para un bloque de pulido visual.
```

---

### P2.3 — Build React dentro de static

Estado: ✅ Cerrado.

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

Resultado:

```text
Enrutamiento SPA corregido: SpaForwardController reenvia /login-react, /dashboard y
/dashboard/** a /react-dashboard/index.html (antes /index.html, rompiendo deep-links
en produccion). Dockerfile alineado: VITE_BASE=/react-dashboard/ en el stage frontend
y COPY del dist a src/main/resources/static/react-dashboard/ (antes raiz de static/,
colisionando con el root Thymeleaf). Cubierto por SpaForwardControllerTest (3/3).
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

Resultado:

```text
✅ CERRADO (2026-08-10)
Creado docs/README_DOCS.md: indice maestro que clasifica los 65 documentos del directorio
docs/ en 9 categorias (maestros, specs/plans, auditorias/hardening, despliegue/VPS/produccion,
dominio/DNS/SSL, QA/testing, backup/operaciones, demo/lanzamiento, desarrollo) con enlace
relativo a cada documento y reglas de mantenimiento para evitar nueva duplicidad.
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

Resultado:

```text
✅ CERRADO (2026-08-10)
Añadido el diagrama oficial de arquitectura híbrida en formato Mermaid (flowchart LR) a la
sección 0 de docs/ARQUITECTURA_INTERFACES.md: navegador (SPA React, Web SSR, CMS legacy, APIs),
Spring Security con sesión JSESSIONID, controllers, servicios @Transactional con eventos
AFTER_COMMIT, MySQL 8 (Flyway) y Redis (sesiones + caché). Complementa el flowchart de flujo de
acceso admin ya existente en la sección 3.2 del spec de P2.2.
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
9. Documentar decision Thymeleaf + React.                    ✅ P2.2
10. Crear indice de documentacion.                           ✅ P3.1
11. Anadir diagrama de flujos visual.                        ✅ P3.2
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
Estado: P0, P1, P2.4, P2.3, P2.2, P2.1, P3.1 y P3.2 cerrados
Riesgo general: medio controlado
Pendiente: ninguna tarea P2/P3 del backlog
Siguiente paso: verificacion final de produccion (T4)
```

---

## Frase guia

Hardening no es meter miedo.

Es cerrar puertas antes de invitar al mundo a entrar.
