package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ErrorDto;
import com.monteastur.envios.dto.api.PushSubscriptionRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Tag(name = "Push Notifications", description = "Suscripción y prueba de notificaciones push para la PWA (no requiere autenticación)")
@RestController
@RequestMapping("/api/v1/push")
public class PushSubscriptionController {

    private final Map<String, Object> subscriptions = new ConcurrentHashMap<>();

    private final boolean pushTestEnabled;

    public PushSubscriptionController(@Value("${app.push.test-enabled:true}") boolean pushTestEnabled) {
        this.pushTestEnabled = pushTestEnabled;
    }

    @Operation(summary = "Suscribir a notificaciones push", description = "Registra un endpoint de suscripción push para recibir notificaciones")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suscripción registrada")
    })
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody PushSubscriptionRequest req) {
        System.out.println("Push subscribed: " + req.getEndpoint());
        subscriptions.put(req.getEndpoint(), req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Desuscribir de notificaciones push", description = "Elimina un endpoint de suscripción push")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suscripción eliminada")
    })
    @PostMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribe(@RequestBody PushSubscriptionRequest req) {
        System.out.println("Push unsubscribed: " + req.getEndpoint());
        subscriptions.remove(req.getEndpoint());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Probar notificaciones push", description = "Simula el envío de una notificación a todos los dispositivos suscritos (controlado por app.push.test-enabled)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Simulación ejecutada"),
        @ApiResponse(responseCode = "403", description = "Endpoint deshabilitado (app.push.test-enabled=false)",
            content = @Content(schema = @Schema(implementation = com.monteastur.envios.dto.api.ErrorDto.class)))
    })
    @PostMapping("/test")
    public ResponseEntity<?> testPush() {
        if (!pushTestEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorDto(Instant.now().toString(), 403, "Push test endpoint disabled"));
        }
        System.out.println("Simulating push notification for " + subscriptions.size() + " subscribers");
        // In a real PWA/Push server, we would send the payload here.
        // Simulated response for demo purposes.
        return ResponseEntity.ok(Map.of("message", "Simulando envío a " + subscriptions.size() + " dispositivos"));
    }
}
