# Bloque 12 — Motor de Generación PDF, Etiquetas Térmicas y Códigos de Barras/QR

- Fecha: 2026-08-02
- Estado: Aprobado (decisiones de diseño acordadas con el usuario)
- Base: `main` tras Bloque 11 (cambios del Bloque 11 aún sin commitear; ver `docs/handoff.md`)

## Objetivo

Dotar a la plataforma de un subsistema nativo de generación de documentación
comercial y logística en caliente con **cero I/O en disco** (memoria pura):

1. **Etiqueta térmica individual** (PDF 100×150 mm) con Code128 (codigoUnico) y QR
   (URL pública de tracking).
2. **Descarga masiva de etiquetas por lote** (PDF multipágina en streaming, página a
   página hacia el OutputStream de la respuesta HTTP).
3. **Manifiesto de carga / guía de remisión** (PDF A4 tabulado) con desglose de envíos
   del lote, sumatorias de peso, metadatos del cliente y firma de despacho.
4. **Auditoría de emisión** persistente en `documentos_generados` + endpoint de consulta.

## Restricciones críticas

- **Prohibido Lombok:** entidades, DTOs y modelos en Java puro (constructor vacío
  obligatorio para JPA, constructores parametrizados, getters/setters explícitos).
- **Inyección por constructor:** campos `private final` inicializados en el constructor.
- **Migración Flyway V6 exacta** en `src/main/resources/db/migration/V6__create_documents_tables.sql`
  (SQL del usuario, tabla `documentos_generados`).
- **Memoria pura:** ningún artefacto (barras, QR, PDF) se escribe en disco; todo se
  transmite como `byte[]` o `OutputStream` con `Content-Disposition` adecuada.
- **Stack autorizado:** `com.google.zxing:core:3.5.3`, `com.google.zxing:javase:3.5.3`
  y `com.github.librepdf:openpdf:1.3.40` (verificado en Maven Central; compatible Java 17).
- **TDD:** tests unitarios y de integración (`@WebMvcTest`, `@SpringBootTest`),
  AssertJ. Objetivo `mvn clean test` con BUILD SUCCESS.

## Decisiones de diseño aprobadas

1. **Vínculo envíos ↔ lote (migración V7 + relleno en worker):** `envios_tracking` no
   tenía forma de saber a qué lote pertenece cada envío. Se añade
   `ALTER TABLE envios_tracking ADD COLUMN batch_id BIGINT NULL` con FK a
   `batch_imports(id) ON DELETE SET NULL` e índice, en la migración
   `V7__add_batch_id_to_envios_tracking.sql`. `BatchImportPersistenceService.procesarChunk`
   asigna `envio.setBatchId(batchId)` al guardar cada envío (único punto de cambio;
   el worker del Bloque 11 no se toca). `EnvioTracking` gana el campo `Long batchId`
   (patrón idéntico a `BatchImport.clienteId`, sin relación JPA).
2. **Descarga masiva en streaming con tope:** `generarEtiquetasLote` escribe páginas
   directamente al `OutputStream` de la respuesta (memoria solo por página, sin disco).
   Tope configurable `app.pdf.max-pages` (default 5000 etiquetas por lote) → 400 si se
   supera. Así un lote de hasta 200 000 filas no compromete la memoria del servidor.
3. **Auditoría con endpoint de consulta:** además de registrar cada emisión en
   `documentos_generados`, se expone `GET /api/v1/admin/documentos?tipo=` para listar
   las emisiones (JSON, ordenadas por fecha descendente).
4. **URL de tracking del QR:** convención ya usada por notificaciones y webhooks
   (`baseUrl + "/" + codigo`), con `app.pdf.tracking.base-url` = `${APP_TRACKING_BASE_URL:http://localhost:8080/tracking}`.
5. **Roles:** todo bajo `/api/v1/admin/documentos` con `@PreAuthorize("hasRole('ROLE_ADMIN')")`.
6. **Una fila de auditoría por artefacto emitido:** etiqueta individual → 1 fila;
   lote de etiquetas → 1 fila (tipo `ETIQUETAS_LOTE`, referencia `batchId`);
   manifiesto → 1 fila (tipo `MANIFIESTO_CARGA`, referencia `batchId`).
