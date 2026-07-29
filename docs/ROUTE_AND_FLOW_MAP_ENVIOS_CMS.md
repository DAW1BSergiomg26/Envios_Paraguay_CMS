# ROUTE_AND_FLOW_MAP_ENVIOS_CMS

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/auditoria-inicial-envios-cms
Tipo: mapa de rutas y flujos sin cambios de codigo
Documentos previos:
- docs/AUDIT_INITIAL_ENVIOS_CMS.md
- docs/PROJECT_MAP_ENVIOS_CMS.md
```

---

## Proposito

Este documento mapea las rutas principales de `Envios_Paraguay_CMS` y explica que flujo sigue cada zona de la aplicacion.

No modifica codigo. Sirve para entender navegacion, seguridad, API REST, CMS, zona cliente, tracking publico y React dashboard antes de hacer cambios reales.

---

## Vision general de flujos

La aplicacion tiene cinco grandes familias de rutas:

```text
1. Web publica Thymeleaf
2. Web publica inglesa Thymeleaf
3. CMS admin Thymeleaf protegido por Spring Security
4. Zona cliente Thymeleaf con sesion propia
5. API REST para admin, cliente, tracking publico y push/PWA
```

Tambien existe un dashboard React compilado en:

```text
src/main/resources/static/react-dashboard
```

---

## Flujo 1 — Web publica ES

Controller principal:

```text
PublicController
```

Rutas detectadas:

```text
GET  /                      → templates/home.html
GET  /casa                  → templates/lacasa.html
GET  /lacasa                → templates/lacasa.html
GET  /entorno               → templates/entorno.html
GET  /reservas              → templates/reservas.html
POST /reservas              → guarda Reserva y notifica por email
GET  /contacto              → templates/contacto.html
POST /contacto              → guarda MensajeContacto y notifica por email
GET  /aviso-legal           → templates/aviso-legal.html
GET  /politica-cookies      → templates/politica-cookies.html
GET  /tracking              → templates/tracking.html
POST /tracking              → busca envio por codigo y muestra eventos
```

Lectura inicial:

```text
La web publica aun conserva paginas de tipo Casa Rural y paginas de logistica/envios.
Esto no rompe, pero puede generar incoherencia de identidad si el producto final es Monteastur Envios.
```

---

## Flujo 2 — Web publica EN

Controller principal:

```text
PublicController
```

Rutas detectadas:

```text
GET  /en                    → templates/en/home.html
GET  /en/casa               → templates/en/casa.html
GET  /en/reservas           → templates/en/reservas.html
POST /en/reservas           → guarda Reserva y notifica por email
GET  /en/contacto           → templates/en/contacto.html
POST /en/contacto           → guarda MensajeContacto y notifica por email
GET  /en/aviso-legal        → templates/en/aviso-legal.html
GET  /en/politica-cookies   → templates/en/politica-cookies.html
GET  /en/tracking           → templates/en/tracking.html
POST /en/tracking           → busca envio por codigo y muestra eventos
```

Lectura inicial:

```text
Existe internacionalizacion parcial por duplicacion de templates bajo /en.
No se detecta todavia sistema i18n centralizado.
```

---

## Flujo 3 — Login admin

Controller:

```text
LoginController
```

Rutas:

```text
GET /login                  → templates/login.html
GET /admin/login            → redirect:/login
```

Seguridad:

```text
Spring Security usa /login como pagina de login.
El acceso autenticado redirige por defecto a /admin/dashboard.
```

---

## Flujo 4 — CMS admin Thymeleaf

Controller principal:

```text
AdminController
```

Base route:

```text
/admin
```

Rutas principales:

```text
GET  /admin/dashboard                  → cms/dashboard.html
GET  /admin/mensajesrecibidos          → cms/contactos.html
GET  /admin/reservas                   → cms/reservas.html
POST /admin/reservas/aprobar/{id}      → aprueba reserva y notifica
POST /admin/reservas/cancelar/{id}     → cancela reserva
POST /admin/reservas/eliminar/{id}     → elimina reserva
GET  /admin/imagenes                   → cms/imagenes.html
POST /admin/imagenes                   → sube imagen
POST /admin/imagenes/eliminar/{id}     → elimina imagen y archivo
GET  /admin/textos                     → cms/textos.html
POST /admin/textos                     → actualiza textos legales
GET  /admin/tracking                   → cms/tracking.html
GET  /admin/tracking/nuevo             → cms/tracking-form.html
GET  /admin/tracking/editar/{id}       → cms/tracking-form.html
POST /admin/tracking/guardar           → crea/actualiza envio tracking
POST /admin/tracking/eliminar/{id}     → elimina envio tracking
POST /admin/tracking/evidencia/{id}    → sube evidencia de envio
```

Seguridad:

```text
/admin/** esta protegido por Spring Security.
```

Riesgo detectado:

```text
La subida de imagenes usa app.upload.dir, pero evidencias usa System.getProperty("user.dir") + /uploads/evidencias/.
Conviene unificar criterio de uploads en fase de hardening.
```

---

## Flujo 5 — Zona cliente Thymeleaf

Controller:

```text
ClienteController
```

Base route:

```text
/cliente
```

Rutas:

```text
GET  /cliente/login        → cliente/login.html
POST /cliente/login        → autentica cliente y guarda clienteId en session
GET  /cliente/logout       → invalida session
GET  /cliente/panel        → cliente/panel.html
```

Funcionamiento:

```text
El cliente inicia sesion con email y password.
Si la autenticacion es correcta, se guardan clienteId y clienteNombre en HttpSession.
El panel carga envios, evidencias y eventos del cliente.
```

Seguridad:

```text
No depende directamente de Spring Security.
Usa comprobacion manual de session en ClienteController.
```

Riesgo:

```text
Conviene revisar ClienteService para confirmar hashing de password, validacion y manejo de errores.
```

---

## Flujo 6 — API admin

Controller:

```text
AdminApiController
```

Base route:

```text
/api/v1/admin
```

Endpoints:

```text
GET /api/v1/admin/envios
GET /api/v1/admin/envios/{codigo}
PUT /api/v1/admin/envios/{codigo}/estado
```

Capacidades:

```text
Listado paginado.
Filtro por estado.
Filtro por multiples estados.
Filtro por codigo.
Filtro por rango de fechas.
Busqueda general por codigo, destinatario, origen o destino.
Detalle completo con eventos y evidencias.
Actualizacion de estado.
```

Seguridad:

```text
/api/v1/admin/** esta protegido por Spring Security.
```

---

## Flujo 7 — API cliente

Controller:

```text
ClienteApiController
```

Base route:

```text
/api/v1/cliente
```

Endpoints:

```text
GET /api/v1/cliente/envios
GET /api/v1/cliente/envios/{codigo}
```

Funcionamiento:

```text
Comprueba clienteId en HttpSession.
Si no hay clienteId, responde 403.
En detalle, comprueba que el envio pertenece al cliente autenticado.
```

Seguridad:

```text
No esta protegida por Spring Security en SecurityConfig.
Tiene validacion manual por session dentro del controller.
```

Riesgo:

```text
La proteccion manual parece correcta en los endpoints vistos, pero conviene documentarla y testearla.
```

---

## Flujo 8 — API tracking publico

Controller:

```text
TrackingApiController
```

Base route:

```text
/api/v1/tracking
```

Endpoint:

```text
GET /api/v1/tracking/{codigo}
```

Funcionamiento:

```text
Devuelve datos basicos del envio por codigo.
No devuelve eventos ni evidencias.
```

Seguridad:

```text
Publica por diseno.
```

Riesgo:

```text
Confirmar que no expone informacion sensible del destinatario o contenido.
```

---

## Flujo 9 — API Push / PWA

Controller:

```text
PushSubscriptionController
```

Base route:

```text
/api/v1/push
```

Endpoints:

```text
POST /api/v1/push/subscribe
POST /api/v1/push/unsubscribe
POST /api/v1/push/test
```

Funcionamiento:

```text
Guarda subscriptions en memoria usando ConcurrentHashMap.
Simula envio push en /test.
```

Riesgos:

```text
Las subscriptions se pierden al reiniciar.
Usa System.out.println en lugar de logger.
/test parece endpoint de demo y deberia revisarse antes de produccion.
```

---

## Seguridad global observada

SecurityConfig protege:

```text
/admin/**
/api/v1/admin/**
```

Permite:

```text
anyRequest().permitAll()
```

CSRF:

```text
Activo para Thymeleaf.
Ignorado para /api/**.
```

Headers:

```text
frameOptions deny
referrerPolicy strict-origin-when-cross-origin
```

Autenticacion admin:

```text
InMemoryUserDetailsManager
BCryptPasswordEncoder
credenciales desde variables app.admin.username y app.admin.password
```

---

## Riesgos prioritarios

### P1 — API cliente fuera de Spring Security

```text
/api/v1/cliente/** depende de validacion manual por HttpSession.
No es necesariamente incorrecto, pero debe cubrirse con tests.
```

### P1 — Push endpoints publicos

```text
/api/v1/push/** queda publico por configuracion actual.
/test deberia bloquearse o desactivarse en produccion.
```

### P1 — Uploads de evidencias no usan app.upload.dir

```text
Puede provocar rutas distintas entre local, Docker y produccion.
```

### P2 — Duplicidad identidad Casa Rural / Envios

```text
Rutas como /reservas, /casa, /lacasa y entidades Reserva conviven con tracking/logistica.
Puede ser parte del producto, pero necesita decision de negocio.
```

### P2 — Doble capa UI

```text
Thymeleaf y React conviven.
Debe definirse que vistas son oficiales, legacy o complementarias.
```

---

## Recomendaciones de pruebas

Crear o revisar tests para:

```text
/admin/** requiere autenticacion.
/api/v1/admin/** requiere autenticacion.
/api/v1/cliente/envios sin session devuelve 403.
/api/v1/cliente/envios/{codigo} no permite ver envios de otro cliente.
/api/v1/tracking/{codigo} no devuelve evidencias privadas.
/api/v1/push/test no deberia estar abierto en produccion.
```

---

## Proxima accion recomendada

Antes de modificar codigo, crear una lista de hardening priorizada:

```text
docs/HARDENING_BACKLOG_ENVIOS_CMS.md
```

Debe incluir:

```text
seguridad admin
seguridad cliente
push endpoints
uploads/evidencias
perfil prod
secrets
CSRF/API
tracking publico
```

---

## Decision actual

```text
Estado: mapa de rutas y flujos creado
Riesgo general: medio controlado
Siguiente paso: crear backlog de hardening
```

---

## Frase guia

Una ruta no es solo una URL.

Es una puerta: hay que saber quien entra, que ve y que puede cambiar.
