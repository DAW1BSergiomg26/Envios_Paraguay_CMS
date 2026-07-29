# PUBLIC_TRACKING_REVIEW

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/revision-tracking-publico  
Fase: 3 - Robustez de producto y endpoints sensibles  
Tipo: revision documental

---

## Proposito

Este documento revisa el endpoint publico de tracking antes de aplicar cambios tecnicos.

El objetivo es decidir que informacion debe ser visible para cualquier persona que tenga un codigo de seguimiento.

---

## Endpoint revisado

```text
GET /api/v1/tracking/{codigo}
```

Controller:

```text
TrackingApiController
```

---

## Estado actual

El endpoint busca el envio por codigo unico y devuelve un `TrackingDto` con estos campos:

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

---

## Lectura tecnica

La decision de no exponer eventos ni evidencias en el endpoint publico es positiva.

Sin embargo, algunos campos actuales pueden ser sensibles segun el tipo de envio y el modelo de negocio.

Campos a revisar:

```text
destinatario
peso
contenido
```

---

## Riesgos

### P1/P2 - Exposicion de informacion de destinatario

Riesgo:

```text
Cualquier persona con el codigo de tracking podria ver el destinatario.
```

Impacto:

```text
Puede ser aceptable si el codigo es privado y suficientemente dificil de adivinar, pero debe ser una decision consciente.
```

---

### P2 - Exposicion de peso y contenido

Riesgo:

```text
El peso y el contenido pueden revelar detalles del envio.
```

Impacto:

```text
Depende del negocio. Para tracking publico normalmente conviene mostrar solo informacion minima.
```

---

## Decision recomendada

Separar claramente dos niveles de informacion:

### Tracking publico

Mostrar solo informacion minima:

```text
codigoUnico
estado
origen
destino
ultimaActualizacion
```

### Zona cliente autenticada

Permitir informacion mas completa:

```text
destinatario
peso
contenido
eventos
evidencias visibles para cliente
```

---

## Cambio tecnico recomendado

Crear un DTO publico especifico, por ejemplo:

```text
PublicTrackingDto
```

Campos sugeridos:

```text
codigoUnico
estado
origen
destino
ultimaActualizacion
```

Luego modificar:

```text
TrackingApiController
```

para devolver ese DTO en:

```text
GET /api/v1/tracking/{codigo}
```

---

## Rama tecnica recomendada

```text
feature/public-tracking-dto-minimo
```

Objetivo:

```text
Reducir la informacion expuesta por el tracking publico sin afectar la zona cliente autenticada.
```

No mezclar con:

```text
ClienteApiController
Evidencias
Eventos
SecurityConfig
UI React
Thymeleaf
```

---

## Criterios de aceptacion

```text
El endpoint publico no devuelve destinatario.
El endpoint publico no devuelve peso.
El endpoint publico no devuelve contenido.
La zona cliente sigue mostrando su informacion completa.
El codigo compila.
```

---

## Decision actual

Estado: revision de tracking publico creada.  
Riesgo general: medio controlado.  
Siguiente paso: crear DTO publico minimo.

---

## Frase guia

Un codigo de tracking abre una ventana.

La clave es decidir cuanta casa se ve desde fuera.
