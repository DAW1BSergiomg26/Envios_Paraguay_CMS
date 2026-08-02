package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.DocumentoGeneradoDto;
import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.exception.ResourceNotFoundException;
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
        try {
            documentoPdfService.generarEtiquetasLote(batchId, authentication.getName(), response.getOutputStream());
        } catch (BadRequestException | ResourceNotFoundException ex) {
            response.reset();
            throw ex;
        }
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
