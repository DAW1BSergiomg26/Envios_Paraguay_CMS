# Bloque 13 — Evidencia Digital de Entrega (POD) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Registrar la entrega final de paquetes vía API REST (`POST/GET /api/v1/deliveries/{codigo}/pod`) con firma digital PNG, GPS validado, actualización atómica a `ENTREGADO` y propagación de eventos corporativos.

**Architecture:** Un `EntregaEvidenciaService` transaccional valida y persiste la evidencia, luego invoca `EnvioTrackingService.actualizarEstado(codigo, "ENTREGADO")` dentro de la misma transacción (reutiliza el pipeline existente de webhooks HMAC + notificaciones + evict de caché) y crea el evento del timeline. La API queda bajo `/api/v1/deliveries/**` protegida con autenticación HTTP Basic + roles ADMIN/OPERADOR.

**Tech Stack:** Spring Boot 3.3.5, Java 17, Spring Data JPA, Flyway V8, MySQL 8, Redis, AssertJ, Awaitility, Mockito.

## Global Constraints

- **Prohibido Lombok:** entidad `EntregaEvidencia` y DTOs en Java puro (constructor vacío para JPA, constructor parametrizado, getters/setters explícitos).
- **Inyección por constructor:** campos de dependencias `private final`.
- **Migración Flyway V8 exacta:** `src/main/resources/db/migration/V8__create_proof_of_delivery_tables.sql` (SQL del usuario, tabla `entregas_evidencia`).
- **Validación PNG:** `EntregaValidator` verifica decodificación Base64 y magic bytes `0x89, 0x50, 0x4E, 0x47`.
- **Pipeline de eventos:** `registrarEntrega` invoca `actualizarEstado(codigo, "ENTREGADO")` dentro de la misma transacción. No se modifica `EstadoEnvioActualizadoEvent` ni `WebhookPayloadBuilder`.
- **Coordenadas:** latitud `[-90,90]`, longitud `[-180,180]` (opcionales, validadas si presentes).
- **POD duplicado → 409** (`ConflictException`).
- **Comandos de verificación local** (sin DB/Redis para tests unitarios y `@WebMvcTest`):
  `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=<TestClass>" -q`
- **Comandos de verificación en contenedor** (requiere MySQL/Redis en red `envios_paraguay_cms_backend`):
  `docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn test "-Dtest=<TestClass>" -q`

---

### Task 1: Migración V8 + Entidad `EntregaEvidencia` + Repositorio

**Files:**
- Create: `src/main/resources/db/migration/V8__create_proof_of_delivery_tables.sql`
- Create: `src/main/java/com/monteastur/envios/model/EntregaEvidencia.java`
- Create: `src/main/java/com/monteastur/envios/repository/EntregaEvidenciaRepository.java`

**Interfaces:**
- Produces: entidad `EntregaEvidencia` con constructor `EntregaEvidencia(EnvioTracking envio, String receptorNombre, String receptorDocumento, String firmaBase64, Double latitud, Double longitud, String notas)`, getters/setters, `@PrePersist` para `fechaEntrega`; repositorio `EntregaEvidenciaRepository extends JpaRepository<EntregaEvidencia, Long>` con `Optional<EntregaEvidencia> findByEnvioId(Long envioId)` y `boolean existsByEnvioId(Long envioId)`. Lo consume `EntregaEvidenciaService` (Task 4) y `EntregaEvidenciaIntegrationTest` (Task 7).

- [x] **Step 1: Crear la migración**

`src/main/resources/db/migration/V8__create_proof_of_delivery_tables.sql`:

