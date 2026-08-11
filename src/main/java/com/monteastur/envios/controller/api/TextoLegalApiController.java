package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ActualizarTextoRequest;
import com.monteastur.envios.dto.api.TextoLegalDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.TextoLegal;
import com.monteastur.envios.repository.TextoLegalRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/textos")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin Textos Legales")
public class TextoLegalApiController {

    private final TextoLegalRepository textoRepo;

    public TextoLegalApiController(TextoLegalRepository textoRepo) {
        this.textoRepo = textoRepo;
    }

    @GetMapping
    public List<TextoLegalDto> listar() {
        return textoRepo.findAll().stream()
                .map(t -> toDto(t, false))
                .toList();
    }

    @GetMapping("/{slug}")
    public TextoLegalDto porSlug(@PathVariable String slug) {
        TextoLegal texto = textoRepo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Texto legal no encontrado: " + slug));
        return toDto(texto, true);
    }

    @PutMapping("/{slug}")
    public TextoLegalDto actualizar(@PathVariable String slug,
                                    @RequestBody ActualizarTextoRequest request) {
        if (request == null || request.getTitulo() == null || request.getTitulo().isBlank()
                || request.getContenido() == null || request.getContenido().isBlank()) {
            throw new BadRequestException("Título y contenido son requeridos.");
        }

        TextoLegal texto = textoRepo.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Texto legal no encontrado: " + slug));

        texto.setTitulo(request.getTitulo());
        texto.setContenido(request.getContenido());
        texto.setUpdatedAt(LocalDateTime.now());

        return toDto(textoRepo.save(texto), true);
    }

    private TextoLegalDto toDto(TextoLegal t, boolean incluirContenido) {
        TextoLegalDto dto = new TextoLegalDto();
        dto.setId(t.getId());
        dto.setSlug(t.getSlug());
        dto.setTitulo(t.getTitulo());
        dto.setContenido(incluirContenido ? t.getContenido() : null);
        dto.setUpdatedAt(t.getUpdatedAt());
        return dto;
    }
}
