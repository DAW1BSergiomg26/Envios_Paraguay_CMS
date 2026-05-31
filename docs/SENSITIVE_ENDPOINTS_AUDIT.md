# SENSITIVE_ENDPOINTS_AUDIT

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/auditoria-endpoints-sensibles  
Fase: 3 - Robustez de producto y endpoints sensibles  
Tipo: auditoria documental

---

## Proposito

Este documento inicia la Fase 3 del proyecto.

El objetivo es revisar endpoints publicos, endpoints de cliente, endpoints de demo y configuracion de seguridad antes de aplicar cambios funcionales.

---

## Archivos revisados

- `src/main/java/com/monteastur/envios/controller/api/PushSubscriptionController.java`
- `src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java`
- `src/main/java/com/monteastur/envios/controller/api/TrackingApiController.java`
- `src/main/java/com/monteastur/envios/config/SecurityConfig.java`

---

## Resumen ejecutivo

La aplicacion ya tiene buenas decisiones en varias rutas, especialmente en la API de cliente, donde se valida la sesion y tambien la pertenencia del envio.

El mayor riesgo inicial detectado esta en el endpoint de prueba push:

```text
POST /api/v1/push/test
```

Este endpoint parece pensado para demo o pruebas y no deberia quedar disponible libremente en produccion real.

---

## API Push

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

### Hallazgos

- Las suscripciones se guardan en memoria con `ConcurrentHashMap`.
- Se usan mensajes por consola con `System.out.println`.
- `/test` simula envio de notificacion.
- El propio codigo indica que es una respuesta simulada para demo.

### Riesgos

```text
P1 - /api/v1/push/test parece endpoint de demo y queda bajo una ruta API publica.
P2 - Las suscripciones en memoria se pierden al reiniciar.
P2 - System.out.println deberia sustituirse por logger en una fase posterior.
```

### Decision recomendada

Primera accion tecnica:

```text
Desactivar o limitar /api/v1/push/test en produccion.
```

---

## API Cliente

Base route:

```text
/api/v1/cliente
```

Endpoints:

```text
GET /api/v1/cliente/envios
GET /api/v1/cliente/envios/{codigo}
```

### Hallazgos

- La API comprueba `clienteId` en `HttpSession`.
- Si no hay sesion de cliente, responde `403`.
- En detalle de envio, comprueba que el envio pertenece al cliente.
- Devuelve eventos y evidencias visibles para el cliente.

### Riesgos

```text
P2 - La proteccion depende de validacion manual por sesion.
P2 - Conviene cubrir con tests acceso sin sesion y acceso a envio ajeno.
```

### Decision recomendada

No tocar de momento.

Primero crear tests o documentar casos de acceso:

```text
sin sesion -> 403
cliente A intenta ver envio de cliente B -> 403
cliente valido ve su envio -> 200
```

---

## API Tracking publica

Base route:

```text
/api/v1/tracking
```

Endpoint:

```text
GET /api/v1/tracking/{codigo}
```

### Hallazgos

Devuelve:

```text
codigoUnico
estado
destinatario
origen
destino
peso
contenido
ultimaActualizacion
```

No devuelve:

```text
eventos
evidencias
```

### Riesgos

```text
P1/P2 - destinatario, peso y contenido pueden ser informacion sensible segun el tipo de envio.
```

### Decision recomendada

Crear en una fase posterior un DTO publico mas limitado si el negocio lo requiere.

Campos candidatos a revisar:

```text
destinatario
peso
contenido
```

---

## SecurityConfig

Rutas protegidas por Spring Security:

```text
/admin/**
/api/v1/admin/**
```

Resto:

```text
anyRequest().permitAll()
```

CSRF:

```text
Ignorado para /api/**
```

### Lectura tecnica

Esta configuracion puede ser valida para una arquitectura hibrida MVC + SPA, pero obliga a que cada endpoint publico o semipublico controle bien su propia seguridad.

### Riesgos

```text
P1 - Endpoints API publicos deben revisarse uno por uno.
P2 - CSRF ignorado en /api/** requiere criterio claro por endpoint.
```

---

## Primer cambio tecnico recomendado

Crear rama posterior:

```text
feature/desactivar-push-test-prod
```

Objetivo:

```text
Evitar que /api/v1/push/test quede disponible libremente en produccion real.
```

Opciones posibles:

```text
A. Eliminar endpoint si no se usa.
B. Protegerlo con perfil dev/local.
C. Protegerlo solo para admin.
D. Devolver 404 o 403 cuando el perfil activo sea prod.
```

Recomendacion inicial:

```text
Opcion B o D, para minimizar riesgo y no romper demo local.
```

---

## Cambios no recomendados todavia

```text
No tocar /api/v1/cliente/** sin tests.
No cambiar tracking publico sin decision de negocio.
No modificar SecurityConfig global de golpe.
No mezclar push, cliente, tracking y CSRF en una sola rama.
```

---

## Checklist de auditoria

- [x] Revisar PushSubscriptionController.
- [x] Revisar ClienteApiController.
- [x] Revisar TrackingApiController.
- [x] Revisar SecurityConfig.
- [x] Identificar primer endpoint sensible.
- [x] Definir primera rama tecnica recomendada.

---

## Decision actual

Estado: auditoria inicial de endpoints sensibles creada.  
Riesgo general: medio controlado.  
Siguiente paso: limitar `/api/v1/push/test` en produccion.

---

## Frase guia

Una API publica no es solo una ruta.

Es una puerta abierta: hay que saber quien puede entrar y que puede provocar.
