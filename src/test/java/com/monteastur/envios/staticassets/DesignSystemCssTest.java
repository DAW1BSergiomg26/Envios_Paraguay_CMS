package com.monteastur.envios.staticassets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DesignSystemCssTest {

    private String css() throws IOException {
        return new String(new ClassPathResource("static/css/design-system.css")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void designSystem_definesCanonicalBrandTokens() throws IOException {
        String css = css();
        assertThat(css).contains("--en-rojo: #C8102E");
        assertThat(css).contains("--vi-blanco: #FFFFFF");
        assertThat(css).contains("--os-azul: #0047AB");
        assertThat(css).contains("--monte-amarillo: #E67E22");
    }

    @Test
    void designSystem_definesAccessibleButtonTextToken() throws IOException {
        String css = css();
        assertThat(css).contains("--btn-text: #0F281E");
    }

    @Test
    void designSystem_definesUnifiedCardRadius() throws IOException {
        String css = css();
        assertThat(css).contains("--radius-card: 16px");
    }
}
