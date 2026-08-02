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
