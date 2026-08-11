package com.monteastur.envios.dto.api;

public class ActualizarWebhookRequest {

    private String url;

    private String secretToken;

    private Boolean activo;

    public ActualizarWebhookRequest() {}

    public ActualizarWebhookRequest(String url, String secretToken, Boolean activo) {
        this.url = url;
        this.secretToken = secretToken;
        this.activo = activo;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSecretToken() { return secretToken; }
    public void setSecretToken(String secretToken) { this.secretToken = secretToken; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
