# Bloque 12 — Motor PDF, Etiquetas Térmicas y Códigos de Barras/QR — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el subsistema de generación de documentos PDF en memoria (etiqueta térmica 100×150mm con Code128/QR, etiquetas de lote en streaming, manifiesto A4) con auditoría en `documentos_generados`, bajo TDD y con la suite completa en verde.

**Architecture:** ZXing (Code128/QR → `BufferedImage`) + OpenPDF (maquetación PDF → `byte[]` / `OutputStream`). Capas: `service/pdf` con `BarcodeService`, `PesoUtil`, `EtiquetaPdfGenerator`, `ManifiestoPdfGenerator`; orquestación en `DocumentoPdfService`; API admin en `DocumentosController`. Cero I/O en disco.

**Tech Stack:** Java 17, Spring Boot 3.3.5, OpenPDF 1.3.40, ZXing 3.5.3, Flyway, JUnit 5 + AssertJ + Mockito + Awaitility.

## Global Constraints

- **Prohibido Lombok:** entidades, DTOs y modelos en Java puro (constructor vacío JPA, constructores parametrizados, getters/setters explícitos).
- **Inyección por constructor:** campos `private final` en servicios/componentes/controladores.
- **Migraciones Flyway:** `V6__create_documents_tables.sql` (SQL exacto del usuario) y `V7__add_batch_id_to_envios_tracking.sql`.
- **Memoria pura:** prohibido escribir ficheros temporales; solo `byte[]` / `OutputStream`.
- **Comandos de verificación:** compilación/test unitario local con `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=X,Y" -q` (los `-Dtest` con comas entre comillas). Tests de integración y suite completa en contenedor:
  `docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean test`
- **TDD:** test rojo → implementación mínima → test verde → commit por tarea.

---

## Task 1: Dependencias y Migraciones V6/V7

**Files:**
- Modify: `pom.xml` (bloque de dependencias tras `com.opencsv`)
- Create: `src/main/resources/db/migration/V6__create_documents_tables.sql`
- Create: `src/main/resources/db/migration/V7__add_batch_id_to_envios_tracking.sql`

**Interfaces:**
- Produces: dependencias Maven `com.google.zxing:core/javase:3.5.3`, `com.github.librepdf:openpdf:1.3.40`; tablas `documentos_generados` y columna `envios_tracking.batch_id` que las Tasks 2 y 7 validan en arranque.

- [ ] **Step 1: Añadir las dependencias al `pom.xml`**

Tras el bloque de `com.opencsv` (líneas ~96-100) insertar:

```xml
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.github.librepdf</groupId>
            <artifactId>openpdf</artifactId>
            <version>1.3.40</version>
        </dependency>
```

- [ ] **Step 2: Crear `V6__create_documents_tables.sql`** (contenido exacto del usuario)

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

- [ ] **Step 3: Crear `V7__add_batch_id_to_envios_tracking.sql`**

```sql
ALTER TABLE envios_tracking ADD COLUMN batch_id BIGINT NULL;
ALTER TABLE envios_tracking ADD CONSTRAINT fk_envios_batch
    FOREIGN KEY (batch_id) REFERENCES batch_imports(id) ON DELETE SET NULL;
ALTER TABLE envios_tracking ADD INDEX idx_envios_batch_id (batch_id);
```

- [ ] **Step 4: Verificar que compila y resuelve dependencias**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd -q -DskipTests compile`
Expected: exit 0 sin errores de resolución de `com.google.zxing` / `com.github.librepdf`.

- [ ] **Step 5: Commit**

```bash
git add pom.xml src/main/resources/db/migration/V6__create_documents_tables.sql src/main/resources/db/migration/V7__add_batch_id_to_envios_tracking.sql
git commit -m "feat(pdf): add OpenPDF/ZXing deps and V6/V7 migrations for documents and envio-batch link"
```

---

## Task 2: Modelo y Vínculo Envíos ↔ Lote

**Files:**
- Create: `src/main/java/com/monteastur/envios/model/TipoDocumento.java`
- Create: `src/main/java/com/monteastur/envios/model/DocumentoGenerado.java`
- Create: `src/main/java/com/monteastur/envios/repository/DocumentoGeneradoRepository.java`
- Modify: `src/main/java/com/monteastur/envios/model/EnvioTracking.java` (campo `batchId`)
- Modify: `src/main/java/com/monteastur/envios/repository/EnvioTrackingRepository.java` (2 métodos)
- Modify: `src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java:48-54`
- Create: `src/test/java/com/monteastur/envios/service/batch/BatchImportPersistenceServiceTest.java`
- Modify: `src/test/java/com/monteastur/envios/integration/CsvBatchImportIntegrationTest.java` (1 test nuevo)

**Interfaces:**
- Consumes: repositorios `BatchImportRepository`, `BatchImportErrorRepository`, `EnvioTrackingRepository` (existentes).
- Produces: `TipoDocumento` (enum `ETIQUETA_TERMICA, ETIQUETAS_LOTE, MANIFIESTO_CARGA`), `DocumentoGenerado` (`new DocumentoGenerado(TipoDocumento, String referenciaId, String nombreArchivo, int pesoBytes, String usuarioGeneracion)`), `DocumentoGeneradoRepository.findByOrderByFechaCreacionDesc()`, `findAllByTipoOrderByFechaCreacionDesc(TipoDocumento)`, `countByTipo(TipoDocumento)`; `EnvioTracking.getBatchId()/setBatchId(Long)`; `EnvioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(Long)` y `countByBatchId(Long)`. `BatchImportPersistenceService.procesarChunk` asigna `batchId` a cada envío.

- [ ] **Step 1: Escribir el test que falla (unitario del vínculo)**

`src/test/java/com/monteastur/envios/service/batch/BatchImportPersistenceServiceTest.java`:

```java
package com.monteastur.envios.service.batch;

