package com.monteastur.envios;

import com.monteastur.envios.exception.BadRequestException;
import com.monteastur.envios.service.UploadService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UploadServiceTest {

    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

    @org.junit.jupiter.api.io.TempDir
    private Path tmp;

    private UploadService service() {
        return new UploadService(tmp.toString());
    }

    @Test
    void subirArchivo_escribeArchivoYDevuelveRutaRelativa() throws IOException {
        UploadService service = service();
        MultipartFile foto = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "img".getBytes());

        String relPath = service.subirArchivo(foto, "imagenes");

        assertThat(relPath).startsWith("imagenes/").endsWith(".jpg");
        String nombre = relPath.substring("imagenes/".length(), relPath.length() - ".jpg".length());
        assertThat(UUID_PATTERN.matcher(nombre)).matches();

        Path file = tmp.resolve(relPath);
        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.readAllBytes(file)).isEqualTo("img".getBytes());
    }

    @Test
    void subirArchivo_raiz_cuandoSubDirVacio() throws IOException {
        UploadService service = service();
        MultipartFile foto = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "img".getBytes());

        String relPath = service.subirArchivo(foto, "");

        assertThat(relPath).endsWith(".jpg");
        assertThat(relPath).doesNotContain("/");
        String nombre = relPath.substring(0, relPath.length() - ".jpg".length());
        assertThat(UUID_PATTERN.matcher(nombre)).matches();
    }

    @Test
    void subirArchivo_extensionInvalida_lanzaBadRequest() {
        UploadService service = service();
        MultipartFile exe = new MockMultipartFile("archivo", "foto.exe", "application/x-msdownload", "img".getBytes());

        assertThatThrownBy(() -> service.subirArchivo(exe, "imagenes"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void subirArchivo_archivoVacio_lanzaBadRequest() {
        UploadService service = service();
        MultipartFile vacio = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> service.subirArchivo(vacio, "imagenes"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void subirArchivo_customAllowlist_aceptaPdf() throws IOException {
        UploadService service = service();
        MultipartFile pdf = new MockMultipartFile("archivo", "doc.pdf", "application/pdf", "pdf".getBytes());

        String relPath = service.subirArchivo(pdf, "evidencias", "jpg", "pdf");

        assertThat(relPath).startsWith("evidencias/").endsWith(".pdf");
        assertThat(Files.exists(tmp.resolve(relPath))).isTrue();
    }

    @Test
    void subirArchivo_customAllowlist_rechazaSvg() {
        UploadService service = service();
        MultipartFile svg = new MockMultipartFile("archivo", "img.svg", "image/svg+xml", "svg".getBytes());

        assertThatThrownBy(() -> service.subirArchivo(svg, "evidencias", "jpg", "pdf"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void eliminarArchivo_borraYIdempotente() throws IOException {
        UploadService service = service();
        MultipartFile foto = new MockMultipartFile("archivo", "foto.jpg", "image/jpeg", "img".getBytes());
        String relPath = service.subirArchivo(foto, "imagenes");

        assertThat(Files.exists(tmp.resolve(relPath))).isTrue();

        service.eliminarArchivo(relPath);
        assertThat(Files.exists(tmp.resolve(relPath))).isFalse();

        service.eliminarArchivo(relPath);
        assertThat(Files.exists(tmp.resolve(relPath))).isFalse();
    }
}
