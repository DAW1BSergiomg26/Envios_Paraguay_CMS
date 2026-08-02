# Bloque 11 — Módulo de Carga Masiva de Envíos por Lotes vía CSV (Batch Ingestion)

- Fecha: 2026-08-02
- Estado: Aprobado (decisiones de diseño acordadas con el usuario)
- Base: `main` tras Bloque 10 (commit `a48dcf3`)

## Objetivo

Construir una API de ingestión masiva altamente resiliente y no bloqueante
(`POST /api/v1/admin/imports/csv`) capaz de procesar ficheros CSV mediante lectura
en *streaming* (bajo consumo de RAM), validación sintáctica y semántica línea por
línea, persistencia optimizada por lotes (*batch inserts* en chunks de 100),
auditoría de errores por número de línea en `batch_import_errors` y reporte de
estado/progreso en tiempo real mediante `batch_imports`.

## Restricciones críticas

- **Prohibido Lombok:** entidades, DTOs y modelos en Java puro (constructor vacío
  obligatorio para JPA, constructores parametrizados, getters/setters explícitos).
- **Inyección por constructor:** campos `private final` inicializados en el constructor.
- **Migración Flyway V5** exacta en `src/main/resources/db/migration/V5__create_batch_imports_tables.sql`
  (SQL del usuario: `BOOLEAN` no aplica aquí; `batch_imports` con FK `cliente_id ON DELETE SET NULL`
  y `batch_import_errors` con FK `batch_id ON DELETE CASCADE`).
- **Concurrencia:** ingesta disparada en hilo dedicado `@Async("batchTaskExecutor")`,
  retornando `202 Accepted` + `batch_id` de forma inmediata.
- **Tolerancia a fallos:** una línea con errores no aborta el lote; la incidencia se
  registra en `batch_import_errors` y se continúa con el siguiente registro.
- **TDD:** toda funcionalidad crítica con sus tests unitarios e de integración
  (`@WebMvcTest`, `@SpringBootTest` con MySQL/Redis reales), AssertJ y Awaitility.

## Decisiones de diseño aprobadas

1. **Asociación de cliente a nivel de lote:** el CSV no lleva columna de cliente; el
   `clienteId` se pasa como parámetro opcional al subir el fichero y se guarda en
   `batch_imports.cliente_id`. Todos los envíos válidos del lote se asocian a ese
   cliente (o a ninguno si no se indica).
2. **Duplicados → fallar la línea y auditar:** si `codigo_unico` ya existe en BD o se
   repite dentro del mismo fichero, la línea se registra como error
   (`codigo duplicado`) y se incrementa `fallidos`. Sin upsert.
3. **Commit por chunk (progreso en tiempo real):** cada chunk de 100 se persiste en una
   transacción independiente `REQUIRES_NEW` (vía `BatchImportPersistenceService`).
   Los contadores de `batch_imports` se actualizan y commitean por chunk, de modo que
   `GET /imports/{id}` refleja el progreso en vivo y la memoria queda acotada.
4. **Solo admin:** todo el CRUD bajo `/api/v1/admin/imports` con
   `@PreAuthorize("hasRole('ROLE_ADMIN')")`, coherente con webhooks.
5. **Fichero temporal:** el controller copia el CSV a un directorio temporal
   configurable (`app.batch.tmp-dir`) de forma síncrona; el worker `@Async` lo lee en
   streaming. Desacopla del ciclo de vida del `MultipartFile` y habilita reintentos.
6. **Librería de parsing:** añadir `com.opencsv:opencsv` 5.x (parser RFC-4180,
   streaming con `CSVReader`, manejo de quoting y BOM).
7. **Eventos:** la ingesta masiva **no** publica `EstadoEnvioActualizadoEvent` ni
   dispara notificaciones/webhooks (es carga de datos, no actualización operativa).
   Sí evita la caché `envios.dashboard` al finalizar.

## Arquitectura

```
POST /api/v1/admin/imports/csv (multipart, admin)
        │  valida fichero + copia a app.batch.tmp-dir
        │  BatchImportPersistenceService.crearLote → PENDIENTE
        ▼
BatchImportController ──202 (batch_id)──▶ CsvBatchImportService.procesarLote @Async
        │                                              │  registrarInicio → EN_PROCESO
        │                                              ▼
        │                                   CsvEnvioParser (OpenCSV streaming, por fila)
        │                                              │  válida → EnvioTracking | error (línea)
        │                                              ▼
        │                              acumular 100 ──▶ BatchImportPersistenceService
        │                                                     .procesarChunk (REQUIRES_NEW)
        │                                                     · saveAll envíos
        │                                                     · batch_import_errors
        │                                                     · contadores batch_imports
        ▼
GET /imports/{id} ◀── estado/progreso (commits por chunk)  finalizar → COMPLETADO[_CON_ERRORES]
GET /imports/{id}/errors ◀── errores por línea                  o FALLIDO (catastrófico)
```

