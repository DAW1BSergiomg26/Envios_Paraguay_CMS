package com.monteastur.envios.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignatureTest {

    @Test
    void vectorRfc4231_case2() {
        String firma = WebhookSignature.hmacSha256("Jefe", "what do ya want for nothing?");
        assertThat(firma)
                .isEqualTo("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843");
    }

    @Test
    void esHexLowercaseDeLongitud64() {
        String firma = WebhookSignature.hmacSha256("secret", "{}");
        assertThat(firma).hasSize(64);
        assertThat(firma).matches("[0-9a-f]{64}");
    }

    @Test
    void firmaDependeDelSecretYDelBody() {
        String base = WebhookSignature.hmacSha256("secret", "body");
        assertThat(WebhookSignature.hmacSha256("otro-secret", "body")).isNotEqualTo(base);
        assertThat(WebhookSignature.hmacSha256("secret", "otro-body")).isNotEqualTo(base);
    }
}
