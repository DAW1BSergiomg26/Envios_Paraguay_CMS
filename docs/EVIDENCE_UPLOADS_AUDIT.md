# EVIDENCE_UPLOADS_AUDIT

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/auditoria-evidencias-uploads  
Fase: 3 - Robustez de producto y endpoints sensibles  
Tipo: auditoria documental

---

## Proposito

Este documento revisa la gestion de evidencias, archivos subidos y rutas publicas de uploads.

El objetivo es detectar si existe diferencia entre visibilidad logica en base de datos y acceso real al archivo fisico.

---

## Archivos revisados

- `src/main/java/com/monteastur/envios/config/WebMvcConfig.java`
- `src/main/java/com/monteastur/envios/model/EvidenciaEnvio.java`
- `src/main/java/com/monteastur/envios/service/EvidenciaEnvioService.java`
- `src/main/java/com/monteastur/envios/controller/AdminController.java`
- `src/main/java/com/monteastur/envios/controller/api/ClienteApiController.java`

---

## Hallazgo 1 - uploads publicos

`WebMvcConfig` publica archivos mediante:

```text
/uploads/**
```

Desde la ruta configurada en:

```text
app.upload.dir
```

Lectura tecnica:

```text
Todo archivo servido bajo /uploads/** puede ser accesible directamente si se conoce la URL.
```

---

## Hallazgo 2 - evidencias con visibleCliente

La entidad `EvidenciaEnvio` incluye:

```text
urlArchivo
visibleCliente
```

`visibleCliente` tiene valor inicial:

```text
true
```

Esto permite decidir que evidencias aparecen o no en zona cliente.

---

## Hallazgo 3 - filtrado correcto para cliente

`EvidenciaEnvioService` tiene dos metodos separados:

```text
listarPorEnvio
listarPorEnvioParaCliente
```

El metodo de cliente usa:

```text
findByEnvioTrackingIdAndVisibleClienteTrueOrderByFechaSubidaDesc
```

Lectura tecnica:

```text
La aplicacion filtra correctamente las evidencias visibles para cliente a nivel de listado.
```

---

## Hallazgo 4 - visibilidad logica vs acceso fisico

Riesgo principal:

```text
Una evidencia puede estar marcada como no visible para cliente,
pero su archivo puede seguir siendo accesible directamente por URL si esta bajo /uploads/**.
```

Ejemplo conceptual:

```text
visibleCliente = false
urlArchivo = /uploads/evidencias/archivo.pdf
```

Si alguien conoce la URL exacta, podria intentar abrirla directamente.

---

## Hallazgo 5 - rutas de guardado no unificadas

Estado: ✅ Resuelto.

En el controlador admin se observaba una diferencia:

```text
Imagenes normales usan app.upload.dir.
Evidencias usaban System.getProperty("user.dir") + "/uploads/evidencias/".
```

Riesgo:

```text
La ruta real de evidencias podia no coincidir perfectamente con app.upload.dir en Docker, VPS o produccion.
```

Resolucion:

```text
AdminController.subirEvidencia() unifica el guardado usando app.upload.dir como base y el subdirectorio evidencias/.
El mismo cambio aplica a la eliminacion fisica del archivo.
```

---

## Riesgos detectados

### P1 - Archivo visible por URL directa

```text
visibleCliente controla listados, pero no necesariamente protege el archivo fisico si /uploads/** es publico.
```

Impacto:

```text
Una evidencia no visible podria seguir siendo accesible si se conoce o filtra la URL.
```

---

### P2 - Rutas de subida no unificadas

Estado: ✅ Resuelto — evidencias e imagenes usan `app.upload.dir` como base comun.

```text
Evidencias e imagenes usan app.upload.dir como base.
```

Impacto:

```text
Se elimina el riesgo de comportamiento distinto entre local, Docker y VPS para rutas de evidencias.
```

---

### P2 - visibleCliente por defecto en true

```text
Toda evidencia nueva nace visible para cliente salvo que se cambie despues.
```

Impacto:

```text
Puede ser correcto para negocio, pero conviene que el admin decida visibilidad al subir.
```

---

## Decision recomendada

Separar dos problemas:

### 1. Coherencia de rutas

Estado: ✅ Completado.

```text
Evidencias e imagenes usan app.upload.dir como base.
```

### 2. Proteccion real de archivos sensibles

Mejora posterior:

```text
No servir evidencias sensibles directamente por /uploads/**.
Crear endpoint controlado que valide sesion, cliente y visibleCliente antes de entregar archivo.
```

---

## Primera rama tecnica recomendada

```text
feature/unificar-ruta-evidencias-upload-dir
```

Estado: ✅ Completado.

Objetivo:

```text
Modificar guardado y eliminacion de evidencias para usar app.upload.dir como base comun.
```

No mezclar con:

```text
endpoint privado de descarga
SecurityConfig
ClienteApiController
React
Nginx
Docker Compose
```

---

## Segunda rama recomendada posterior

```text
feature/evidencias-descarga-controlada
```

Objetivo:

```text
Crear un endpoint de descarga que valide permisos antes de devolver archivos de evidencia.
```

---

## Cambios no recomendados todavia

```text
No cerrar /uploads/** globalmente todavia, porque puede afectar imagenes publicas.
No cambiar ClienteApiController sin tests.
No mover archivos existentes sin plan de migracion.
No mezclar evidencias con imagenes del CMS.
```

---

## Checklist de auditoria

- [x] Revisar publicacion de /uploads/**.
- [x] Revisar entidad EvidenciaEnvio.
- [x] Revisar servicio de evidencias.
- [x] Revisar uso de visibleCliente.
- [x] Detectar diferencia entre app.upload.dir y user.dir.
- [x] Unificar ruta de evidencias con app.upload.dir.
- [x] Definir primera rama tecnica segura.

---

## Decision actual

Estado: auditoria de evidencias y uploads creada.  
Riesgo general: medio, pendiente de proteger el archivo fisico tras /uploads/**.  
Progreso: rutas de evidencias unificadas con `app.upload.dir`.  
Siguiente paso: crear endpoint controlado de descarga que valide sesion, cliente y visibleCliente antes de entregar el archivo.

---

## Frase guia

No basta con ocultar un enlace.

Si el archivo sigue servido publicamente, la puerta sigue entreabierta.
