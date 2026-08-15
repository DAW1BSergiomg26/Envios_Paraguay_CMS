package com.monteastur.envios.staticassets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class StaticAssetSelfHostingTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates").toAbsolutePath();
    private static final Path CSS = Paths.get("src/main/resources/static/css").toAbsolutePath();
    private static final Path VENDOR = Paths.get("src/main/resources/static/js/vendor").toAbsolutePath();
    private static final Path FONTS = Paths.get("src/main/resources/static/fonts").toAbsolutePath();

    private static final Set<String> BLOCKED_EXTERNAL_HOSTS = Set.of(
            "https://fonts.googleapis.com",
            "https://fonts.gstatic.com",
            "https://unpkg.com",
            "https://cdn.jsdelivr.net",
            "https://cdnjs.cloudflare.com"
    );

    private Stream<Path> htmlTemplates() throws IOException {
        return Files.walk(TEMPLATES).filter(p -> p.toString().endsWith(".html"));
    }

    private Stream<Path> cssFiles() throws IOException {
        if (!Files.exists(CSS)) {
            return Stream.empty();
        }
        return Files.walk(CSS).filter(p -> p.toString().endsWith(".css"));
    }

    private String content(Path p) throws IOException {
        return Files.readString(p, StandardCharsets.UTF_8);
    }

    @Test
    void templatesDoNotReferenceExternalCdnHosts() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = htmlTemplates()) {
            for (Path p : paths.toList()) {
                String content = content(p);
                for (String host : BLOCKED_EXTERNAL_HOSTS) {
                    if (content.contains(host)) {
                        offenders.add(p.getFileName() + " -> " + host);
                    }
                }
            }
        }
        assertThat(offenders)
                .as("Plantillas que referencian hosts externos de CDN (self-hosting requerido)")
                .isEmpty();
    }

    @Test
    void cssDoesNotReferenceExternalFontHosts() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = cssFiles()) {
            for (Path p : paths.toList()) {
                String content = content(p);
                for (String host : BLOCKED_EXTERNAL_HOSTS) {
                    if (content.contains(host)) {
                        offenders.add(p.getFileName() + " -> " + host);
                    }
                }
            }
        }
        assertThat(offenders)
                .as("CSS que referencian hosts externos de CDN/fuentes (self-hosting requerido)")
                .isEmpty();
    }

    @Test
    void fontsFolderContainsPlusJakartaSansSubsets() {
        assertThat(Files.exists(FONTS))
                .as("Directorio static/fonts debe existir")
                .isTrue();
        for (String subset : List.of("latin", "latin-ext", "cyrillic-ext", "vietnamese")) {
            Path f = FONTS.resolve("plus-jakarta-sans-" + subset + ".woff2");
            assertThat(Files.exists(f))
                    .as("Fuente self-hosted %s debe existir", f.getFileName())
                    .isTrue();
            assertThat(f.toFile().length())
                    .as("Fuente %s no debe estar vacía", f.getFileName())
                    .isGreaterThan(0L);
        }
    }

    @Test
    void html5QrcodeIsSelfHostedInVendorFolder() {
        Path f = VENDOR.resolve("html5-qrcode.min.js");
        assertThat(Files.exists(f))
                .as("html5-qrcode debe estar self-hosted en static/js/vendor")
                .isTrue();
        assertThat(f.toFile().length())
                .as("html5-qrcode.min.js no debe estar vacío")
                .isGreaterThan(0L);
    }

    @Test
    void cssUsesLocalFontFace() throws Exception {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> paths = cssFiles()) {
            for (Path p : paths.toList()) {
                String content = content(p);
                if (content.contains("'Plus Jakarta Sans'") || content.contains("\"Plus Jakarta Sans\"")) {
                    if (!content.contains("@font-face")) {
                        missing.add(p.getFileName().toString());
                    }
                }
            }
        }
        assertThat(missing)
                .as("CSS que usan Plus Jakarta Sans sin @font-face local")
                .isEmpty();
    }
}
