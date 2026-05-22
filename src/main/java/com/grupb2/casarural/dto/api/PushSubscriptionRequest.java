package com.grupb2.casarural.dto.api;

public class PushSubscriptionRequest {
    private String endpoint;
    private Object keys;

    public PushSubscriptionRequest() {}

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public Object getKeys() { return keys; }
    public void setKeys(Object keys) { this.keys = keys; }
}