7. **`usuario_generacion`:** se toma de `Authentication.getName()` en el controller.
8. **Sumatoria de pesos:** `PesoUtil` extrae el número inicial del `String` de peso
   (`"1,5 kg"`, `"2"`, `"12.5"`); si no es parseable se cuenta como bulto sin peso.

## Arquitectura

```
GET /api/v1/admin/documentos/envios/{codigo}/etiqueta        (admin) → byte[] PDF inline
GET /api/v1/admin/documentos/lotes/{batchId}/etiquetas       (admin) → PDF streaming attachment
GET /api/v1/admin/documentos/lotes/{batchId}/manifiesto      (admin) → byte[] PDF attachment
GET /api/v1/admin/documentos?tipo=...                        (admin) → JSON auditoría
        │
        ▼
DocumentosController (ROLE_ADMIN)
        │  usuario_generacion = Authentication.getName()
        ▼
DocumentoPdfService
        │  - buscar envío/lote (404 si no existe)
        │  - validar tope max-pages (400 si se supera)
        │  - auditar en documentos_generados
        ▼
BarcodeService (ZXing)          EtiquetaPdfGenerator (OpenPDF)      ManifiestoPdfGenerator (OpenPDF)
  · Code128 → BufferedImage      · 100×150 mm → byte[]              · A4 → byte[]
  · QR      → BufferedImage      · Code128 + QR incrustados         · tabla, totales, firma
        ▼
EnvioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc / countByBatchId
DocumentoGeneradoRepository
```

### Componentes (main)

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `TipoDocumento` (enum) | `model` | `ETIQUETA_TERMICA`, `ETIQUETAS_LOTE`, `MANIFIESTO_CARGA` |
| `DocumentoGenerado` (entidad) | `model` | Registro de auditoría (tabla `documentos_generados`, `@PrePersist` para fecha) |
| `DocumentoGeneradoRepository` | `repository` | `findByOrderByFechaCreacionDesc`, `findAllByTipoOrderByFechaCreacionDesc`, `countByTipo` |
| `BarcodeService` | `service.pdf` | Code128 y QR → `BufferedImage` / PNG `byte[]` (memoria) |
| `EtiquetaPdfGenerator` | `service.pdf` | Etiqueta térmica 100×150 mm → `byte[]` |
| `ManifiestoPdfGenerator` | `service.pdf` | Manifiesto A4 tabulado → `byte[]` |
| `PesoUtil` | `service.pdf` | Parseo del peso (`String`) → número; `OptionalDouble` |
| `DocumentoPdfService` | `service` | Orquestación: generar etiqueta/manifiesto (`byte[]`), etiquetas de lote (streaming), listar emisiones, auditoría |
| `DocumentosController` | `controller.api` | Endpoints REST admin |
| `DocumentoGeneradoDto` | `dto.api` | Respuesta JSON de auditoría |

### Modificaciones a código existente

- `EnvioTracking`: nuevo campo `Long batchId` (columna `batch_id`), getter/setter.
- `EnvioTrackingRepository`: `List<EnvioTracking> findByBatchIdOrderByCodigoUnicoAsc(Long batchId)` y `long countByBatchId(Long batchId)`.
- `BatchImportPersistenceService.procesarChunk`: `envio.setBatchId(batchId)` antes del `saveAll`.
- `pom.xml`: añadir OpenPDF y ZXing.

## Migraciones

### V6 (exacta del usuario)

```sql
CREATE TABLE documentos_generados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo VARCHAR(50) NOT NULL COMMENT 'ETIQUETA_TERMICA, ETIQUETAS_LOTE, MANIFIESTO_CARGA',
    referencia_id VARCHAR(100) NOT NULL COMMENT 'codigoUnico del envío o batch_id del lote',
    nombre_archivo VARCHAR(255) NOT NULL,
    peso_bytes INT NOT NULL,
    usuario_generacion VARCHAR(100) NULL,
    fecha_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### V7 (vínculo envíos ↔ lote)

```sql
ALTER TABLE envios_tracking ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE envios_tracking ADD CONSTRAINT fk_envios_batch
    FOREIGN KEY (batch_id) REFERENCES batch_imports(id) ON DELETE SET NULL;
