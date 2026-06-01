# QA_ENDPOINTS_SENSIBLES_PLAN

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/qa-endpoints-sensibles  
Fase: 4 - QA y estabilizacion funcional  
Tipo: plan de pruebas manuales y tecnicas

---

## Proposito

Este documento define las pruebas necesarias para validar los endpoints sensibles reforzados en la Fase 3.

El objetivo no es crear funcionalidades nuevas, sino comprobar que lo construido responde bien en casos normales, casos prohibidos y casos de error.

---

## Alcance

Endpoints y rutas a validar:

```text
POST /api/v1/push/test
GET  /api/v1/tracking/{codigo}
GET  /api/v1/cliente/envios
GET  /api/v1/cliente/envios/{codigo}
GET  /api/v1/cliente/evidencias/{id}/archivo
GET  /uploads/evidencias/**
```

---

## Prueba 1 - Push test en local/dev

Endpoint:

```text
POST /api/v1/push/test
```

Contexto:

```text
Perfil activo distinto de prod.
```

Resultado esperado:

```text
200 OK
Devuelve mensaje de simulacion.
```

---

## Prueba 2 - Push test en produccion

Endpoint:

```text
POST /api/v1/push/test
```

Contexto:

```text
Perfil activo contiene prod.
```

Resultado esperado:

```text
403 Forbidden
No ejecuta simulacion de envio.
```

---

## Prueba 3 - Tracking publico existente

Endpoint:

```text
GET /api/v1/tracking/{codigo_valido}
```

Resultado esperado:

```text
200 OK
Devuelve codigoUnico, estado, origen, destino y ultimaActualizacion.
No devuelve destinatario.
No devuelve peso.
No devuelve contenido.
```

---

## Prueba 4 - Tracking publico inexistente

Endpoint:

```text
GET /api/v1/tracking/{codigo_inexistente}
```

Resultado esperado:

```text
404 Not Found
Mensaje: Tracking no encontrado
```

---

## Prueba 5 - Cliente sin sesion lista envios

Endpoint:

```text
GET /api/v1/cliente/envios
```

Contexto:

```text
Sin sesion cliente.
```

Resultado esperado:

```text
403 Forbidden
Mensaje: Acceso denegado
```

---

## Prueba 6 - Cliente con sesion lista sus envios

Endpoint:

```text
GET /api/v1/cliente/envios
```

Contexto:

```text
Sesion cliente valida.
```

Resultado esperado:

```text
200 OK
Devuelve solo envios del cliente autenticado.
```

---

## Prueba 7 - Cliente consulta envio propio

Endpoint:

```text
GET /api/v1/cliente/envios/{codigo_propio}
```

Resultado esperado:

```text
200 OK
Devuelve detalle completo del envio.
Incluye evidencias visibles.
Las evidencias apuntan a /api/v1/cliente/evidencias/{id}/archivo.
```

---

## Prueba 8 - Cliente consulta envio ajeno

Endpoint:

```text
GET /api/v1/cliente/envios/{codigo_ajeno}
```

Resultado esperado:

```text
403 Forbidden
Mensaje: Acceso denegado
```

---

## Prueba 9 - Descarga evidencia sin sesion

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id}/archivo
```

Contexto:

```text
Sin sesion cliente.
```

Resultado esperado:

```text
403 Forbidden
Mensaje: Acceso denegado
```

---

## Prueba 10 - Descarga evidencia propia visible

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id_visible_propio}/archivo
```

Resultado esperado:

```text
200 OK
Devuelve el archivo.
```

---

## Prueba 11 - Descarga evidencia no visible

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id_no_visible}/archivo
```

Resultado esperado:

```text
403 Forbidden
Mensaje: Acceso denegado
```

---

## Prueba 12 - Descarga evidencia ajena

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id_de_otro_cliente}/archivo
```

Resultado esperado:

```text
403 Forbidden
Mensaje: Acceso denegado
```

---

## Prueba 13 - Evidencia inexistente

Endpoint:

```text
GET /api/v1/cliente/evidencias/{id_inexistente}/archivo
```

Resultado esperado:

```text
404 Not Found
Mensaje: Evidencia no encontrada
```

---

## Prueba 14 - Archivo fisico inexistente

Contexto:

```text
La evidencia existe en base de datos, pero el archivo ya no existe en disco.
```

Resultado esperado:

```text
404 Not Found
Mensaje: Archivo no encontrado
```

---

## Prueba 15 - URL directa antigua

Ruta:

```text
/uploads/evidencias/<archivo>
```

Resultado esperado actual:

```text
Puede seguir respondiendo si el archivo existe, porque /uploads/** sigue publicado.
```

Decision:

```text
No cerrar todavia /uploads/** hasta separar imagenes publicas del CMS y evidencias privadas.
```

---

## Validacion tecnica minima

Comando recomendado:

```text
mvn -DskipTests package
```

Resultado esperado:

```text
BUILD SUCCESS
```

---

## Riesgos pendientes despues del QA

```text
[ ] Crear tests automatizados para ClienteApiController.
[ ] Separar storage publico de imagenes y storage privado de evidencias.
[ ] Revisar flujo completo en navegador con usuario cliente real.
[ ] Revisar comportamiento bajo perfil prod.
[ ] Documentar datos demo necesarios para las pruebas.
```

---

## Decision actual

Estado: plan QA creado.  
Siguiente paso: ejecutar pruebas manuales basicas y convertir las mas criticas en tests automatizados.

---

## Frase guia

Una mejora no esta terminada cuando compila.

Esta terminada cuando falla bien, responde bien y no abre puertas por accidente.
