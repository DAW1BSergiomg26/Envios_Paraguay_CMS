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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateAssetIntegrityTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates").toAbsolutePath();
    private static final Pattern CSS_REF = Pattern.compile("(?:th:href=\"@\\{)?/css/([a-zA-Z0-9\\-]+(?:\\.css))(?=[(?\"])");

    private static final Set<String> ALLOWED_CSS = Set.of("design-system.css");

    private List<String> cssRefs(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        List<String> refs = new ArrayList<>();
        Matcher m = CSS_REF.matcher(content);
        while (m.find()) {
            refs.add(m.group(1));
        }
        return refs;
    }

    private Stream<Path> templates() throws IOException {
        return Files.walk(TEMPLATES).filter(p -> p.toString().endsWith(".html"));
    }

    @Test
    void allTemplatesReferenceOnlyDesignSystemCss() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = templates()) {
            for (Path p : paths.toList()) {
                for (String css : cssRefs(p)) {
                    if (!ALLOWED_CSS.contains(css)) {
                        offenders.add(p.getFileName() + " -> " + css);
                    }
                }
            }
        }
        assertThat(offenders)
                .as("Plantillas que referencian CSS legacy (fuera del whitelist)")
                .isEmpty();
    }

    @Test
    void referencedCssFilesExistInStaticFolder() throws Exception {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> paths = templates()) {
            for (Path p : paths.toList()) {
                for (String css : cssRefs(p)) {
                    Path target = Paths.get("src/main/resources/static/css", css);
                    if (!Files.exists(target)) {
                        missing.add(css + " (desde " + p.getFileName() + ")");
                    }
                }
            }
        }
        assertThat(missing).as("CSS referenciados que no existen en static/css").isEmpty();
    }

    @Test
    void noLegacyStubTemplatesExist() throws Exception {
        for (String stub : List.of("contact.html", "error-404.html", "index.html", "admin-layout.html", "header.html")) {
            Path p = TEMPLATES.resolve(stub);
            assertThat(Files.exists(p))
                    .as("Plantilla stub legacy %s debe eliminarse", stub)
                    .isFalse();
        }
    }

    @Test
    void publicHeadFragmentIsRemoved() throws Exception {
        Path p = TEMPLATES.resolve("fragments/public-head.html");
        assertThat(Files.exists(p)).as("fragments/public-head.html debe eliminarse").isFalse();
    }
}