ALTER TABLE envios_tracking ADD INDEX idx_envios_batch_id (batch_id);
```

## Etiqueta térmica (100×150 mm)

- Página: `new Rectangle(283.46f, 425.2f)` (mm → pt: 100×2.8346, 150×2.8347).
- Contenido: cabecera «MONTEASTUR ENVÍOS», código único en texto grande, Code128
  (`codigoUnico`), QR (URL tracking), destinatario, origen → destino, peso, contenido,
  estado/ubicación, fecha de emisión.
- Salida: `byte[]`, `Content-Disposition: inline; filename="etiqueta-{codigo}.pdf"`.

## Manifiesto (A4)

- Página A4 vertical. Cabecera corporativa con nombre del cliente, fecha de lote,
  `batch_id`. Tabla: código, destinatario, contenido, peso, estado. Fila de totales:
  nº de bultos y peso total (PesoUtil). Sección «Firma de despacho».
- Salida: `byte[]`, `Content-Disposition: attachment; filename="manifiesto-lote-{batchId}.pdf"`.

## Propiedades (`application.properties`)

```properties
# =========================
# PDF / ETIQUETAS / BARCODES
# =========================
app.pdf.enabled=${APP_PDF_ENABLED:true}
app.pdf.max-pages=${APP_PDF_MAX_PAGES:5000}
app.pdf.tracking.base-url=${APP_TRACKING_BASE_URL:http://localhost:8080/tracking}
app.pdf.qr.size=${APP_PDF_QR_SIZE:200}
app.pdf.barcode.width=${APP_PDF_BARCODE_WIDTH:500}
app.pdf.barcode.height=${APP_PDF_BARCODE_HEIGHT:120}
```

## Manejo de errores

- `ResourceNotFoundException` (404): envío por código o lote inexistente.
- `BadRequestException` (400): lote supera `app.pdf.max-pages`.
- Fallo de generación PDF → 500 genérico (`GlobalExceptionHandler` ya existente).
- Respuesta no-REST (vistas) no aplica: todos los endpoints son `/api/**`.

## Estrategia de tests (TDD)

| Test | Alcance |
|---|---|
| `BarcodeServiceTest` | Code128/QR → imagen no nula con dimensiones esperadas; PNG con cabecera válida; contenido en blanco → `IllegalArgumentException` |
| `PesoUtilTest` | `"1,5 kg"`→1.5, `"2"`→2, `"12.5"`→12.5, `"n/a"`→vacío |
| `EtiquetaPdfGeneratorTest` | bytes `%PDF`, 1 página, tamaño 100×150 mm (rectángulo), codigoUnico presente en los bytes |
| `ManifiestoPdfGeneratorTest` | `%PDF`, A4, 1 página con pocos envíos, tabla y totales |
| `DocumentoPdfServiceTest` | mocks de repos: auditoría guardada con tipo/referencia/peso/bytes/usuario, 404 envío/lote, 400 por tope de páginas |
| `DocumentosControllerTest` | `@WebMvcTest`: 200 PDF con `Content-Type: application/pdf` y `Content-Disposition` correcta, 404, 400, acceso anónimo denegado, GET lista JSON |
| `CsvBatchImportIntegrationTest` (extensión) | tras importar un CSV, los envíos guardados tienen `batchId` asignado |
| `DocumentosIntegrationTest` | `@SpringBootTest`: generar etiqueta de un envío real (persiste auditoría), lote de etiquetas y manifiesto |

## Criterio de finalización

- `mvn clean test` (contenedor Docker) con BUILD SUCCESS y suite completa en verde.
- Sin ficheros temporales escritos en disco por el motor PDF (verificación por diseño:
  solo `byte[]` / `OutputStream`).
- `docs/handoff.md` actualizado con el Bloque 12.