### Componentes (main)

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `BatchImport` | `model` | Entidad JPA `batch_imports`, estado enum `BatchImportEstado`. |
| `BatchImportError` | `model` | Entidad JPA `batch_import_errors` (`@PrePersist` fecha). |
| `BatchImportEstado` | `model` | Enum `PENDIENTE, EN_PROCESO, COMPLETADO, COMPLETADO_CON_ERRORES, FALLIDO`. |
| `BatchImportRepository` | `repository` | JPA de `BatchImport` + `findByClienteIdOrderByFechaCreacionDesc`. |
| `BatchImportErrorRepository` | `repository` | JPA de errores + `findByBatchIdOrderByLineaNumeroAsc`, `countByBatchId`. |
| `CsvEnvioRow` | `service.batch` | Fila CSV parseada (campos + `lineNumber`), Java puro. |
| `CsvEnvioParser` | `service.batch` | Parser OpenCSV streaming: por fila → `EnvioTracking` o `CsvImportLineError`. |
| `CsvImportLineError` | `service.batch` | Error de línea (`lineaNumero`, `codigoRastreo`, `errorMensaje`). |
| `CsvBatchImportService` | `service` | Orquestador `@Async("batchTaskExecutor")` sin `@Transactional`. |
| `BatchImportPersistenceService` | `service` | Métodos `@Transactional(REQUIRES_NEW)`: `crearLote`, `procesarChunk`, `finalizar`, `marcarFallido`, `registrarInicio`. |
| `BatchImportController` | `controller.api` | Endpoints admin (POST/GET status/GET errors). |
| `BatchImportResponseDto` | `dto.api` | Estado del lote (sin datos sensibles). |
| `BatchImportErrorDto` | `dto.api` | Error por línea. |
| `BatchImportHttpConfig` | `config` | Bean `batchTaskExecutor` (`ThreadPoolTaskExecutor` + `CallerRunsPolicy`) y props `app.batch.*`. |

### Fichero CSV (formato)

```
codigo,estado,destinatario,origen,destino,peso,contenido,observaciones
MT-2026-0101,RECIBIDO,María López,Asturias,Asunción,5 kg,Documentos,Paquete frágil
```

- Cabecera obligatoria (se omite la primera fila). `estado` ∈ `{RECIBIDO, EN_ADUANA_ORIGEN,
  EN_TRANSITO, EN_ADUANA_DESTINO, EN_REPARTO, ENTREGADO}`.
- Campos opcionales vacíos → `null`. `codigo`, `estado`, `destinatario` obligatorios.
- Longitudes máximas: `VARCHAR(255)` (codigo, estado, destinatario, origen, destino,
  peso, contenido); `observaciones` TEXT.

### Reglas de validación por línea

1. `codigo` obligatorio y no vacío tras trim.
2. `estado` obligatorio y en el conjunto válido.
3. `destinatario` obligatorio y no vacío tras trim.
4. Opcionales (origen/destino/peso/contenido/observaciones): vacío → `null`; truncar a
   `VARCHAR(255)` si excede.
5. Duplicado (BD o dentro del fichero) → error de línea `codigo duplicado`.
6. Campos que excedan longitud máxima se truncan con aviso? → **no**: se registran como
   error de línea si exceden 255 (evita datos corruptos silenciosos). Las opcionales se
   truncan únicamente si el exceso es solo de espacios.

### Flujo transaccional del worker

```
procesarLote(batchId, tmpFile, clienteId, nombreArchivo)  [sin @Transactional]
  1. persistence.registrarInicio(batchId)                 → EN_PROCESO (REQUIRES_NEW)
  2. abrir CSV (streaming), detectar BOM, saltar cabecera
  3. por fila: CsvEnvioParser.parse → válida o error
       · acumular en chunk (máx 100)
       · si chunk lleno → persistence.procesarChunk(batchId, validos, errores)  (REQUIRES_NEW)
       · vaciar chunk
  4. persistir resto (< 100)
  5. persistence.finalizar(batchId, COMPLETADO|COMPLETADO_CON_ERRORES, totalRegistros, null)
  6. evictar caché envios.dashboard
  catch (IOException/any) → persistence.marcarFallido(batchId, resumen) + log
  finally → borrar fichero temporal
```

- `totalRegistros` = número de filas de datos leídas (se fija en `finalizar`).
- Límite `app.batch.max-rows`: al superarse, se detiene la lectura y se finaliza como
  `COMPLETADO_CON_ERRORES` con resumen.
