package com.monteastur.envios.controller.api;

import com.monteastur.envios.dto.api.ActualizarOrdenImagenRequest;
import com.monteastur.envios.dto.api.ImagenDto;
import com.monteastur.envios.exception.ResourceNotFoundException;
import com.monteastur.envios.model.Imagen;
import com.monteastur.envios.repository.ImagenRepository;
import com.monteastur.envios.service.UploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/imagenes")
@PreAuthorize("hasRole('ROLE_ADMIN')")
@Tag(name = "Admin Galeria imagenes")
public class ImagenApiController {

    private final ImagenRepository imagenRepo;
    private final UploadService uploadService;

    public ImagenApiController(ImagenRepository imagenRepo, UploadService uploadService) {
        this.imagenRepo = imagenRepo;
        this.uploadService = uploadService;
    }

    @GetMapping
    public List<ImagenDto> listar() {
        return imagenRepo.findAllByOrderByOrdenAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImagenDto subir(
            @RequestParam String titulo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String categoria,
            @RequestParam Integer orden,
            @RequestPart("archivo") MultipartFile archivo) throws IOException {
        String relPath = uploadService.subirArchivo(archivo, "");
        Imagen imagen = new Imagen(titulo, descripcion, "/uploads/" + relPath, categoria, orden);
        return toDto(imagenRepo.save(imagen));
    }

    @PatchMapping("/{id}/orden")
    public ImagenDto cambiarOrden(@PathVariable Long id,
                                  @RequestBody ActualizarOrdenImagenRequest request) {
        Imagen imagen = imagenRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Imagen " + id));
        imagen.setOrden(request.getOrden());
        return toDto(imagenRepo.save(imagen));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        imagenRepo.findById(id).ifPresent(imagen -> {
            uploadService.eliminarArchivo(imagen.getUrl().replaceFirst("^/uploads/", ""));
            imagenRepo.delete(imagen);
        });
    }

    private ImagenDto toDto(Imagen i) {
        ImagenDto dto = new ImagenDto();
        dto.setId(i.getId());
        dto.setTitulo(i.getTitulo());
        dto.setDescripcion(i.getDescripcion());
        dto.setUrl(i.getUrl());
        dto.setCategoria(i.getCategoria());
        dto.setOrden(i.getOrden());
        dto.setCreatedAt(i.getCreatedAt());
        return dto;
    }
}
