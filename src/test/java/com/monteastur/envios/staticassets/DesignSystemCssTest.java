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
        assertThat(css).contains("--tri-rojo: #C8102E");
        assertThat(css).contains("--tri-blanco: #FFFFFF");
        assertThat(css).contains("--tri-azul: #0047AB");
        assertThat(css).contains("--monte-amarillo: #E67E22");
    }

    @Test
    void designSystem_definesLogoTricolorClass() throws IOException {
        String css = css();
        assertThat(css).contains(".logo-tricolor");
        assertThat(css).contains(".tri-rojo");
        assertThat(css).contains(".tri-blanco");
        assertThat(css).contains(".tri-azul");
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

    @Test
    void designSystem_modalHonorsHiddenAttribute() throws IOException {
        String css = css();
        assertThat(css).contains(".modal[hidden]");
    }

    @Test
    void designSystem_modalHiddenHidesModal() throws IOException {
        String css = css();
        int idx = css.indexOf(".modal[hidden]");
        assertThat(idx).isGreaterThanOrEqualTo(0);
        int end = css.indexOf("}", idx);
        assertThat(css.substring(idx, end)).contains("display: none");
    }

    @Test
    void h8_definesTrackingSelectors() throws IOException {
        String css = css();
        assertThat(css).contains(".status-badge");
        assertThat(css).contains(".tracking-result-page");
        assertThat(css).contains(".tracking-step");
        assertThat(css).contains(".tracking-event");
        assertThat(css).contains(".tracking-404-card");
        assertThat(css).contains(".tracking-form-glass");
        assertThat(css).contains(".tracking-help-card");
        assertThat(css).contains(".tracking-pod");
        assertThat(css).contains(".tracking-evidencia");
    }
}
