# CONTROLLED_EVIDENCE_DOWNLOAD_PLAN

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/evidencias-descarga-controlada  
Fase: 3 - Robustez de producto y endpoints sensibles  
Tipo: plan tecnico previo

---

## Proposito

Este documento define el plan para que las evidencias de envio no dependan solo de una URL publica directa.

La meta es que el acceso a archivos de evidencia pase por una validacion de permisos antes de entregar el archivo.

---

## Problema actual

Las evidencias se guardan con una URL como:

```text
/uploads/evidencias/<archivo>
```

Y la aplicacion sirve recursos estaticos bajo:

```text
/uploads/**
```

Eso significa que `visibleCliente` puede controlar si el enlace aparece en la zona cliente, pero no necesariamente impide que alguien abra el archivo si conoce la URL exacta.

---

## Objetivo

Crear una ruta controlada para descargar evidencias desde la zona cliente.

Endpoint recomendado:

```text
GET /api/v1/cliente/evidencias/{id}/archivo
```

---

## Validaciones necesarias

Antes de entregar un archivo, el endpoint debe comprobar:

```text
1. Existe sesion de cliente.
2. Existe la evidencia.
3. La evidencia pertenece a un envio.
4. El envio pertenece al cliente de la sesion.
5. visibleCliente == true.
6. El archivo existe en app.upload.dir/evidencias/.
7. El nombre del archivo no permite path traversal.
```

---

## Respuestas esperadas

```text
Sin sesion cliente -> 403
Evidencia inexistente -> 404
Evidencia de otro cliente -> 403
Evidencia no visible -> 403
Archivo fisico no encontrado -> 404
Archivo valido -> 200 con contenido del archivo
```

---

## Estrategia por fases

### Fase A - Endpoint controlado

Crear endpoint nuevo sin eliminar todavia las URLs antiguas.

```text
GET /api/v1/cliente/evidencias/{id}/archivo
```

### Fase B - DTO cliente apunta al endpoint controlado

Modificar la respuesta de evidencias para cliente para que `urlArchivo` apunte al nuevo endpoint.

Ejemplo:

```text
/api/v1/cliente/evidencias/123/archivo
```

### Fase C - Revisar exposicion directa

Evaluar como separar:

```text
imagenes publicas del CMS
archivos de evidencias
```

No cerrar `/uploads/**` globalmente hasta confirmar que no rompe imagenes publicas.

---

## Primer cambio tecnico recomendado

Crear el endpoint controlado dentro de:

```text
ClienteApiController
```

Motivo:

```text
El controlador ya trabaja con sesion de cliente y evidencias visibles.
```

---

## Cambios no recomendados todavia

```text
No cerrar /uploads/** globalmente.
No mover archivos existentes.
No cambiar SecurityConfig de golpe.
No tocar zona admin.
No cambiar rutas de imagenes publicas del CMS.
```

---

## Criterios de aceptacion

```text
El endpoint compila.
Sin sesion devuelve 403.
Evidencia no visible devuelve 403.
Evidencia ajena devuelve 403.
Evidencia valida devuelve archivo.
No se permite path traversal.
```

---

## Siguiente rama tecnica recomendada

La rama actual puede contener el primer cambio si se mantiene pequeno:

```text
feature/evidencias-descarga-controlada
```

Primer commit tecnico sugerido:

```text
Añade endpoint controlado de descarga de evidencias
```

---

## Frase guia

Ocultar un enlace ayuda.

Controlar la descarga protege de verdad.
