package com.monteastur.envios.service;

import com.monteastur.envios.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private static final List<String> EXT_PERMITIDAS = List.of("jpg", "jpeg", "png", "webp", "gif", "svg");

    private final Path uploadDir;

    public UploadService(@Value("${app.upload.dir}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir);
    }

    public String subirArchivo(MultipartFile archivo, String subDir) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new BadRequestException("Debes seleccionar un archivo.");
        }

        String originalName = archivo.getOriginalFilename();
        String ext = extraerExtension(originalName);
        if (!EXT_PERMITIDAS.contains(ext)) {
            throw new BadRequestException("Extensión de archivo no permitida.");
        }

        String dir = subDir == null ? "" : subDir.trim();
        String relPath = (dir.isBlank() ? "" : dir + "/") + UUID.randomUUID() + "." + ext;

        Path target = uploadDir.resolve(relPath);
        Files.createDirectories(target.getParent());
        Files.write(target, archivo.getBytes());

        return relPath;
    }

    private String extraerExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    public void eliminarArchivo(String pathRelativo) {
        try {
            Path target = uploadDir.resolve(pathRelativo).normalize();
            Files.deleteIfExists(target);
        } catch (IOException | IllegalArgumentException e) {
            // idempotent: ignore missing/unexpected failures
        }
    }
}
