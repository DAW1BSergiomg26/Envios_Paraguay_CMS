package com.monteastur.envios.dto.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Detalle completo de un envío con eventos y evidencias")
public class TrackingDto {
    @Schema(description = "Código único de seguimiento", example = "MT-2026-0001")
    private String codigoUnico;

    @Schema(description = "Estado actual del envío", example = "EN_TRANSITO",
        allowableValues = {"RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO", "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO"})
    private String estado;

    @Schema(description = "Nombre del destinatario", example = "María González")
    private String destinatario;

    @Schema(description = "Ciudad o país de origen", example = "Asturias, España")
    private String origen;

    @Schema(description = "Ciudad o país de destino", example = "Asunción, Paraguay")
    private String destino;
    private String peso;
    private String contenido;
    private LocalDateTime ultimaActualizacion;
    private List<EventoDto> eventos;
    private List<EvidenciaDto> evidencias;
    private String clienteNombre;
    private String clienteEmail;

    // Getters y Setters
    public String getCodigoUnico() { return codigoUnico; }
    public void setCodigoUnico(String codigoUnico) { this.codigoUnico = codigoUnico; }
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
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public List<EventoDto> getEventos() { return eventos; }
    public void setEventos(List<EventoDto> eventos) { this.eventos = eventos; }
    public List<EvidenciaDto> getEvidencias() { return evidencias; }
    public void setEvidencias(List<EvidenciaDto> evidencias) { this.evidencias = evidencias; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteEmail() { return clienteEmail; }
    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }
}