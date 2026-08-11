package com.monteastur.envios.dto.api;

public class ActualizarEnvioRequest {

    private String estado;
    private String destinatario;
    private String origen;
    private String destino;
    private String peso;
    private String contenido;
    private String observaciones;
    private Long clienteId;

    public ActualizarEnvioRequest() {}

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getPeso() { return peso; }
    public void setPeso(String peso) { this.peso = peso; }
    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
}
