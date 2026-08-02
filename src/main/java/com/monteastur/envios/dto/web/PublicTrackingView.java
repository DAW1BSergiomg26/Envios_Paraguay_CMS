package com.monteastur.envios.dto.web;

import com.monteastur.envios.model.EnvioTracking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vista plana (cacheable en Redis) del portal público de rastreo.
 * No expone referencias a entidades JPA.
 */
public class PublicTrackingView {

    public static final List<String> PASOS_CANONICOS = List.of(
            "RECIBIDO", "EN_ADUANA_ORIGEN", "EN_TRANSITO",
            "EN_ADUANA_DESTINO", "EN_REPARTO", "ENTREGADO");

    private String codigoUnico;
    private String estado;
    private String destinatario;
    private String origen;
    private String destino;
    private String peso;
    private String contenido;
    private String observaciones;
    private String ubicacionActual;
    private LocalDateTime fechaCreacion;
    private LocalDateTime ultimaActualizacion;
    private String clienteNombre;
    private Long batchId;
    private int pasoActual = -1;
    private List<String> pasos = new ArrayList<>(PASOS_CANONICOS);
    private List<EventoView> eventos = new ArrayList<>();
    private List<EvidenciaView> evidencias = new ArrayList<>();
    private EntregaView entrega;

    public PublicTrackingView() {}

    public PublicTrackingView(String codigoUnico, String estado, String destinatario,
                              String origen, String destino, String peso, String contenido,
                              String observaciones, String ubicacionActual,
                              LocalDateTime fechaCreacion, LocalDateTime ultimaActualizacion,
                              String clienteNombre, Long batchId, int pasoActual,
                              List<String> pasos, List<EventoView> eventos,
                              List<EvidenciaView> evidencias, EntregaView entrega) {
        this.codigoUnico = codigoUnico;
        this.estado = estado;
        this.destinatario = destinatario;
        this.origen = origen;
        this.destino = destino;
        this.peso = peso;
        this.contenido = contenido;
        this.observaciones = observaciones;
        this.ubicacionActual = ubicacionActual;
        this.fechaCreacion = fechaCreacion;
        this.ultimaActualizacion = ultimaActualizacion;
        this.clienteNombre = clienteNombre;
        this.batchId = batchId;
        this.pasoActual = pasoActual;
        this.pasos = pasos != null ? pasos : new ArrayList<>(PASOS_CANONICOS);
        this.eventos = eventos != null ? eventos : new ArrayList<>();
        this.evidencias = evidencias != null ? evidencias : new ArrayList<>();
        this.entrega = entrega;
    }

    public static PublicTrackingView from(EnvioTracking envio, List<EventoView> eventos,
                                          List<EvidenciaView> evidencias, EntregaView entrega) {
        String clienteNombre = envio.getCliente() != null ? envio.getCliente().getNombre() : null;
        return new PublicTrackingView(
                envio.getCodigoUnico(), envio.getEstado(), envio.getDestinatario(),
                envio.getOrigen(), envio.getDestino(), envio.getPeso(), envio.getContenido(),
                envio.getObservaciones(), envio.getUbicacionActual(),
                envio.getFechaCreacion(), envio.getUltimaActualizacion(),
                clienteNombre, envio.getBatchId(),
                PASOS_CANONICOS.indexOf(envio.getEstado()),
                new ArrayList<>(PASOS_CANONICOS), eventos, evidencias, entrega);
    }

    // Getters y setters explícitos de todos los campos (necesarios para la deserialización JSON en Redis)
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
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public String getUbicacionActual() { return ubicacionActual; }
    public void setUbicacionActual(String ubicacionActual) { this.ubicacionActual = ubicacionActual; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
    public void setUltimaActualizacion(LocalDateTime ultimaActualizacion) { this.ultimaActualizacion = ultimaActualizacion; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public int getPasoActual() { return pasoActual; }
    public void setPasoActual(int pasoActual) { this.pasoActual = pasoActual; }
    public List<String> getPasos() { return pasos; }
    public void setPasos(List<String> pasos) { this.pasos = pasos; }
    public List<EventoView> getEventos() { return eventos; }
    public void setEventos(List<EventoView> eventos) { this.eventos = eventos; }
    public List<EvidenciaView> getEvidencias() { return evidencias; }
    public void setEvidencias(List<EvidenciaView> evidencias) { this.evidencias = evidencias; }
    public EntregaView getEntrega() { return entrega; }
    public void setEntrega(EntregaView entrega) { this.entrega = entrega; }
}