```sql
CREATE TABLE entregas_evidencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    envio_id BIGINT NOT NULL UNIQUE,
    receptor_nombre VARCHAR(150) NOT NULL,
    receptor_documento VARCHAR(50) NOT NULL,
    firma_base64 LONGTEXT NOT NULL,
    latitud DECIMAL(10, 8) NULL,
    longitud DECIMAL(11, 8) NULL,
    notas TEXT NULL,
    fecha_entrega DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_entregas_evidencia_envio FOREIGN KEY (envio_id) REFERENCES envios_tracking(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [x] **Step 2: Crear la entidad**

`src/main/java/com/monteastur/envios/model/EntregaEvidencia.java`:

```java
package com.monteastur.envios.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregas_evidencia")
public class EntregaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_id", nullable = false, unique = true)
    private EnvioTracking envio;

    @Column(name = "receptor_nombre", nullable = false, length = 150)
    private String receptorNombre;

    @Column(name = "receptor_documento", nullable = false, length = 50)
    private String receptorDocumento;

    @Column(name = "firma_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String firmaBase64;

    @Column(precision = 10, scale = 8)
    private Double latitud;

    @Column(precision = 11, scale = 8)
    private Double longitud;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    public EntregaEvidencia() {}

    public EntregaEvidencia(EnvioTracking envio, String receptorNombre, String receptorDocumento,
                            String firmaBase64, Double latitud, Double longitud, String notas) {
        this.envio = envio;
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
    }

    @PrePersist
    void asignarFechaEntrega() {
        if (fechaEntrega == null) {
            fechaEntrega = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EnvioTracking getEnvio() { return envio; }
    public void setEnvio(EnvioTracking envio) { this.envio = envio; }
    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public void setReceptorDocumento(String receptorDocumento) { this.receptorDocumento = receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}
```

- [x] **Step 3: Crear el repositorio**

`src/main/java/com/monteastur/envios/repository/EntregaEvidenciaRepository.java`:

```java
package com.monteastur.envios.repository;

import com.monteastur.envios.model.EntregaEvidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntregaEvidenciaRepository extends JpaRepository<EntregaEvidencia, Long> {
    Optional<EntregaEvidencia> findByEnvioId(Long envioId);
    boolean existsByEnvioId(Long envioId);
}
```

- [x] **Step 4: Compilar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd compile -q`
Expected: `BUILD SUCCESS` (el mapeo JPA se valida en la integración, Task 7).

- [x] **Step 5: Commit**

```bash
git add src/main/resources/db/migration/V8__create_proof_of_delivery_tables.sql src/main/java/com/monteastur/envios/model/EntregaEvidencia.java src/main/java/com/monteastur/envios/repository/EntregaEvidenciaRepository.java
git commit -m "feat(pod): add V8 migration and EntregaEvidencia entity/repository"
```

---

### Task 2: `EntregaValidator` (validación estricta)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/EntregaValidator.java`
- Test: `src/test/java/com/monteastur/envios/service/EntregaValidatorTest.java`

**Interfaces:**
- Consumes: `RegistrarEntregaRequest` (Task 3) — por eso Task 2 depende de Task 3; para mantener la TDD, en Task 2 el validador expone métodos por parámetro y el plan crea primero los DTOs.
- Produces: `public static void validar(RegistrarEntregaRequest request)`, `public static void validarFirmaBase64(String firmaBase64)`, `public static void validarCoordenadas(Double latitud, Double longitud)`. Lanza `BadRequestException` (400). Lo consume `EntregaEvidenciaService` (Task 4).

> **Nota de orden:** primero crea los DTOs de la Task 3 (son requeridos por la firma del validador), luego el validador y su test, y commitea ambos junto con la Task 3.

- [x] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/EntregaValidatorTest.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntregaValidatorTest {

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private RegistrarEntregaRequest requestValido() {
        RegistrarEntregaRequest req = new RegistrarEntregaRequest();
        req.setReceptorNombre("Ana López");
        req.setReceptorDocumento("12345678");
        req.setFirmaBase64(PNG_1X1);
        req.setLatitud(-25.2637421);
        req.setLongitud(-57.575926);
        return req;
    }

    @Test
    void requestValido_noLanzaExcepcion() {
        assertThatCode(() -> EntregaValidator.validar(requestValido()))
                .doesNotThrowAnyException();
    }

    @Test
    void receptorNombreVacio_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setReceptorNombre("  ");
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("receptor");
    }

    @Test
    void receptorDocumentoNulo_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setReceptorDocumento(null);
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("documento");
    }

    @Test
    void firmaNula_lanza400() {
        RegistrarEntregaRequest req = requestValido();
        req.setFirmaBase64(null);
        assertThatThrownBy(() -> EntregaValidator.validar(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("firma");
    }

    @Test
    void firmaNoBase64_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarFirmaBase64("###no-es-base64###"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void firmaBase64NoPng_lanza400() {
        String base64 = java.util.Base64.getEncoder().encodeToString("hola".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThatThrownBy(() -> EntregaValidator.validarFirmaBase64(base64))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PNG");
    }

    @Test
    void firmaPngValida_noLanza() {
        assertThatCode(() -> EntregaValidator.validarFirmaBase64(PNG_1X1))
                .doesNotThrowAnyException();
    }

    @Test
    void latitudFueraDeRango_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarCoordenadas(90.5, 0.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Latitud");
    }

    @Test
    void longitudFueraDeRango_lanza400() {
        assertThatThrownBy(() -> EntregaValidator.validarCoordenadas(0.0, 181.0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Longitud");
    }

    @Test
    void bordesValidos_noLanzan() {
        assertThatCode(() -> EntregaValidator.validarCoordenadas(-90.0, 180.0)).doesNotThrowAnyException();
        assertThatCode(() -> EntregaValidator.validarCoordenadas(90.0, -180.0)).doesNotThrowAnyException();
    }

    @Test
    void coordenadasNulas_noLanzan() {
        assertThatCode(() -> EntregaValidator.validarCoordenadas(null, null)).doesNotThrowAnyException();
    }
}
```

- [x] **Step 2: Ejecutar para verlo fallar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaValidatorTest" -q`
Expected: FAIL — `EntregaValidator` no existe (compilación de la clase de test con `RegistrarEntregaRequest` también pendiente).

- [x] **Step 3: Crear los DTOs (Task 3 adelantada)**

`src/main/java/com/monteastur/envios/dto/api/RegistrarEntregaRequest.java`:

```java
package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitud de registro de evidencia de entrega (POD)")
public class RegistrarEntregaRequest {

    @Schema(description = "Nombre completo del receptor", example = "Ana López")
    private String receptorNombre;

    @Schema(description = "Documento del receptor (DNI/CI)", example = "12345678")
    private String receptorDocumento;

    @Schema(description = "Firma manuscrita codificada en Base64 (PNG)")
    private String firmaBase64;

    @Schema(description = "Latitud de la entrega (opcional)", example = "-25.2637421")
    private Double latitud;

    @Schema(description = "Longitud de la entrega (opcional)", example = "-57.575926")
    private Double longitud;

    @Schema(description = "Notas o incidencias de entrega (opcional)")
    private String notas;

    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public void setReceptorDocumento(String receptorDocumento) { this.receptorDocumento = receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
}
```

`src/main/java/com/monteastur/envios/dto/api/EntregaEvidenciaDto.java`:

```java
package com.monteastur.envios.dto.api;

import com.monteastur.envios.model.EntregaEvidencia;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evidencia digital de entrega (POD)")
public class EntregaEvidenciaDto {

    private final Long id;
    private final String codigoRastreo;
    private final String receptorNombre;
    private final String receptorDocumento;
    private final String firmaBase64;
    private final Double latitud;
    private final Double longitud;
    private final String notas;
    private final String fechaEntrega;

    private EntregaEvidenciaDto(Long id, String codigoRastreo, String receptorNombre,
                                String receptorDocumento, String firmaBase64, Double latitud,
                                Double longitud, String notas, String fechaEntrega) {
        this.id = id;
        this.codigoRastreo = codigoRastreo;
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
        this.fechaEntrega = fechaEntrega;
    }

    public static EntregaEvidenciaDto from(EntregaEvidencia evidencia) {
        return new EntregaEvidenciaDto(evidencia.getId(),
                evidencia.getEnvio().getCodigoUnico(),
                evidencia.getReceptorNombre(),
                evidencia.getReceptorDocumento(),
                evidencia.getFirmaBase64(),
                evidencia.getLatitud(),
                evidencia.getLongitud(),
                evidencia.getNotas(),
                evidencia.getFechaEntrega() != null ? evidencia.getFechaEntrega().toString() : null);
    }

    public Long getId() { return id; }
    public String getCodigoRastreo() { return codigoRastreo; }
    public String getReceptorNombre() { return receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public String getNotas() { return notas; }
    public String getFechaEntrega() { return fechaEntrega; }
}
```

- [x] **Step 4: Implementar el validador**

`src/main/java/com/monteastur/envios/service/EntregaValidator.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;

import java.util.Base64;

public final class EntregaValidator {

    private static final int MAX_NOMBRE_LENGTH = 150;
    private static final int MAX_DOCUMENTO_LENGTH = 50;
    private static final int MAX_FIRMA_BASE64_LENGTH = 5_242_880;
    private static final int MAX_NOTAS_LENGTH = 2000;
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};

    private EntregaValidator() {}

    public static void validar(RegistrarEntregaRequest request) {
        validarReceptorNombre(request.getReceptorNombre());
        validarReceptorDocumento(request.getReceptorDocumento());
        validarFirmaBase64(request.getFirmaBase64());
        validarCoordenadas(request.getLatitud(), request.getLongitud());
        validarNotas(request.getNotas());
    }

    public static void validarFirmaBase64(String firmaBase64) {
        if (firmaBase64 == null || firmaBase64.isBlank()) {
            throw new BadRequestException("La firma es obligatoria");
        }
        if (firmaBase64.length() > MAX_FIRMA_BASE64_LENGTH) {
            throw new BadRequestException("La firma supera el tamaño máximo permitido");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(firmaBase64);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("La firma no es un Base64 válido");
        }
        if (bytes.length < PNG_MAGIC.length
                || bytes[0] != PNG_MAGIC[0] || bytes[1] != PNG_MAGIC[1]
                || bytes[2] != PNG_MAGIC[2] || bytes[3] != PNG_MAGIC[3]) {
            throw new BadRequestException("La firma debe ser una imagen PNG");
        }
    }

    public static void validarCoordenadas(Double latitud, Double longitud) {
        if (latitud != null && (latitud < -90.0 || latitud > 90.0)) {
            throw new BadRequestException("Latitud fuera de rango: debe estar entre -90 y 90");
        }
        if (longitud != null && (longitud < -180.0 || longitud > 180.0)) {
            throw new BadRequestException("Longitud fuera de rango: debe estar entre -180 y 180");
        }
    }

    private static void validarReceptorNombre(String receptorNombre) {
        if (receptorNombre == null || receptorNombre.isBlank()) {
            throw new BadRequestException("El nombre del receptor es obligatorio");
        }
        if (receptorNombre.length() > MAX_NOMBRE_LENGTH) {
            throw new BadRequestException("El nombre del receptor no puede superar 150 caracteres");
        }
    }

    private static void validarReceptorDocumento(String receptorDocumento) {
        if (receptorDocumento == null || receptorDocumento.isBlank()) {
            throw new BadRequestException("El documento del receptor es obligatorio");
        }
        if (receptorDocumento.length() > MAX_DOCUMENTO_LENGTH) {
            throw new BadRequestException("El documento del receptor no puede superar 50 caracteres");
        }
    }

    private static void validarNotas(String notas) {
        if (notas != null && notas.length() > MAX_NOTAS_LENGTH) {
            throw new BadRequestException("Las notas no pueden superar 2000 caracteres");
        }
    }
}
```

- [x] **Step 5: Ejecutar para verlo pasar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaValidatorTest" -q`
Expected: PASS (11 tests, 0 failures).

- [x] **Step 6: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/EntregaValidator.java src/test/java/com/monteastur/envios/service/EntregaValidatorTest.java src/main/java/com/monteastur/envios/dto/api/RegistrarEntregaRequest.java src/main/java/com/monteastur/envios/dto/api/EntregaEvidenciaDto.java
git commit -m "feat(pod): add strict firma/GPS validation and POD DTOs"
```

---

### Task 3: `EntregaEvidenciaService` (orquestación transaccional)

**Files:**
- Create: `src/main/java/com/monteastur/envios/service/EntregaEvidenciaService.java`
- Test: `src/test/java/com/monteastur/envios/service/EntregaEvidenciaServiceTest.java`

**Interfaces:**
- Consumes: `EntregaEvidenciaRepository` (Task 1), `EnvioTrackingRepository.findWithClienteByCodigoUnico` (existente), `EnvioTrackingService.actualizarEstado(String codigo, String nuevoEstado)` (existente), `EventoTrackingService.crearEvento(EnvioTracking envio, String estadoAnterior)` (existente), `EntregaValidator` (Task 2), DTOs (Task 2).
- Produces: `@Transactional public EntregaEvidencia registrarEntrega(String codigo, RegistrarEntregaRequest request)`, `@Transactional(readOnly = true) public EntregaEvidenciaDto obtenerEntrega(String codigo)`. Lo consume `EntregaEvidenciaController` (Task 5).

- [x] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/service/EntregaEvidenciaServiceTest.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.EventoTracking;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregaEvidenciaServiceTest {

    @Mock private EntregaEvidenciaRepository entregaRepository;
    @Mock private EnvioTrackingRepository envioTrackingRepository;
    @Mock private EnvioTrackingService envioTrackingService;
    @Mock private EventoTrackingService eventoTrackingService;

    private EntregaEvidenciaService service;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @BeforeEach
    void setUp() {
        service = new EntregaEvidenciaService(entregaRepository, envioTrackingRepository,
                envioTrackingService, eventoTrackingService);
    }

    private RegistrarEntregaRequest requestValido() {
        RegistrarEntregaRequest req = new RegistrarEntregaRequest();
        req.setReceptorNombre("Ana López");
        req.setReceptorDocumento("12345678");
        req.setFirmaBase64(PNG_1X1);
        req.setLatitud(-25.2637421);
        req.setLongitud(-57.575926);
        return req;
    }

    private EnvioTracking envioRecibido(Long id) {
        EnvioTracking envio = new EnvioTracking("MT-1", "RECIBIDO", "Receptor",
                "Origen", "Destino", "10 kg", "Documentos");
        envio.setId(id);
        return envio;
    }

    @Test
    void registrarEntrega_actualizaEstadoYGuarda() {
        EnvioTracking envio = envioRecibido(1L);
        EnvioTracking entregado = envioRecibido(1L);
        entregado.setEstado("ENTREGADO");
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.existsByEnvioId(1L)).thenReturn(false);
        when(entregaRepository.save(any(EntregaEvidencia.class)))
                .thenAnswer(inv -> { EntregaEvidencia e = inv.getArgument(0); e.setId(99L); return e; });
        when(envioTrackingService.actualizarEstado("MT-1", "ENTREGADO")).thenReturn(entregado);
        when(eventoTrackingService.crearEvento(entregado, "RECIBIDO"))
                .thenReturn(Optional.of(new EventoTracking()));

        EntregaEvidencia resultado = service.registrarEntrega("MT-1", requestValido());

        assertThat(resultado.getId()).isEqualTo(99L);
        assertThat(resultado.getReceptorNombre()).isEqualTo("Ana López");
        verify(envioTrackingService).actualizarEstado("MT-1", "ENTREGADO");
        verify(eventoTrackingService).crearEvento(entregado, "RECIBIDO");
        verify(entregaRepository).save(any(EntregaEvidencia.class));
    }

    @Test
    void registrarEntrega_envioInexistente_lanza404() {
        when(envioTrackingRepository.findWithClienteByCodigoUnico(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarEntrega("MT-NOPE", requestValido()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(entregaRepository, never()).save(any(EntregaEvidencia.class));
    }

    @Test
    void registrarEntrega_yaExistePod_lanza409() {
        EnvioTracking envio = envioRecibido(1L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.existsByEnvioId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarEntrega("MT-1", requestValido()))
                .isInstanceOf(ConflictException.class);
        verify(entregaRepository, never()).save(any(EntregaEvidencia.class));
        verify(envioTrackingService, never()).actualizarEstado(anyString(), anyString());
    }

    @Test
    void registrarEntrega_validacionFallida_lanza400() {
        RegistrarEntregaRequest request = requestValido();
        request.setFirmaBase64(null);

        assertThatThrownBy(() -> service.registrarEntrega("MT-1", request))
                .isInstanceOf(BadRequestException.class);
        verify(envioTrackingRepository, never()).findWithClienteByCodigoUnico(anyString());
    }

    @Test
    void obtenerEntrega_retornaDto() {
        EnvioTracking envio = envioRecibido(1L);
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, "Ana López", "12345678",
                PNG_1X1, -25.2637421, -57.575926, "Recibido en mano");
        evidencia.setId(7L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.findByEnvioId(1L)).thenReturn(Optional.of(evidencia));

        EntregaEvidenciaDto dto = service.obtenerEntrega("MT-1");

        assertThat(dto.getCodigoRastreo()).isEqualTo("MT-1");
        assertThat(dto.getReceptorNombre()).isEqualTo("Ana López");
        assertThat(dto.getFirmaBase64()).isEqualTo(PNG_1X1);
        assertThat(dto.getLatitud()).isEqualTo(-25.2637421);
    }

    @Test
    void obtenerEntrega_sinEvidencia_lanza404() {
        EnvioTracking envio = envioRecibido(1L);
        when(envioTrackingRepository.findWithClienteByCodigoUnico("MT-1")).thenReturn(Optional.of(envio));
        when(entregaRepository.findByEnvioId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerEntrega("MT-1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

- [x] **Step 2: Ejecutar para verlo fallar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaEvidenciaServiceTest" -q`
Expected: FAIL — `EntregaEvidenciaService` no existe.

- [x] **Step 3: Implementar el servicio**

`src/main/java/com/monteastur/envios/service/EntregaEvidenciaService.java`:

```java
package com.monteastur.envios.service;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EntregaEvidenciaService {

    private final EntregaEvidenciaRepository entregaRepository;
    private final EnvioTrackingRepository envioTrackingRepository;
    private final EnvioTrackingService envioTrackingService;
    private final EventoTrackingService eventoTrackingService;

    public EntregaEvidenciaService(EntregaEvidenciaRepository entregaRepository,
                                   EnvioTrackingRepository envioTrackingRepository,
                                   EnvioTrackingService envioTrackingService,
                                   EventoTrackingService eventoTrackingService) {
        this.entregaRepository = entregaRepository;
        this.envioTrackingRepository = envioTrackingRepository;
        this.envioTrackingService = envioTrackingService;
        this.eventoTrackingService = eventoTrackingService;
    }

    @Transactional
    @CacheEvict(value = "envios.dashboard", allEntries = true)
    public EntregaEvidencia registrarEntrega(String codigo, RegistrarEntregaRequest request) {
        EntregaValidator.validar(request);
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        if (entregaRepository.existsByEnvioId(envio.getId())) {
            throw new ConflictException("El envío " + codigo + " ya tiene evidencia de entrega registrada");
        }
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, request.getReceptorNombre(),
                request.getReceptorDocumento(), request.getFirmaBase64(),
                request.getLatitud(), request.getLongitud(), request.getNotas());
        EntregaEvidencia guardada = entregaRepository.save(evidencia);
        String estadoAnterior = envio.getEstado();
        EnvioTracking actualizado = envioTrackingService.actualizarEstado(codigo, "ENTREGADO");
        eventoTrackingService.crearEvento(actualizado, estadoAnterior);
        return guardada;
    }

    @Transactional(readOnly = true)
    public EntregaEvidenciaDto obtenerEntrega(String codigo) {
        EnvioTracking envio = envioTrackingRepository.findWithClienteByCodigoUnico(codigo.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Tracking no encontrado: " + codigo));
        EntregaEvidencia evidencia = entregaRepository.findByEnvioId(envio.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe evidencia de entrega para el envío: " + codigo));
        return EntregaEvidenciaDto.from(evidencia);
    }
}
```

- [x] **Step 4: Ejecutar para verlo pasar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaEvidenciaServiceTest" -q`
Expected: PASS (6 tests, 0 failures).

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/service/EntregaEvidenciaService.java src/test/java/com/monteastur/envios/service/EntregaEvidenciaServiceTest.java
git commit -m "feat(pod): add transactional delivery registration service"
```

---

### Task 4: Seguridad — HTTP Basic + `/api/v1/deliveries/**` autenticado

**Files:**
- Modify: `src/main/java/com/monteastur/envios/config/SecurityConfig.java`
- Test: `src/test/java/com/monteastur/envios/config/SecurityConfigTest.java` (añadir método)

**Interfaces:**
- Consumes: nada nuevo. Produce: `SecurityFilterChain` que protege `/api/v1/deliveries/**` con `authenticated()` y habilita HTTP Basic para clientes REST. Lo ejercita `EntregaEvidenciaControllerTest` (Task 5).

- [x] **Step 1: Escribir el test que falla**

Añadir a `src/test/java/com/monteastur/envios/config/SecurityConfigTest.java` (dentro de la clase, junto a los tests existentes) el método:

```java
    @Test
    void deliveriesSinAuth_devuelve401Json() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod")
                .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
```

- [x] **Step 2: Ejecutar para verlo fallar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=SecurityConfigTest" -q`
Expected: FAIL — `deliveriesSinAuth_devuelve401Json` no pasa (hoy devuelve 200/404 porque es `permitAll`).

- [x] **Step 3: Modificar `SecurityConfig`**

En `src/main/java/com/monteastur/envios/config/SecurityConfig.java`:
1. Añadir import: `import org.springframework.security.config.Customizer;`
2. En el matcher de `authorizeHttpRequests`, añadir `/api/v1/deliveries/**` a la línea 27:

```java
                .requestMatchers("/admin/**", "/api/v1/admin/**", "/api/v1/deliveries/**").authenticated()
```

3. Añadir la cadena `.httpBasic(Customizer.withDefaults())` justo después del bloque `.formLogin(...)` (línea 38):

```java
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/admin/dashboard")
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults())
```

- [x] **Step 4: Ejecutar para verlo pasar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=SecurityConfigTest" -q`
Expected: PASS (4 tests, 0 failures).

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/config/SecurityConfig.java src/test/java/com/monteastur/envios/config/SecurityConfigTest.java
git commit -m "feat(security): protect deliveries API with HTTP Basic and authenticated()"
```

---

### Task 5: `EntregaEvidenciaController` REST + tests web

**Files:**
- Create: `src/main/java/com/monteastur/envios/controller/api/EntregaEvidenciaController.java`
- Test: `src/test/java/com/monteastur/envios/controller/api/EntregaEvidenciaControllerTest.java`

**Interfaces:**
- Consumes: `EntregaEvidenciaService` (Task 3): `registrarEntrega(String, RegistrarEntregaRequest)` y `obtenerEntrega(String)`; DTOs (Task 2).
- Produces: endpoints `POST /api/v1/deliveries/{codigo}/pod` (201) y `GET /api/v1/deliveries/{codigo}/pod` (200) con `@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_OPERADOR')")`. Lo ejercita `EntregaEvidenciaIntegrationTest` (Task 6).

- [x] **Step 1: Escribir el test que falla**

`src/test/java/com/monteastur/envios/controller/api/EntregaEvidenciaControllerTest.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.config.RBACAccessLogger;
import com.monteastur.envios.config.SecurityConfig;
import com.monteastur.envios.controller.GlobalExceptionHandler;
import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.security.CustomAccessDeniedHandler;
import com.monteastur.envios.service.EntregaEvidenciaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EntregaEvidenciaController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class, CustomAccessDeniedHandler.class})
@WithMockUser(username = "operador", roles = "OPERADOR")
class EntregaEvidenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntregaEvidenciaService entregaEvidenciaService;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private RBACAccessLogger rbacAccessLogger;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    private EntregaEvidencia evidenciaValida() {
        EnvioTracking envio = new EnvioTracking("MT-1", "ENTREGADO", "Receptor",
                "Origen", "Destino", "10 kg", "Documentos");
        EntregaEvidencia evidencia = new EntregaEvidencia(envio, "Ana López", "12345678",
                PNG_1X1, -25.2637421, -57.575926, null);
        evidencia.setId(5L);
        return evidencia;
    }

    @Test
    void registrarPod_retorna201ConDto() throws Exception {
        when(entregaEvidenciaService.registrarEntrega("MT-1", any()))
                .thenReturn(evidenciaValida());

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana López\",\"receptorDocumento\":\"12345678\","
                                + "\"firmaBase64\":\"" + PNG_1X1 + "\",\"latitud\":-25.2637421,\"longitud\":-57.575926}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoRastreo").value("MT-1"))
                .andExpect(jsonPath("$.receptorNombre").value("Ana López"))
                .andExpect(jsonPath("$.receptorDocumento").value("12345678"))
                .andExpect(jsonPath("$.firmaBase64").value(PNG_1X1))
                .andExpect(jsonPath("$.latitud").value(-25.2637421));
    }

    @Test
    void registrarPod_validacionFallida_retorna400() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new BadRequestException("La firma debe ser una imagen PNG"));

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void registrarPod_envioInexistente_retorna404() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new ResourceNotFoundException("Tracking no encontrado: MT-NOPE"));

        mockMvc.perform(post("/api/v1/deliveries/MT-NOPE/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void registrarPod_podExistente_retorna409() throws Exception {
        when(entregaEvidenciaService.registrarEntrega(anyString(), any()))
                .thenThrow(new ConflictException("El envío MT-1 ya tiene evidencia de entrega registrada"));

        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void obtenerPod_retorna200ConDto() throws Exception {
        EntregaEvidenciaDto dto = EntregaEvidenciaDto.from(evidenciaValida());
        when(entregaEvidenciaService.obtenerEntrega("MT-1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoRastreo").value("MT-1"))
                .andExpect(jsonPath("$.receptorNombre").value("Ana López"))
                .andExpect(jsonPath("$.firmaBase64").value(PNG_1X1));
    }

    @Test
    void obtenerPod_sinEvidencia_retorna404() throws Exception {
        when(entregaEvidenciaService.obtenerEntrega(anyString()))
                .thenThrow(new ResourceNotFoundException("No existe evidencia de entrega para el envío: MT-1"));

        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithAnonymousUser
    void sinAutenticacion_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/deliveries/MT-1/pod")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cliente", roles = "CLIENTE")
    void rolCliente_denegado() throws Exception {
        mockMvc.perform(post("/api/v1/deliveries/MT-1/pod")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receptorNombre\":\"Ana\",\"receptorDocumento\":\"1\",\"firmaBase64\":\"AAAA\"}"))
                .andExpect(status().isBadRequest());
    }
}
```

- [x] **Step 2: Ejecutar para verlo fallar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaEvidenciaControllerTest" -q`
Expected: FAIL — el controller no existe.

- [x] **Step 3: Implementar el controller**

`src/main/java/com/monteastur/envios/controller/api/EntregaEvidenciaController.java`:

```java
package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.dto.api.RegistrarEntregaRequest;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.service.EntregaEvidenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/deliveries")
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_OPERADOR')")
public class EntregaEvidenciaController {

    private final EntregaEvidenciaService entregaEvidenciaService;

    public EntregaEvidenciaController(EntregaEvidenciaService entregaEvidenciaService) {
        this.entregaEvidenciaService = entregaEvidenciaService;
    }

    @PostMapping("/{codigo}/pod")
    public ResponseEntity<EntregaEvidenciaDto> registrarPod(@PathVariable String codigo,
                                                            @RequestBody RegistrarEntregaRequest request) {
        EntregaEvidencia evidencia = entregaEvidenciaService.registrarEntrega(codigo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(EntregaEvidenciaDto.from(evidencia));
    }

    @GetMapping("/{codigo}/pod")
    public ResponseEntity<EntregaEvidenciaDto> obtenerPod(@PathVariable String codigo) {
        return ResponseEntity.ok(entregaEvidenciaService.obtenerEntrega(codigo));
    }
}
```

- [x] **Step 4: Ejecutar para verlo pasar**

Run: `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd test "-Dtest=EntregaEvidenciaControllerTest" -q`
Expected: PASS (8 tests, 0 failures).

- [x] **Step 5: Commit**

```bash
git add src/main/java/com/monteastur/envios/controller/api/EntregaEvidenciaController.java src/test/java/com/monteastur/envios/controller/api/EntregaEvidenciaControllerTest.java
git commit -m "feat(pod): add delivery evidence REST controller"
```

---

### Task 6: Integración end-to-end + suite completa + handoff

**Files:**
- Create: `src/test/java/com/monteastur/envios/integration/EntregaEvidenciaIntegrationTest.java`
- Modify: `docs/handoff.md`

**Interfaces:**
- Consumes: beans reales `EntregaEvidenciaService`, repos, `EnvioTrackingService`, `EventoTrackingRepository`, `NotificacionRepository`, `TransactionTemplate`; `EmailService` mockeado.

- [x] **Step 1: Escribir el test de integración**

`src/test/java/com/monteastur/envios/integration/EntregaEvidenciaIntegrationTest.java`:

```java
package com.monteastur.envios.integration;

import com.monteastur.envios.dto.api.EntregaEvidenciaDto;
import com.monteastur.envios.exception.ConflictException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.EntregaEvidencia;
import com.monteastur.envios.model.EnvioTracking;
import com.monteastur.envios.model.Notificacion;
import com.monteastur.envios.repository.EntregaEvidenciaRepository;
import com.monteastur.envios.repository.EnvioTrackingRepository;
import com.monteastur.envios.repository.EventoTrackingRepository;
import com.monteastur.envios.repository.NotificacionRepository;
import com.monteastur.envios.service.EmailService;
import com.monteastur.envios.service.EnvioTrackingService;
import com.monteastur.envios.service.EntregaEvidenciaService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class EntregaEvidenciaIntegrationTest {

    @Autowired private EntregaEvidenciaService entregaEvidenciaService;
    @Autowired private EntregaEvidenciaRepository entregaEvidenciaRepository;
    @Autowired private EnvioTrackingRepository envioTrackingRepository;
    @Autowired private EventoTrackingRepository eventoTrackingRepository;
    @Autowired private NotificacionRepository notificacionRepository;
    @Autowired private EnvioTrackingService envioTrackingService;
    @Autowired private TransactionTemplate transactionTemplate;

    @MockBean private EmailService emailService;

    private Long envioId;

    private static final String PNG_1X1 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==";

    @AfterEach
    void limpiar() {
        if (envioId != null) {
            entregaEvidenciaRepository.deleteAll(entregaEvidenciaRepository.findByEnvioId(envioId).stream().toList());
            eventoTrackingRepository.deleteAll(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envioId));
            notificacionRepository.deleteAll(notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envioId));
            envioTrackingRepository.deleteById(envioId);
        }
        envioId = null;
    }

    private EnvioTracking crearEnvioSinCliente(String codigo) {
        EnvioTracking guardado = transactionTemplate.execute(status -> {
            EnvioTracking envio = envioTrackingService.guardar(new EnvioTracking(codigo, "RECIBIDO",
                    "Destinatario Test", "Madrid, España", "Asunción, Paraguay", "10 kg", "Documentos"));
            return envio;
        });
        envioId = guardado.getId();
        return guardado;
    }

    @Test
    void registrarPod_marcaEntregadoPersisteYPropagaEventos() {
        String codigo = "PY-POD-" + System.nanoTime();
        EnvioTracking envio = crearEnvioSinCliente(codigo);

        com.monteastur.envios.dto.api.RegistrarEntregaRequest request =
                new com.monteastur.envios.dto.api.RegistrarEntregaRequest();
        request.setReceptorNombre("Ana López");
        request.setReceptorDocumento("12345678");
        request.setFirmaBase64(PNG_1X1);
        request.setLatitud(-25.2637421);
        request.setLongitud(-57.575926);

        EntregaEvidencia evidencia = entregaEvidenciaService.registrarEntrega(codigo, request);

        assertThat(evidencia.getId()).isNotNull();
        EnvioTracking recargado = envioTrackingRepository.findById(envio.getId()).orElseThrow();
        assertThat(recargado.getEstado()).isEqualTo("ENTREGADO");

        assertThat(entregaEvidenciaRepository.findByEnvioId(envio.getId())).isPresent();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<Notificacion> notificaciones =
                    notificacionRepository.findByEnvioIdOrderByFechaCreacionDesc(envio.getId());
            assertThat(notificaciones).hasSize(1);
            assertThat(notificaciones.get(0).getEstado())
                    .isEqualTo(Notificacion.EstadoNotificacion.OMITIDO_SIN_DESTINATARIO);
        });

        assertThat(eventoTrackingRepository.findByEnvioTrackingIdOrderByFechaEventoDesc(envio.getId()))
                .anyMatch(e -> "ENTREGADO".equals(e.getEstado()));

        EntregaEvidenciaDto dto = entregaEvidenciaService.obtenerEntrega(codigo);
        assertThat(dto.getFirmaBase64()).isEqualTo(PNG_1X1);
        assertThat(dto.getCodigoRastreo()).isEqualTo(codigo);
    }

    @Test
    void registroDuplicado_lanza409() {
        String codigo = "PY-POD-" + System.nanoTime();
        crearEnvioSinCliente(codigo);

        com.monteastur.envios.dto.api.RegistrarEntregaRequest request =
                new com.monteastur.envios.dto.api.RegistrarEntregaRequest();
        request.setReceptorNombre("Ana López");
        request.setReceptorDocumento("12345678");
        request.setFirmaBase64(PNG_1X1);

        entregaEvidenciaService.registrarEntrega(codigo, request);

        assertThatThrownBy(() -> entregaEvidenciaService.registrarEntrega(codigo, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void obtenerSinEvidencia_lanza404() {
        String codigo = "PY-POD-" + System.nanoTime();
        crearEnvioSinCliente(codigo);

        assertThatThrownBy(() -> entregaEvidenciaService.obtenerEntrega(codigo))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

> **Nota:** el método `crearEnvioSinCliente` devuelve el envío guardado; si `transactionTemplate.execute` devuelve null se lanza `NullPointerException`. Ese caso no ocurre en estos tests porque `guardar` siempre retorna la entidad persistida.

- [x] **Step 2: Ejecutar el test de integración (requiere DB/Redis)**

Run (contenedor):
`docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn test "-Dtest=EntregaEvidenciaIntegrationTest" -q`
Expected: PASS (3 tests, 0 failures).

- [x] **Step 3: Ejecutar la suite completa (BUILD SUCCESS)**

Run (contenedor):
`docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-17 mvn clean test`
Expected: `BUILD SUCCESS`, `Tests run: 159` base + 28 nuevos ≈ **187 tests, 0 failures, 0 errors**.

- [x] **Step 4: Actualizar `docs/handoff.md`**

Añadir una entrada del Bloque 13 tras la del Bloque 12: migración V8 (`entregas_evidencia`, `envio_id UNIQUE`, FK `ON DELETE CASCADE`), entidad/repositorio `EntregaEvidencia`, `EntregaValidator` (Base64+PNG `\x89PNG`, rangos GPS), `EntregaEvidenciaService` transaccional (POD → `actualizarEstado("ENTREGADO")` → eventos webhook/notificación async + `@CacheEvict` dashboard), endpoints `/api/v1/deliveries/{codigo}/pod` (POST 201 / GET 200) con `@PreAuthorize` ADMIN/OPERADOR, `SecurityConfig` con HTTP Basic, 409 duplicado, 404 inexistentes. Actualizar el bloque «Estado Git Actual» (HEAD y working tree limpio) y la suite total (≈187).

- [x] **Step 5: Commit**

```bash
git add src/test/java/com/monteastur/envios/integration/EntregaEvidenciaIntegrationTest.java docs/handoff.md
git commit -m "test(pod): add end-to-end POD integration tests and update handoff"
```

- [x] **Step 6: Verificar working tree limpio**

Run: `git status`
Expected: `nothing to commit, working tree clean`.

---

## Self-Review

- **Spec coverage:** migración V8 exacta (Task 1); entidad/repositorio (Task 1); validador Base64+PNG + rangos GPS + campos obligatorios (Task 2); DTOs (Task 2); servicio transaccional con `actualizarEstado` + `crearEvento` + `@CacheEvict` dashboard (Task 3); seguridad HTTP Basic + `authenticated()` en `/api/v1/deliveries/**` + `@PreAuthorize` ADMIN/OPERADOR (Tasks 4 y 5); endpoints POST 201 / GET 200 (Task 5); 409 duplicado, 404 envío/POD inexistentes (Tasks 3 y 5); propagación de eventos verificada en integración vía `Notificacion` + `EventoTracking` (Task 6); handoff + suite completa (Task 6). Sin huecos.
- **Placeholder scan:** todos los pasos incluyen código o comandos concretos; sin TBD/TODO.
- **Type consistency:** firmas idénticas entre tasks — `EntregaEvidencia(EnvioTracking, String, String, String, Double, Double, String)`, `findByEnvioId`/`existsByEnvioId`, `EntregaValidator.validar(RegistrarEntregaRequest)`, `registrarEntrega(String, RegistrarEntregaRequest)`, `obtenerEntrega(String)`, `EntregaEvidenciaDto.from(EntregaEvidencia)`, `EntregaEvidenciaController` en `/api/v1/deliveries`.
