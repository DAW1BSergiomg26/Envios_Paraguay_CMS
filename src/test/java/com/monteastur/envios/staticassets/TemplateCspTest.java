package com.monteastur.envios.staticassets;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Garantiza que las plantillas HTML sean compatibles con la CSP estricta
 * (script-src 'self'): cero scripts inline y cero manejadores de eventos inline.
 */
class TemplateCspTest {

    private static final Path TEMPLATES = Paths.get("src/main/resources/templates").toAbsolutePath();
    private static final Path STATIC_JS = Paths.get("src/main/resources/static/js").toAbsolutePath();

    private static final Pattern INLINE_SCRIPT =
            Pattern.compile("(?i)<script\\b(?![^>]*\\bsrc\\s*=)[^>]*>");

    private static final Pattern INLINE_EVENT_HANDLER =
            Pattern.compile("(?i)\\son(click|load|error|change|input|submit|keydown|keyup|mouseover|mouseout|focus|blur)\\s*=");

    private static final Pattern SCRIPT_SRC =
            Pattern.compile("(?i)src=\"(/js/[^\"]+)\"");

    private Stream<Path> htmlTemplates() throws IOException {
        return Files.walk(TEMPLATES).filter(p -> p.toString().endsWith(".html"));
    }

    @Test
    void templatesDoNotContainInlineScripts() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = htmlTemplates()) {
            for (Path p : paths.toList()) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                Matcher matcher = INLINE_SCRIPT.matcher(content);
                while (matcher.find()) {
                    offenders.add(p.getFileName() + " -> " + matcher.group().trim());
                }
            }
        }
        assertThat(offenders)
                .as("Plantillas con <script> inline (la CSP script-src 'self' los bloquea)")
                .isEmpty();
    }

    @Test
    void templatesDoNotContainInlineEventHandlers() throws Exception {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = htmlTemplates()) {
            for (Path p : paths.toList()) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                Matcher matcher = INLINE_EVENT_HANDLER.matcher(content);
                while (matcher.find()) {
                    offenders.add(p.getFileName() + " -> " + matcher.group().trim());
                }
            }
        }
        assertThat(offenders)
                .as("Plantillas con atributos on* inline (la CSP bloquea los manejadores inline)")
                .isEmpty();
    }

    @Test
    void scriptSrcReferencesPointToExistingJsFiles() throws Exception {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> paths = htmlTemplates()) {
            for (Path p : paths.toList()) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                Matcher matcher = SCRIPT_SRC.matcher(content);
                while (matcher.find()) {
                    String ref = matcher.group(1);
                    Path js = STATIC_JS.resolve(ref.substring("/js/".length()));
                    if (!Files.exists(js)) {
                        missing.add(p.getFileName() + " -> " + ref);
                    }
                }
            }
        }
        assertThat(missing)
                .as("Referencias a /js/ sin archivo físico en static/js")
                .isEmpty();
    }
}
