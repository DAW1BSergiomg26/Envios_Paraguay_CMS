# PRODUCT_ROBUSTNESS_PHASE_3_CLOSURE

## Estado

Proyecto: Envios_Paraguay_CMS  
Fase: 3 - Robustez de producto y endpoints sensibles  
Estado: cerrada documentalmente

---

## Proposito

Este documento cierra la Fase 3 del proyecto.

Durante esta fase se revisaron y reforzaron endpoints sensibles, tracking publico, evidencias, uploads y acceso a archivos asociados a clientes.

---

## Documentos creados durante la fase

- docs/SENSITIVE_ENDPOINTS_AUDIT.md
- docs/PUBLIC_TRACKING_REVIEW.md
- docs/EVIDENCE_UPLOADS_AUDIT.md
- docs/CONTROLLED_EVIDENCE_DOWNLOAD_PLAN.md

---

## Cambios tecnicos integrados

### 1. Endpoint push de prueba limitado en produccion

Archivo:

- src/main/java/com/monteastur/envios/controller/api/PushSubscriptionController.java

Mejora:

- `/api/v1/push/test` queda bloqueado cuando el perfil activo contiene `prod`.
- Se mantiene disponible para desarrollo o demo local.

---

### 2. Tracking publico con DTO minimo

Archivos:

- src/main/java/com/monteastur/envios/dto/api/PublicTrackingDto.java
- src/main/java/com/monteastur/envios/controller/api/TrackingApiController.java

Mejora:

- El tracking publico deja de exponer destinatario, peso y contenido.
- El endpoint publico devuelve solo codigo, estado, origen, destino y ultima actualizacion.

---

### 3. Auditoria de evidencias y uploads

Documento:

- docs/EVIDENCE_UPLOADS_AUDIT.md

Mejora:

- Se identifico la diferencia entre visibilidad logica y acceso real al archivo.
- Se documento el riesgo de que `/uploads/**` sirva archivos directamente si se conoce la URL.

---

### 4. Rutas de evidencias unificadas

Archivo:

- src/main/java/com/monteastur/envios/controller/AdminController.java

Mejora:

- Las evidencias pasan a usar `app.upload.dir` como base.
- Se reduce la diferencia entre local, Docker y VPS.

---

### 5. Descarga controlada de evidencias

Archivo:

- src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java

Mejora:

- Las evidencias visibles para cliente apuntan ahora a un endpoint controlado.
- Se agrega descarga controlada con validacion de sesion, evidencia, visibilidad, pertenencia al cliente y archivo valido.

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id}/archivo
```

---

## Validacion tecnica

Se ejecuto compilacion local con Maven:

```text
mvn -DskipTests package
```

Resultado:

```text
BUILD SUCCESS
```

---

## Riesgos cerrados

- Endpoint de prueba push disponible en produccion.
- Tracking publico exponiendo informacion excesiva.
- Evidencias usando rutas menos coherentes que el resto de uploads.
- Enlaces de evidencias de cliente apuntando directamente a `/uploads/evidencias/...`.
- Falta de plan tecnico para descarga controlada.

---

## Riesgos pendientes

- `/uploads/**` sigue existiendo para recursos estaticos y puede servir archivos si la ruta es conocida.
- Falta una separacion completa entre imagenes publicas del CMS y evidencias privadas.
- Faltan tests automatizados para acceso a evidencias.
- Falta validar el flujo completo en navegador real con usuario cliente.
- Falta revisar rutas heredadas del proyecto Monteastur / Casa Rural.

---

## Decision final

La Fase 3 queda cerrada.

El proyecto tiene ahora una base de producto mas robusta:

- menos informacion publica innecesaria;
- endpoints de demo mas controlados;
- evidencias con descarga validada;
- rutas de archivos mas coherentes;
- auditorias documentadas antes de cambios tecnicos.

---

## Siguiente fase recomendada

FASE 4 - Pruebas, QA y estabilizacion funcional

Objetivo:

- crear o reforzar tests para endpoints sensibles;
- validar flujos reales en navegador;
- comprobar login cliente, tracking, evidencias y admin;
- preparar checklist de preproduccion.

Primera rama recomendada:

```text
feature/qa-endpoints-sensibles
```

---

## Checklist de cierre

- [x] Endpoints sensibles auditados.
- [x] Push test limitado en produccion.
- [x] Tracking publico reducido.
- [x] Evidencias auditadas.
- [x] Ruta de evidencias unificada.
- [x] Descarga controlada implementada.
- [x] Build Maven validado.
- [x] Fase 3 documentada.
