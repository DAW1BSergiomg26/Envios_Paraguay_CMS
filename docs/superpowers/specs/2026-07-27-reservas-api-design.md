# Diseño: API REST de Reservas

**Fecha:** 2026-07-27
**Proyecto:** Envios_Paraguay_CMS (Monteastur Envios)
**Estado:** Aprobado

---

## 1. Objetivo

Crear una API REST completa para la gestión de reservas que cubra:

- **Panel admin** (dashboard React): CRUD completo + gestión de estados
- **Booking público**: creación de reservas online con validación server-side de disponibilidad

Actualmente las reservas solo se gestionan vía formularios Thymeleaf (`PublicController`). Esta API habilita la migración al frontend React.

## 2. Superficie de endpoints

### 2.1 Admin API — `/api/v1/admin/reservas` (autenticado)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `GET` | `/api/v1/admin/reservas` | Listar todas (paginado, filtros por estado/fechas/búsqueda) |
| `GET` | `/api/v1/admin/reservas/{id}` | Detalle de una reserva |
| `PUT` | `/api/v1/admin/reservas/{id}` | Editar campos de la reserva |
| `PATCH` | `/api/v1/admin/reservas/{id}/estado` | Cambiar estado |
| `DELETE` | `/api/v1/admin/reservas/{id}` | Eliminar reserva |

### 2.2 Pública API — `/api/v1/reservas` (anónimo)

| Método | Ruta | Descripción |
|--------|------|-------------|
| `POST` | `/api/v1/reservas` | Crear reserva online con validación de disponibilidad |

## 3. DTOs

### `ReservaAdminDto` (respuesta)

```java
public class ReservaAdminDto {
    private Long id;
    private String nombreCliente;
    private String email;
    private String telefono;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer numeroHuespedes;
    private String comentarios;
    private String estado;
    private LocalDateTime createdAt;
    // getters/setters
}
```

### `CrearReservaPublicRequest` (booking público)

```java
public class CrearReservaPublicRequest {
    @NotBlank private String nombreCliente;
    @NotBlank @Email private String email;
    private String telefono;
    @NotNull private LocalDate fechaEntrada;   // debe ser >= hoy
    @NotNull private LocalDate fechaSalida;    // debe ser > fechaEntrada
    @NotNull @Min(1) private Integer numeroHuespedes;
    private String comentarios;
}
```

### `ActualizarEstadoRequest`

```java
public class ActualizarEstadoRequest {
    @NotBlank private String estado;
    // valores válidos: PENDIENTE, APROBADA, CONFIRMADA, CANCELADA
}
```

### `ActualizarReservaRequest` (PUT edición)

```java
public class ActualizarReservaRequest {
    private String nombreCliente;
    private String email;
    private String telefono;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer numeroHuespedes;
    private String comentarios;
}
```

## 4. Lógica de negocio

### 4.1 Validación de disponibilidad (booking público)

El servicio verifica que no exista solapamiento con reservas en estado `PENDIENTE`, `APROBADA` o `CONFIRMADA`:

```
Conflicto si: existe reserva activa donde:
  reserva.fechaInicio < nueva.fechaFin AND reserva.fechaFin > nueva.fechaInicio
```

Si hay conflicto → HTTP 409.

### 4.2 Transiciones de estado válidas

```
PENDIENTE → APROBADA | CANCELADA
APROBADA  → CONFIRMADA | CANCELADA
CONFIRMADA → CANCELADA
```

Transición inválida → HTTP 400.

### 4.3 Edición con cambio de fechas

Si el PUT modifica `fechaEntrada` o `fechaSalida`, se re-verifica disponibilidad excluyendo la propia reserva.

## 5. Seguridad

| Ruta | Autenticación | CSRF |
|------|--------------|------|
| `/api/v1/admin/reservas/**` | Requerida (JSESSIONID) | Deshabilitado |
| `/api/v1/reservas` (POST) | No requerida | Deshabilitado |

Sin cambios en `SecurityConfig` — las rutas `/api/v1/admin/**` ya están protegidas.

## 6. Respuestas de error

| Escenario | HTTP | Error |
|-----------|------|-------|
| Reserva no encontrada | 404 | "Reserva no encontrada" |
| Fechas ocupadas | 409 | "Las fechas seleccionadas no están disponibles" |
| Campos obligatorios faltantes | 400 | "Campos obligatorios faltantes o inválidos" |
| Transición de estado inválida | 400 | "Transición de estado no permitida" |
| Datos de edición inválidos | 400 | "Datos de actualización inválidos" |

## 7. Archivos

### 7.1 Nuevos (4)

| Archivo | Paquete |
|---------|---------|
| `controller/api/ReservaApiController.java` | `com.monteastur.envios.controller.api` |
| `controller/api/ReservaPublicApiController.java` | `com.monteastur.envios.controller.api` |
| `dto/api/ReservaAdminDto.java` | `com.monteastur.envios.dto.api` |
| `dto/api/CrearReservaPublicRequest.java` | `com.monteastur.envios.dto.api` |

### 7.2 Modificados (2)

| Archivo | Cambios |
|---------|---------|
| `service/ReservaService.java` | Añadir: `actualizar()`, `cambiarEstado()`, `verificarDisponibilidad(fechaEntrada, fechaSalida, excludedId)`, `crearPublico()` |
| `repository/ReservaRepository.java` | Añadir: `existsByEstadoInAndFechaEntradaBeforeAndFechaSalidaAfter(estados, fechaEntrada, fechaSalida)` para check de overlap. Para excluir la reserva en edición, el service usa `findAll` + filtrado en memoria o un query con `AND id != :excludeId` |

### 7.3 Sin cambios

- `SecurityConfig.java`
- `PublicController.java` (Thymeleaf se mantiene como fallback)
- Modelo `Reserva.java` (sin cambios de schema)
