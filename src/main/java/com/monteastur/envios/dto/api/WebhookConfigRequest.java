package com.monteastur.envios.dto.api;

public class WebhookConfigRequest {

    private Long clienteId;

    private String url;

    private String secretToken;

    private Boolean activo;

    public WebhookConfigRequest() {}

    public WebhookConfigRequest(Long clienteId, String url, String secretToken, Boolean activo) {
        this.clienteId = clienteId;
        this.url = url;
        this.secretToken = secretToken;
        this.activo = activo;
    }

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSecretToken() { return secretToken; }
    public void setSecretToken(String secretToken) { this.secretToken = secretToken; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