import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchImportPersistenceServiceTest {

    @Mock private BatchImportRepository batchImportRepository;
    @Mock private BatchImportErrorRepository batchImportErrorRepository;
    @Mock private EnvioTrackingRepository envioTrackingRepository;

    private BatchImportPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new BatchImportPersistenceService(batchImportRepository,
                batchImportErrorRepository, envioTrackingRepository);
    }

    @Test
    void procesarChunk_asignaBatchIdAEnvios() {
        BatchImport lote = new BatchImport(null, "lote.csv", BatchImportEstado.EN_PROCESO);
        lote.setId(7L);
        when(batchImportRepository.findById(7L)).thenReturn(Optional.of(lote));

        EnvioTracking e1 = new EnvioTracking("MT-P1", "RECIBIDO", "Ana", "O", "D", "1 kg", "Docs");
        EnvioTracking e2 = new EnvioTracking("MT-P2", "RECIBIDO", "Luis", "O", "D", "2 kg", "Docs");

        service.procesarChunk(7L, List.of(e1, e2), List.of());

        assertThat(e1.getBatchId()).isEqualTo(7L);
        assertThat(e2.getBatchId()).isEqualTo(7L);
        verify(envioTrackingRepository).saveAll(List.of(e1, e2));
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=BatchImportPersistenceServiceTest" -q`
Expected: FAIL (compilación rota: no existe `EnvioTracking.getBatchId()`).

- [ ] **Step 3: Implementación mínima**

Crear `TipoDocumento.java`:

```java
package com.monteastur.envios.model;

public enum TipoDocumento {
    ETIQUETA_TERMICA,
    ETIQUETAS_LOTE,
    MANIFIESTO_CARGA
}
```

Crear `DocumentoGenerado.java`:

```java
package com.monteastur.envios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_generados")
public class DocumentoGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoDocumento tipo;

    @Column(name = "referencia_id", nullable = false, length = 100)
    private String referenciaId;

    @Column(name = "nombre_archivo", nullable = false, length = 255)
    private String nombreArchivo;

    @Column(name = "peso_bytes", nullable = false)
    private int pesoBytes;

    @Column(name = "usuario_generacion", length = 100)
    private String usuarioGeneracion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public DocumentoGenerado() {}

    public DocumentoGenerado(TipoDocumento tipo, String referenciaId, String nombreArchivo,
                             int pesoBytes, String usuarioGeneracion) {
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.nombreArchivo = nombreArchivo;
        this.pesoBytes = pesoBytes;
        this.usuarioGeneracion = usuarioGeneracion;
    }

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TipoDocumento getTipo() { return tipo; }
    public void setTipo(TipoDocumento tipo) { this.tipo = tipo; }
    public String getReferenciaId() { return referenciaId; }
    public void setReferenciaId(String referenciaId) { this.referenciaId = referenciaId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public int getPesoBytes() { return pesoBytes; }
    public void setPesoBytes(int pesoBytes) { this.pesoBytes = pesoBytes; }
    public String getUsuarioGeneracion() { return usuarioGeneracion; }
    public void setUsuarioGeneracion(String usuarioGeneracion) { this.usuarioGeneracion = usuarioGeneracion; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
```

Crear `DocumentoGeneradoRepository.java`:

```java
package com.monteastur.envios.repository;

import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoGeneradoRepository extends JpaRepository<DocumentoGenerado, Long> {
    List<DocumentoGenerado> findByOrderByFechaCreacionDesc();
    List<DocumentoGenerado> findAllByTipoOrderByFechaCreacionDesc(TipoDocumento tipo);
    long countByTipo(TipoDocumento tipo);
}
```

Modificar `EnvioTracking.java` — añadir el campo tras `cliente` (líneas 47-49) y sus getters/setters:

```java
    @Column(name = "batch_id")
    private Long batchId;
```

```java
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
```

Modificar `EnvioTrackingRepository.java` — añadir tras `findWithClienteByCodigoUnico`:

```java
    List<EnvioTracking> findByBatchIdOrderByCodigoUnicoAsc(Long batchId);
    long countByBatchId(Long batchId);
```

Modificar `BatchImportPersistenceService.java:54` — asignar el vínculo antes del `saveAll`:

```java
        envios.forEach(envio -> envio.setBatchId(batchId));
        envioTrackingRepository.saveAll(envios);
```

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=BatchImportPersistenceServiceTest" -q`
Expected: PASS (1 test, 0 failures).

- [ ] **Step 5: Añadir el test de integración del vínculo**

Añadir al final de `CsvBatchImportIntegrationTest` (antes de la llave de cierre), un test que verifica que la importación real asigna `batchId`:

```java
    @Test
    void importacion_asignaBatchIdAEnvios() throws Exception {
        Path fichero = escribirCsv(cabecera()
                + "MT-B6-01,RECIBIDO,María,,,,\n"
                + "MT-B6-02,ENTREGADO,Pedro,,,,\n");
        codigosCreados.add("MT-B6-01");
        codigosCreados.add("MT-B6-02");

        BatchImport lote = persistence.crearLote(null, "lote.csv");
        batchIds.add(lote.getId());
        csvBatchImportService.procesarLote(lote.getId(), fichero.toString(), null);

        esperarEstado(lote.getId(), BatchImportEstado.COMPLETADO);
        assertThat(envioTrackingRepository.findByCodigoUnico("MT-B6-01").orElseThrow().getBatchId())
                .isEqualTo(lote.getId());
        assertThat(envioTrackingRepository.findByCodigoUnico("MT-B6-02").orElseThrow().getBatchId())
                .isEqualTo(lote.getId());
        assertThat(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(lote.getId()))
                .hasSize(2);
    }
```

- [ ] **Step 6: Ejecutar la integración del vínculo (requiere DB/Redis)**

Run (contenedor):
`docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn test "-Dtest=CsvBatchImportIntegrationTest" -q`
Expected: PASS (7 tests, 0 failures). Las migraciones V6/V7 se aplican por Flyway al levantar el contexto.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/monteastur/envios/model/TipoDocumento.java src/main/java/com/monteastur/envios/model/DocumentoGenerado.java src/main/java/com/monteastur/envios/repository/DocumentoGeneradoRepository.java src/main/java/com/monteastur/envios/model/EnvioTracking.java src/main/java/com/monteastur/envios/repository/EnvioTrackingRepository.java src/main/java/com/monteastur/envios/service/batch/BatchImportPersistenceService.java src/test/java/com/monteastur/envios/service/batch/BatchImportPersistenceServiceTest.java src/test/java/com/monteastur/envios/integration/CsvBatchImportIntegrationTest.java
git commit -m "feat(pdf): document entity/repo and envio-batch link via batch_id"
```

---

## Task 3: `PesoUtil`

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/pdf/PesoUtil.java`
- Create: `src/test/java/com/monteastur/envios/service/pdf/PesoUtilTest.java`

**Interfaces:**
- Produces: `public static OptionalDouble parsear(String peso)` — usado por `ManifiestoPdfGenerator` (Task 6) para sumar pesos.

- [ ] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/pdf/PesoUtilTest.java`:

```java
package com.monteastur.envios.service.pdf;

import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;

class PesoUtilTest {

    @Test
    void parsear_pesoDecimalEspacioUnidad() {
        assertThat(PesoUtil.parsear("1.5 kg")).hasValue(1.5);
    }

    @Test
    void parsear_pesoEntero() {
        assertThat(PesoUtil.parsear("2")).hasValue(2.0);
    }

    @Test
    void parsear_commaDecimal() {
        assertThat(PesoUtil.parsear("1,5 kg")).hasValue(1.5);
    }

    @Test
    void parsear_soloNumeroYEspacios() {
        assertThat(PesoUtil.parsear("  12.5  ")).hasValue(12.5);
    }

    @Test
    void parsear_null_o_vacio_devuelveVacio() {
        assertThat(PesoUtil.parsear(null)).isEmpty();
        assertThat(PesoUtil.parsear("")).isEmpty();
        assertThat(PesoUtil.parsear("   ")).isEmpty();
    }

    @Test
    void parsear_invalido_devuelveVacio() {
        assertThat(PesoUtil.parsear("n/a")).isEmpty();
        assertThat(PesoUtil.parsear("peso no declarado")).isEmpty();
        assertThat(PesoUtil.parsear(".")).isEmpty();
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=PesoUtilTest" -q`
Expected: FAIL (no existe `PesoUtil`).

- [ ] **Step 3: Implementación mínima**

`src/main/java/com/monteastur/envios/service/pdf/PesoUtil.java`:

```java
package com.monteastur.envios.service.pdf;

import java.util.OptionalDouble;

public final class PesoUtil {

    private PesoUtil() {}

    public static OptionalDouble parsear(String peso) {
        if (peso == null || peso.isBlank()) {
            return OptionalDouble.empty();
        }
        String normalizado = peso.trim().replace(',', '.');
        int i = 0;
        while (i < normalizado.length() && (Character.isDigit(normalizado.charAt(i))
                || normalizado.charAt(i) == '.')) {
            i++;
        }
        if (i == 0) {
            return OptionalDouble.empty();
        }
        try {
            double valor = Double.parseDouble(normalizado.substring(0, i));
            return valor >= 0 ? OptionalDouble.of(valor) : OptionalDouble.empty();
        } catch (NumberFormatException ex) {
            return OptionalDouble.empty();
        }
    }
}
```

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=PesoUtilTest" -q`
Expected: PASS (6 tests, 0 failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/pdf/PesoUtil.java src/test/java/com/monteastur/envios/service/pdf/PesoUtilTest.java
git commit -m "feat(pdf): add PesoUtil tolerant parser for weight strings"
```

---

## Task 4: `BarcodeService` (ZXing)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/pdf/BarcodeService.java`
- Create: `src/test/java/com/monteastur/envios/service/pdf/BarcodeServiceTest.java`

**Interfaces:**
- Produces: `BufferedImage generarCode128(String contenido, int ancho, int alto)`, `BufferedImage generarQr(String contenido, int lado)`, `byte[] toPng(BufferedImage)` — consumidos por `EtiquetaPdfGenerator` (Task 5).

- [ ] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/pdf/BarcodeServiceTest.java`:

```java
package com.monteastur.envios.service.pdf;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarcodeServiceTest {

    private final BarcodeService service = new BarcodeService();

    @Test
    void generarCode128_devuelveImagenConDimensiones() {
        BufferedImage img = service.generarCode128("MT-2026-0001", 300, 80);
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(300);
        assertThat(img.getHeight()).isEqualTo(80);
    }

    @Test
    void generarQr_devuelveImagenCuadrada() {
        BufferedImage img = service.generarQr("https://tracking.example/MT-2026-0001", 200);
        assertThat(img).isNotNull();
        assertThat(img.getWidth()).isEqualTo(200);
        assertThat(img.getHeight()).isEqualTo(200);
    }

    @Test
    void generarCode128_contenidoVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.generarCode128("   ", 100, 50))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void generarQr_contenidoVacio_lanzaIllegalArgument() {
        assertThatThrownBy(() -> service.generarQr(null, 100))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toPng_devuelveBytesPng() {
        BufferedImage img = service.generarQr("MT-1", 100);
        byte[] png = service.toPng(img);
        assertThat(png).isNotEmpty();
        assertThat(png[0] & 0xFF).isEqualTo(0x89);
        assertThat(png[1] & 0xFF).isEqualTo(0x50);
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=BarcodeServiceTest" -q`
Expected: FAIL (no existe `BarcodeService`).

- [ ] **Step 3: Implementación mínima**

`src/main/java/com/monteastur/envios/service/pdf/BarcodeService.java`:

```java
package com.monteastur.envios.service.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Service
public class BarcodeService {

    private static final Map<EncodeHintType, Object> HINTS_CODE128 = new EnumMap<>(EncodeHintType.class);
    private static final Map<EncodeHintType, Object> HINTS_QR = new EnumMap<>(EncodeHintType.class);

    static {
        HINTS_CODE128.put(EncodeHintType.CHARACTER_SET, StandardCharsets.ISO_8859_1.name());
        HINTS_CODE128.put(EncodeHintType.MARGIN, 0);
        HINTS_QR.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        HINTS_QR.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        HINTS_QR.put(EncodeHintType.MARGIN, 1);
    }

    public BufferedImage generarCode128(String contenido, int ancho, int alto) {
        validar(contenido, "El contenido del código de barras no puede estar vacío");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.CODE_128,
                    ancho, alto, HINTS_CODE128);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException ex) {
            throw new IllegalArgumentException("No se pudo generar el código Code128", ex);
        }
    }

    public BufferedImage generarQr(String contenido, int lado) {
        validar(contenido, "El contenido del código QR no puede estar vacío");
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(contenido, BarcodeFormat.QR_CODE,
                    lado, lado, HINTS_QR);
            return MatrixToImageWriter.toBufferedImage(matrix);
        } catch (WriterException ex) {
            throw new IllegalArgumentException("No se pudo generar el código QR", ex);
        }
    }

    public byte[] toPng(BufferedImage imagen) {
        if (imagen == null) {
            throw new IllegalArgumentException("La imagen no puede ser nula");
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(imagen, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo codificar la imagen PNG", ex);
        }
    }

    private void validar(String contenido, String mensaje) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
```

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=BarcodeServiceTest" -q`
Expected: PASS (5 tests, 0 failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/pdf/BarcodeService.java src/test/java/com/monteastur/envios/service/pdf/BarcodeServiceTest.java
git commit -m "feat(pdf): add ZXing BarcodeService for Code128 and QR generation"
```

---

## Task 5: `EtiquetaPdfGenerator` (OpenPDF, 100×150mm)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/pdf/EtiquetaPdfGenerator.java`
- Create: `src/test/java/com/monteastur/envios/service/pdf/EtiquetaPdfGeneratorTest.java`

**Interfaces:**
- Consumes: `BarcodeService` (Task 4), `EnvioTracking` (existente).
- Produces: `byte[] generar(EnvioTracking envio, String trackingUrl)`, `Rectangle tamanoPagina()`, `void anadirEtiqueta(Document document, EnvioTracking envio, String trackingUrl)` — `anadirEtiqueta`/`tamanoPagina` los consume `DocumentoPdfService` (Task 7) para el streaming del lote.

- [ ] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/pdf/EtiquetaPdfGeneratorTest.java`:

```java
package com.monteastur.envios.service.pdf;

import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtiquetaPdfGeneratorTest {

    private final EtiquetaPdfGenerator generator = new EtiquetaPdfGenerator(new BarcodeService());

    @Test
    void generar_devuelvePdfValido() {
        EnvioTracking envio = new EnvioTracking("MT-2026-0099", "EN_TRANSITO", "María López",
                "Asturias, España", "Asunción, Paraguay", "1.5 kg", "Documentos");
        byte[] pdf = generator.generar(envio, "http://localhost:8080/tracking/MT-2026-0099");

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        assertThat(new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1))
                .contains("MT-2026-0099")
                .contains("María López");
    }

    @Test
    void tamanoPagina_es100x150mm() {
        assertThat(EtiquetaPdfGenerator.ANCHO_PT).isCloseTo(283.46f, org.assertj.core.data.Offset.offset(0.5f));
        assertThat(EtiquetaPdfGenerator.ALTO_PT).isCloseTo(425.2f, org.assertj.core.data.Offset.offset(0.5f));
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EtiquetaPdfGeneratorTest" -q`
Expected: FAIL (no existe `EtiquetaPdfGenerator`).

- [ ] **Step 3: Implementación mínima**

`src/main/java/com/monteastur/envios/service/pdf/EtiquetaPdfGenerator.java`:

```java
package com.monteastur.envios.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.monteastur.envios.model.EnvioTracking;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class EtiquetaPdfGenerator {

    public static final float ANCHO_PT = 100f * 72f / 25.4f;
    public static final float ALTO_PT = 150f * 72f / 25.4f;

    private static final Font TITULO = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font CODIGO = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font LABEL = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font VALOR = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font PIE = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);

    private final BarcodeService barcodeService;

    public EtiquetaPdfGenerator(BarcodeService barcodeService) {
        this.barcodeService = barcodeService;
    }

    public byte[] generar(EnvioTracking envio, String trackingUrl) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(tamanoPagina(), 18f, 18f, 18f, 18f);
            PdfWriter.getInstance(document, out);
            document.open();
            anadirEtiqueta(document, envio, trackingUrl);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar la etiqueta PDF", ex);
        }
    }

    public Rectangle tamanoPagina() {
        return new Rectangle(ANCHO_PT, ALTO_PT);
    }

    public void anadirEtiqueta(Document document, EnvioTracking envio, String trackingUrl) {
        document.add(new Paragraph("MONTEASTUR ENVÍOS", TITULO));
        document.add(new Paragraph("ETIQUETA DE ENVÍO", LABEL));
        document.add(new Paragraph("Código: " + envio.getCodigoUnico(), CODIGO));
        try {
            Image code128 = Image.getInstance(
                    barcodeService.generarCode128(envio.getCodigoUnico(), 500, 120), null, true);
            code128.scaleToFit(235f, 55f);
            code128.setAlignment(Element.ALIGN_CENTER);
            document.add(code128);

            Image qr = Image.getInstance(barcodeService.generarQr(trackingUrl, 200), null, true);
            qr.scaleToFit(60f, 60f);
            qr.setAlignment(Element.ALIGN_CENTER);
            document.add(qr);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudieron incrustar los códigos en la etiqueta", ex);
        }
        PdfPTable tabla = new PdfPTable(2);
        tabla.setWidthPercentage(100);
        anadirFila(tabla, "DESTINATARIO", envio.getDestinatario());
        anadirFila(tabla, "RUTA", (envio.getOrigen() != null ? envio.getOrigen() : "—")
                + " → " + (envio.getDestino() != null ? envio.getDestino() : "—"));
        anadirFila(tabla, "PESO", envio.getPeso() != null ? envio.getPeso() : "—");
        anadirFila(tabla, "CONTENIDO", envio.getContenido() != null ? envio.getContenido() : "—");
        anadirFila(tabla, "ESTADO", envio.getEstado() + " — " + envio.getUbicacionActual());
        document.add(tabla);
        document.add(new Paragraph("Emitida: " + FECHA.format(LocalDateTime.now()), PIE));
    }

    private void anadirFila(PdfPTable tabla, String label, String valor) {
        PdfPCell celdaLabel = new PdfPCell(new Phrase(label, LABEL));
        celdaLabel.setBorder(Rectangle.NO_BORDER);
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor != null ? valor : "—", VALOR));
        celdaValor.setBorder(Rectangle.NO_BORDER);
        tabla.addCell(celdaLabel);
        tabla.addCell(celdaValor);
    }
}
```

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EtiquetaPdfGeneratorTest" -q`
Expected: PASS (2 tests, 0 failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/pdf/EtiquetaPdfGenerator.java src/test/java/com/monteastur/envios/service/pdf/EtiquetaPdfGeneratorTest.java
git commit -m "feat(pdf): add thermal label generator (100x150mm) with Code128 and QR"
```

---

## Task 6: `ManifiestoPdfGenerator` (OpenPDF, A4)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/pdf/ManifiestoPdfGenerator.java`
- Create: `src/test/java/com/monteastur/envios/service/pdf/ManifiestoPdfGeneratorTest.java`

**Interfaces:**
- Consumes: `PesoUtil` (Task 3), `EnvioTracking`.
- Produces: `byte[] generar(Long batchId, List<EnvioTracking> envios, String clienteNombre)` — consumido por `DocumentoPdfService` (Task 7).

- [ ] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/pdf/ManifiestoPdfGeneratorTest.java`:

```java
package com.monteastur.envios.service.pdf;

import com.monteastur.envios.model.EnvioTracking;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ManifiestoPdfGeneratorTest {

    private final ManifiestoPdfGenerator generator = new ManifiestoPdfGenerator();

    @Test
    void generar_devuelvePdfValidoConTotales() {
        List<EnvioTracking> envios = List.of(
                new EnvioTracking("MT-M1", "RECIBIDO", "Ana", "O", "D", "1,5 kg", "Documentos"),
                new EnvioTracking("MT-M2", "ENTREGADO", "Luis", "O", "D", "2 kg", "Caja"));

        byte[] pdf = generator.generar(42L, envios, "Cliente Demo");

        assertThat(pdf).isNotEmpty();
        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido)
                .contains("MANIFIESTO DE CARGA")
                .contains("Cliente Demo")
                .contains("3.50")
                .contains("MT-M1");
    }

    @Test
    void generar_pesosInvalido_muestraGuion() {
        List<EnvioTracking> envios = List.of(
                new EnvioTracking("MT-M3", "RECIBIDO", "Ana", "O", "D", "n/a", "Documentos"));

        byte[] pdf = generator.generar(43L, envios, null);

        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido).doesNotContain("0.00");
    }

    @Test
    void anchoA4_es595pt() {
        assertThat(ManifiestoPdfGenerator.ANCHO_A4_PT).isCloseTo(595.28f, org.assertj.core.data.Offset.offset(0.5f));
        assertThat(ManifiestoPdfGenerator.ALTO_A4_PT).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.5f));
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=ManifiestoPdfGeneratorTest" -q`
Expected: FAIL (no existe `ManifiestoPdfGenerator`).

- [ ] **Step 3: Implementación mínima**

`src/main/java/com/monteastur/envios/service/pdf/ManifiestoPdfGenerator.java`:

```java
package com.monteastur.envios.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.monteastur.envios.model.EnvioTracking;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

@Component
public class ManifiestoPdfGenerator {

    public static final float ANCHO_A4_PT = 595.28f;
    public static final float ALTO_A4_PT = 841.89f;

    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font CABECERA_TABLA = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Font CELDA = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TOTAL = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT);

    public byte[] generar(Long batchId, List<EnvioTracking> envios, String clienteNombre) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40f, 40f, 40f, 40f);
            PdfWriter.getInstance(document, out);
            document.open();
            anadirCabecera(document, batchId, clienteNombre);
            anadirTabla(document, envios);
            anadirFirma(document);
            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el manifiesto PDF", ex);
        }
    }

    private void anadirCabecera(Document document, Long batchId, String clienteNombre) {
        document.add(new Paragraph("MONTEASTUR ENVÍOS", TITULO));
        document.add(new Paragraph("MANIFIESTO DE CARGA / GUÍA DE REMISIÓN", SUBTITULO));
        document.add(new Paragraph("Lote: " + batchId + "   Fecha: " + FECHA.format(LocalDate.now()), SUBTITULO));
        document.add(new Paragraph("Cliente: " + (clienteNombre != null ? clienteNombre : "—"), SUBTITULO));
    }

    private void anadirTabla(Document document, List<EnvioTracking> envios) {
        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidths(new float[]{2.2f, 2.6f, 2.8f, 1.0f, 1.8f});
        tabla.setWidthPercentage(100);
        for (String cabecera : new String[]{"CÓDIGO", "DESTINATARIO", "CONTENIDO", "PESO", "ESTADO"}) {
            PdfPCell cell = new PdfPCell(new Phrase(cabecera, CABECERA_TABLA));
            cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
            tabla.addCell(cell);
        }
        double totalPeso = 0;
        for (EnvioTracking e : envios) {
            tabla.addCell(new PdfPCell(new Phrase(e.getCodigoUnico(), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getDestinatario()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getContenido()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getPeso()), CELDA)));
            tabla.addCell(new PdfPCell(new Phrase(normalizar(e.getEstado()), CELDA)));
            OptionalDouble peso = PesoUtil.parsear(e.getPeso());
            if (peso.isPresent()) {
                totalPeso += peso.getAsDouble();
            }
        }
        PdfPCell totalLabel = new PdfPCell(new Phrase("TOTAL (" + envios.size() + " bultos)", TOTAL));
        totalLabel.setColspan(4);
        PdfPCell totalPesoCell = new PdfPCell(new Phrase(
                String.format(Locale.ROOT, "%.2f kg", totalPeso), TOTAL));
        tabla.addCell(totalLabel);
        tabla.addCell(totalPesoCell);
        document.add(tabla);
    }

    private void anadirFirma(Document document) {
        document.add(new Paragraph("\n\n"));
        document.add(new Paragraph("Firma de despacho", new Font(Font.HELVETICA, 12, Font.BOLD)));
        document.add(new Paragraph("_______________________________________", SUBTITULO));
        document.add(new Paragraph("Firma y aclaración del agente autorizado", SUBTITULO));
    }

    private String normalizar(String valor) {
        return valor != null ? valor : "—";
    }
}
```

Nota: `PageSize.A4` es `Rectangle(595, 842)`; las constantes `ANCHO_A4_PT`/`ALTO_A4_PT` documentan el tamaño y las verifica el test.

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=ManifiestoPdfGeneratorTest" -q`
Expected: PASS (3 tests, 0 failures). Si `contains("3.50")` falla por escritura de fuente, ajustar la aserción a la suma exacta `1.5+2.0=3.50` (formato `%.2f` garantiza `3.50`).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/pdf/ManifiestoPdfGenerator.java src/test/java/com/monteastur/envios/service/pdf/ManifiestoPdfGeneratorTest.java
git commit -m "feat(pdf): add A4 load manifest generator with weight totals and signature"
```

---

## Task 7: `DocumentoPdfService` + DTO + Propiedades

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/DocumentoPdfService.java`
- Create: `src/main/java/com/monteastur/envios/dto/api/DocumentoGeneradoDto.java`
- Modify: `src/main/resources/application.properties` (bloque `app.pdf.*`)
- Create: `src/test/java/com/monteastur/envios/service/DocumentoPdfServiceTest.java`

**Interfaces:**
- Consumes: `EnvioTrackingRepository`, `ClienteRepository`, `DocumentoGeneradoRepository`, `BatchImportPersistenceService`, `EtiquetaPdfGenerator`, `ManifiestoPdfGenerator` (Tasks 2/5/6).
- Produces: `byte[] generarEtiqueta(String codigoUnico, String usuario)`, `void generarEtiquetasLote(Long batchId, String usuario, OutputStream destino)`, `byte[] generarManifiesto(Long batchId, String usuario)`, `List<DocumentoGenerado> listarEmisiones(TipoDocumento tipo)` — consumidos por `DocumentosController` (Task 8).

- [ ] **Step 1: Añadir las propiedades a `application.properties`**

Insertar al final del fichero (tras la sección REDIS/SESSION):

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

- [ ] **Step 2: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/DocumentoPdfServiceTest.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.Cliente;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.pdf.EtiquetaPdfGenerator;
import com.monteastur.envios.service.pdf.ManifiestoPdfGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentoPdfServiceTest {

    @Mock private EnvioTrackingRepository envioTrackingRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private DocumentoGeneradoRepository documentoRepository;
    @Mock private BatchImportPersistenceService persistence;
    @Mock private EtiquetaPdfGenerator etiquetaGenerator;
    @Mock private ManifiestoPdfGenerator manifiestoGenerator;

    private DocumentoPdfService service;

    @BeforeEach
    void setUp() {
        service = new DocumentoPdfService(envioTrackingRepository, clienteRepository,
                documentoRepository, persistence, etiquetaGenerator, manifiestoGenerator,
                true, 5000, "http://localhost:8080/tracking");
    }

    private EnvioTracking envio() {
        return new EnvioTracking("MT-D1", "RECIBIDO", "Ana", "O", "D", "1 kg", "Docs");
    }

    @Test
    void generarEtiqueta_generaYAudita() {
        when(envioTrackingRepository.findByCodigoUnico("MT-D1")).thenReturn(Optional.of(envio()));
        when(etiquetaGenerator.generar(any(), any())).thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        byte[] pdf = service.generarEtiqueta("MT-D1", "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        ArgumentCaptor<DocumentoGenerado> captor = ArgumentCaptor.forClass(DocumentoGenerado.class);
        verify(documentoRepository).save(captor.capture());
        DocumentoGenerado audit = captor.getValue();
        assertThat(audit.getTipo()).isEqualTo(TipoDocumento.ETIQUETA_TERMICA);
        assertThat(audit.getReferenciaId()).isEqualTo("MT-D1");
        assertThat(audit.getPesoBytes()).isEqualTo(4);
        assertThat(audit.getUsuarioGeneracion()).isEqualTo("admin");
    }

    @Test
    void generarEtiqueta_envioInexistente_lanza404() {
        when(envioTrackingRepository.findByCodigoUnico("MT-NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generarEtiqueta("MT-NOPE", "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void generarEtiqueta_deshabilitado_lanza400() {
        DocumentoPdfService apagado = new DocumentoPdfService(envioTrackingRepository,
                clienteRepository, documentoRepository, persistence, etiquetaGenerator,
                manifiestoGenerator, false, 5000, "http://localhost:8080/tracking");

        assertThatThrownBy(() -> apagado.generarEtiqueta("MT-D1", "admin"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void generarEtiquetasLote_streamingYAudita() throws Exception {
        BatchImport lote = new BatchImport(null, "lote.csv", BatchImportEstado.COMPLETADO);
        lote.setId(9L);
        when(persistence.obtenerLote(9L)).thenReturn(lote);
        when(envioTrackingRepository.countByBatchId(9L)).thenReturn(2L);
        when(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(9L))
                .thenReturn(List.of(envio()));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.generarEtiquetasLote(9L, "admin", out);

        assertThat(out.toByteArray()).isEmpty();
        ArgumentCaptor<DocumentoGenerado> captor = ArgumentCaptor.forClass(DocumentoGenerado.class);
        verify(documentoRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoDocumento.ETIQUETAS_LOTE);
        assertThat(captor.getValue().getReferenciaId()).isEqualTo("9");
    }

    @Test
    void generarEtiquetasLote_superaMaxPages_lanza400() {
        when(persistence.obtenerLote(9L)).thenReturn(new BatchImport(null, "l", BatchImportEstado.COMPLETADO));
        when(envioTrackingRepository.countByBatchId(9L)).thenReturn(6000L);

        assertThatThrownBy(() -> service.generarEtiquetasLote(9L, "admin", new ByteArrayOutputStream()))
                .isInstanceOf(BadRequestException.class);
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void generarManifiesto_generaYAudita() {
        BatchImport lote = new BatchImport(5L, "lote.csv", BatchImportEstado.COMPLETADO);
        lote.setId(9L);
        when(persistence.obtenerLote(9L)).thenReturn(lote);
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(new Cliente("c@x.com", "p", "Cliente X", "+595")));
        when(envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(9L)).thenReturn(List.of(envio()));
        when(manifiestoGenerator.generar(9L, List.of(envio()), "Cliente X"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        byte[] pdf = service.generarManifiesto(9L, "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        verify(documentoRepository).save(any(DocumentoGenerado.class));
    }

    @Test
    void listarEmisiones_sinTipo_devuelveTodas() {
        when(documentoRepository.findByOrderByFechaCreacionDesc()).thenReturn(List.of());

        assertThat(service.listarEmisiones(null)).isEmpty();
        verify(documentoRepository, times(1)).findByOrderByFechaCreacionDesc();
    }
}
```

- [ ] **Step 3: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=DocumentoPdfServiceTest" -q`
Expected: FAIL (no existe `DocumentoPdfService`).

- [ ] **Step 4: Implementación mínima**

Crear `src/main/java/com/monteastur/envios/service/DocumentoPdfService.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import com.monteastur.envios.service.pdf.EtiquetaPdfGenerator;
import com.monteastur.envios.service.pdf.ManifiestoPdfGenerator;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class DocumentoPdfService {

    private final EnvioTrackingRepository envioTrackingRepository;
    private final ClienteRepository clienteRepository;
    private final DocumentoGeneradoRepository documentoRepository;
    private final BatchImportPersistenceService persistence;
    private final EtiquetaPdfGenerator etiquetaGenerator;
    private final ManifiestoPdfGenerator manifiestoGenerator;
    private final boolean enabled;
    private final int maxPages;
    private final String trackingBaseUrl;

    public DocumentoPdfService(EnvioTrackingRepository envioTrackingRepository,
                               ClienteRepository clienteRepository,
                               DocumentoGeneradoRepository documentoRepository,
                               BatchImportPersistenceService persistence,
                               EtiquetaPdfGenerator etiquetaGenerator,
                               ManifiestoPdfGenerator manifiestoGenerator,
                               @Value("${app.pdf.enabled:true}") boolean enabled,
                               @Value("${app.pdf.max-pages:5000}") int maxPages,
                               @Value("${app.pdf.tracking.base-url:http://localhost:8080/tracking}") String trackingBaseUrl) {
        this.envioTrackingRepository = envioTrackingRepository;
        this.clienteRepository = clienteRepository;
        this.documentoRepository = documentoRepository;
        this.persistence = persistence;
        this.etiquetaGenerator = etiquetaGenerator;
        this.manifiestoGenerator = manifiestoGenerator;
        this.enabled = enabled;
        this.maxPages = maxPages;
        this.trackingBaseUrl = trackingBaseUrl;
    }

    public byte[] generarEtiqueta(String codigoUnico, String usuario) {
        verificarHabilitado();
        EnvioTracking envio = envioTrackingRepository.findByCodigoUnico(codigoUnico)
                .orElseThrow(() -> new ResourceNotFoundException("Envío no encontrado: " + codigoUnico));
        byte[] pdf = etiquetaGenerator.generar(envio, urlTracking(codigoUnico));
        auditar(TipoDocumento.ETIQUETA_TERMICA, codigoUnico, "etiqueta-" + codigoUnico + ".pdf", pdf.length, usuario);
        return pdf;
    }

    public void generarEtiquetasLote(Long batchId, String usuario, OutputStream destino) {
        verificarHabilitado();
        persistence.obtenerLote(batchId);
        long total = envioTrackingRepository.countByBatchId(batchId);
        if (total > maxPages) {
            throw new BadRequestException("El lote tiene " + total
                    + " envíos y el máximo permitido por descarga es " + maxPages);
        }
        List<EnvioTracking> envios = envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(batchId);
        CountingOutputStream contador = new CountingOutputStream(destino);
        Document document = new Document(etiquetaGenerator.tamanoPagina(), 18f, 18f, 18f, 18f);
        try {
            PdfWriter writer = PdfWriter.getInstance(document, contador);
            writer.setCloseStream(false);
            document.open();
            for (int i = 0; i < envios.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                etiquetaGenerator.anadirEtiqueta(document, envios.get(i), urlTracking(envios.get(i).getCodigoUnico()));
            }
            document.close();
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el PDF de etiquetas del lote " + batchId, ex);
        }
        auditar(TipoDocumento.ETIQUETAS_LOTE, String.valueOf(batchId),
                "etiquetas-lote-" + batchId + ".pdf", contador.getContador(), usuario);
    }

    public byte[] generarManifiesto(Long batchId, String usuario) {
        verificarHabilitado();
        BatchImport lote = persistence.obtenerLote(batchId);
        String clienteNombre = nombreCliente(lote.getClienteId());
        List<EnvioTracking> envios = envioTrackingRepository.findByBatchIdOrderByCodigoUnicoAsc(batchId);
        byte[] pdf = manifiestoGenerator.generar(batchId, envios, clienteNombre);
        auditar(TipoDocumento.MANIFIESTO_CARGA, String.valueOf(batchId),
                "manifiesto-lote-" + batchId + ".pdf", pdf.length, usuario);
        return pdf;
    }

    public List<DocumentoGenerado> listarEmisiones(TipoDocumento tipo) {
        if (tipo == null) {
            return documentoRepository.findByOrderByFechaCreacionDesc();
        }
        return documentoRepository.findAllByTipoOrderByFechaCreacionDesc(tipo);
    }

    private String nombreCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteRepository.findById(clienteId).map(cliente -> cliente.getNombre()).orElse(null);
    }

    private String urlTracking(String codigo) {
        String base = trackingBaseUrl.endsWith("/")
                ? trackingBaseUrl.substring(0, trackingBaseUrl.length() - 1)
                : trackingBaseUrl;
        return base + "/" + codigo;
    }

    private void auditar(TipoDocumento tipo, String referenciaId, String nombreArchivo,
                         int pesoBytes, String usuario) {
        documentoRepository.save(new DocumentoGenerado(tipo, referenciaId, nombreArchivo, pesoBytes, usuario));
    }

    private void verificarHabilitado() {
        if (!enabled) {
            throw new BadRequestException("La generación de documentos PDF está deshabilitada");
        }
    }

    private static final class CountingOutputStream extends OutputStream {
        private final OutputStream destino;
        private int contador;

        private CountingOutputStream(OutputStream destino) {
            this.destino = destino;
        }

        @Override
        public void write(int b) throws IOException {
            destino.write(b);
            contador++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            destino.write(b, off, len);
            contador += len;
        }

        @Override
        public void flush() throws IOException {
            destino.flush();
        }

        int getContador() {
            return contador;
        }
    }
}
```

Crear `src/main/java/com/monteastur/envios/dto/api/DocumentoGeneradoDto.java`:

```java
package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.DocumentoGenerado;

public class DocumentoGeneradoDto {

    private final Long id;
    private final String tipo;
    private final String referenciaId;
    private final String nombreArchivo;
    private final int pesoBytes;
    private final String usuarioGeneracion;
    private final String fechaCreacion;

    private DocumentoGeneradoDto(Long id, String tipo, String referenciaId, String nombreArchivo,
                                 int pesoBytes, String usuarioGeneracion, String fechaCreacion) {
        this.id = id;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.nombreArchivo = nombreArchivo;
        this.pesoBytes = pesoBytes;
        this.usuarioGeneracion = usuarioGeneracion;
        this.fechaCreacion = fechaCreacion;
    }

    public static DocumentoGeneradoDto from(DocumentoGenerado doc) {
        return new DocumentoGeneradoDto(doc.getId(), doc.getTipo().name(), doc.getReferenciaId(),
                doc.getNombreArchivo(), doc.getPesoBytes(), doc.getUsuarioGeneracion(),
                doc.getFechaCreacion() != null ? doc.getFechaCreacion().toString() : null);
    }

    public Long getId() { return id; }
    public String getTipo() { return tipo; }
    public String getReferenciaId() { return referenciaId; }
    public String getNombreArchivo() { return nombreArchivo; }
    public int getPesoBytes() { return pesoBytes; }
    public String getUsuarioGeneracion() { return usuarioGeneracion; }
    public String getFechaCreacion() { return fechaCreacion; }
}
```

- [ ] **Step 5: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=DocumentoPdfServiceTest" -q`
Expected: PASS (7 tests, 0 failures).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/DocumentoPdfService.java src/main/java/com/monteastur/envios/dto/api/DocumentoGeneradoDto.java src/main/resources/application.properties src/test/java/com/monteastur/envios/service/DocumentoPdfServiceTest.java
git commit -m "feat(pdf): add DocumentoPdfService orchestration, audit DTO and app.pdf properties"
```

---

## Task 8: `DocumentosController` REST Admin

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/api/DocumentosController.java`
- Create: `src/test/java/com/monteastur/envios/controller/api/DocumentosControllerTest.java`

**Interfaces:**
- Consumes: `DocumentoPdfService` (Task 7) y `DocumentoGeneradoDto`.
- Produces: endpoints `GET /api/v1/admin/documentos/envios/{codigo}/etiqueta`, `GET /api/v1/admin/documentos/lotes/{batchId}/etiquetas`, `GET /api/v1/admin/documentos/lotes/{batchId}/manifiesto`, `GET /api/v1/admin/documentos?tipo=`.

- [ ] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/controller/api/DocumentosControllerTest.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.DocumentoPdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import java.io.OutputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentosController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@WithMockUser(username = "admin", roles = "ADMIN")
class DocumentosControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentoPdfService documentoPdfService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    @MockBean
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    void etiqueta_retorna200PdfInline() throws Exception {
        when(documentoPdfService.generarEtiqueta("MT-1", "admin"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/v1/admin/documentos/envios/MT-1/etiqueta"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "inline; filename=\"etiqueta-MT-1.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiqueta_envioInexistente_retorna404() throws Exception {
        when(documentoPdfService.generarEtiqueta("MT-NOPE", "admin"))
                .thenThrow(new com.monteastur.envios.exception.ResourceNotFoundException("Envío no encontrado: MT-NOPE"));

        mockMvc.perform(get("/api/v1/admin/documentos/envios/MT-NOPE/etiqueta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void etiquetasLote_retorna200PdfAttachment() throws Exception {
        doAnswer(inv -> {
            OutputStream out = inv.getArgument(2);
            out.write(new byte[]{'%', 'P', 'D', 'F'});
            return null;
        }).when(documentoPdfService).generarEtiquetasLote(eq(9L), eq("admin"), any());

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/etiquetas"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"etiquetas-lote-9.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void etiquetasLote_superaTope_retorna400() throws Exception {
        when(documentoPdfService.generarEtiquetasLote(anyLong(), anyString(), any()))
                .thenThrow(new com.monteastur.envios.exception.BadRequestException("El lote tiene 6000 envíos"));

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/etiquetas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void manifiesto_retorna200PdfAttachment() throws Exception {
        when(documentoPdfService.generarManifiesto(9L, "admin"))
                .thenReturn(new byte[]{'%', 'P', 'D', 'F'});

        mockMvc.perform(get("/api/v1/admin/documentos/lotes/9/manifiesto"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"manifiesto-lote-9.pdf\""))
                .andExpect(content().bytes(new byte[]{'%', 'P', 'D', 'F'}));
    }

    @Test
    void listar_retornaJsonDeEmisiones() throws Exception {
        DocumentoGenerado doc = new DocumentoGenerado(TipoDocumento.ETIQUETA_TERMICA,
                "MT-1", "etiqueta-MT-1.pdf", 1234, "admin");
        when(documentoPdfService.listarEmisiones(null)).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/admin/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("ETIQUETA_TERMICA"))
                .andExpect(jsonPath("$[0].referenciaId").value("MT-1"))
                .andExpect(jsonPath("$[0].pesoBytes").value(1234))
                .andExpect(jsonPath("$[0].usuarioGeneracion").value("admin"));
    }

    @Test
    void listar_conTipo_filtra() throws Exception {
        when(documentoPdfService.listarEmisiones(TipoDocumento.MANIFIESTO_CARGA)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/documentos").param("tipo", "MANIFIESTO_CARGA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_redirigeAlLogin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/documentos"))
                .andExpect(status().is3xxRedirection());
    }
}
```

- [ ] **Step 2: Ejecutar el test para verlo fallar (rojo)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=DocumentosControllerTest" -q`
Expected: FAIL (no existe `DocumentosController`).

- [ ] **Step 3: Implementación mínima**

`src/main/java/com/monteastur/envios/controller/api/DocumentosController.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.DocumentoGeneradoDto;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.service.DocumentoPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@Tag(name = "Documentos Admin", description = "Generación de PDFs, etiquetas y auditoría (requiere ROLE_ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/documentos")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class DocumentosController {

    private final DocumentoPdfService documentoPdfService;

    public DocumentosController(DocumentoPdfService documentoPdfService) {
        this.documentoPdfService = documentoPdfService;
    }

    @Operation(summary = "Etiqueta térmica de un envío", description = "PDF 100x150mm con Code128 y QR del tracking")
    @ApiResponse(responseCode = "200", description = "PDF de la etiqueta (inline)")
    @ApiResponse(responseCode = "404", description = "Envío no encontrado")
    @GetMapping("/envios/{codigo}/etiqueta")
    public ResponseEntity<byte[]> etiqueta(@PathVariable String codigo, Authentication authentication) {
        byte[] pdf = documentoPdfService.generarEtiqueta(codigo, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"etiqueta-" + codigo + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Etiquetas térmicas de un lote", description = "PDF multipágina en streaming de todas las etiquetas del lote")
    @ApiResponse(responseCode = "200", description = "PDF multipágina (attachment)")
    @ApiResponse(responseCode = "400", description = "El lote supera el máximo de etiquetas")
    @GetMapping("/lotes/{batchId}/etiquetas")
    public void etiquetasLote(@PathVariable Long batchId, Authentication authentication,
                              HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"etiquetas-lote-" + batchId + ".pdf\"");
        documentoPdfService.generarEtiquetasLote(batchId, authentication.getName(), response.getOutputStream());
    }

    @Operation(summary = "Manifiesto de carga del lote", description = "PDF A4 tabulado con totales y firma de despacho")
    @ApiResponse(responseCode = "200", description = "PDF del manifiesto (attachment)")
    @ApiResponse(responseCode = "404", description = "Lote no encontrado")
    @GetMapping("/lotes/{batchId}/manifiesto")
    public ResponseEntity<byte[]> manifiesto(@PathVariable Long batchId, Authentication authentication) {
        byte[] pdf = documentoPdfService.generarManifiesto(batchId, authentication.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"manifiesto-lote-" + batchId + ".pdf\"")
                .body(pdf);
    }

    @Operation(summary = "Listado de emisiones", description = "Auditoría de documentos generados, filtrable por tipo")
    @ApiResponse(responseCode = "200", description = "Lista de emisiones")
    @GetMapping
    public ResponseEntity<List<DocumentoGeneradoDto>> listar(
            @RequestParam(required = false) TipoDocumento tipo) {
        return ResponseEntity.ok(documentoPdfService.listarEmisiones(tipo).stream()
                .map(DocumentoGeneradoDto::from)
                .toList());
    }
}
```

- [ ] **Step 4: Ejecutar el test para verlo pasar (verde)**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=DocumentosControllerTest" -q`
Expected: PASS (8 tests, 0 failures).

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/DocumentosController.java src/test/java/com/monteastur/envios/controller/api/DocumentosControllerTest.java
git commit -m "feat(pdf): add admin DocumentosController for labels, manifests and audit list"
```

---

## Task 9: Integración End-to-End + Suite Completa + Handoff

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/DocumentosIntegrationTest.java`
- Modify: `docs/handoff.md`

**Interfaces:**
- Consumes: beans reales `DocumentoPdfService`, repos, `BatchImportPersistenceService`, generadores.

- [ ] **Step 1: Escribir el test de integración**

`src/test/java/com/monteastur/envios/integration/DocumentosIntegrationTest.java`:

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.model.BatchImportEstado;
import com.monteastur.envios.model.DocumentoGenerado;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.TipoDocumento;
import com.monteastur.envios.repository.BatchImportErrorRepository;
import com.monteastur.envios.repository.BatchImportRepository;
import com.monteastur.envios.repository.DocumentoGeneradoRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.service.DocumentoPdfService;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class DocumentosIntegrationTest {

    @Autowired private DocumentoPdfService documentoPdfService;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private BatchImportRepository batchImportRepository;
    @Autowired private BatchImportErrorRepository batchImportErrorRepository;
    @Autowired private DocumentoGeneradoRepository documentoRepository;
    @Autowired private BatchImportPersistenceService persistence;

    @MockBean private EmailService emailService;

    private final List<Long> batchIds = new ArrayList<>();
    private final List<Long> envioIds = new ArrayList<>();
    private final List<Long> documentoIds = new ArrayList<>();

    @AfterEach
    void limpiar() {
        documentoIds.forEach(documentoRepository::deleteById);
        documentoIds.clear();
        batchIds.forEach(batchId -> {
            batchImportErrorRepository.deleteAll(batchImportErrorRepository.findByBatchIdOrderByLineaNumeroAsc(batchId));
            batchImportRepository.deleteById(batchId);
        });
        batchIds.clear();
        envioIds.forEach(envioTrackingRepository::deleteById);
        envioIds.clear();
    }

    private EnvioTracking guardarEnvio(String codigo, String peso) {
        EnvioTracking envio = new EnvioTracking(codigo, "RECIBIDO", "Ana Test",
                "Asturias", "Asunción", peso, "Documentos");
        EnvioTracking guardado = envioTrackingRepository.save(envio);
        envioIds.add(guardado.getId());
        return guardado;
    }

    private BatchImport crearLote() {
        BatchImport lote = persistence.crearLote(null, "lote-int.csv");
        batchIds.add(lote.getId());
        return lote;
    }

    @Test
    void generaEtiqueta_retornaPdfYAudita() {
        guardarEnvio("MT-INT-01", "1.5 kg");

        byte[] pdf = documentoPdfService.generarEtiqueta("MT-INT-01", "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        List<DocumentoGenerado> emisiones = documentoRepository.findByOrderByFechaCreacionDesc();
        DocumentoGenerado audit = emisiones.stream()
                .filter(d -> d.getReferenciaId().equals("MT-INT-01"))
                .findFirst().orElseThrow();
        assertThat(audit.getTipo()).isEqualTo(TipoDocumento.ETIQUETA_TERMICA);
        assertThat(audit.getPesoBytes()).isEqualTo(pdf.length);
        assertThat(audit.getUsuarioGeneracion()).isEqualTo("admin");
        documentoIds.add(audit.getId());
    }

    @Test
    void generaEtiqueta_envioInexistente_lanza404() {
        assertThatThrownBy(() -> documentoPdfService.generarEtiqueta("MT-NO-EXISTE", "admin"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void generaEtiquetasDeLote_streamingYAudita() throws Exception {
        BatchImport lote = crearLote();
        EnvioTracking e1 = guardarEnvio("MT-INT-B1", "1 kg");
        EnvioTracking e2 = guardarEnvio("MT-INT-B2", "2 kg");
        e1.setBatchId(lote.getId());
        e2.setBatchId(lote.getId());
        envioTrackingRepository.saveAll(List.of(e1, e2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        documentoPdfService.generarEtiquetasLote(lote.getId(), "admin", out);

        assertThat(out.toByteArray()).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        DocumentoGenerado audit = documentoRepository.findByOrderByFechaCreacionDesc().stream()
                .filter(d -> d.getTipo() == TipoDocumento.ETIQUETAS_LOTE
                        && d.getReferenciaId().equals(String.valueOf(lote.getId())))
                .findFirst().orElseThrow();
        assertThat(audit.getPesoBytes()).isGreaterThan(0);
        documentoIds.add(audit.getId());
    }

    @Test
    void generaManifiesto_retornaPdfYAudita() {
        BatchImport lote = crearLote();
        EnvioTracking e1 = guardarEnvio("MT-INT-M1", "1,5 kg");
        EnvioTracking e2 = guardarEnvio("MT-INT-M2", "2 kg");
        e1.setBatchId(lote.getId());
        e2.setBatchId(lote.getId());
        envioTrackingRepository.saveAll(List.of(e1, e2));

        byte[] pdf = documentoPdfService.generarManifiesto(lote.getId(), "admin");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        String contenido = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(contenido).contains("3.50");
        DocumentoGenerado audit = documentoRepository.findByOrderByFechaCreacionDesc().stream()
                .filter(d -> d.getTipo() == TipoDocumento.MANIFIESTO_CARGA
                        && d.getReferenciaId().equals(String.valueOf(lote.getId())))
                .findFirst().orElseThrow();
        assertThat(audit.getPesoBytes()).isEqualTo(pdf.length);
        documentoIds.add(audit.getId());
    }
}
```

- [ ] **Step 2: Ejecutar el test de integración (requiere DB/Redis)**

Run (contenedor):
`docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn test "-Dtest=DocumentosIntegrationTest" -q`
Expected: PASS (4 tests, 0 failures).

- [ ] **Step 3: Ejecutar la suite completa (BUILD SUCCESS)**

Run (contenedor):
`docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean test`
Expected: `BUILD SUCCESS`, `Tests run: 122` base + 34 nuevos ≈ **156 tests, 0 failures, 0 errors**.

- [ ] **Step 4: Actualizar `docs/handoff.md`**

Añadir una entrada del Bloque 12 tras la del Bloque 11: motor PDF con OpenPDF/ZXing, migraciones V6/V7, vínculo envíos↔lote, endpoints `/api/v1/admin/documentos`, streaming con tope `app.pdf.max-pages`, auditoría en `documentos_generados`, tests (desglose) y suite total (≈156). Actualizar el bloque «Estado Git Actual» (HEAD y pendientes).

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/DocumentosIntegrationTest.java docs/handoff.md
git commit -m "test(pdf): add end-to-end document generation integration tests and update handoff"
```

---

## Self-Review

- **Spec coverage:** V6 exacta (Task 1), V7 vínculo (Task 1+2), `documentos_generados` entidad/repo (Task 2), memoria pura (diseño `byte[]`/`OutputStream`, Task 5/6/7), Code128+QR (Task 4+5), streaming lote con tope (Task 7+8), manifiesto A4 con totales/firma (Task 6+8), auditoría + endpoint de consulta (Task 2+7+8), propiedades `app.pdf.*` (Task 7), endpoints ROLE_ADMIN (Task 8), suite integración (Task 9). Sin huecos.
- **Placeholder scan:** todos los pasos incluyen código o comandos concretos; sin TBD.
- **Type consistency:** `generar`, `anadirEtiqueta`, `tamanoPagina`, `PesoUtil.parsear`, `DocumentoGenerado` (constructor 5 args), `findByBatchIdOrderByCodigoUnicoAsc`, `countByBatchId`, `listarEmisiones`, `generarEtiquetasLote(batchId, usuario, OutputStream)` — firmas idénticas entre Tasks y en `DocumentosController`.
