package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.BatchImportErrorDto;
import com.monteastur.envios.dto.api.BatchImportResponseDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.model.BatchImport;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.service.CsvBatchImportService;
import com.monteastur.envios.service.batch.BatchImportPersistenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Tag(name = "Imports Admin", description = "Carga masiva de envíos por CSV (requiere ROLE_ADMIN)")
@RestController
@RequestMapping("/api/v1/admin/imports")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class BatchImportController {

    private final BatchImportPersistenceService persistence;
    private final CsvBatchImportService csvBatchImportService;
    private final ClienteRepository clienteRepository;
    private final boolean enabled;
    private final Path tmpDir;
    private final long maxFileSizeBytes;

    public BatchImportController(BatchImportPersistenceService persistence,
                                 CsvBatchImportService csvBatchImportService,
                                 ClienteRepository clienteRepository,
                                 @Value("${app.batch.enabled:true}") boolean enabled,
                                 @Value("${app.batch.tmp-dir:./uploads/batch-imports}") String tmpDir,
                                 @Value("${app.batch.max-file-size:5MB}") String maxFileSize) {
        this.persistence = persistence;
        this.csvBatchImportService = csvBatchImportService;
        this.clienteRepository = clienteRepository;
        this.enabled = enabled;
        this.tmpDir = Path.of(tmpDir);
        this.maxFileSizeBytes = DataSize.parse(maxFileSize).toBytes();
    }

    @Operation(summary = "Subir CSV de envíos", description = "Inicia una carga masiva asíncrona y devuelve el batch_id (202)")
    @ApiResponse(responseCode = "202", description = "Aceptado, lote en proceso")
    @ApiResponse(responseCode = "400", description = "Fichero ausente, vacío, extensión o tamaño inválidos")
    @ApiResponse(responseCode = "404", description = "clienteId inexistente")
    @PostMapping("/csv")
    public ResponseEntity<BatchImportResponseDto> importarCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long clienteId) throws IOException {
        if (!enabled) {
            throw new BadRequestException("La carga masiva está deshabilitada");
        }
        validarFichero(file);
        validarCliente(clienteId);

        Path destino = copiarTemporal(file);
        BatchImport lote = persistence.crearLote(clienteId, file.getOriginalFilename());
        csvBatchImportService.procesarLote(lote.getId(), destino.toString(), clienteId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(BatchImportResponseDto.from(lote));
    }

    @Operation(summary = "Estado del lote", description = "Progreso y contadores del lote en tiempo real")
    @ApiResponse(responseCode = "200", description = "Estado del lote")
    @ApiResponse(responseCode = "404", description = "Lote inexistente")
    @GetMapping("/{id}")
    public ResponseEntity<BatchImportResponseDto> estado(@PathVariable Long id) {
        return ResponseEntity.ok(BatchImportResponseDto.from(persistence.obtenerLote(id)));
    }

    @Operation(summary = "Errores por línea", description = "Errores de validación del lote ordenados por número de línea")
    @ApiResponse(responseCode = "200", description = "Lista de errores")
    @ApiResponse(responseCode = "404", description = "Lote inexistente")
    @GetMapping("/{id}/errors")
    public ResponseEntity<List<BatchImportErrorDto>> errores(@PathVariable Long id) {
        persistence.obtenerLote(id);
        return ResponseEntity.ok(persistence.listarErrores(id).stream()
                .map(BatchImportErrorDto::from)
                .toList());
    }

    private void validarFichero(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El fichero CSV es obligatorio y no puede estar vacío");
        }
        String nombre = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        if (!nombre.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new BadRequestException("El fichero debe tener extensión .csv");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("El fichero supera el tamaño máximo permitido");
        }
    }

    private void validarCliente(Long clienteId) {
        if (clienteId != null && !clienteRepository.existsById(clienteId)) {
            throw new com.monteastur.envios.exception.ResourceNotFoundException(
                    "Cliente no encontrado: " + clienteId);
        }
    }

    private Path copiarTemporal(MultipartFile file) throws IOException {
        Files.createDirectories(tmpDir);
        Path destino = tmpDir.resolve(System.nanoTime() + "_" + sanitizarNombre(file.getOriginalFilename()));
        try (var in = file.getInputStream()) {
            Files.copy(in, destino);
        }
        return destino;
    }

    private String sanitizarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "importe.csv";
        }
        String limpio = nombre.replaceAll("[^a-zA-Z0-9._-]", "_");
        return limpio.isBlank() ? "importe.csv" : limpio;
    }
}