- `procesarChunk` inserta los `EnvioTracking` con `saveAll` (IDs con
  `GenerationType.IDENTITY` → MySQL no usa batching JDBC real, pero el commit por chunk
  acota memoria y trabajo; `spring.jpa.properties.hibernate.jdbc.batch_size` queda sin
  tocar por no aplicarse a IDENTITY).

### Endpoints

| Método | Ruta | Código | Descripción |
|---|---|---|---|
| POST | `/api/v1/admin/imports/csv` | 202 | Sube CSV (multipart `file` + `clienteId` opcional). Devuelve `BatchImportResponseDto`. |
| GET | `/api/v1/admin/imports/{id}` | 200/404 | Estado y contadores del lote. |
| GET | `/api/v1/admin/imports/{id}/errors` | 200/404 | Errores por línea (`lineaNumero`, `codigoRastreo`, `errorMensaje`). |

Validaciones de la subida: fichero presente (400), no vacío (400), extensión `.csv`
case-insensitive (400), tamaño ≤ `app.batch.max-file-size` (400). `clienteId` si se
indica debe existir (404/400).

### Seguridad

- Solo `ROLE_ADMIN` (autenticación form login; CSRF deshabilitado para `/api/**`).
- `clienteId` inexistente → error claro, sin datos expuestos.
- `batch_import_errors` no expone secretos (no los hay).
- Kill-switch `app.batch.enabled=false` para apagar la ingesta sin redeploy.
- Protección anti-DoS: límite de filas (`max-rows`), de longitud de línea
  (`max-line-length`), tamaño de fichero y extensión.

### Props nuevas (`application.properties`)

```properties
app.batch.enabled=${APP_BATCH_ENABLED:true}
app.batch.chunk-size=${APP_BATCH_CHUNK_SIZE:100}
app.batch.tmp-dir=${BATCH_TMP_DIR:./uploads/batch-imports}
app.batch.max-file-size=${BATCH_MAX_FILE_SIZE:5MB}
app.batch.max-line-length=${BATCH_MAX_LINE_LENGTH:10000}
app.batch.max-rows=${BATCH_MAX_ROWS:200000}
app.batch.executor.core-size=${APP_BATCH_EXECUTOR_CORE:2}
app.batch.executor.max-size=${APP_BATCH_EXECUTOR_MAX:4}
app.batch.executor.queue-capacity=${APP_BATCH_EXECUTOR_QUEUE:500}
```

Nota: `app.batch.max-file-size` debe ser ≤ `spring.servlet.multipart.max-file-size`
(5MB actual). Subir el límite exige ajustar también el multipart global.

### Dependencia nueva

```xml
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>
```

## Testing (TDD)

### Unitarios

| Test | Qué verifica |
|---|---|
| `CsvEnvioParserTest` | BOM UTF-8, cabecera saltada, fila válida, filas inválidas (codigo/estado/destinatario), opcionales vacíos → null, estado inválido, quoting con comas internas, `lineNumber` correcto, exceso >255 en obligatorio → error. |
| `BatchImportHttpConfigTest` | Bean `batchTaskExecutor` con `CallerRunsPolicy` y shutdown graceful. |
| `BatchImportControllerTest` (`@WebMvcTest`) | 202 + batchId, 400 (sin fichero / vacío / extensión / tamaño), 404 (lote inexistente), 200 status y errors, sin auth → redirect, `clienteId` inexistente → 404. |

### Integración (`@SpringBootTest` + Awaitility)

| Test | Qué verifica |
|---|---|
| `loteValido_completaYPersiste` | CSV válido → COMPLETADO, contadores correctos, envíos en BD. |
| `loteConErroresParciales_completaConErrores` | Filas inválidas → COMPLETADO_CON_ERRORES, `exitosos/fallidos` correctos, `batch_import_errors` con línea. |
| `loteConDuplicados_auditaLinea` | Duplicado en BD y dentro del fichero → líneas auditadas como error. |
| `ficheroInexistente_marcaFallido` | `procesarLote` con path inexistente → FALLIDO + `errorResumen`. |
| `loteConMasFilasQueMaxRows_seCorta` | Exceso de `max-rows` → corte + COMPLETADO_CON_ERRORES con resumen. |

Limpieza `@AfterEach`: borrar `batch_import_errors`, `batch_imports`, envíos del lote,
cliente de prueba. `@MockBean EmailService` para aislar notificaciones.

## Definición de terminado

- `mvn clean test` completo en contenedor (MySQL/Redis) → `BUILD SUCCESS`.
- Suite ampliada a 92+ tests (88 actuales + nuevos).
- Fichero temporal siempre eliminado (incluso en fallo).
- Caché `envios.dashboard` evictada al finalizar.
- `docs/handoff.md` actualizado con el Bloque 11.
