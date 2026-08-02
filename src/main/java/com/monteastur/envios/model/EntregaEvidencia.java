package com.monteastur.envios.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "entregas_evidencia")
public class EntregaEvidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "envio_id", nullable = false, unique = true)
    private EnvioTracking envio;

    @Column(name = "receptor_nombre", nullable = false, length = 150)
    private String receptorNombre;

    @Column(name = "receptor_documento", nullable = false, length = 50)
    private String receptorDocumento;

    @Column(name = "firma_base64", nullable = false, columnDefinition = "LONGTEXT")
    private String firmaBase64;

    @Column(precision = 10, scale = 8)
    private Double latitud;

    @Column(precision = 11, scale = 8)
    private Double longitud;

    @Column(columnDefinition = "TEXT")
    private String notas;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    public EntregaEvidencia() {}

    public EntregaEvidencia(EnvioTracking envio, String receptorNombre, String receptorDocumento,
                            String firmaBase64, Double latitud, Double longitud, String notas) {
        this.envio = envio;
        this.receptorNombre = receptorNombre;
        this.receptorDocumento = receptorDocumento;
        this.firmaBase64 = firmaBase64;
        this.latitud = latitud;
        this.longitud = longitud;
        this.notas = notas;
    }

    @PrePersist
    void asignarFechaEntrega() {
        if (fechaEntrega == null) {
            fechaEntrega = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EnvioTracking getEnvio() { return envio; }
    public void setEnvio(EnvioTracking envio) { this.envio = envio; }
    public String getReceptorNombre() { return receptorNombre; }
    public void setReceptorNombre(String receptorNombre) { this.receptorNombre = receptorNombre; }
    public String getReceptorDocumento() { return receptorDocumento; }
    public void setReceptorDocumento(String receptorDocumento) { this.receptorDocumento = receptorDocumento; }
    public String getFirmaBase64() { return firmaBase64; }
    public void setFirmaBase64(String firmaBase64) { this.firmaBase64 = firmaBase64; }
    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }
    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }
}
