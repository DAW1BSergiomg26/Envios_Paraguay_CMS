package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ActualizarWebhookRequest;
import com.monteastur.envios.dto.api.WebhookConfigDto;
import com.monteastur.envios.dto.api.WebhookConfigRequest;
import com.monteastur.envios.dto.api.WebhookLogDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.WebhookConfig;
import com.monteastur.envios.model.WebhookLog;
import com.monteastur.envios.repository.ClienteRepository;
import com.monteastur.envios.repository.WebhookConfigRepository;
import com.monteastur.envios.repository.WebhookLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Webhooks Admin", description = "Gestión de configuraciones de webhooks outbound (requiere autenticación)")
@RestController
@RequestMapping("/api/v1/admin/webhooks")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class WebhookConfigController {

    private final WebhookConfigRepository webhookConfigRepository;
    private final ClienteRepository clienteRepository;
    private final WebhookLogRepository webhookLogRepository;

    public WebhookConfigController(WebhookConfigRepository webhookConfigRepository,
                                   ClienteRepository clienteRepository,
                                   WebhookLogRepository webhookLogRepository) {
        this.webhookConfigRepository = webhookConfigRepository;
        this.clienteRepository = clienteRepository;
        this.webhookLogRepository = webhookLogRepository;
    }

    @Operation(summary = "Listar configuraciones de webhook", description = "Devuelve todos los webhooks o los de un cliente (nunca expone el secret_token)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de webhooks",
            content = @Content(schema = @Schema(implementation = WebhookConfigDto.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping
    public ResponseEntity<List<WebhookConfigDto>> listar(@RequestParam(required = false) Long clienteId) {
        List<WebhookConfig> configs = clienteId != null
                ? webhookConfigRepository.findByClienteId(clienteId)
                : webhookConfigRepository.findAll();
        return ResponseEntity.ok(configs.stream().map(WebhookConfigDto::from).collect(Collectors.toList()));
    }

    @Operation(summary = "Crear configuración de webhook", description = "Registra una URL y secret_token para el cliente indicado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Webhook creado",
            content = @Content(schema = @Schema(implementation = WebhookConfigDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PostMapping
    public ResponseEntity<WebhookConfigDto> crear(@RequestBody WebhookConfigRequest request) {
        if (request.getClienteId() == null) {
            throw new BadRequestException("clienteId es obligatorio");
        }
        if (request.getUrl() == null || request.getUrl().isBlank()) {
            throw new BadRequestException("url es obligatoria");
        }
        validarUrl(request.getUrl());
        if (request.getSecretToken() == null || request.getSecretToken().isBlank()) {
            throw new BadRequestException("secretToken es obligatorio");
        }
        if (!clienteRepository.existsById(request.getClienteId())) {
            throw new ResourceNotFoundException("Cliente no encontrado: " + request.getClienteId());
        }
        WebhookConfig config = new WebhookConfig(request.getClienteId(), request.getUrl(), request.getSecretToken());
        if (request.getActivo() != null) {
            config.setActivo(request.getActivo());
        }
        WebhookConfig guardado = webhookConfigRepository.save(config);
        return ResponseEntity.status(HttpStatus.CREATED).body(WebhookConfigDto.from(guardado));
    }

    private void validarUrl(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("url no es válida");
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new BadRequestException("url debe usar esquema http o https");
        }
        if (uri.getHost() == null) {
            throw new BadRequestException("url no es válida");
        }
    }

    @Operation(summary = "Actualizar configuración de webhook", description = "Modifica url, secretToken y/o activo; los campos en blanco se ignoran")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook actualizado",
            content = @Content(schema = @Schema(implementation = WebhookConfigDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Webhook no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigDto> actualizar(@PathVariable Long id,
                                                       @RequestBody ActualizarWebhookRequest request) {
        WebhookConfig config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Webhook no encontrado: " + id));
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            validarUrl(request.getUrl());
            config.setUrl(request.getUrl());
        }
        if (request.getSecretToken() != null && !request.getSecretToken().isBlank()) {
            config.setSecretToken(request.getSecretToken());
        }
        if (request.getActivo() != null) {
            config.setActivo(request.getActivo());
        }
        return ResponseEntity.ok(WebhookConfigDto.from(webhookConfigRepository.save(config)));
    }

    @Operation(summary = "Historial de despachos de un webhook", description = "Devuelve los registros de despacho (sin payload), opcionalmente filtrados por éxito")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historial de despachos",
            content = @Content(schema = @Schema(implementation = WebhookLogDto.class))),
        @ApiResponse(responseCode = "404", description = "Webhook no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @GetMapping("/{id}/logs")
    public ResponseEntity<List<WebhookLogDto>> logs(@PathVariable Long id,
                                                    @RequestParam(required = false) Boolean exitoso) {
        if (!webhookConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("Webhook no encontrado: " + id);
        }
        List<WebhookLog> logs = webhookLogRepository.findByWebhookIdOrderByFechaCreacionDesc(id);
        if (exitoso != null) {
            logs = logs.stream().filter(log -> log.isExitoso() == exitoso).collect(Collectors.toList());
        }
        return ResponseEntity.ok(logs.stream().map(WebhookLogDto::from).collect(Collectors.toList()));
    }

    @Operation(summary = "Eliminar configuración de webhook", description = "Borra la configuración; los logs asociados se eliminan en cascada")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Eliminado"),
        @ApiResponse(responseCode = "404", description = "Webhook no encontrado",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!webhookConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("Webhook no encontrado: " + id);
        }
        webhookConfigRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
